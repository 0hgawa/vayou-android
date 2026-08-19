package dev.vayou.feature.player

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import dagger.hilt.android.AndroidEntryPoint
import dev.vayou.core.common.extensions.displayNameOf
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.ui.theme.VayouTheme
import javax.inject.Inject

/**
 * Its own activity rather than a destination in the library's graph.
 *
 * Playback wants the whole screen with the system bars gone, a landscape it can ask for, a task the
 * recents list can show on its own, and a floating window it can shrink into; all four are
 * properties of an activity, and putting the player inside another one means fighting that activity
 * for each of them.
 */
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private var player: Player? = null
    private var isInPip by mutableStateOf(false)
    private var toggleReceiver: BroadcastReceiver? = null

    // The same instance the screen composes with: hiltViewModel() inside setContent resolves
    // against this activity's store, and both ask for it under the same key.
    private val viewModel: PlayerViewModel by viewModels()

    private var request: PlaybackRequest? = null

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    /** Read where it is used rather than held: what is on disk can change while this screen is up,
     *  and the two that matter here -- which way up, and whether to shrink -- are both read late. */
    private val preferences: PlayerPreferences
        get() = preferencesRepository.playerPreferences.value

    /**
     * Set by the "carry on in the background" action, read once on the way out.
     *
     * A field rather than a preference: this is a decision about this film on this occasion, and
     * writing it down would change what happens the next time the player is closed by the back key.
     */
    private var isLeavingToBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Both scrims transparent. The bare call lays a nine-tenths white plate under the
        // navigation bar, which on a screen that is a frame of film is a white strip along the
        // bottom of the picture.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        showSystemBars(false)
        applyOrientation(preferences.playerScreenOrientation)

        toggleReceiver = PictureInPicture.registerToggleReceiver(this) {
            player?.let { if (it.isPlaying) it.pause() else it.play() }
            updatePipParams()
        }

        // No uri is a legitimate way in, not a fault: the cast bar brings this forward without one
        // because the session already holds the film. Only the transport is wanted then.
        val request = playbackRequestFrom(intent, ::displayNameOf)
        this.request = request
        setContent {
            // Dark whatever the rest of the app is set to, and not a question the viewer is asked.
            // Everything here sits on a frame of film against black, and a pale dialog over that is
            // a floodlight in a dark room.
            VayouTheme(darkTheme = true) {
                // Brought forward from the cast bar with nothing named: the film is already on a
                // television and the session knows which, so all that is wanted is the transport.
                if (request == null) {
                    CastOnlyScreen(onBack = ::finish)
                    return@VayouTheme
                }
                PlayerScreen(
                    request = request,
                    isInPictureInPicture = isInPip,
                    onBack = ::finish,
                    onPlayerReady = { player = it },
                    onPlayInBackground = {
                        isLeavingToBackground = true
                        finish()
                    },
                    onEnterPictureInPicture = ::enterPictureInPictureNow,
                )
            }
        }
    }

    /**
     * The last chance to write the position down while the player still has one.
     *
     * onDestroy is too late: the ViewModel is cleared alongside it and releases the player, and a
     * released player reports nothing. Leaving by the back button, by the recents list or by
     * turning the screen off all pass through here.
     */
    override fun onStop() {
        viewModel.savePosition()
        super.onStop()
    }

    /**
     * Tells the caller where playback got to, for the apps that asked to be told.
     *
     * On finish and not on stop: leaving for another app and coming back is not the end of
     * anything, and a result sent then would have the caller file the film away as watched.
     */
    override fun finish() {
        val asked = request?.reportsResult == true
        val player = player
        if (asked && player != null) {
            setResult(
                RESULT_OK,
                playbackResult(
                    finished = player.playbackState == Player.STATE_ENDED,
                    durationMs = player.duration,
                    positionMs = player.currentPosition,
                ),
            )
        }
        super.finish()
    }

    override fun onDestroy() {
        // isFinishing, not every destroy: a rotation destroys this activity too, and pausing there
        // would stop the film every time the phone turned.
        if (isFinishing) viewModel.stopPlayback(keepPlaying = isLeavingToBackground)
        // The receiver outlives the composition and has to be taken down by hand; leaving it
        // registered leaks the activity for as long as the process lives.
        toggleReceiver?.let { unregisterReceiver(it) }
        toggleReceiver = null
        super.onDestroy()
    }

    /**
     * Asks for the floating window, and stays put if the system refuses.
     *
     * The window manager can fail this call from inside itself -- it has thrown while dismissing a
     * previous floating window -- and the exception arrives here across the binder. The published
     * build died of exactly that on a real device. A refused shrink is a button that did nothing,
     * not a reason to close the film.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipOrStay(params: PictureInPictureParams) {
        try {
            enterPictureInPictureMode(params)
        } catch (_: RuntimeException) {
            // Nothing to undo: the player is still on screen, which is where it was.
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (preferences.autoPip && PictureInPicture.isSupported && player?.isPlaying == true) {
            enterPipOrStay(PictureInPicture.paramsFor(requireNotNull(player), this, true))
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPip = isInPictureInPictureMode
    }

    /** The button in the window is drawn once; changing what it does means handing the system a new
     *  set of actions. */
    /** Null where the system has no floating window, so the button is absent rather than inert. */
    private fun enterPictureInPictureNow() {
        val current = player ?: return
        if (!PictureInPicture.isSupported) return
        enterPipOrStay(PictureInPicture.paramsFor(current, this, current.isPlaying))
    }

    private fun updatePipParams() {
        val current = player ?: return
        if (!PictureInPicture.isSupported || !isInPip) return
        setPictureInPictureParams(PictureInPicture.paramsFor(current, this, current.isPlaying))
    }

    /**
     * The bars come and go with the controls.
     *
     * Hiding them for good leaves the controls measuring against a window whose edges the system
     * still owns, and the bottom row ends up under the gesture handle. It is also what a viewer
     * expects: while you are pressing things, the clock and the battery are worth having.
     *
     * Swipe from an edge still brings them back for a moment on its own.
     */
    fun showSystemBars(visible: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (visible) show(WindowInsetsCompat.Type.systemBars()) else hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        private const val EXTRA_URI = "uri"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_POSITION = "position"
        private const val EXTRA_SUBTITLES = "subs"
        private const val EXTRA_LIVE = "is_live"
        private const val EXTRA_QUEUE = "video_list"
        private const val EXTRA_QUEUE_NAMES = "video_list.name"

        /**
         * [title] is what to call the file until the player has read it for itself. Absent when the
         * file came from outside the library, which knows nothing about it beyond its uri.
         *
         * [subtitles] are the ones already found beside the file, which a share has and the local
         * library does not: on this device the player finds them itself, and over a network that
         * would mean a second directory listing on a connection it does not hold.
         *
         * [startAtBeginning] overrides the remembered position with zero. The same extra the
         * remembered position travels in, so the player needs no second way to be told.
         */
        fun intentFor(
            context: Context,
            uri: String,
            title: String? = null,
            subtitles: List<String> = emptyList(),
            startAtBeginning: Boolean = false,
            /** The caller's running order, with [queueTitles] naming it by position. */
            queue: List<String> = emptyList(),
            queueTitles: List<String> = emptyList(),
            /** True from the channel list, so the player opens as a channel rather than becoming one. */
            isLive: Boolean = false,
        ): Intent = Intent(context, PlayerActivity::class.java)
            .putExtra(EXTRA_URI, uri)
            .putExtra(EXTRA_TITLE, title)
            .apply {
                if (startAtBeginning) putExtra(EXTRA_POSITION, 0)
                if (subtitles.isNotEmpty()) {
                    putParcelableArrayListExtra(EXTRA_SUBTITLES, ArrayList(subtitles.map(Uri::parse)))
                }
                if (isLive) putExtra(EXTRA_LIVE, true)
                if (queue.isNotEmpty()) {
                    putParcelableArrayListExtra(EXTRA_QUEUE, ArrayList(queue.map(Uri::parse)))
                    putExtra(EXTRA_QUEUE_NAMES, queueTitles.toTypedArray())
                }
            }
    }
}
