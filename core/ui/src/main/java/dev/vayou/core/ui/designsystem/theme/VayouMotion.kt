package dev.vayou.core.ui.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class VayouMotion(
    val durationShort: Int,
    val durationMedium: Int,
    val durationLong: Int,
    val easingStandard: Easing,
    val easingDecelerate: Easing,
    val easingAccelerate: Easing,
)

val VayouDefaultMotion = VayouMotion(
    durationShort = 150,
    durationMedium = 300,
    durationLong = 500,
    easingStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    easingDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f),
    easingAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f),
)

val LocalVayouMotion = staticCompositionLocalOf { VayouDefaultMotion }
