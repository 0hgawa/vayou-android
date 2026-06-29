package dev.vayou.tv

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import dev.vayou.core.common.storagePermission
import dev.vayou.core.media.services.MediaService
import dev.vayou.core.media.sync.MediaSynchronizer
import dev.vayou.tv.feature.player.TvPlayerScreen
import dev.vayou.tv.feature.player.TvPlayerViewModel
import dev.vayou.tv.screens.FolderFavoritesScreen
import dev.vayou.tv.screens.HomeScreen
import dev.vayou.tv.screens.IptvFavoritesScreen
import dev.vayou.tv.screens.PlaylistDetailScreen
import dev.vayou.tv.screens.PlaylistDetailViewModel
import dev.vayou.tv.screens.SmbBrowserScreen
import dev.vayou.tv.screens.SmbBrowserViewModel
import dev.vayou.tv.screens.TvSettingsScreen
import javax.inject.Inject

private const val RouteHome = "home"
private const val RouteSettings = "settings"
private const val RoutePlayer = "player/{${TvPlayerViewModel.VideoUriArg}}"
private const val RouteSmbBrowser =
    "smb/{${SmbBrowserViewModel.HostArg}}?share={${SmbBrowserViewModel.ShareArg}}&path={${SmbBrowserViewModel.PathArg}}"
private const val RoutePlaylist = "playlist/{${PlaylistDetailViewModel.UrlArg}}"
private const val RouteIptvFavorites = "favorites/iptv"
private const val RouteFolderFavorites = "favorites/folder"

private fun playerRoute(uri: String): String = "player/${Uri.encode(uri)}"
private fun smbRoute(host: String, share: String? = null, path: String? = null): String = buildString {
    append("smb/").append(Uri.encode(host))
    val q = listOfNotNull(
        share?.takeIf { it.isNotBlank() }?.let { "share=${Uri.encode(it)}" },
        path?.takeIf { it.isNotBlank() }?.let { "path=${Uri.encode(it)}" },
    )
    if (q.isNotEmpty()) append("?").append(q.joinToString("&"))
}
private fun playlistRoute(url: String): String = "playlist/${Uri.encode(url)}"

private val VayouAmber = Color(0xFFFFB300)

private val NetflixDarkScheme = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF0F0F0F),
    primary = VayouAmber,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF2C2C2C),
    onPrimaryContainer = VayouAmber,
    secondary = VayouAmber,
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF2C2C2C),
    onSecondaryContainer = VayouAmber,
)

@AndroidEntryPoint
class TvMainActivity : ComponentActivity() {

    @Inject lateinit var synchronizer: MediaSynchronizer
    @Inject lateinit var mediaService: MediaService

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaService.initialize(this)
        setContent {
            MaterialTheme(colorScheme = NetflixDarkScheme) {
                LaunchedEffect(Unit) { reportFullyDrawn() }
                val permission = rememberPermissionState(storagePermission)

                LifecycleEventEffect(Lifecycle.Event.ON_START) {
                    if (!permission.status.isGranted) permission.launchPermissionRequest()
                }
                LaunchedEffect(permission.status.isGranted) {
                    if (permission.status.isGranted) synchronizer.startSync()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    if (permission.status.isGranted) {
                        TvApp()
                    } else {
                        PermissionMissing()
                    }
                }
            }
        }
    }
}

@Composable
private fun TvApp() {
    val navController = rememberNavController()
    TvNavGraph(navController = navController, modifier = Modifier.fillMaxSize())
}

@Composable
private fun TvNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = RouteHome,
        modifier = modifier,
    ) {
        composable(RouteHome) {
            HomeScreen(
                onPlayUri = { uri -> navController.navigate(playerRoute(uri)) },
                onOpenServer = { server -> navController.navigate(smbRoute(server.host)) },
                onConnectServer = { host -> navController.navigate(smbRoute(host)) },
                onOpenFolderFavorites = { navController.navigate(RouteFolderFavorites) },
                onOpenPlaylist = { playlist -> navController.navigate(playlistRoute(playlist.url)) },
                onOpenIptvFavorites = { navController.navigate(RouteIptvFavorites) },
                onSettingsClick = { navController.navigate(RouteSettings) },
            )
        }
        composable(RouteSettings) {
            TvSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = RoutePlayer,
            arguments = listOf(
                navArgument(TvPlayerViewModel.VideoUriArg) { type = NavType.StringType },
            ),
        ) {
            TvPlayerScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = RouteSmbBrowser,
            arguments = listOf(
                navArgument(SmbBrowserViewModel.HostArg) { type = NavType.StringType },
                navArgument(SmbBrowserViewModel.ShareArg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(SmbBrowserViewModel.PathArg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            SmbBrowserScreen(
                onPlay = { uri -> navController.navigate(playerRoute(uri.toString())) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = RoutePlaylist,
            arguments = listOf(
                navArgument(PlaylistDetailViewModel.UrlArg) { type = NavType.StringType },
            ),
        ) {
            PlaylistDetailScreen(
                onPlay = { channel -> navController.navigate(playerRoute(channel.url)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(RouteIptvFavorites) {
            IptvFavoritesScreen(
                onPlay = { channel -> navController.navigate(playerRoute(channel.url)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(RouteFolderFavorites) {
            FolderFavoritesScreen(
                onOpen = { fav -> navController.navigate(smbRoute(fav.host, fav.share, fav.path)) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun PermissionMissing() {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Permissão de armazenamento necessária",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Conceda a permissão para listar seus vídeos.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
