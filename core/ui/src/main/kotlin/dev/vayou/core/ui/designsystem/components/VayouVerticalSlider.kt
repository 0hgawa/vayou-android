package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A slider drawn on its side, for a set of them read as a curve.
 *
 * A `Canvas` and not a rotated [VayouSlider]: ten of these across a phone are ten pixels of track
 * each, and Material's slider brings a 48dp thumb, a value label and a ripple to every one of them.
 * What is wanted here is a line and a dot, and the whole row of them redrawn on every frame of a
 * drag.
 *
 * [showCenterHighlight] fills from zero rather than from the bottom, because a band is read as cut
 * or lifted from flat, not as an amount out of a total.
 */
@Composable
fun VayouVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    showCenterHighlight: Boolean = false,
) {
    val range = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)

    val activeColor = VayouTheme.colors.accent
    val inactiveColor = VayouTheme.colors.surfaceVariant
    val thumbColor = VayouTheme.colors.onSurface

    Canvas(
        modifier = modifier.pointerInput(valueRange) {
            detectVerticalDragGestures(onDragEnd = { onValueChangeFinished?.invoke() }) { change, _ ->
                change.consume()
                val inset = ThumbRadius.toPx()
                val travel = (size.height - inset * 2f).coerceAtLeast(1f)
                val dragged = (1f - (change.position.y - inset) / travel).coerceIn(0f, 1f)
                onValueChange(valueRange.start + dragged * range)
            }
        },
    ) {
        val trackWidth = TrackWidth.toPx()
        val centreX = size.width / 2f
        val trackX = centreX - trackWidth / 2f
        val corner = CornerRadius(trackWidth / 2f)
        val thumbRadius = ThumbRadius.toPx()
        val trackHeight = (size.height - thumbRadius * 2f).coerceAtLeast(0f)
        val thumbY = thumbRadius + trackHeight * (1f - fraction)

        drawRoundRect(
            color = inactiveColor,
            topLeft = Offset(trackX, thumbRadius),
            size = Size(trackWidth, trackHeight),
            cornerRadius = corner,
        )

        if (showCenterHighlight) {
            val zeroFraction = ((0f - valueRange.start) / range).coerceIn(0f, 1f)
            val zeroY = thumbRadius + trackHeight * (1f - zeroFraction)
            val top = minOf(thumbY, zeroY)
            val bottom = maxOf(thumbY, zeroY)
            if (bottom > top) {
                drawRect(
                    color = activeColor,
                    topLeft = Offset(trackX, top),
                    size = Size(trackWidth, bottom - top),
                )
            }
            // The line marking flat, so a band at zero is seen to be at zero rather than merely
            // near the middle.
            drawRect(
                color = inactiveColor.copy(alpha = 0.7f),
                topLeft = Offset(centreX - ZeroMarkWidth.toPx() / 2f, zeroY - ZeroMarkHeight.toPx() / 2f),
                size = Size(ZeroMarkWidth.toPx(), ZeroMarkHeight.toPx()),
            )
        } else {
            val activeHeight = thumbRadius + trackHeight - thumbY
            if (activeHeight > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(trackX, thumbY),
                    size = Size(trackWidth, activeHeight),
                    cornerRadius = corner,
                )
            }
        }

        drawCircle(color = thumbColor, radius = thumbRadius, center = Offset(centreX, thumbY))
    }
}

/** Vertical inset the track leaves at each end so the thumb is never clipped. */
private val ThumbRadius = 8.dp

private val TrackWidth = 4.dp

private val ZeroMarkWidth = 16.dp

private val ZeroMarkHeight = 1.dp
