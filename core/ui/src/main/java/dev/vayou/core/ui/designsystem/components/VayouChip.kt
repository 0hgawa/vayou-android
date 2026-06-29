package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = VayouTheme.shapes.full
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) VayouTheme.colors.accent else VayouTheme.colors.surfaceVariant,
        animationSpec = tween(220),
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) VayouTheme.colors.onAccent else VayouTheme.colors.onSurfaceVariant,
        animationSpec = tween(220),
        label = "chipFg",
    )

    Row(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }
        Text(
            text = label,
            style = VayouTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}
