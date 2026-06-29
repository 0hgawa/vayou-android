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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = VayouTheme.colors.accent,
    inactiveColor: Color = VayouTheme.colors.outlineVariant,
) {
    val progress = remember { Animatable(if (checked) 1f else 0f) }

    LaunchedEffect(checked) {
        progress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    val alpha = if (enabled) 1f else 0.38f

    Canvas(
        modifier = modifier
            .requiredSize(width = 44.dp, height = 24.dp)
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            }
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        onClick = { onCheckedChange(!checked) },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        val trackHeight = 14.dp.toPx()
        val trackWidth = 44.dp.toPx()
        val trackRadius = CornerRadius(trackHeight / 2f)
        val thumbRadius = 10.dp.toPx()
        val canvasHeight = 24.dp.toPx()
        val trackY = (canvasHeight - trackHeight) / 2f

        val p = progress.value
        val trackColor = lerp(inactiveColor, activeColor, p)

        drawRoundRect(
            color = trackColor.copy(alpha = alpha * 0.4f),
            topLeft = Offset(0f, trackY),
            cornerRadius = trackRadius,
            size = Size(trackWidth, trackHeight),
        )

        val thumbPadding = thumbRadius
        val thumbX = thumbPadding + (trackWidth - thumbPadding * 2) * p
        val thumbColor = lerp(inactiveColor, activeColor, p)

        drawCircle(
            color = thumbColor.copy(alpha = alpha),
            radius = thumbRadius,
            center = Offset(thumbX, canvasHeight / 2f),
        )
    }
}

private fun lerp(start: Color, end: Color, fraction: Float): Color = Color(
    red = start.red + (end.red - start.red) * fraction,
    green = start.green + (end.green - start.green) * fraction,
    blue = start.blue + (end.blue - start.blue) * fraction,
    alpha = start.alpha + (end.alpha - start.alpha) * fraction,
)
