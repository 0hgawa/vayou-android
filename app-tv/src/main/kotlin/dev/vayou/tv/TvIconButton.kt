package dev.vayou.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.VayouIcons

/**
 * A single mark at the head of a screen: back, search, settings.
 *
 * A circle, and nothing behind it until the focus arrives. These are not places a viewer goes, they
 * are the one or two things they can do once there, and a plate around each one turns a header into
 * a row of competing buttons. A glyph on its own reads as one, and the white disc the focus draws is
 * the only plate the row ever shows -- which is what makes it obvious where the remote is.
 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Shown beside the mark, for a button whose answer is worth reading before it is pressed.
     *
     * Null for back, search and settings: what they do is in the shape, and a word beside each one
     * turns a header into a sentence.
     */
    caption: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = if (caption == null) modifier.size(TvControlHeight) else modifier.height(TvControlHeight),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            // A bare mark fills its plate, or it sits against the left edge of a circle it was meant
            // to be in the middle of. A captioned one fills only the height: filling the width of
            // what a header row offers is filling the row, and the button swallowed the title and
            // pushed the search mark off the screen.
            modifier = if (caption == null) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = CaptionInset)
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CaptionGap, Alignment.CenterHorizontally),
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(TvControlMark))
            caption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = CaptionWidth),
                )
            }
        }
    }
}

private val CaptionInset = 16.dp

private val CaptionGap = 8.dp

/** A group's name can run to a sentence; the button says what it can and stops. */
private val CaptionWidth = 220.dp

/**
 * Which way a listing runs, and the way to change it.
 *
 * The axis is written out beside the arrow rather than left to a glyph. Sorted by size and sorted by
 * date are the same picture otherwise, and a viewer who cannot see which one is in force has to open
 * the list to find out -- which is why the phone writes it over its own grid as well.
 */
@Composable
fun TvOrderButton(isAscending: Boolean, label: String, onClick: () -> Unit) {
    TvIconButton(
        icon = if (isAscending) VayouIcons.SortAscending else VayouIcons.SortDescending,
        label = label,
        onClick = onClick,
    )
}

/**
 * The way out of a screen that was pushed on top of the others.
 *
 * A remote has a back key and every viewer knows it, which is exactly why this is here: the key is
 * invisible, and a screen with no visible way back is a screen a viewer has to remember how to
 * leave. It sits at the head of the row, where the eye starts.
 */
@Composable
fun TvBackButton(label: String, onBack: () -> Unit) {
    TvIconButton(icon = VayouIcons.ArrowBack, label = label, onClick = onBack)
}
