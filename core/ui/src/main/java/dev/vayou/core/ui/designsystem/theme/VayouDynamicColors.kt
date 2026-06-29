package dev.vayou.core.ui.designsystem.theme

import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicColors(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun vayouDynamicColors(context: Context, isDark: Boolean, highContrast: Boolean = false): VayouColors {
    val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val pureBlack = highContrast && isDark
    return VayouColors(
        accent = scheme.primary,
        folderColor = scheme.primaryFixedDim,
        accentOnBackground = scheme.primary,
        onAccent = scheme.onPrimary,
        accentContainer = scheme.primaryContainer,
        onAccentContainer = scheme.onPrimaryContainer,
        background = if (pureBlack) VayouPureBlackColors.background else scheme.background,
        onBackground = scheme.onBackground,
        surface = if (pureBlack) VayouPureBlackColors.surface else scheme.surface,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant,
        surfaceContainer = if (pureBlack) VayouPureBlackColors.surfaceContainer else scheme.surfaceContainer,
        surfaceContainerHigh = if (pureBlack) VayouPureBlackColors.surfaceContainerHigh else scheme.surfaceContainerHigh,
        surfaceContainerHighest = if (pureBlack) VayouPureBlackColors.surfaceContainerHighest else scheme.surfaceContainerHighest,
        surfaceDim = if (pureBlack) VayouPureBlackColors.surfaceDim else scheme.surfaceDim,
        surfaceBright = if (pureBlack) VayouPureBlackColors.surfaceBright else scheme.surfaceBright,
        outline = scheme.outline,
        outlineVariant = scheme.outlineVariant,
        divider = scheme.outlineVariant,
        error = scheme.error,
        onError = scheme.onError,
        errorContainer = scheme.errorContainer,
        onErrorContainer = scheme.onErrorContainer,
        inverseSurface = scheme.inverseSurface,
        inverseOnSurface = scheme.inverseOnSurface,
        scrim = scheme.scrim,
        disabled = scheme.outlineVariant,
        onDisabled = scheme.outline,
    )
}
