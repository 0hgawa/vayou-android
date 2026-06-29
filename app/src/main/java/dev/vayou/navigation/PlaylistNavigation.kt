package dev.vayou.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import dev.vayou.feature.player.PlayerActivity
import dev.vayou.feature.player.utils.PlayerApi
import dev.vayou.playlist.FavoritesScreen
import dev.vayou.playlist.PlaylistDetailScreen
import dev.vayou.playlist.PlaylistScreen
import dev.vayou.playlist.PrivateScreen

const val PLAYLIST_GRAPH_ROUTE = "playlist_graph"
private const val PLAYLIST_ROUTE = "playlist_route"
private const val PLAYLIST_DETAIL_ROUTE = "playlist_detail"
private const val FAVORITES_ROUTE = "favorites_route"
private const val PRIVATE_ROUTE = "private_route"

fun NavGraphBuilder.playlistGraph(navController: NavController) {
    navigation(startDestination = PLAYLIST_ROUTE, route = PLAYLIST_GRAPH_ROUTE) {
        composable(PLAYLIST_ROUTE) {
            PlaylistScreen(
                onPlaylistClick = { id -> navController.navigate("$PLAYLIST_DETAIL_ROUTE/$id") },
                onFavoritesClick = { navController.navigate(FAVORITES_ROUTE) },
                onPrivateClick = { navController.navigate(PRIVATE_ROUTE) },
            )
        }
        composable("$PLAYLIST_DETAIL_ROUTE/{playlistId}") {
            val context = LocalContext.current
            PlaylistDetailScreen(
                onNavigateUp = { navController.navigateUp() },
                onPlayVideos = { context.startPlayer(it) },
            )
        }
        composable(FAVORITES_ROUTE) {
            val context = LocalContext.current
            FavoritesScreen(
                onNavigateUp = { navController.navigateUp() },
                onPlayVideos = { context.startPlayer(it) },
            )
        }
        composable(PRIVATE_ROUTE) {
            val context = LocalContext.current
            PrivateScreen(
                onNavigateUp = { navController.navigateUp() },
                onPlayVideos = { context.startPlayer(it) },
            )
        }
    }
}

private fun Context.startPlayer(uris: List<Uri>) {
    startActivity(
        Intent(this, PlayerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uris.first()
            putParcelableArrayListExtra(PlayerApi.API_PLAYLIST, ArrayList(uris))
        },
    )
}
