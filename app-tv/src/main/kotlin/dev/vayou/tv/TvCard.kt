package dev.vayou.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouFolderGraphic

/**
 * A thing to play: its picture, with its name written underneath.
 *
 * Underneath because the picture is the whole of the card. A frame from a film or the sleeve of a
 * record is what a viewer recognises from across a room, and a name laid over it would cover the
 * part they are reading. [TvTile] is the other half of this pair, for the cards that have no
 * picture to cover.
 */
@Composable
fun TvCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** A second, quieter line: how long the film runs, or which group the channel is in. */
    subtitle: String? = null,
    onLongClick: (() -> Unit)? = null,
    face: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TvCardTitleGap)) {
        Plate(onClick = onClick, onLongClick = onLongClick, content = face)
        // The two lines belong together and are set apart from the picture, so they are spaced as a
        // pair rather than as three things in a row: the name reads first, and what is under it
        // reads as a note about the name rather than as a second name.
        Column(verticalArrangement = Arrangement.spacedBy(LineGap)) {
            Title(title)
            subtitle?.let { Subtitle(it) }
        }
    }
}

/**
 * A place to go: its mark and its name, both inside the card.
 *
 * Inside, and that is the whole difference from [TvCard]. A folder or a server has no picture, so
 * the card was a grey rectangle with a small grey mark adrift in the middle of it and the name
 * floating below -- three quarters of the card doing nothing while the words that say what it is sat
 * outside it. Put in, the name is what the card is, and the mark becomes the note beside it.
 */
@Composable
fun TvTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** A second, quieter line: how many films are in the thing this card opens. */
    subtitle: String? = null,
    onLongClick: (() -> Unit)? = null,
    mark: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TvCardTitleGap)) {
        Plate(onClick = onClick, onLongClick = onLongClick) {
            // The mark above the words rather than behind them: centred, it sat under the name on a
            // card this shallow and the two read as one smudge.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(TvRowInset),
                content = mark,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(TvRowInset),
                verticalArrangement = Arrangement.spacedBy(LineGap),
            ) {
                Title(title)
                subtitle?.let { Subtitle(it) }
            }
        }
    }
}

/** The card itself: the shape both kinds share, and the ring that says the remote is on it. */
@Composable
private fun Plate(onClick: () -> Unit, onLongClick: (() -> Unit)?, content: @Composable BoxScope.() -> Unit) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        // A ring around the card rather than a card that grows. Growing shoves its neighbours
        // about and blurs whatever picture is on it; a ring is a mark drawn on top of a still
        // grid, which is the one thing a viewer can follow while holding a direction down.
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusRing, MaterialTheme.colorScheme.onSurface),
                inset = FocusRingInset,
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CardAspect),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Subtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        // Quieter than the name by colour as well as by size. The same grey at the same weight makes
        // a genre look like part of the title, which is how a wall of cards turns into a wall of
        // text.
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = SubtitleAlpha),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The face of a card with nothing to press: the same picture, at rest.
 *
 * For the screens that show a viewer what they are about to act on rather than offering it to them
 * again. It is deliberately not a [Surface]: a plate that takes the focus in a menu of options is a
 * plate the remote can land on, and there is nothing to do to it there.
 */
@Composable
fun TvCardFace(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(CardAspect)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * What a card shows when there is no picture to show: the kind of thing it opens.
 *
 * One quiet grey for all of them. The amber was doing two jobs at once -- it is the colour of the
 * focus ring and of the seek bar, so a wall of amber marks read as a wall of things already chosen.
 * A card is a place to look; what tells them apart is the shape and the name under it.
 */
@Composable
fun TvCardMark(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(MarkSize),
    )
}

/**
 * Starred, in the one colour that means it.
 *
 * The exception to the grey. A star is not a kind of thing the card opens -- it is a mark the viewer
 * put there, and it is amber wherever it appears in this app and on the phone.
 */
@Composable
fun TvCardStar(modifier: Modifier = Modifier) {
    Icon(
        imageVector = VayouIcons.StarFilled,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(MarkSize),
    )
}

/** A folder is drawn rather than iconed, the same shape the phone draws. */
@Composable
fun TvCardFolder() {
    VayouFolderGraphic(modifier = Modifier.width(FolderWidth))
}

/** One sentence in the middle of the screen: what a television can say without a place to tap. */
@Composable
fun TvMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Overscan: a television clips the edges of what it is sent, and the amount is its own business. */
val TvScreenInset = 48.dp

val TvTitleInset = 24.dp

val TvCardGap = 16.dp

/**
 * How far into a thing the viewer got, along the bottom edge of its card.
 *
 * Drawn on the card and not under it, where a row of them would push the titles down and change
 * the height of every card in the row for the sake of the few that have been started.
 *
 * Nothing at all where the fraction is not known -- a film watched before lengths were written
 * down has a position and no total, and a bar guessed from that would be a bar that lies.
 */
@Composable
fun BoxScope.TvWatchedBar(watched: Float?) {
    if (watched == null || watched <= 0f) return
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            // Off the edges rather than along them, as the television's own home draws it. Flush
            // with the corners, a bar on a rounded card is a straight line running out from under
            // two curves; lifted, it reads as something laid on the picture. The inset comes before
            // the width, so the bar is shortened rather than merely having its ground moved.
            .padding(BarInset)
            .fillMaxWidth()
            .height(BarHeight)
            .clip(CircleShape)
            // Translucent white under the fill, as the phone draws it: this lies on a frame nobody
            // chose, and a colour from the palette reads as a block on some of them.
            .background(Color.White.copy(alpha = BarTrackAlpha)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(watched)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private val BarHeight = 4.dp

/** How far off the corners it sits, which is about what the curve of a card takes up. */
private val BarInset = 8.dp

private const val BarTrackAlpha = 0.4f

/** One width everywhere, so a row of films and a grid of folders read as the same library. */
val TvCardWidth = 200.dp

val TvCardTitleGap = 8.dp

/**
 * A row in a list: how far its content sits from the edges, and how far the parts sit from each
 * other. One set for the player's menus and the settings alike -- they are the same row, and a
 * viewer moving between them should not be able to tell which screen they are on by the gaps.
 */
val TvRowInset = 12.dp

val TvRowGap = 8.dp

/** The gutter a tick sits in at the head of a row, held open whether the row is ticked or not. */
val TvRowTick = 20.dp

/** Tighter than the gap under the picture: the name and its note are one block. */
private val LineGap = 2.dp

private const val SubtitleAlpha = 0.6f

private const val CardAspect = 16f / 9f

private val FocusRing = 3.dp

/** Set off the card, as the ring on a television's own launcher is. Touching the corner reads as a
 *  second border on the picture rather than as the focus being somewhere. */
private val FocusRingInset = 3.dp

/**
 * The height of everything that can be pressed: a mark in a header, a control over a film, the way
 * out of a question.
 *
 * Measured rather than chosen. The television's own reference draws a button 45dp tall around a
 * glyph of 10.5dp, and the set puts the floor for one at 40. The header marks here were already 44
 * and everything else was 48, which is how one panel came to have three heights for one kind of
 * thing.
 */
val TvControlHeight = 44.dp

/** Half of it, which is the ratio the set draws its own icon buttons at. */
val TvControlMark = 22.dp

private val MarkSize = 40.dp

/** The folder drawing's own width over its height, as `VayouFolderGraphic` lays it out. */
private const val FolderAspect = 20 / 17f

/** As tall as [MarkSize], which is what makes a folder read as the same size as everything else:
 *  the drawing is wider than it is tall, so matching the widths would leave it towering over them. */
private val FolderWidth = MarkSize * FolderAspect
