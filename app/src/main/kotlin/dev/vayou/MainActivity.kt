package dev.vayou

import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import dev.vayou.core.common.storagePermission
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.media.sync.MediaSynchronizer
import dev.vayou.core.model.ThemeConfig
import dev.vayou.core.ui.designsystem.components.LocalShowsPlayedProgress
import dev.vayou.core.ui.designsystem.components.LocalVayouMessages
import dev.vayou.core.ui.designsystem.components.VayouMessageHost
import dev.vayou.core.ui.designsystem.components.VayouNavBar
import dev.vayou.core.ui.designsystem.components.VayouNavBarItem
import dev.vayou.core.ui.designsystem.components.VayouNavRail
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.core.ui.designsystem.components.rememberVayouMessages
import dev.vayou.core.ui.theme.VayouDynamicColor
import dev.vayou.core.ui.theme.VayouTheme
import dev.vayou.feature.library.LibraryScreen
import dev.vayou.feature.music.MusicMiniController
import dev.vayou.feature.music.MusicPlayerActivity
import dev.vayou.feature.music.MusicScreen
import dev.vayou.feature.network.NetworkScreen
import dev.vayou.feature.player.CastMiniController
import dev.vayou.feature.player.PlayerActivity
import dev.vayou.feature.settings.SettingsScreen
import javax.inject.Inject

/**
 * A [FragmentActivity] and not a plain ComponentActivity, for one reason: the system's own lock
 * prompt is a fragment, and the private folder asks for it. Everything else here is the same.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var synchronizer: MediaSynchronizer

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences by preferencesRepository.applicationPreferences.collectAsStateWithLifecycle()
            val isDark = when (preferences.themeConfig) {
                ThemeConfig.SYSTEM -> isSystemInDarkTheme()
                ThemeConfig.OFF -> false
                ThemeConfig.ON -> true
            }

            // Transparent on both, following whichever scheme the app is drawing in. The bare call
            // lays a nine-tenths white plate under the navigation bar, and edge-to-edge with an
            // opaque strip at the bottom is not edge-to-edge.
            //
            // Reapplied when the scheme changes, so picking a theme turns the system bars' icons
            // with everything else rather than leaving them dark on a dark bar.
            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { isDark },
                )
                // Android would otherwise put its own translucent plate behind a transparent bar.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }

            VayouTheme(
                darkTheme = isDark,
                // Only for a dark theme that was asked for, never one inherited from the system:
                // black is the fourth choice in the settings, and "follow the system" is a
                // different one. An older build could store the pair that means both.
                highContrastDarkTheme = preferences.useHighContrastDarkTheme &&
                    preferences.themeConfig == ThemeConfig.ON,
                dynamicColor = if (preferences.useDynamicColors) {
                    VayouDynamicColor.Full
                } else {
                    VayouDynamicColor.None
                },
            ) {
                CompositionLocalProvider(
                    LocalShowsPlayedProgress provides preferences.markLastPlayedMedia,
                ) {
                    VayouApp(
                        onPermissionGranted = synchronizer::startSync,
                        onPlayVideo = { request ->
                            startActivity(
                                PlayerActivity.intentFor(
                                    context = this,
                                    uri = request.uri,
                                    title = request.title,
                                    subtitles = request.subtitles,
                                    startAtBeginning = request.startAtBeginning,
                                    queue = request.queue,
                                    queueTitles = request.queueTitles,
                                    isLive = request.isLive,
                                ),
                            )
                        },
                        onPlaySong = { uri, queue ->
                            startActivity(MusicPlayerActivity.intentFor(this, uri, queue))
                        },
                        onPlayNetworkTrack = { uri, title ->
                            startActivity(MusicPlayerActivity.streamIntent(this, uri, title))
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun VayouApp(
    onPermissionGranted: () -> Unit,
    onPlayVideo: (PlaybackTarget) -> Unit,
    onPlaySong: (uri: String, queue: List<String>) -> Unit,
    /** A track on a share: an address and a name, which is all the library knows about it. */
    onPlayNetworkTrack: (uri: String, title: String) -> Unit,
) {
    val permission = rememberPermissionState(storagePermission)
    val granted = permission.status.isGranted

    // Asked for on arrival rather than behind a button: there is nothing to show without it, and a
    // screen whose only content is a request for the thing it needs is a worse way to ask.
    LaunchedEffect(Unit) {
        if (!granted) permission.launchPermissionRequest()
    }

    // The scan runs once the answer is yes, and again on a later yes if the first was no.
    LaunchedEffect(granted) {
        if (granted) onPermissionGranted()
    }

    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(TopLevelDestination.Video) }
    val items = TopLevelDestination.entries.map {
        VayouNavBarItem(icon = it.icon, selectedIcon = it.selectedIcon, label = stringResource(it.label))
    }
    val onSelect: (Int) -> Unit = { destination = TopLevelDestination.entries[it] }

    // Material puts navigation on a side rail once the window is Medium wide. On a phone that is
    // landscape, where a bottom bar would also eat height that is already scarce.
    val useRail = calculateWindowSizeClass(LocalActivity.current as FragmentActivity)
        .widthSizeClass != WindowWidthSizeClass.Compact

    // What each destination scrolled to and had open, kept while another is on screen. Without
    // this, leaving a tab disposes its composition and coming back starts it at the top.
    val stateHolder = rememberSaveableStateHolder()

    // One place for the whole app to say what it just did. Held here rather than per screen: the
    // message outlives the sheet or the dialog that caused it, and a tab change should not cut a
    // sentence in half.
    val messages = rememberVayouMessages()

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            VayouNavRail(items = items, selectedIndex = destination.ordinal, onItemSelected = onSelect)
        }
        VayouScaffold(
            snackbarHost = { VayouMessageHost(messages) },
            modifier = Modifier
                .weight(1f)
                // The rail already padded the leading inset. Marking it consumed stops the screen
                // beside it from padding that side a second time.
                .then(
                    if (useRail) {
                        Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                    } else {
                        Modifier
                    },
                ),
            bottomBar = {
                // Above the navigation, and outside the rail check: on a wide window there is no
                // bottom bar to sit on, and the bar is still where a minimised player lives.
                Column {
                    // Above the music bar: a film on a television is the louder of the two, and
                    // only one of them is ever showing in practice.
                    CastMiniController(
                        onExpand = {
                            // Brought forward rather than opened afresh: the player is already
                            // behind this screen with the film it sent, and a new instance would
                            // ask the service to open it a second time.
                            context.startActivity(
                                Intent(context, PlayerActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                            )
                        },
                    )
                    MusicMiniController()
                    if (!useRail) {
                        VayouNavBar(items = items, selectedIndex = destination.ordinal, onItemSelected = onSelect)
                    }
                }
            },
        ) {
            CompositionLocalProvider(LocalVayouMessages provides messages) {
                stateHolder.SaveableStateProvider(destination) {
                    when (destination) {
                        TopLevelDestination.Video -> LibraryScreen(
                            onPlayVideo = { uri, title -> onPlayVideo(PlaybackTarget(uri, title)) },
                            onPlayFromStart = { uri, title ->
                                onPlayVideo(PlaybackTarget(uri, title, startAtBeginning = true))
                            },
                        )
                        TopLevelDestination.Audio -> MusicScreen(
                            onPlaySong = { song, queue -> onPlaySong(song.uriString, queue.map { it.uriString }) },
                        )
                        TopLevelDestination.Network -> NetworkScreen(
                            onPlay = { uri, title, subtitles ->
                                onPlayVideo(PlaybackTarget(uri, title, subtitles))
                            },
                            onPlayFromStart = { uri, title, subtitles ->
                                onPlayVideo(PlaybackTarget(uri, title, subtitles, startAtBeginning = true))
                            },
                            onPlayChannel = { uri, title, queue, queueTitles ->
                                onPlayVideo(
                                    PlaybackTarget(
                                        uri = uri,
                                        title = title,
                                        queue = queue,
                                        queueTitles = queueTitles,
                                        isLive = true,
                                    ),
                                )
                            },
                            onPlayAudio = onPlayNetworkTrack,
                            onBackAtRoot = { destination = TopLevelDestination.Video },
                        )
                        TopLevelDestination.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}

/**
 * What to open the player on.
 *
 * A type rather than four positional arguments: two of them are lists of strings and a boolean, and
 * a call site that gets them in the wrong order still compiles.
 */
private data class PlaybackTarget(
    val uri: String,
    val title: String,
    val subtitles: List<String> = emptyList(),
    val startAtBeginning: Boolean = false,
    val queue: List<String> = emptyList(),
    val queueTitles: List<String> = emptyList(),
    val isLive: Boolean = false,
)
