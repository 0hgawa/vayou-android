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
import dev.vayou.core.ui.theme.VayouTheme

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
    // Neutral rather than accent: a row of chips is a filter, not a call to action, and inverting
    // the surface reads as "selected" at any theme without competing with the content below.
    val backgroundColor by animateColorAsState(
        // One step off the surface, not two. A chip is drawn *on* a surface and has to read as a
        // shape on any of them, and the highest container was heavier than a filter needs to be --
        // on a white page a row of them came out as grey slabs competing with the list below.
        //
        // Not the plain container, though: that is the token a bottom sheet fills itself with, and a
        // row of chips on a sheet was once #1E1E1E on #1E1E1E -- five words with no chip around
        // them, and only the selected one looking like a control. High clears a sheet at both ends.
        targetValue = if (selected) VayouTheme.colors.onSurface else VayouTheme.colors.surfaceContainerHigh,
        animationSpec = tween(220),
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) VayouTheme.colors.surface else VayouTheme.colors.onSurface,
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
