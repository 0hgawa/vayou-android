package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Which of two views of the same thing is open.
 *
 * One outlined track cut into segments, and not a row of separate tiles: a tile is a thing you pick,
 * and a segment is a place you go. Two of them side by side with a gap between read as two options
 * of equal standing with whatever is picked below them -- which is exactly the confusion this
 * replaces.
 *
 * The chosen segment turns the surface over, as every other "this one" in the app does.
 */
@Composable
fun VayouSegmentedButtons(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = VayouTheme.shapes.full
    // Drawn shorter than it is hit, the way a selectable tile is. A strip this wide is easy to find
    // and does not need to be as tall as it is reachable; the difference is transparent margin, so
    // the thumb still gets the app's floor while the sheet gets the height back.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TargetHeight)
            .selectableGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .border(BorderWidth, VayouTheme.colors.outlineVariant, shape)
                .clip(shape),
        ) {
            labels.forEachIndexed { index, label ->
                Segment(label = label, selected = index == selectedIndex, modifier = Modifier.weight(1f))
            }
        }

        // The targets, over the drawing and the full height of it. Nothing is drawn here, and the
        // segments below draw no ripple either, so moving the tap up costs no feedback.
        Row(modifier = Modifier.matchParentSize()) {
            labels.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = index == selectedIndex,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                )
            }
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier = Modifier) {
    // Crossfaded rather than switched, so the strip reads as one control whose filled half slid
    // across rather than two buttons that both blinked.
    val background by animateColorAsState(
        targetValue = if (selected) VayouTheme.colors.onSurface else VayouTheme.colors.surface.copy(alpha = 0f),
        animationSpec = tween(SwitchDuration),
        label = "segmentBackground",
    )
    val content by animateColorAsState(
        targetValue = if (selected) VayouTheme.colors.surface else VayouTheme.colors.onSurfaceVariant,
        animationSpec = tween(SwitchDuration),
        label = "segmentContent",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = VayouTheme.typography.labelLarge, color = content)
    }
}

/** What is hit: the app's floor for a target, held whatever the strip is drawn at. */
private val TargetHeight = 48.dp

/** What is drawn. Two words in a pill do not need the whole target to read well. */
private val TrackHeight = 40.dp

private val BorderWidth = 1.dp

private const val SwitchDuration = 220
