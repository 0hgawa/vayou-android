package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A switch drawn as a glyph -- shuffle, repeat, anything that is simply on or off.
 *
 * The glyph takes the accent when it is on, and nothing else changes. A tonal disc behind it was
 * tried and taken out: on a row of two or three of these it reads as a button that has grown rather
 * than a state that has changed, and the glyph on its own was already saying it.
 *
 * Here rather than in each sheet because both queues have the same pair, and a toggle that answers
 * differently on the two screens is two toggles.
 */
@Composable
fun VayouToggleIconButton(
    icon: ImageVector,
    isOn: Boolean,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (isOn) VayouTheme.colors.accent else VayouTheme.colors.onSurfaceVariant,
        animationSpec = tween(SwitchMs),
        label = "toggleTint",
    )

    VayouIconButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}

private const val SwitchMs = 150
