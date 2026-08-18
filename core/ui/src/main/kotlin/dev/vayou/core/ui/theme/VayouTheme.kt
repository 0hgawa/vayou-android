package dev.vayou.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Reads the tokens in scope. `VayouTheme.colors.surface` inside anything the [VayouTheme] composable
 * wraps.
 */
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

/**
 * Puts the app's tokens in scope, and Material's beside them for the components still taken from
 * there.
 */
@Composable
fun VayouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastDarkTheme: Boolean = false,
    dynamicColor: VayouDynamicColor = VayouDynamicColor.None,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Built once and kept: the palette moves only when the wallpaper or a preference does, and this
    // sits above every screen in the app.
    val colors = remember(darkTheme, highContrastDarkTheme, dynamicColor) {
        val base = when {
            darkTheme && highContrastDarkTheme -> VayouPureBlackColors
            darkTheme -> VayouDarkColors
            else -> VayouLightColors
        }
        if (dynamicColor == VayouDynamicColor.None || !supportsDynamicColors()) {
            base
        } else {
            val dynamic = vayouDynamicColors(context, darkTheme, highContrastDarkTheme)
            if (dynamicColor == VayouDynamicColor.Full) dynamic else base.withAccentFrom(dynamic)
        }
    }

    // Remembered, and more urgently than the palette: this builds a scheme of forty colours and
    // hands it to a CompositionLocal. Rebuilt on every recomposition it would be a new instance
    // each time, invalidating every reader of MaterialTheme.colorScheme below -- the whole app.
    //
    // Copied onto the matching factory rather than written out per theme: the two schemes differ in
    // which defaults they start from and in nothing else.
    val materialScheme = remember(colors, darkTheme) {
        val defaults = if (darkTheme) darkColorScheme() else lightColorScheme()
        defaults.copy(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentContainer,
            onPrimaryContainer = colors.onAccentContainer,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainerHigh,
            surfaceContainerHighest = colors.surfaceContainerHighest,
            surfaceDim = colors.surfaceDim,
            surfaceBright = colors.surfaceBright,
            outline = colors.outline,
            outlineVariant = colors.outlineVariant,
            error = colors.error,
            onError = colors.onError,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.onErrorContainer,
            inverseSurface = colors.inverseSurface,
            inverseOnSurface = colors.inverseOnSurface,
            scrim = colors.scrim,
        )
    }

    MaterialTheme(colorScheme = materialScheme) {
        CompositionLocalProvider(
            LocalVayouColors provides colors,
            LocalVayouTypography provides VayouDefaultTypography,
            LocalVayouShapes provides VayouDefaultShapes,
            LocalVayouMotion provides VayouDefaultMotion,
            LocalVayouSpacing provides VayouDefaultSpacing,
            LocalVayouIconSize provides VayouDefaultIconSize,
            LocalContentColor provides colors.onSurface,
            LocalTextStyle provides VayouDefaultTypography.bodyMedium,
            content = content,
        )
    }
}
