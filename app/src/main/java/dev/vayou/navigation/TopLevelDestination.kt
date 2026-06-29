package dev.vayou.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.VayouIcons

enum class TopLevelDestination(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    @StringRes val labelRes: Int,
) {
    HOME(VayouIcons.Home, VayouIcons.HomeFilled, R.string.home),
    NETWORK(VayouIcons.Network, VayouIcons.NetworkFilled, R.string.network),
    PLAYLIST(VayouIcons.Playlist, VayouIcons.PlaylistFilled, R.string.playlists),
    SETTINGS(VayouIcons.Settings, VayouIcons.SettingsFilled, R.string.settings),
}
