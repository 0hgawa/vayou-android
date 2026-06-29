package dev.vayou.core.player.service

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.util.Log
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import kotlin.math.abs
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.CommandButton
import androidx.media3.session.CommandButton.ICON_UNDEFINED
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import dev.vayou.core.smb.SmbAwareDataSourceFactory
import dev.vayou.core.common.extensions.getFilenameFromUri
import dev.vayou.core.common.extensions.getLocalSubtitles
import dev.vayou.core.common.extensions.getPath
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.DecoderPriority
import dev.vayou.core.model.EqPreset
import dev.vayou.core.model.LoopMode
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.Resume
import dev.vayou.core.player.R
import dev.vayou.core.ui.R as coreUiR
import dev.vayou.core.player.extensions.addAdditionalSubtitleConfiguration
import dev.vayou.core.player.extensions.audioTrackIndex
import dev.vayou.core.player.extensions.copy
import dev.vayou.core.player.extensions.getManuallySelectedTrackIndex
import dev.vayou.core.player.extensions.playbackSpeed
import dev.vayou.core.player.extensions.positionMs
import dev.vayou.core.player.extensions.setExtras
import dev.vayou.core.player.extensions.setIsScrubbingModeEnabled
import dev.vayou.core.player.extensions.subtitleDelayMilliseconds
import dev.vayou.core.player.extensions.subtitleSpeed
import dev.vayou.core.player.extensions.subtitleTrackIndex
import dev.vayou.core.player.extensions.switchTrack
import dev.vayou.core.player.extensions.uriToSubtitleConfiguration
import dev.vayou.core.player.extensions.videoZoom
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleDelayMilliseconds
import io.github.anilbeesetti.nextlib.media3ext.renderer.subtitleSpeed
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlayerService : MediaSessionService() {

    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaSession: MediaSession? = null
    private var artworkLoadJob: Job? = null

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var mediaRepository: MediaRepository

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var smbAwareDataSourceFactory: SmbAwareDataSourceFactory

    @Inject
    lateinit var sessionActivityProvider: PlayerSessionActivityProvider

    private val playerPreferences: PlayerPreferences
        get() = preferencesRepository.playerPreferences.value

    private val customCommands = CustomCommands.asSessionCommands()

    private var isMediaItemReady = false

    // Set by ADD_SUBTITLE_TRACK; consumed on the next onTracksChanged that shows the new track.
    private var pendingAutoSelectTextUri: String? = null

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentVolumeGain: Int = 0
    private var equalizer: Equalizer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

    private val playbackStateListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) return
            // SEEK-triggered transitions come from our own replace dance (e.g. adding a
            // subtitle track); they would otherwise stomp the current position with the
            // saved resume point and reset the track-restore gate.
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) return
            isMediaItemReady = false
            loadArtworkForCurrentMediaItem()
            mediaItem?.mediaMetadata?.let { metadata ->
                mediaSession?.player?.run {
                    setPlaybackSpeed(metadata.playbackSpeed ?: playerPreferences.defaultPlaybackSpeed)
                    playerSpecificSubtitleDelayMilliseconds = metadata.subtitleDelayMilliseconds ?: 0L
                    playerSpecificSubtitleSpeed = metadata.subtitleSpeed ?: 1f
                }

                metadata.positionMs?.takeIf { playerPreferences.resume == Resume.YES }?.let {
                    mediaSession?.player?.seekTo(it)
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            val oldMediaItem = oldPosition.mediaItem ?: return

            when (reason) {
                DISCONTINUITY_REASON_SEEK,
                DISCONTINUITY_REASON_AUTO_TRANSITION,
                -> {
                    if (newPosition.mediaItem == null || oldMediaItem == newPosition.mediaItem) return
                    // Vayou always plays a single MediaItem per session, so a cross-item
                    // discontinuity only happens during our addMediaItem+seekTo+removeMediaItem
                    // replace dance (e.g. adding a subtitle track). The replaceMediaItem
                    // below would clobber the just-added item with a copy of the old one,
                    // causing a visible restart — skip it.
                    if (reason == DISCONTINUITY_REASON_SEEK) return

                    val updatedPosition = C.TIME_UNSET
                    mediaSession?.player?.replaceMediaItem(
                        oldPosition.mediaItemIndex,
                        oldMediaItem.copy(positionMs = updatedPosition),
                    )
                    serviceScope.launch {
                        mediaRepository.updateMediumPosition(
                            uri = oldMediaItem.mediaId,
                            position = updatedPosition,
                        )
                    }
                }

                DISCONTINUITY_REASON_REMOVE -> {
                    serviceScope.launch {
                        val durationMs = oldMediaItem.mediaMetadata.durationMs
                        val isAtEnd = durationMs != null && oldPosition.positionMs >= durationMs - 1000
                        mediaRepository.updateMediumPosition(
                            uri = oldMediaItem.mediaId,
                            position = if (isAtEnd) C.TIME_UNSET else oldPosition.positionMs,
                        )
                    }
                }

                else -> return
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            if (error.errorCode != PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) return
            val player = mediaSession?.player ?: return
            player.seekToDefaultPosition()
            player.prepare()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
            if (mediaMetadata.artworkData == null) return
            // Strip extractor-provided raw artwork bytes (often malformed for IPTV/live
            // streams) so the session notification falls back to our artworkUri.
            val player = mediaSession?.player ?: return
            val currentMediaItem = player.currentMediaItem ?: return
            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.buildUpon()
                    .setMediaMetadata(
                        mediaMetadata.buildUpon()
                            .setArtworkData(null, null)
                            .build(),
                    )
                    .build(),
            )
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            val player = mediaSession?.player ?: return

            if (!isMediaItemReady && tracks.groups.isNotEmpty()) {
                isMediaItemReady = true

                if (playerPreferences.rememberSelections) {
                    val metadata = player.mediaMetadata
                    metadata.audioTrackIndex?.let { player.switchTrack(C.TRACK_TYPE_AUDIO, it) }
                    metadata.subtitleTrackIndex?.let { player.switchTrack(C.TRACK_TYPE_TEXT, it) }
                }
            }

            val pendingUri = pendingAutoSelectTextUri ?: return
            // Media3 prefixes the subtitle Format.id with an integer (e.g. "3:file:///…").
            val suffix = ":$pendingUri"
            var matchIndex = -1
            var textCursor = 0
            for (group in tracks.groups) {
                if (group.type != C.TRACK_TYPE_TEXT || !group.isSupported) continue
                for (i in 0 until group.length) {
                    val id = group.getTrackFormat(i).id ?: continue
                    if (id == pendingUri || id.endsWith(suffix)) {
                        matchIndex = textCursor
                        break
                    }
                }
                if (matchIndex >= 0) break
                textCursor++
            }
            if (matchIndex >= 0) {
                pendingAutoSelectTextUri = null
                player.switchTrack(C.TRACK_TYPE_TEXT, matchIndex)
            }
        }

        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
            super.onTrackSelectionParametersChanged(parameters)
            val player = mediaSession?.player ?: return
            val currentMediaItem = player.currentMediaItem ?: return

            val audioTrackIndex = player.getManuallySelectedTrackIndex(C.TRACK_TYPE_AUDIO)
            val subtitleTrackIndex = player.getManuallySelectedTrackIndex(C.TRACK_TYPE_TEXT)

            if (audioTrackIndex != null) {
                serviceScope.launch {
                    mediaRepository.updateMediumAudioTrack(
                        uri = currentMediaItem.mediaId,
                        audioTrackIndex = audioTrackIndex,
                    )
                }
            }

            if (subtitleTrackIndex != null) {
                serviceScope.launch {
                    mediaRepository.updateMediumSubtitleTrack(
                        uri = currentMediaItem.mediaId,
                        subtitleTrackIndex = subtitleTrackIndex,
                    )
                }
            }

            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.copy(
                    audioTrackIndex = audioTrackIndex,
                    subtitleTrackIndex = subtitleTrackIndex,
                ),
            )
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            super.onPlaybackParametersChanged(playbackParameters)
            val player = mediaSession?.player ?: return
            val currentMediaItem = player.currentMediaItem ?: return
            val playbackSpeed = playbackParameters.speed

            serviceScope.launch {
                mediaRepository.updateMediumPlaybackSpeed(
                    uri = currentMediaItem.mediaId,
                    playbackSpeed = playbackSpeed,
                )
            }
            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.copy(playbackSpeed = playbackSpeed),
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                mediaSession?.player?.trackSelectionParameters = TrackSelectionParameters.DEFAULT
                mediaSession?.player?.setPlaybackSpeed(playerPreferences.defaultPlaybackSpeed)
            }

            if (playbackState == Player.STATE_READY) {
                mediaSession?.player?.let {
                    serviceScope.launch {
                        mediaRepository.updateMediumLastPlayedTime(
                            uri = it.currentMediaItem?.mediaId ?: return@launch,
                            lastPlayedTime = System.currentTimeMillis(),
                        )
                    }
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)

            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                if (mediaSession?.player?.repeatMode != Player.REPEAT_MODE_OFF) {
                    mediaSession?.player?.seekTo(0)
                    mediaSession?.player?.play()
                    return
                }
                mediaSession?.run {
                    player.clearMediaItems()
                    player.stop()
                }
                stopSelf()
            }
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            val player = mediaSession?.player ?: return
            val currentMediaItem = player.currentMediaItem ?: return
            // Update the media metadata duration so that it will be used later in position discontinuity handling
            player.replaceMediaItem(
                player.currentMediaItemIndex,
                currentMediaItem.copy(durationMs = player.duration.coerceAtLeast(0))
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            mediaSession?.run {
                serviceScope.launch {
                    mediaRepository.updateMediumPosition(
                        uri = player.currentMediaItem?.mediaId ?: return@launch,
                        position = player.currentPosition,
                    )
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            serviceScope.launch {
                preferencesRepository.updatePlayerPreferences {
                    it.copy(
                        loopMode = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> LoopMode.OFF
                            Player.REPEAT_MODE_ONE -> LoopMode.ONE
                            Player.REPEAT_MODE_ALL -> LoopMode.ALL
                            else -> LoopMode.OFF
                        },
                    )
                }
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            super.onAudioSessionIdChanged(audioSessionId)
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
            if (playerPreferences.enableVolumeBoost) {
                try {
                    loudnessEnhancer?.release()
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                    if (currentVolumeGain > 0) setEnhancerTargetGain(currentVolumeGain)
                } catch (e: Exception) {
                    Log.w("PlayerService", "audio effect error", e)
                    loudnessEnhancer = null
                }
            }
            setupEqualizer(audioSessionId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setupNightMode(audioSessionId)
            }
        }
    }

    private fun setEnhancerTargetGain(gain: Int) {
        val enhancer = loudnessEnhancer ?: return
        try {
            enhancer.setTargetGain(gain)
            enhancer.enabled = gain > 0
            currentVolumeGain = enhancer.targetGain.toInt()
        } catch (e: Exception) {
            Log.w("PlayerService", "audio effect error", e)
        }
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                val prefs = playerPreferences
                if (prefs.equalizerBandGains.isEmpty()) {
                    applyPresetGains(prefs.equalizerPreset)
                } else {
                    prefs.equalizerBandGains.forEachIndexed { i, gain ->
                        if (i < numberOfBands) setBandLevel(i.toShort(), gain.toShort())
                    }
                }
                enabled = prefs.equalizerEnabled
            }
        } catch (e: Exception) {
            Log.w("PlayerService", "audio effect error", e)
            equalizer = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupNightMode(audioSessionId: Int) {
        try {
            dynamicsProcessing?.release()
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2, false, 0, false, 0, false, 0, true,
            ).build()
            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                setLimiterAllChannelsTo(
                    DynamicsProcessing.Limiter(true, true, 0, 50f, 400f, 6f, -12f, 3f),
                )
                enabled = playerPreferences.nightModeEnabled
            }
        } catch (e: Exception) {
            Log.w("PlayerService", "audio effect error", e)
            dynamicsProcessing = null
        }
    }

    private fun Equalizer.applyPresetGains(preset: EqPreset) {
        val gains = preset.gains
        for (band in 0 until numberOfBands) {
            val centerHz = getCenterFreq(band.toShort()) / 1000
            val gain = gains.minByOrNull { abs(it.key - centerHz) }?.value ?: 0
            val range = bandLevelRange
            setBandLevel(band.toShort(), gain.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            return MediaSession.ConnectionResult.accept(
                connectionResult.availableSessionCommands
                    .buildUpon()
                    .addSessionCommands(customCommands)
                    .build(),
                connectionResult.availablePlayerCommands,
            )
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = serviceScope.future(Dispatchers.Default) {
            val updatedMediaItems = updatedMediaItemsWithMetadata(mediaItems)
            return@future MediaSession.MediaItemsWithStartPosition(updatedMediaItems, startIndex, startPositionMs)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = serviceScope.future(Dispatchers.Default) {
            val updatedMediaItems = updatedMediaItemsWithMetadata(mediaItems)
            return@future updatedMediaItems.toMutableList()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> = serviceScope.future {
            val command = CustomCommands.fromSessionCommand(customCommand)
                ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)

            when (command) {
                CustomCommands.ADD_SUBTITLE_TRACK -> {
                    val subtitleUri = args.getString(CustomCommands.SUBTITLE_TRACK_URI_KEY)?.toUri()
                        ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)

                    val newSubConfiguration = uriToSubtitleConfiguration(
                        uri = subtitleUri,
                        subtitleEncoding = playerPreferences.subtitleTextEncoding,
                    )
                    mediaSession?.player?.let { player ->
                        val currentMediaItem = player.currentMediaItem ?: return@let
                        val textTracks = player.currentTracks.groups.filter {
                            it.type == C.TRACK_TYPE_TEXT && it.isSupported
                        }

                        mediaRepository.updateMediumPosition(
                            uri = currentMediaItem.mediaId,
                            position = player.currentPosition,
                        )
                        mediaRepository.updateMediumSubtitleTrack(
                            uri = currentMediaItem.mediaId,
                            subtitleTrackIndex = textTracks.size,
                        )
                        mediaRepository.addExternalSubtitleToMedium(
                            uri = currentMediaItem.mediaId,
                            subtitleUri = subtitleUri,
                        )
                        pendingAutoSelectTextUri = subtitleUri.toString()
                        player.addAdditionalSubtitleConfiguration(newSubConfiguration)
                    }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.SET_SKIP_SILENCE_ENABLED -> {
                    val enabled = args.getBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY)
                    mediaSession?.player?.playerSpecificSkipSilenceEnabled = enabled
                    mediaSession?.sessionExtras = Bundle().apply {
                        putBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY, enabled)
                    }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_SKIP_SILENCE_ENABLED -> {
                    val enabled = mediaSession?.player?.playerSpecificSkipSilenceEnabled ?: false
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putBoolean(CustomCommands.SKIP_SILENCE_ENABLED_KEY, enabled)
                        },
                    )
                }

                CustomCommands.SET_IS_SCRUBBING_MODE_ENABLED -> {
                    val enabled = args.getBoolean(CustomCommands.IS_SCRUBBING_MODE_ENABLED_KEY)
                    val exoPlayer = mediaSession?.player as? ExoPlayer
                    exoPlayer?.setSeekParameters(if (enabled) SeekParameters.PREVIOUS_SYNC else SeekParameters.EXACT)
                    exoPlayer?.isScrubbingModeEnabled = enabled
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.IS_LOUDNESS_GAIN_SUPPORTED -> {
                    val isSupported = loudnessEnhancer != null
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putBoolean(CustomCommands.IS_LOUDNESS_GAIN_SUPPORTED_KEY, isSupported)
                        },
                    )
                }

                CustomCommands.SET_LOUDNESS_GAIN -> {
                    val gain = args.getInt(CustomCommands.LOUDNESS_GAIN_KEY, 0)
                    setEnhancerTargetGain(gain)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_LOUDNESS_GAIN -> {
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putInt(CustomCommands.LOUDNESS_GAIN_KEY, currentVolumeGain)
                        },
                    )
                }

                CustomCommands.GET_SUBTITLE_DELAY -> {
                    val subtitleDelay = mediaSession?.player?.playerSpecificSubtitleDelayMilliseconds ?: 0
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putLong(CustomCommands.SUBTITLE_DELAY_KEY, subtitleDelay)
                        },
                    )
                }

                CustomCommands.SET_SUBTITLE_DELAY -> {
                    val subtitleDelay = args.getLong(CustomCommands.SUBTITLE_DELAY_KEY)
                    mediaSession?.player?.playerSpecificSubtitleDelayMilliseconds = subtitleDelay
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_SUBTITLE_SPEED -> {
                    val subtitleSpeed = mediaSession?.player?.playerSpecificSubtitleSpeed ?: 0f
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putFloat(CustomCommands.SUBTITLE_SPEED_KEY, subtitleSpeed)
                        },
                    )
                }

                CustomCommands.SET_SUBTITLE_SPEED -> {
                    val subtitleSpeed = args.getFloat(CustomCommands.SUBTITLE_SPEED_KEY)
                    mediaSession?.player?.playerSpecificSubtitleSpeed = subtitleSpeed
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.GET_EQUALIZER_BANDS -> {
                    val eq = equalizer
                    if (eq == null) {
                        return@future SessionResult(
                            SessionResult.RESULT_SUCCESS,
                            Bundle().apply { putInt(CustomCommands.EQ_BAND_COUNT_KEY, 0) },
                        )
                    }
                    val count = eq.numberOfBands.toInt()
                    return@future SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        Bundle().apply {
                            putInt(CustomCommands.EQ_BAND_COUNT_KEY, count)
                            putIntArray(CustomCommands.EQ_BAND_INDICES_KEY, IntArray(count) { it })
                            putIntArray(CustomCommands.EQ_BAND_CENTER_FREQS_KEY, IntArray(count) { i -> eq.getCenterFreq(i.toShort()) / 1000 })
                            putIntArray(CustomCommands.EQ_BAND_LEVELS_KEY, IntArray(count) { i -> eq.getBandLevel(i.toShort()).toInt() })
                            putInt(CustomCommands.EQ_BAND_MIN_KEY, eq.bandLevelRange[0].toInt())
                            putInt(CustomCommands.EQ_BAND_MAX_KEY, eq.bandLevelRange[1].toInt())
                        },
                    )
                }

                CustomCommands.SET_EQUALIZER_ENABLED -> {
                    val enabled = args.getBoolean(CustomCommands.EQ_ENABLED_KEY)
                    equalizer?.enabled = enabled
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.SET_EQUALIZER_BAND_LEVEL -> {
                    val index = args.getInt(CustomCommands.EQ_BAND_INDEX_KEY).toShort()
                    val level = args.getInt(CustomCommands.EQ_BAND_LEVEL_KEY).toShort()
                    equalizer?.setBandLevel(index, level)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.APPLY_EQUALIZER_PRESET -> {
                    val presetName = args.getString(CustomCommands.EQ_PRESET_KEY) ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)
                    val preset = runCatching { EqPreset.valueOf(presetName) }.getOrNull() ?: return@future SessionResult(SessionError.ERROR_BAD_VALUE)
                    val eq = equalizer ?: return@future SessionResult(SessionResult.RESULT_SUCCESS)
                    if (preset != EqPreset.CUSTOM) eq.applyPresetGains(preset)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.SET_NIGHT_MODE_ENABLED -> {
                    val enabled = args.getBoolean(CustomCommands.NIGHT_MODE_ENABLED_KEY)
                    dynamicsProcessing?.enabled = enabled
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                CustomCommands.STOP_PLAYER_SESSION -> {
                    mediaSession?.run {
                        serviceScope.launch {
                            mediaRepository.updateMediumPosition(
                                uri = player.currentMediaItem?.mediaId ?: return@launch,
                                position = player.currentPosition,
                            )
                        }
                    }
                    mediaSession?.run {
                        player.clearMediaItems()
                        player.stop()
                    }
                    stopSelf()
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onCreate() {
        super.onCreate()
        val renderersFactory = NextRenderersFactory(applicationContext)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(
                when (playerPreferences.decoderPriority) {
                    DecoderPriority.DEVICE_ONLY -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    DecoderPriority.PREFER_DEVICE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    DecoderPriority.PREFER_APP -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                },
            )

        val trackSelector = DefaultTrackSelector(applicationContext).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage(playerPreferences.preferredAudioLanguage)
                    .setPreferredTextLanguage(playerPreferences.preferredSubtitleLanguage),
            )
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(applicationContext)
            .setDataSourceFactory(smbAwareDataSourceFactory)

        val player = ExoPlayer.Builder(applicationContext)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                playerPreferences.requireAudioFocus,
            )
            .setHandleAudioBecomingNoisy(playerPreferences.pauseOnHeadsetDisconnect)
            .build()
            .also {
                it.addListener(playbackStateListener)
                it.pauseAtEndOfMediaItems = !playerPreferences.autoplay
                it.repeatMode = when (playerPreferences.loopMode) {
                    LoopMode.OFF -> Player.REPEAT_MODE_OFF
                    LoopMode.ONE -> Player.REPEAT_MODE_ONE
                    LoopMode.ALL -> Player.REPEAT_MODE_ALL
                }
            }

        try {
            mediaSession = MediaSession.Builder(this, player).apply {
                setSessionActivity(
                    PendingIntent.getActivity(
                        this@PlayerService,
                        0,
                        Intent(this@PlayerService, sessionActivityProvider.activityClass),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                setCallback(mediaSessionCallback)
                setCustomLayout(
                    listOf(
                        CommandButton.Builder(ICON_UNDEFINED)
                            .setCustomIconResId(coreUiR.drawable.ic_close)
                            .setDisplayName(getString(coreUiR.string.stop_player_session))
                            .setSessionCommand(CustomCommands.STOP_PLAYER_SESSION.sessionCommand)
                            .setEnabled(true)
                            .build(),
                    ),
                )
            }.build()
        } catch (e: Exception) {
            Log.w("PlayerService", "audio effect error", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        artworkLoadJob?.cancel()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        equalizer?.release()
        equalizer = null
        dynamicsProcessing?.release()
        dynamicsProcessing = null
        mediaSession?.run {
            player.clearMediaItems()
            player.stop()
            player.removeListener(playbackStateListener)
            player.release()
            release()
            mediaSession = null
        }
        // Downloaded subtitles are referenced from Video.subtitleStreams and must
        // survive the session. Android reclaims cacheDir under memory pressure.
        serviceScope.cancel()
    }

    private suspend fun updatedMediaItemsWithMetadata(
        mediaItems: List<MediaItem>,
    ): List<MediaItem> = supervisorScope {
        mediaItems.map { mediaItem ->
            async {
                val uri = mediaItem.mediaId.toUri()
                val video = mediaRepository.getVideoByUri(uri = mediaItem.mediaId)
                val videoState = mediaRepository.getVideoState(uri = mediaItem.mediaId)

                val externalSubs = videoState?.externalSubs ?: emptyList()
                val localSubs = (videoState?.path ?: getPath(uri))?.let {
                    File(it).getLocalSubtitles(
                        context = this@PlayerService,
                        excludeSubsList = externalSubs,
                    )
                } ?: emptyList()

                val existingSubConfigurations = mediaItem.localConfiguration?.subtitleConfigurations ?: emptyList()
                val subConfigurations = (localSubs + externalSubs).map { subtitleUri ->
                    uriToSubtitleConfiguration(
                        uri = subtitleUri,
                        subtitleEncoding = playerPreferences.subtitleTextEncoding,
                    )
                }

                // Use placeholder artwork initially - actual artwork will be loaded in background
                val artworkUri = getDefaultArtworkUri()

                val title = mediaItem.mediaMetadata.title ?: video?.nameWithExtension ?: getFilenameFromUri(uri)
                val positionMs = mediaItem.mediaMetadata.positionMs ?: videoState?.position
                val videoScale = mediaItem.mediaMetadata.videoZoom ?: videoState?.videoScale
                val playbackSpeed = mediaItem.mediaMetadata.playbackSpeed ?: videoState?.playbackSpeed
                val audioTrackIndex = mediaItem.mediaMetadata.audioTrackIndex ?: videoState?.audioTrackIndex
                val subtitleTrackIndex = mediaItem.mediaMetadata.subtitleTrackIndex ?: videoState?.subtitleTrackIndex
                val subtitleDelay = mediaItem.mediaMetadata.subtitleDelayMilliseconds ?: videoState?.subtitleDelayMilliseconds
                val subtitleSpeed = mediaItem.mediaMetadata.subtitleSpeed ?: videoState?.subtitleSpeed

                mediaItem.buildUpon().apply {
                    setSubtitleConfigurations(existingSubConfigurations + subConfigurations)
                    setMediaMetadata(
                        MediaMetadata.Builder().apply {
                            setTitle(title)
                            setArtworkUri(artworkUri)
                            setExtras(
                                positionMs = positionMs,
                                videoScale = videoScale,
                                playbackSpeed = playbackSpeed,
                                audioTrackIndex = audioTrackIndex,
                                subtitleTrackIndex = subtitleTrackIndex,
                                subtitleDelayMilliseconds = subtitleDelay,
                                subtitleSpeed = subtitleSpeed,
                            )
                        }.build(),
                    )
                }.build()
            }
        }.awaitAll()
    }
    
    private fun getDefaultArtworkUri(): Uri = Uri.Builder().apply {
        val defaultArtwork = R.drawable.artwork_default
        scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        authority(resources.getResourcePackageName(defaultArtwork))
        appendPath(resources.getResourceTypeName(defaultArtwork))
        appendPath(resources.getResourceEntryName(defaultArtwork))
    }.build()

    private fun loadArtworkForCurrentMediaItem() {
        artworkLoadJob?.cancel()
        artworkLoadJob = serviceScope.launch(Dispatchers.Main) {
            val player = mediaSession?.player ?: return@launch
            val currentMediaItem = player.currentMediaItem ?: return@launch
            if (currentMediaItem.mediaMetadata.artworkData != null) return@launch

            val artworkUri = loadArtworkForMediaItem(currentMediaItem) ?: return@launch

            val updatedPlayer = mediaSession?.player ?: return@launch
            val updatedMediaItem = updatedPlayer.currentMediaItem ?: return@launch
            if (updatedMediaItem.mediaId != currentMediaItem.mediaId) return@launch

            updatedPlayer.replaceMediaItem(
                updatedPlayer.currentMediaItemIndex,
                updatedMediaItem.withArtwork(artworkUri),
            )
        }
    }
    private suspend fun loadArtworkForMediaItem(mediaItem: MediaItem): Uri? = withContext(Dispatchers.IO) {
        val uri = mediaItem.mediaId.toUri()
        return@withContext try {
            val request = ImageRequest.Builder(this@PlayerService)
                .data(uri)
                .size(512, 512)
                .build()
            imageLoader.execute(request)
            val diskCache = imageLoader.diskCache ?: return@withContext null
            return@withContext diskCache.openSnapshot(uri.toString())?.use { snapshot ->
                snapshot.data.toFile().toUri()
            }
        } catch (_: Throwable) {
            null
        }
    }
    private fun MediaItem.withArtwork(uri: Uri): MediaItem = buildUpon()
        .setMediaMetadata(
            mediaMetadata.buildUpon()
                .setArtworkUri(uri)
                .build(),
        )
        .build()
}

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSkipSilenceEnabled: Boolean
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.skipSilenceEnabled
        else -> false
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.skipSilenceEnabled = value
        }
    }

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSubtitleDelayMilliseconds: Long
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.subtitleDelayMilliseconds
        else -> 0L
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.subtitleDelayMilliseconds = value
        }
    }

@get:UnstableApi
@set:UnstableApi
private var Player.playerSpecificSubtitleSpeed: Float
    @OptIn(UnstableApi::class)
    get() = when (this) {
        is ExoPlayer -> this.subtitleSpeed
        else -> 0f
    }
    set(value) {
        when (this) {
            is ExoPlayer -> this.subtitleSpeed = value
        }
    }
