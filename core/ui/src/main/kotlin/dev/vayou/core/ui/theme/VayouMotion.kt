package dev.vayou.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/** How long a change takes and how it accelerates. Milliseconds, because that is what Compose asks for. */
@Immutable
data class VayouMotion(
    val durationShort: Int = 150,
    val durationMedium: Int = 300,
    val durationLong: Int = 500,
    /**
     * How long a title that overflows stands still before it starts to travel.
     *
     * Long enough to be read from the start. A line that begins moving the moment it appears is one
     * the eye has to chase, and a mini player redraws its title on every track.
     */
    val marqueeDelay: Int = 1_200,
    /** Something moving from one place to another on screen. */
    val easingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    /** Something arriving: fast in, settling. */
    val easingDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f),
    /** Something leaving: slow to start, gone quickly. */
    val easingAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f),
)

val VayouDefaultMotion = VayouMotion()

val LocalVayouMotion = staticCompositionLocalOf { VayouDefaultMotion }
