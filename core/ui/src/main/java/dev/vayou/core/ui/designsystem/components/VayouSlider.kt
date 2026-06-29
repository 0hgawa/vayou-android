package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun VayouSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = VayouTheme.colors.accent,
    inactiveColor: Color = VayouTheme.colors.surfaceVariant,
    thumbColor: Color = VayouTheme.colors.accent,
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val alpha = if (enabled) 1f else 0.38f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragEnd = { onValueChangeFinished?.invoke() },
                            ) { change, _ ->
                                change.consume()
                                val newFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                                onValueChange(newValue)
                            }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        val trackHeight = 4.dp.toPx()
        val thumbRadius = 6.dp.toPx()
        val centerY = size.height / 2f
        val trackY = centerY - trackHeight / 2f
        val trackRadius = CornerRadius(trackHeight / 2f)

        drawRoundRect(
            color = inactiveColor.copy(alpha = alpha),
            topLeft = Offset(0f, trackY),
            size = Size(size.width, trackHeight),
            cornerRadius = trackRadius,
        )

        val activeWidth = size.width * fraction
        if (activeWidth > 0f) {
            drawRoundRect(
                color = activeColor.copy(alpha = alpha),
                topLeft = Offset(0f, trackY),
                size = Size(activeWidth, trackHeight),
                cornerRadius = trackRadius,
            )
        }

        val thumbX = size.width * fraction
        drawCircle(
            color = thumbColor.copy(alpha = alpha),
            radius = thumbRadius,
            center = Offset(thumbX, centerY),
        )
    }
}
