package dev.vayou

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vayou.core.ui.designsystem.VayouIcons

/**
 * The four places the app can be, in the order they appear along the bottom.
 *
 * An enum and not a list of routes: the bar draws them in order, the index it reports is the
 * ordinal, and what is on screen is decided by `when` over these. Nothing here is a string that
 * could be mistyped.
 */
enum class TopLevelDestination(val icon: ImageVector, val selectedIcon: ImageVector, @param:StringRes val label: Int) {
    Video(VayouIcons.VideoLibrary, VayouIcons.VideoLibraryFilled, R.string.destination_video),
    Audio(VayouIcons.Audio, VayouIcons.AudioFilled, R.string.destination_audio),
    Network(VayouIcons.Network, VayouIcons.NetworkFilled, R.string.destination_network),
    Settings(VayouIcons.Settings, VayouIcons.SettingsFilled, R.string.destination_settings),
}
