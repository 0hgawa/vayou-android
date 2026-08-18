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
import dev.vayou.core.ui.theme.VayouTheme

/**
 * One of a set, where exactly one is in force.
 *
 * Drawn rather than taken from Material, which sizes its own touch target and lays 12dp of padding
 * around the ring. Inside a list row the row is already the target, and that padding only pushed the
 * text off the margin every other row starts on.
 *
 * `onClick` is null wherever something around it carries the tap, so the two do not both take one.
 */
@Composable
fun VayouRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = VayouTheme.colors.accent,
    unselectedColor: Color = VayouTheme.colors.outline,
) {
    // The dot springs in rather than appearing, so a choice moving between rows is one thing moving
    // rather than two things blinking.
    val dotScale = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        dotScale.animateTo(
            targetValue = if (selected) 1f else 0f,
            animationSpec = spring(dampingRatio = DotDamping, stiffness = Spring.StiffnessMedium),
        )
    }

    val alpha = if (enabled) 1f else DisabledAlpha

    Canvas(
        modifier = modifier
            .requiredSize(RadioSize)
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
        val strokeWidth = RingWidth.toPx()

        drawCircle(
            color = (if (selected) selectedColor else unselectedColor).copy(alpha = alpha),
            radius = outerRadius - strokeWidth / 2f,
            style = Stroke(width = strokeWidth),
        )

        val dotRadius = (outerRadius - strokeWidth * DotInset) * dotScale.value
        if (dotRadius > 0f) {
            drawCircle(color = selectedColor.copy(alpha = alpha), radius = dotRadius)
        }
    }
}

private val RadioSize = 20.dp

private val RingWidth = 2.dp

/** How far inside the ring the dot sits, in ring widths. */
private const val DotInset = 2.5f

private const val DotDamping = 0.55f

private const val DisabledAlpha = 0.38f
