package dev.vayou.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The sizes an icon is drawn at, by the job it is doing.
 *
 * - [xs] inline indicator, badge, a chip's leading glyph
 * - [sm] dropdown row, compact toolbar, a list's leading glyph
 * - [md] app bar, navigation, a list's trailing glyph
 * - [lg] the primary glyph of a card
 * - [xl] transport buttons
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
