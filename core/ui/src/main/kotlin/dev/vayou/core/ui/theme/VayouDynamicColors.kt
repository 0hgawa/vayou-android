package dev.vayou.core.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.lerp

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicColors(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** How much of the system's palette a screen takes. */
enum class VayouDynamicColor {
    /** None of it: the app's own amber on the app's own greys. */
    None,

    /**
     * The accent only -- what is drawn *on* things: a slider, an active icon, a picked tile. The
     * surfaces underneath stay the app's.
     *
     * For the players. Their panels lie over content whose colour the app does not choose, a frame
     * of video or an album cover, so a background from the wallpaper lands on top of one and the
     * contrast becomes a matter of luck. The accent has no such problem.
     */
    Accent,

    /** All of it, surfaces included. For the library screens, which have no content of their own
     *  for a system colour to clash with. */
    Full,
}

// Says out loud what the guard above already enforces: the schemes it reads are Android 12's, and
// a caller that has not asked [supportsDynamicColors] first has no business here.
@RequiresApi(Build.VERSION_CODES.S)
fun vayouDynamicColors(context: Context, isDark: Boolean, highContrast: Boolean = false): VayouColors {
    val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val base = if (highContrast && isDark) VayouPureBlackColors else null

    return VayouColors(
        accent = scheme.primary,
        // primaryFixedDim holds one value across the light and the dark scheme, which is the whole
        // requirement: the things that take it should stay the colour they are.
        accentFixed = scheme.primaryFixedDim,
        // Stepped off the front rather than picked from the palette. Every role deep enough to read
        // as the tab is far deeper than the front -- primary is forty tones below primaryFixedDim in
        // a light scheme, which came out as a near-brown flap on a pale folder. Both ends of this
        // are fixed tones of one hue, so the step is in depth and never in colour.
        folderTabColor = lerp(scheme.primaryFixedDim, scheme.onPrimaryFixedVariant, FolderTabStep),
        onAccent = scheme.onPrimary,
        accentContainer = scheme.primaryContainer,
        onAccentContainer = scheme.onPrimaryContainer,
        background = base?.background ?: scheme.background,
        onBackground = scheme.onBackground,
        surface = base?.surface ?: scheme.surface,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant,
        surfaceContainer = base?.surfaceContainer ?: scheme.surfaceContainer,
        surfaceContainerHigh = base?.surfaceContainerHigh ?: scheme.surfaceContainerHigh,
        surfaceContainerHighest = base?.surfaceContainerHighest ?: scheme.surfaceContainerHighest,
        surfaceDim = base?.surfaceDim ?: scheme.surfaceDim,
        surfaceBright = base?.surfaceBright ?: scheme.surfaceBright,
        outline = scheme.outline,
        outlineVariant = scheme.outlineVariant,
        error = scheme.error,
        onError = scheme.onError,
        errorContainer = scheme.errorContainer,
        onErrorContainer = scheme.onErrorContainer,
        inverseSurface = scheme.inverseSurface,
        inverseOnSurface = scheme.inverseOnSurface,
        scrim = scheme.scrim,
        onDisabled = scheme.outline,
    )
}

/** How far the folder's tab sits below its front. Enough to part the panels, little enough that
 *  they stay one object at the size a row draws them. */
private const val FolderTabStep = 0.25f
