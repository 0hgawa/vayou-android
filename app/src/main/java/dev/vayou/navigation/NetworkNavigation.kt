package dev.vayou.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import dev.vayou.feature.player.PlayerActivity
import dev.vayou.feature.videopicker.navigation.NETWORK_BROWSER_ROUTE
import dev.vayou.feature.videopicker.navigation.networkBrowserScreen

const val NETWORK_ROUTE = NETWORK_BROWSER_ROUTE

fun NavController.navigateToNetwork(navOptions: NavOptions? = null) {
    navigate(NETWORK_ROUTE, navOptions)
}

fun NavGraphBuilder.networkScreen(
    context: Context,
    onBackAtRoot: () -> Unit,
) {
    fun launchPlayer(uri: Uri, subtitleUris: List<Uri>, fromStart: Boolean) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            if (fromStart) putExtra("position", 0)
            if (subtitleUris.isNotEmpty()) {
                putParcelableArrayListExtra("subs", ArrayList(subtitleUris))
                putParcelableArrayListExtra("subs.enable", arrayListOf(subtitleUris.first()))
            }
        }
        context.startActivity(intent)
    }
    networkBrowserScreen(
        onPlayVideo = { uri, subs -> launchPlayer(uri, subs, fromStart = false) },
        onPlayVideoFromStart = { uri, subs -> launchPlayer(uri, subs, fromStart = true) },
        onBackAtRoot = onBackAtRoot,
    )
}
