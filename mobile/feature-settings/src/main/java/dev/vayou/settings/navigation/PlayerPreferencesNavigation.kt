package dev.vayou.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import dev.vayou.settings.screens.player.PlayerPreferencesScreen

const val playerPreferencesNavigationRoute = "player_preferences_route"

fun NavController.navigateToPlayerPreferences(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(playerPreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.playerPreferencesScreen(onNavigateUp: () -> Unit) {
    composable(route = playerPreferencesNavigationRoute) {
        PlayerPreferencesScreen(
            onNavigateUp = onNavigateUp,
        )
    }
}
