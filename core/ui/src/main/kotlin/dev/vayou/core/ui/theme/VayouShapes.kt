package dev.vayou.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** How round a corner is, by the size of the thing it belongs to. */
@Immutable
data class VayouShapes(
    val extraSmall: RoundedCornerShape = RoundedCornerShape(4.dp),
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val largeIncreased: RoundedCornerShape = RoundedCornerShape(20.dp),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(28.dp),
    /** A capsule: half the height, whatever the height turns out to be. */
    val full: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

val VayouDefaultShapes = VayouShapes()

val LocalVayouShapes = staticCompositionLocalOf { VayouDefaultShapes }
