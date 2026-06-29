package dev.vayou.feature.videopicker.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.vayou.feature.videopicker.screens.networkbrowser.NetworkBrowserScreen

const val NETWORK_BROWSER_ROUTE = "network_browser_route"

fun NavGraphBuilder.networkBrowserScreen(
    onPlayVideo: (Uri, List<Uri>) -> Unit,
    onPlayVideoFromStart: (Uri, List<Uri>) -> Unit,
    onBackAtRoot: () -> Unit,
) {
    composable(NETWORK_BROWSER_ROUTE) {
        NetworkBrowserScreen(
            onPlayVideo = onPlayVideo,
            onPlayVideoFromStart = onPlayVideoFromStart,
            onBackAtRoot = onBackAtRoot,
        )
    }
}
