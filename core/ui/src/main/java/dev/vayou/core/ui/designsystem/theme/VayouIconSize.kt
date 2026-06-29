package dev.vayou.core.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Semantic icon-size scale. Use this instead of hardcoded `Modifier.size(N.dp)` so similar
 * roles render at consistent sizes across screens.
 *
 * - [xs] tiny inline indicators, chip leading icons, badges
 * - [sm] dropdown items, compact toolbars, list leading icons
 * - [md] standard Material icons (app bars, navigation, list trailing)
 * - [lg] prominent list/card primary icons
 * - [xl] hero action buttons (play/pause, FAB-style)
 */
@Immutable
data class VayouIconSize(
    val xs: Dp = 16.dp,
    val sm: Dp = 20.dp,
    val md: Dp = 24.dp,
    val lg: Dp = 28.dp,
    val xl: Dp = 48.dp,
)

val VayouDefaultIconSize = VayouIconSize()

val LocalVayouIconSize = staticCompositionLocalOf { VayouDefaultIconSize }
