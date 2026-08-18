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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * One of a set, where any number may be in force.
 *
 * Drawn rather than taken from Material, for the reason [VayouRadio] is: inside a list row the row
 * is the target, and Material's own 12dp of padding around the box only pushes the text off the
 * margin the rest of the list starts on.
 */
@Composable
fun VayouCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /**
     * Near-black with a light mark, not the accent.
     *
     * Unlike a switch or a slider, a checkbox has something drawn *inside* it, and a tick has to be
     * legible against the box it is in. White on the brand amber is 1.79:1 and disappears; dark on
     * amber reads, but a dark mark inside a bright box is the one combination that looks like a
     * mistake. This is what the theme tiles on the appearance screen already do.
     */
    checkedColor: Color = VayouTheme.colors.onSurface,
    // The empty box only has to say "this is pickable"; at full outline strength a screen of them
    // reads as a grid of boxes competing with the titles beside them.
    uncheckedColor: Color = VayouTheme.colors.outlineVariant,
    /** What sits on [checkedColor]: the surface, so the tick is light on dark and dark on light. */
    checkmarkColor: Color = VayouTheme.colors.surface,
) {
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) {
        progress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(dampingRatio = MarkDamping, stiffness = Spring.StiffnessMedium),
        )
    }

    val alpha = if (enabled) 1f else DisabledAlpha

    Canvas(
        modifier = modifier
            .requiredSize(CheckboxSize)
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
        val cornerRadius = CornerRadius(CornerSize.toPx())
        val strokeWidth = StrokeWidth.toPx()
        val p = progress.value

        if (p > 0f) {
            drawRoundRect(
                color = checkedColor.copy(alpha = alpha * p),
                cornerRadius = cornerRadius,
                size = Size(boxSize, boxSize),
            )
            drawPath(
                path = Path().apply {
                    val pad = boxSize * 0.25f
                    moveTo(pad, boxSize * 0.5f)
                    lineTo(boxSize * 0.4f, boxSize * 0.7f)
                    lineTo(boxSize - pad, boxSize * 0.3f)
                },
                color = checkmarkColor.copy(alpha = alpha * p),
                style = Stroke(width = strokeWidth),
            )
        }

        if (p < 1f) {
            drawRoundRect(
                color = uncheckedColor.copy(alpha = alpha * (1f - p)),
                cornerRadius = cornerRadius,
                size = Size(boxSize, boxSize),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

private val CheckboxSize = 20.dp

private val CornerSize = 3.dp

private val StrokeWidth = 2.dp

private const val MarkDamping = 0.6f

private const val DisabledAlpha = 0.38f
