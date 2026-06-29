package dev.vayou.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.vayou.core.ui.designsystem.theme.VayouColors
import dev.vayou.core.ui.designsystem.theme.VayouDarkColors
import dev.vayou.core.ui.designsystem.theme.VayouLightColors
import dev.vayou.core.ui.designsystem.theme.VayouPureBlackColors
import dev.vayou.core.ui.designsystem.theme.VayouThemeProvider
import dev.vayou.core.ui.designsystem.theme.supportsDynamicColors
import dev.vayou.core.ui.designsystem.theme.vayouDynamicColors

@Composable
fun VayouPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastDarkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colors = when {
        dynamicColor && supportsDynamicColors() -> vayouDynamicColors(context, darkTheme, highContrastDarkTheme)
        darkTheme && highContrastDarkTheme -> VayouPureBlackColors
        darkTheme -> VayouDarkColors
        else -> VayouLightColors
    }

    // Compatibility bridge: M3 components still in use read from MaterialTheme.
    // Remove this layer once all M3 components are replaced by Vayou equivalents.
    val m3Scheme = if (darkTheme) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
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

    MaterialTheme(colorScheme = m3Scheme) {
        VayouThemeProvider(colors = colors, content = content)
    }
}
