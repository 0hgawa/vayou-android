package dev.vayou.feature.music

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dagger.hilt.android.AndroidEntryPoint
import dev.vayou.core.media.Lyrics
import dev.vayou.core.media.Song
import dev.vayou.core.player.ui.musicMediaItem
import dev.vayou.core.player.ui.rememberMusicController
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The audio player, in a window of its own.
 *
 * Its own activity, for the same reason the video player is one: what plays outlives the screen
 * that started it, and the notification has to have somewhere to reopen to. Started from a row or
 * from the notification, and in both cases it attaches to the session that is already running
 * rather than starting a second one.
 */
@AndroidEntryPoint
class MusicPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        riseFromTheBar()

        val startAt = intent.getStringExtra(ExtraUri)
        val queueUris = intent.getStringArrayListExtra(ExtraQueue).orEmpty().toList()
        // Present only for a track from a share. Its absence is what says "this one is in the
        // library", which is the difference that decides whether there is anything to look up.
        val streamTitle = intent.getStringExtra(ExtraTitle)

        setContent {
            // Dark whatever the app is set to: this screen is a cover on a tinted ground, and the
            // tint is taken from that cover, which is never light enough to read black text on.
            VayouTheme(darkTheme = true) {
                val viewModel: MusicPlayerViewModel = hiltViewModel()
                var hasStarted by remember { mutableStateOf(false) }

                val player = rememberMusicController()

                // Once, on the first connection. Reopened from the notification there is no queue
                // in the intent and the session is already playing, and setting one there would
                // restart what the listener is in the middle of.
                LaunchedEffect(player) {
                    val controller = player ?: return@LaunchedEffect
                    if (hasStarted || queueUris.isEmpty()) return@LaunchedEffect
                    hasStarted = true

                    val queue = if (streamTitle == null) {
                        viewModel.resolve(queueUris).map(::musicMediaItem)
                    } else {
                        listOf(musicMediaItem(uri = queueUris.first(), title = streamTitle))
                    }
                    if (queue.isEmpty()) return@LaunchedEffect
                    controller.setMediaItems(
                        queue,
                        queue.indexOfFirst { it.mediaId == startAt }.coerceAtLeast(0),
                        0L,
                    )
                    controller.prepare()
                    controller.play()
                }

                val preferences by viewModel.preferences.collectAsStateWithLifecycle()

                // Which track the menu is about. Followed from the session rather than passed in
                // with the intent: the queue moves on by itself, and a menu still pointing at the
                // track before this one would edit the wrong file.
                var playingUri by remember { mutableStateOf<String?>(null) }
                DisposableEffect(player) {
                    val controller = player ?: return@DisposableEffect onDispose {}
                    playingUri = controller.currentMediaItem?.mediaId
                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            playingUri = mediaItem?.mediaId
                        }
                    }
                    controller.addListener(listener)
                    onDispose { controller.removeListener(listener) }
                }
                // Null for a track from a share, which the library has never seen: there is nothing
                // to tag, star or delete, so the key is not drawn at all.
                var playingSong by remember { mutableStateOf<Song?>(null) }
                LaunchedEffect(playingUri) {
                    playingSong = playingUri?.let { viewModel.resolve(listOf(it)).firstOrNull() }
                }
                val library: MusicViewModel = hiltViewModel()
                // Read once for the two things below: the star asks what is starred and
                // the menu asks what lists exist, and two subscriptions to one flow is one
                // too many.
                val playlists by library.playlists.collectAsStateWithLifecycle()

                // Looked up when the track changes, off the main thread, and null while it is being
                // looked up: the key appears the moment there is something behind it.
                var lyrics by remember { mutableStateOf<Lyrics?>(null) }
                LaunchedEffect(playingSong) {
                    lyrics = playingSong?.let { viewModel.lyricsFor(it) }
                }

                NowPlayingScreen(
                    player = player,
                    preferences = preferences,
                    onSavePreferences = viewModel::updatePreferences,
                    onBack = ::finish,
                    menu = {
                        playingSong?.let { NowPlayingMenu(song = it, viewModel = library, playlists = playlists) }
                    },
                    lyrics = lyrics,
                    favourite = {
                        playingSong?.let {
                            NowPlayingStar(song = it, viewModel = library, favouriteUris = playlists.favouriteUris)
                        }
                    },
                )
            }
        }
    }

    /**
     * Claimed when this screen comes forward, not when it is built.
     *
     * How the system bars look belongs to whoever is in front. Asked for in onCreate, a player
     * rebuilt off-screen while the task is restored takes the bars dark before the library has
     * drawn a frame, and the library then sets them back -- the flip you see on opening the app.
     *
     * Both scrims transparent, which is the whole point: the default edge-to-edge call lays a
     * nine-tenths white plate under the navigation bar, and that is the white strip under this
     * screen's own gradient.
     */
    override fun onStart() {
        super.onStart()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
    }

    /**
     * Up from the foot of the screen, and back down on the way out.
     *
     * The bar this is opened from is the same player collapsed, so the screen has to read as that
     * bar growing rather than as another place arriving from the side -- which is what the arrow at
     * the top of it has been promising all along. The film player keeps its sideways step: opening a
     * film is going somewhere else, not unfolding something already at hand.
     */
    private fun riseFromTheBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.player_enter_up, R.anim.player_hold)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.player_hold, R.anim.player_exit_down)
            return
        }
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.player_enter_up, R.anim.player_hold)
    }

    /**
     * The way out, for the Androids that cannot be told in advance.
     *
     * Below 34 the closing pair is set as the window is leaving, not when it arrives, so it has to
     * be hung here -- and here rather than on the back press alone, because the arrow, the gesture
     * and the key all end up in the same place.
     */
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.player_hold, R.anim.player_exit_down)
    }

    companion object {
        private const val ExtraUri = "uri"
        private const val ExtraQueue = "queue"

        private const val ExtraTitle = "title"

        /**
         * [queue] is what plays after this track, in order. Empty attaches to whatever is already
         * playing, which is what the notification sends.
         *
         * Addresses and not tracks: a queue of five hundred would not fit in a binder transaction,
         * and the store already has every one of them.
         */
        fun intentFor(context: Context, uri: String, queue: List<String> = emptyList()): Intent =
            Intent(context, MusicPlayerActivity::class.java)
                .putExtra(ExtraUri, uri)
                .putStringArrayListExtra(ExtraQueue, ArrayList(queue.ifEmpty { listOf(uri) }))

        /**
         * A track that is not in the library.
         *
         * It plays as itself: the address opens it and the file's name is what it is called. A
         * share is browsed a file at a time, so it goes alone -- what sits beside it in a folder
         * is not an album.
         */
        fun streamIntent(context: Context, uri: String, title: String): Intent =
            intentFor(context, uri).putExtra(ExtraTitle, title)
    }
}
