package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Marks a row's leading visual when the row is picked.
 *
 * On the artwork rather than at the far end of the row: the thing being chosen is the file, and the
 * file is the thumbnail. A tick in the trailing slot puts the answer to "is this one picked?" at the
 * opposite margin from the picture that prompted the question, and it only exists in a list -- a
 * grid has no trailing slot, so a row and a cell end up saying the same thing two different ways.
 *
 * The two colours the pills at the top of every screen use, and no third: a filled disc in the
 * foreground colour with the tick and the ring cut out of it in the background one. The accent was
 * here once, which made the mark the single part of a selection that carried the wallpaper's colour
 * -- a selection is a state, not a place, and the rest of the app says states in black and white.
 */
@Composable
fun VayouSelectionMark(selected: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        content()
        if (selected) {
            Icon(
                imageVector = VayouIcons.Check,
                contentDescription = null,
                tint = VayouTheme.colors.surface,
                modifier = Modifier
                    // Bottom left, not right: on a video frame the right corner already holds the
                    // duration, and the mark landed on top of it. 6dp rather than 4 clears the
                    // watched-progress bar that runs along the very bottom of the same frame.
                    .align(Alignment.BottomStart)
                    .padding(MarkInset)
                    .background(VayouTheme.colors.onSurface, CircleShape)
                    // Drawn as a border on the disc rather than as a second disc behind it: same
                    // ring, one modifier and one padding fewer. The ring is what keeps the disc off
                    // a frame that happens to be the same colour as the accent.
                    .border(RingWidth, VayouTheme.colors.surface, CircleShape)
                    .padding(DiscPadding)
                    .size(TickSize),
            )
        }
    }
}

private val MarkInset = 6.dp

private val RingWidth = 2.dp

private val DiscPadding = 5.dp

private val TickSize = 14.dp
