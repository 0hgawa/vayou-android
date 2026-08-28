package dev.vayou.tv

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import dev.vayou.core.common.audioPermission
import dev.vayou.core.common.storagePermission
import dev.vayou.core.media.sync.MediaSynchronizer
import dev.vayou.tv.library.TvLibraryScreen
import dev.vayou.tv.music.TvMusicScreen
import dev.vayou.tv.music.TvNetworkAudioScreen
import dev.vayou.tv.music.TvNetworkAudioViewModel
import dev.vayou.tv.network.TvChannelsScreen
import dev.vayou.tv.network.TvServerScreen
import dev.vayou.tv.network.TvServerViewModel
import dev.vayou.tv.network.UrlArg
import dev.vayou.tv.player.TvPlayerScreen
import dev.vayou.tv.player.TvPlayerViewModel
import dev.vayou.tv.settings.TvSettingsScreen
import javax.inject.Inject

/**
 * The one screen the television has, with everything else drawn inside it.
 *
 * Landscape and nothing else, declared in the manifest: a television has one orientation and a
 * layout that can be turned is a layout with a case that will never happen in it.
 */
@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    @Inject
    lateinit var synchronizer: MediaSynchronizer

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvTheme {
                // Both media kinds in one ask. They are one permission below Android 13, and above
                // it a viewer who says yes to films and is then asked again about music has been
                // asked twice for the same thing.
                val media = rememberMultiplePermissionsState(listOf(storagePermission, audioPermission).distinct())
                val canReadVideo = media.permissions.first().status.isGranted

                // Asked on every arrival rather than once at startup: a television is left on for
                // days, and the one refusal a viewer made by accident should not be the last word.
                LifecycleEventEffect(Lifecycle.Event.ON_START) {
                    if (!media.allPermissionsGranted) media.launchMultiplePermissionRequest()
                }
                LaunchedEffect(canReadVideo) {
                    if (canReadVideo) synchronizer.startSync()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .ignoringOrphanPress(),
                ) {
                    if (canReadVideo) {
                        TvNavigation()
                    } else {
                        PermissionMissing()
                    }
                }
            }
        }
    }
}

/**
 * The two screens there are so far, and the way between them.
 *
 * A film's address travels as one segment of the route rather than as something held on the side,
 * so a player rebuilt by the system knows what it was showing without anyone having kept it.
 */
@Composable
private fun TvNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = RouteHome) {
        composable(RouteHome) {
            TvHomeScreen(
                onPlayVideo = { video -> navController.navigate(playerRoute(video.uriString)) },
                onPlayNetwork = { uri -> navController.navigate(playerRoute(uri)) },
                onOpenServer = { host -> navController.navigate(serverRoute(host)) },
                onOpenFolder = { folder ->
                    navController.navigate(serverRoute(folder.host, folder.share, folder.path))
                },
                onOpenPlaylist = { playlist -> navController.navigate(channelsRoute(playlist.url)) },
                // No address: the screen falls back to whatever list it has, and what it shows is
                // the starred of all of them regardless.
                onOpenStarredChannels = { navController.navigate("channels") },
                onOpenLibrary = { navController.navigate(libraryRoute()) },
                onOpenMusic = { navController.navigate(RouteMusic) },
                onOpenSettings = { navController.navigate(RouteSettings) },
                onSearch = { navController.navigate(libraryRoute(startSearching = true)) },
            )
        }
        composable(
            route = "$RouteLibrary?$SearchArg={$SearchArg}",
            arguments = listOf(
                navArgument(SearchArg) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            TvLibraryScreen(
                onPlayVideo = { video, isFromStart ->
                    navController.navigate(playerRoute(video.uriString, isFromStart = isFromStart))
                },
                onBack = { navController.popBackStack() },
                startSearching = entry.arguments?.getBoolean(SearchArg) == true,
            )
        }
        composable(RouteMusic) { TvMusicScreen(onBack = { navController.popBackStack() }) }
        composable(RouteSettings) { TvSettingsScreen(onBack = { navController.popBackStack() }) }
        composable(
            route = "server/{${TvServerViewModel.HostArg}}" +
                "?${TvServerViewModel.ShareArg}={${TvServerViewModel.ShareArg}}" +
                "&${TvServerViewModel.PathArg}={${TvServerViewModel.PathArg}}",
            arguments = listOf(
                navArgument(TvServerViewModel.HostArg) { type = NavType.StringType },
                navArgument(TvServerViewModel.ShareArg) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(TvServerViewModel.PathArg) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            TvServerScreen(
                onPlayVideo = { uri -> navController.navigate(playerRoute(uri)) },
                onPlayAudio = { uri -> navController.navigate(audioRoute(uri)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "channels?$UrlArg={$UrlArg}",
            // Optional, because the row of saved lists opens one by name and the card at the foot of
            // the home screen opens whichever was last used. The screen falls back to the first list
            // it has when it is handed nothing.
            arguments = listOf(
                navArgument(UrlArg) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            // A channel is played as itself and not as one of a queue: the list runs to hundreds,
            // and a receiver handed all of them walks them a failure at a time.
            TvChannelsScreen(
                onPlay = { channel -> navController.navigate(playerRoute(channel.url, isLive = true)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "audio/{${TvNetworkAudioViewModel.UriArg}}",
            arguments = listOf(navArgument(TvNetworkAudioViewModel.UriArg) { type = NavType.StringType }),
        ) {
            TvNetworkAudioScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "player/{${TvPlayerViewModel.VideoUriArg}}" +
                "?live={${TvPlayerViewModel.IsLiveArg}}&start={${TvPlayerViewModel.FromStartArg}}",
            arguments = listOf(
                navArgument(TvPlayerViewModel.VideoUriArg) { type = NavType.StringType },
                navArgument(TvPlayerViewModel.IsLiveArg) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                // Declared, and not merely written into the route above. An argument the route
                // names but does not type arrives as the text "true", and a view model that reads
                // it as the boolean it plainly looks like is handed a String -- which is a cast
                // that throws before the player has drawn anything. Every film goes through here.
                navArgument(TvPlayerViewModel.FromStartArg) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            TvPlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}

private const val RouteHome = "home"

private const val RouteLibrary = "library"

private const val SearchArg = "search"

/** [startSearching] opens the shelf with its box already up, for the magnifier on the home screen. */
private fun libraryRoute(startSearching: Boolean = false): String = "$RouteLibrary?$SearchArg=$startSearching"

private const val RouteMusic = "music"

private const val RouteSettings = "settings"

/**
 * [isLive] travels with the address because the caller knows and the player cannot.
 *
 * Whether a stream is live is only readable once its manifest has arrived, so a player told nothing
 * opens as a film -- a scrub bar, a clock, skip buttons -- and rearranges itself into a channel a
 * second later in front of the viewer.
 */
private fun playerRoute(uri: String, isLive: Boolean = false, isFromStart: Boolean = false): String =
    "player/${Uri.encode(uri)}?live=$isLive&start=$isFromStart"

/**
 * [share] and [path] are where to land, for a folder pinned to the home screen.
 *
 * Encoded once here and decoded once by navigation on the way in, which is the whole of the
 * agreement: a Windows path is full of backslashes and a file's own name can hold a `#`, and either
 * one spelled straight into a route would be read as part of the route instead.
 */
private fun serverRoute(host: String, share: String = "", path: String = ""): String =
    "server/${Uri.encode(host)}?share=${Uri.encode(share)}&path=${Uri.encode(path)}"

/** Music has a screen of its own, as it does on the phone: a cover, not a black rectangle. */
private fun audioRoute(uri: String): String = "audio/${Uri.encode(uri)}"

private fun channelsRoute(url: String): String = "channels?$UrlArg=${Uri.encode(url)}"

/**
 * What the screen says when it may not read anything.
 *
 * A sentence rather than a button: the request is already on screen when this is, and a television
 * has no settings app a viewer can be sent to with a remote in one hand.
 */
@Composable
private fun PermissionMissing() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MessageInset),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.permission_needed),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val MessageInset = 48.dp
