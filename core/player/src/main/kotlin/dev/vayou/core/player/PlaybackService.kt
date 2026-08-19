package dev.vayou.core.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.ImageLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.AudioEffectType
import dev.vayou.core.model.EqPreset
import dev.vayou.core.player.cast.CastMediaItemConverter
import dev.vayou.core.player.cast.CastMediaServer
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleDelayMilliseconds
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Where the player actually lives.
 *
 * It used to live in the screen's ViewModel, which is the right home for anything that should stop
 * when the screen does — and playback is not that. A film paused to answer the door, a lecture
 * listened to with the phone in a pocket, a notification drawn while the app is closed: none of
 * them survive a player owned by a composition, however long-lived.
 *
 * A session and not just a service, because the session is what the notification, the headset
 * buttons, the lock screen, a watch and a car all talk to. Media3 draws and updates that
 * notification on its own once the session exists.
 */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playerFactory: PlayerFactory

    /** Which screen to reopen when the notification is tapped. Named by the feature that owns it,
     *  since a module this low cannot see the one with the activity in it. */
    @Inject
    lateinit var sessionActivity: SessionActivityProvider

    @Inject
    lateinit var castAvailability: CastAvailability

    @Inject
    lateinit var preferences: PreferencesRepository

    /** The same loader every cover on screen comes from -- see [CoilBitmapLoader]. */
    @Inject
    lateinit var imageLoader: ImageLoader

    private var session: MediaSession? = null

    private val audioEffects = AudioEffects()

    /**
     * The ExoPlayer underneath, kept because some things are only it.
     *
     * The session is handed a [CastPlayer], which forwards to this one until a television takes
     * over -- so `session.player as? ExoPlayer` is null even with nothing being cast, and everything
     * asked of the renderer through that cast silently did nothing: the subtitle delay, the skip
     * silence, the keyframe seeking. None of them is a thing a receiver has, so they belong here.
     */
    private var localPlayer: ExoPlayer? = null

    /** Started only once something is actually cast, so a phone that never casts never listens. */
    private var castMediaServer: CastMediaServer? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var sleepTimerJob: Job? = null
    private var sleepTimerMinutes: Int = PlaybackCommands.Off
    private var sleepTimerDeadlineMs: Long = 0L

    private val callback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .also { builder -> PlaybackCommands.All.forEach(builder::add) }
                    .build(),
            )
            .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> = when (customCommand.customAction) {
            PlaybackCommands.Close -> {
                // Emptied, not just stopped: a stopped player with a queue still describes
                // something to come back to, and the panel would draw the row again.
                session.player.stop()
                session.player.clearMediaItems()
                stopSelf()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.SetSleepTimer -> {
                setSleepTimer(args.getInt(PlaybackCommands.MinutesKey, PlaybackCommands.Off))
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.GetSleepTimer -> Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_SUCCESS,
                    Bundle().apply {
                        putInt(PlaybackCommands.MinutesKey, sleepTimerMinutes)
                        putLong(PlaybackCommands.DeadlineKey, sleepTimerDeadlineMs)
                    },
                ),
            )

            PlaybackCommands.GetEqualizerBands -> {
                val bands = audioEffects.bands()
                Futures.immediateFuture(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            // Absent rather than empty: the caller reads a missing array as "no
                            // equalizer on this device", which is a different answer from zero bands.
                            bands ?: return@apply
                            putIntArray(PlaybackCommands.CentreFreqsKey, bands.centreFreqsHz)
                            putIntArray(PlaybackCommands.LevelsKey, bands.levelsMillibels)
                            putInt(PlaybackCommands.MinLevelKey, bands.minMillibels)
                            putInt(PlaybackCommands.MaxLevelKey, bands.maxMillibels)
                        },
                    ),
                )
            }

            PlaybackCommands.SetEqualizerEnabled -> {
                audioEffects.setEnabled(args.getBoolean(PlaybackCommands.EnabledKey))
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.SetEqualizerBandLevel -> {
                audioEffects.setBandLevel(
                    band = args.getInt(PlaybackCommands.BandKey).toShort(),
                    millibels = args.getInt(PlaybackCommands.LevelKey).toShort(),
                )
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.ApplyEqualizerPreset -> {
                val preset = args.getString(PlaybackCommands.PresetKey)
                    ?.let { name -> EqPreset.entries.find { it.name == name } }
                // CUSTOM is what the bands say when they match no preset, not a curve to apply.
                if (preset != null && preset != EqPreset.CUSTOM) audioEffects.applyPreset(preset)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.GetSupportedAudioEffects -> Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_SUCCESS,
                    Bundle().apply {
                        putStringArray(
                            PlaybackCommands.EffectsKey,
                            audioEffects.supportedStrengthEffects().map { it.name }.toTypedArray(),
                        )
                    },
                ),
            )

            PlaybackCommands.SetScrubbing -> {
                val isScrubbing = args.getBoolean(PlaybackCommands.ScrubbingKey)
                localPlayer?.let { exo ->
                    exo.setSeekParameters(if (isScrubbing) SeekParameters.PREVIOUS_SYNC else SeekParameters.EXACT)
                    exo.isScrubbingModeEnabled = isScrubbing
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.SetSubtitleDelay -> {
                // Straight onto the text renderer. Only an ExoPlayer has one: a cast receiver draws
                // its own subtitles on a television this app cannot reach into.
                localPlayer?.subtitleDelayMilliseconds = args.getLong(PlaybackCommands.SubtitleDelayKey)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.SetVolumeBoost -> {
                audioEffects.setVolumeBoost(args.getInt(PlaybackCommands.BoostKey))
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.GetVolumeBoostSupport -> Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_SUCCESS,
                    Bundle().apply {
                        putBoolean(PlaybackCommands.BoostSupportedKey, audioEffects.isVolumeBoostSupported())
                    },
                ),
            )

            PlaybackCommands.SetSkipSilence -> {
                localPlayer?.skipSilenceEnabled =
                    args.getBoolean(PlaybackCommands.SkipSilenceKey)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.GetSkipSilence -> Futures.immediateFuture(
                SessionResult(
                    SessionResult.RESULT_SUCCESS,
                    Bundle().apply {
                        putBoolean(
                            PlaybackCommands.SkipSilenceKey,
                            localPlayer?.skipSilenceEnabled == true,
                        )
                    },
                ),
            )

            PlaybackCommands.SetNightMode -> {
                audioEffects.setNightMode(args.getBoolean(PlaybackCommands.NightModeKey))
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            PlaybackCommands.SetAudioEffectStrength -> {
                val type = args.getString(PlaybackCommands.EffectKey)
                    ?.let { name -> AudioEffectType.entries.find { it.name == name } }
                type?.let { audioEffects.setStrength(it, args.getInt(PlaybackCommands.StrengthKey)) }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            // SessionError's code and not SessionResult's: the old constant still compiles and is
            // no longer one of the values a result may carry, which is a controller being told
            // something the library will not recognise.
            else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    private val playerListener = object : Player.Listener {
        /** The end of a track has no hour to count towards, so it is answered here instead. */
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (sleepTimerMinutes != PlaybackCommands.EndOfTrack) return
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
            stopForSleepTimer()
        }

        /**
         * The audio session is rebuilt whenever the renderer is, which happens without anyone
         * asking -- a new file, a different decoder, a route change. Every effect attached to the
         * old one is now attached to nothing, so they are rebuilt against the new one and set back
         * to what the listener left them at.
         */
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            audioEffects.bind(audioSessionId, preferences.playerPreferences.value)
        }
    }

    override fun onCreate() {
        super.onCreate()
        session = MediaSession.Builder(this, castAwarePlayer().also { it.addListener(playerListener) })
            .setCallback(callback)
            // Cached around, because the notification asks for the same cover on every update and
            // the system control asks again for its own copy.
            .setBitmapLoader(CacheBitmapLoader(CoilBitmapLoader(this, imageLoader, scope)))
            // The way out, in the panel itself. Paused media sits in the shade until something
            // clears it, and on this phone a swipe does not: the row is what the system draws for
            // an active session, and only the session can say it is over.
            .setCustomLayout(
                listOf(
                    CommandButton.Builder(CommandButton.ICON_STOP)
                        .setSessionCommand(SessionCommand(PlaybackCommands.Close, Bundle.EMPTY))
                        .setDisplayName(getString(R.string.close_session))
                        .build(),
                ),
            )
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, sessionActivity.activityClass),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
    }

    /**
     * The player, which may be a television.
     *
     * A [CastPlayer] wrapping the local one, which is what Media3 asks for: it forwards to ExoPlayer
     * while nothing is connected, moves the queue and the position across when a route is picked,
     * and moves them back when it is dropped. Everything above this -- the screens, the
     * notification, the session -- goes on talking to one Player and never learns which.
     *
     * Written this way rather than as a session manager of our own, which is what the old build
     * had: 300 lines to copy a queue between two players, own a state machine and hold two locks,
     * all of it now inside the library. What is not inside the library is the server below, because
     * a receiver cannot read a `content://` address however the player is built.
     *
     * Falls back to the local player alone when the Cast framework is missing -- a device without
     * Play services, which is a device that was never going to cast.
     */
    @OptIn(UnstableApi::class)
    private fun castAwarePlayer(): Player {
        val local = playerFactory.create()
        localPlayer = local
        // Asked first, and before anything is built: on a shell that cannot cast, everything below
        // is start-up time spent on a feature that will never be reached.
        if (!castAvailability.isSupported()) return local
        val server = runCatching { CastMediaServer(contentResolver).also { it.ensureStarted() } }.getOrNull()
            ?: return local
        castMediaServer = server
        return runCatching {
            // The converter belongs to the remote half: it is what turns an address on this phone
            // into one the receiver can fetch, and the local half never needs translating.
            val remote = RemoteCastPlayer.Builder(this)
                .setMediaItemConverter(CastMediaItemConverter(server))
                .build()
            CastPlayer.Builder(this)
                .setLocalPlayer(local)
                .setRemotePlayer(remote)
                .build()
        }.getOrElse {
            server.stop()
            castMediaServer = null
            local
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * The app was swiped out of the recents list.
     *
     * Something still playing carries on, because a swipe is how people put a phone away, not how
     * they stop the music. Anything else has no reason to keep a service alive.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    /**
     * Counted here and not on the screen that armed it, because arming one is followed by putting
     * the phone down: the screen goes off, the player is closed, and a countdown living in either
     * would go with them.
     */
    private fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepTimerMinutes = minutes
        sleepTimerDeadlineMs = 0L
        if (minutes <= PlaybackCommands.Off) return

        val runsForMs = minutes * MillisPerMinute
        sleepTimerDeadlineMs = SystemClock.elapsedRealtime() + runsForMs
        sleepTimerJob = scope.launch {
            delay(runsForMs)
            stopForSleepTimer()
        }
    }

    /** Paused and not stopped: the viewer fell asleep, and tomorrow they carry on from here. */
    private fun stopForSleepTimer() {
        sleepTimerJob = null
        sleepTimerMinutes = PlaybackCommands.Off
        sleepTimerDeadlineMs = 0L
        session?.player?.pause()
    }

    override fun onDestroy() {
        scope.cancel()
        audioEffects.release()
        castMediaServer?.stop()
        castMediaServer = null
        session?.run {
            player.removeListener(playerListener)
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}

/**
 * The activity the notification reopens.
 *
 * An interface with one implementation, which is usually a smell and here is the only way round a
 * dependency: the service is in the module every player feature builds on, and the activity is in
 * one of those features. The arrow has to point the other way.
 */
interface SessionActivityProvider {
    val activityClass: Class<out android.app.Activity>
}

private const val MillisPerMinute = 60_000L

/**
 * Whether this shell can cast at all.
 *
 * A phone can: a film in a hand is a film somebody wants on the television across the room. A
 * television cannot, and not for want of trying -- it is the receiver. Asked because the answer is
 * not free: the wrapper drags in the Cast framework, which loads a Play services module, and stands
 * up a local web server for the receiver to fetch from. Both happen the first time anything is
 * played, and on a television both are paid for something that can never be used.
 */
@OptIn(UnstableApi::class)
fun interface CastAvailability {
    fun isSupported(): Boolean
}
