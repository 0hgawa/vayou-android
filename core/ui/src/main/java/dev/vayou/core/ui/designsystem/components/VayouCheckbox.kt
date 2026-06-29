package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = VayouTheme.colors.accent,
    uncheckedColor: Color = VayouTheme.colors.outline,
    checkmarkColor: Color = VayouTheme.colors.onAccent,
) {
    val progress = remember { Animatable(if (checked) 1f else 0f) }

    LaunchedEffect(checked) {
        progress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    val alpha = if (enabled) 1f else 0.38f

    Canvas(
        modifier = modifier
            .requiredSize(20.dp)
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onClick = { onCheckedChange(!checked) },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        val boxSize = size.minDimension
        val cornerRadius = CornerRadius(3.dp.toPx())
        val strokeWidth = 2.dp.toPx()
        val p = progress.value

        if (p > 0f) {
            // Filled box
            drawRoundRect(
                color = checkedColor.copy(alpha = alpha * p),
                cornerRadius = cornerRadius,
                size = Size(boxSize, boxSize),
            )

            // Checkmark
            val checkPath = Path().apply {
                val pad = boxSize * 0.25f
                moveTo(pad, boxSize * 0.5f)
                lineTo(boxSize * 0.4f, boxSize * 0.7f)
                lineTo(boxSize - pad, boxSize * 0.3f)
            }
            drawPath(
                path = checkPath,
                color = checkmarkColor.copy(alpha = alpha * p),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        if (p < 1f) {
            // Unchecked outline
            drawRoundRect(
                color = uncheckedColor.copy(alpha = alpha * (1f - p)),
                cornerRadius = cornerRadius,
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}
