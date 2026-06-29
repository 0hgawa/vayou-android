package dev.vayou.core.ui.designsystem.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object VayouTheme {
    val colors: VayouColors
        @Composable @ReadOnlyComposable
        get() = LocalVayouColors.current

    val typography: VayouTypography
        @Composable @ReadOnlyComposable
        get() = LocalVayouTypography.current

    val shapes: VayouShapes
        @Composable @ReadOnlyComposable
        get() = LocalVayouShapes.current

    val motion: VayouMotion
        @Composable @ReadOnlyComposable
        get() = LocalVayouMotion.current

    val spacing: VayouSpacing
        @Composable @ReadOnlyComposable
        get() = LocalVayouSpacing.current

    val iconSize: VayouIconSize
        @Composable @ReadOnlyComposable
        get() = LocalVayouIconSize.current
}

@Composable
fun VayouThemeProvider(
    colors: VayouColors = VayouDarkColors,
    typography: VayouTypography = VayouDefaultTypography,
    shapes: VayouShapes = VayouDefaultShapes,
    motion: VayouMotion = VayouDefaultMotion,
    spacing: VayouSpacing = VayouDefaultSpacing,
    iconSize: VayouIconSize = VayouDefaultIconSize,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalVayouColors provides colors,
        LocalVayouTypography provides typography,
        LocalVayouShapes provides shapes,
        LocalVayouMotion provides motion,
        LocalVayouSpacing provides spacing,
        LocalVayouIconSize provides iconSize,
        LocalContentColor provides colors.onSurface,
        LocalTextStyle provides typography.bodyMedium,
        content = content,
    )
}
