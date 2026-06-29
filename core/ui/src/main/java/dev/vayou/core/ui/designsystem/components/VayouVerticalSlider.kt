package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = VayouTheme.colors.accent,
    inactiveColor: Color = VayouTheme.colors.surfaceVariant,
    thumbColor: Color = Color.White,
    showCenterHighlight: Boolean = false,
) {
    val range = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .pointerInput(valueRange) {
                detectVerticalDragGestures(
                    onDragEnd = { onValueChangeFinished?.invoke() },
                ) { change, _ ->
                    change.consume()
                    val newFraction = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + newFraction * range)
                }
            },
    ) {
        val trackWidth = 4.dp.toPx()
        val thumbRadius = 8.dp.toPx()
        val centerX = size.width / 2f
        val trackX = centerX - trackWidth / 2f
        val cornerRadius = CornerRadius(trackWidth / 2f)
        val thumbY = size.height * (1f - fraction)

        drawRoundRect(
            color = inactiveColor,
            topLeft = Offset(trackX, 0f),
            size = Size(trackWidth, size.height),
            cornerRadius = cornerRadius,
        )

        if (showCenterHighlight) {
            val zeroFraction = ((0f - valueRange.start) / range).coerceIn(0f, 1f)
            val zeroY = size.height * (1f - zeroFraction)
            val top = minOf(thumbY, zeroY)
            val bottom = maxOf(thumbY, zeroY)
            if (bottom > top) {
                drawRect(
                    color = activeColor,
                    topLeft = Offset(trackX, top),
                    size = Size(trackWidth, bottom - top),
                )
            }
            drawRect(
                color = inactiveColor.copy(alpha = 0.7f),
                topLeft = Offset(centerX - 8.dp.toPx(), zeroY - 0.5.dp.toPx()),
                size = Size(16.dp.toPx(), 1.dp.toPx()),
            )
        } else {
            val activeHeight = size.height - thumbY
            if (activeHeight > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(trackX, thumbY),
                    size = Size(trackWidth, activeHeight),
                    cornerRadius = cornerRadius,
                )
            }
        }

        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = Offset(centerX, thumbY),
        )
    }
}
