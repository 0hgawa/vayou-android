package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun VayouSegmentedButtonRow(
    modifier: Modifier = Modifier,
    showBorder: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = VayouTheme.shapes.full

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (showBorder) Modifier.border(width = 1.dp, color = VayouTheme.colors.outline, shape = shape) else Modifier)
            .clip(shape),
        content = content,
    )
}

@Composable
fun VayouSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) VayouTheme.colors.accentContainer else VayouTheme.colors.surface,
        animationSpec = tween(220),
        label = "segBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) VayouTheme.colors.onAccentContainer else VayouTheme.colors.onSurface,
        animationSpec = tween(220),
        label = "segFg",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                icon()
            }
            Text(
                text = label,
                style = VayouTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}
