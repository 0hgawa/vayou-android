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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = VayouTheme.colors.accent,
    unselectedColor: Color = VayouTheme.colors.outline,
) {
    val dotScale = remember { Animatable(if (selected) 1f else 0f) }

    LaunchedEffect(selected) {
        dotScale.animateTo(
            targetValue = if (selected) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    val alpha = if (enabled) 1f else 0.38f

    Canvas(
        modifier = modifier
            .requiredSize(20.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        val outerRadius = size.minDimension / 2f
        val strokeWidth = 2.dp.toPx()
        val ringColor = if (selected) selectedColor else unselectedColor

        // Outer ring
        drawCircle(
            color = ringColor.copy(alpha = alpha),
            radius = outerRadius - strokeWidth / 2f,
            style = Stroke(width = strokeWidth),
        )

        // Inner dot
        val dotRadius = (outerRadius - strokeWidth * 2.5f) * dotScale.value
        if (dotRadius > 0f) {
            drawCircle(
                color = selectedColor.copy(alpha = alpha),
                radius = dotRadius,
            )
        }
    }
}
