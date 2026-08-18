package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * What a row looks like while it is being dragged: lifted off the list.
 *
 * A shadow and a plate under it, because a row being moved has to stop being part of the column it
 * came from. Without it the reorder works and reads as nothing happening -- the rows below shuffle
 * about and the one under the finger looks like all the others.
 *
 * The plate is opaque on purpose: a sheet's rows are transparent over its own surface, and a
 * translucent row lifted over its neighbours shows them through itself.
 */
@Composable
fun Modifier.draggedLift(isDragging: Boolean): Modifier {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) LiftHeight else 0.dp,
        animationSpec = tween(LiftMs),
        label = "dragLift",
    )
    return if (elevation == 0.dp) {
        this
    } else {
        this
            .shadow(elevation, VayouTheme.shapes.medium)
            .background(VayouTheme.colors.surfaceContainerHigh, VayouTheme.shapes.medium)
    }
}

/** High enough to read as picked up, low enough not to look like a dialog. */
private val LiftHeight = 6.dp

private const val LiftMs = 120
