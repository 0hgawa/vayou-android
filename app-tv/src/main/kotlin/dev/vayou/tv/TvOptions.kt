package dev.vayou.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * What can be done to one thing, over the whole screen.
 *
 * Held rather than tapped, and the whole screen rather than a menu beside the card, which is how
 * every television does it and for the same two reasons. A remote has one button and it is already
 * spoken for by "open this"; holding it is the only gesture left, and it has to lead somewhere
 * obvious. And a card three metres away is too small to hang a list off -- by the time a menu is
 * legible at that distance it is most of the screen anyway, so it may as well own it.
 *
 * The thing on the left with its picture, what can be done to it on the right. The picture is the
 * whole point of the arrangement: it is the same card the viewer was just looking at, so the menu
 * answers "which one is this about" before the question is asked -- and the remote only ever travels
 * up and down one column, which is the shortest walk a list of options can have.
 *
 * The actions are passed as a list rather than as content to write out, so that this can put the
 * focus on the first of them: a caller handing over a block of rows could not be asked which one
 * comes first without being asked to say it twice.
 */
@Composable
fun TvOptions(
    title: String,
    options: List<TvOptionItem>,
    onDismiss: () -> Unit,
    /** A second, quieter line: what the thing is, when its name does not say. */
    subtitle: String? = null,
    /** The same picture the card carried, so this reads as that card and not as a new screen. */
    face: @Composable BoxScope.() -> Unit,
) {
    val first = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { first.claim { hasFocus } }

    TvOverlay(onDismiss = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(TvScreenInset),
            // Held together in the middle rather than pushed to the two edges. Given the whole
            // width, the list of actions stretched to the far side and left the card stranded at
            // the near one, with the gap between them wider than either -- two things on one
            // screen that did not look like they were about each other.
            horizontalArrangement = Arrangement.spacedBy(TvScreenInset, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(ColumnWidth),
                verticalArrangement = Arrangement.spacedBy(TvCardTitleGap),
            ) {
                TvCardFace(content = face)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                // The same width as the card beside it, which is what balanced means, and enough
                // for the longest thing that can be done to one with room to spare. The two of
                // them and the gap come to about the width this app gives any other question.
                modifier = Modifier.width(ColumnWidth),
                verticalArrangement = Arrangement.spacedBy(TvRowGap),
            ) {
                options.forEachIndexed { index, option ->
                    TvRow(
                        onClick = {
                            option.onChoose()
                            onDismiss()
                        },
                        modifier = if (index == 0) {
                            Modifier
                                .focusRequester(first)
                                .onFocusChanged { hasFocus = it.isFocused }
                        } else {
                            Modifier
                        },
                    ) { isFocused ->
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(OptionMark),
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = tvTone(isFocused, isStrong = true),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** One line of the menu. Choosing it closes the menu, so no caller has to remember to. */
class TvOptionItem(val icon: ImageVector, val label: String, val onChoose: () -> Unit)

/** Wider than a card in a grid, because it is the only picture on the screen. */
private val ColumnWidth = 300.dp

private val OptionMark = 20.dp
