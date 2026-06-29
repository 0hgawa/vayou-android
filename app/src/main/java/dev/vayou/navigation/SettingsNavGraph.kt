package dev.vayou.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import dev.vayou.settings.Setting
import dev.vayou.settings.navigation.aboutPreferencesScreen
import dev.vayou.settings.navigation.appearancePreferencesScreen
import dev.vayou.settings.navigation.audioPreferencesScreen
import dev.vayou.settings.navigation.decoderPreferencesScreen
import dev.vayou.settings.navigation.folderPreferencesScreen
import dev.vayou.settings.navigation.generalPreferencesScreen
import dev.vayou.settings.navigation.librariesScreen
import dev.vayou.settings.navigation.mediaLibraryPreferencesScreen
import dev.vayou.settings.navigation.navigateToAboutPreferences
import dev.vayou.settings.navigation.navigateToAppearancePreferences
import dev.vayou.settings.navigation.gesturePreferencesScreen
import dev.vayou.settings.navigation.navigateToAudioPreferences
import dev.vayou.settings.navigation.navigateToDecoderPreferences
import dev.vayou.settings.navigation.navigateToGesturePreferences
import dev.vayou.settings.navigation.navigateToFolderPreferencesScreen
import dev.vayou.settings.navigation.navigateToGeneralPreferences
import dev.vayou.settings.navigation.navigateToLibraries
import dev.vayou.settings.navigation.navigateToMediaLibraryPreferencesScreen
import dev.vayou.settings.navigation.navigateToPlayerPreferences
import dev.vayou.settings.navigation.navigateToSubtitlePreferences
import dev.vayou.settings.navigation.navigateToThumbnailPreferencesScreen
import dev.vayou.settings.navigation.playerPreferencesScreen
import dev.vayou.settings.navigation.settingsNavigationRoute
import dev.vayou.settings.navigation.settingsScreen
import dev.vayou.settings.navigation.subtitlePreferencesScreen
import dev.vayou.settings.navigation.thumbnailPreferencesScreen

const val SETTINGS_ROUTE = "settings_nav_route"

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
) {
    navigation(
        startDestination = settingsNavigationRoute,
        route = SETTINGS_ROUTE,
    ) {
        settingsScreen(
            onNavigateUp = navController::navigateUp,
            onItemClick = { setting ->
                when (setting) {
                    Setting.APPEARANCE -> navController.navigateToAppearancePreferences()
                    Setting.MEDIA_LIBRARY -> navController.navigateToMediaLibraryPreferencesScreen()
                    Setting.PLAYER -> navController.navigateToPlayerPreferences()
                    Setting.GESTURES -> navController.navigateToGesturePreferences()
                    Setting.DECODER -> navController.navigateToDecoderPreferences()
                    Setting.AUDIO -> navController.navigateToAudioPreferences()
                    Setting.SUBTITLE -> navController.navigateToSubtitlePreferences()
                    Setting.GENERAL -> navController.navigateToGeneralPreferences()
                    Setting.ABOUT -> navController.navigateToAboutPreferences()
                }
            },
        )
        appearancePreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        mediaLibraryPreferencesScreen(
            onNavigateUp = navController::navigateUp,
            onFolderSettingClick = navController::navigateToFolderPreferencesScreen,
            onThumbnailSettingClick = navController::navigateToThumbnailPreferencesScreen,
        )
        thumbnailPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        folderPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        playerPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        gesturePreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        decoderPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        audioPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        subtitlePreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        generalPreferencesScreen(
            onNavigateUp = navController::navigateUp,
        )
        aboutPreferencesScreen(
            onLibrariesClick = navController::navigateToLibraries,
            onNavigateUp = navController::navigateUp,
        )
        librariesScreen(
            onNavigateUp = navController::navigateUp,
        )
    }
}
