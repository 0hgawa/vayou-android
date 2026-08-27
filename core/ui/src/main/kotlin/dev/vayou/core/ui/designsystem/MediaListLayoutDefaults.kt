package dev.vayou.core.ui.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.theme.VayouTheme

/** How a list of media is laid out, in one place, so two lists of the same thing cannot differ. */
object MediaListLayoutDefaults {

    /**
     * Asymmetric on purpose. The leading artwork sets the list's left margin, but a trailing button
     * centres a 24dp glyph inside a 36dp target -- that slack already reads as a gap, and matching
     * the left inset beside it leaves the icons floating too far from the edge.
     */
    val ListItemPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(
            start = VayouTheme.spacing.lg,
            end = VayouTheme.spacing.sm,
            top = VayouTheme.spacing.sm,
            bottom = VayouTheme.spacing.sm,
        )

    /**
     * A row inside a bottom sheet, which has a wider margin than a screen: the sheet is narrower
     * than what is behind it, and a row on the screen's inset would sit almost on its edge.
     */
    val SheetItemPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(
            horizontal = VayouSheetDefaults.HorizontalPadding,
            vertical = VayouTheme.spacing.sm,
        )

    /**
     * What the grid's container insets itself by.
     *
     * A row carries its whole 16dp margin itself; a cell carries half and the container the other
     * half, because the gap *between* two cells is made of both their paddings. Without this the
     * column of cards sits 8dp further left than the rows it replaced, and toggling the layout
     * slides everything sideways.
     */
    val GridOuterInset: Dp
        @Composable
        @ReadOnlyComposable
        get() = VayouTheme.spacing.sm

    /**
     * What a full-width header adds on top of [GridOuterInset] to stay where it was.
     *
     * The header spans the grid, so it sees the container's share and none of the cell's. In a list
     * it sees neither and has to supply both.
     */
    @Composable
    @ReadOnlyComposable
    fun headerInset(isGrid: Boolean): Dp = if (isGrid) 0.dp else VayouTheme.spacing.sm

    /**
     * A cell in the grid. Even, unlike a row: a card has no trailing button whose own slack stands
     * in for a margin, and the gap between two cards is made of both their paddings.
     *
     * Seen as the inset of the plate a picked cell draws. The gap between two frames is these two
     * paddings plus [ItemSpacing], which the container lays between cells on both axes -- and that
     * last couple of dp is what keeps two picked plates from meeting.
     */
    /**
     * The narrowest a cell in the grid may be. How many fit is then the window's answer rather than
     * a number decided here.
     *
     * A count fixed in advance is a count that only suits the window it was chosen for: three
     * across a phone held upright is a cover of some 115dp, and the same three across the same
     * phone turned sideways is 275dp -- a folder taking a third of the screen to say what it said
     * at a third of the size. A tablet reached 400dp. Asked for a width instead, the grid keeps the
     * cover the size it was and lays out more of them.
     *
     * Three to a line on a phone, not four. The name under the card is not a caption -- for a folder
     * it is the only thing telling one from another, since every folder is drawn the same -- and a
     * fourth column leaves it 76dp, at which three names in four are cut to an ellipsis. A grid you
     * cannot read finds you less per screen than the list it replaced, not more.
     *
     * One measure for both libraries. A film is a wide frame and a cover is a square, but they are
     * looked through the same way and at the same arm's length, and two numbers that had come to
     * hold the same value would only drift apart again.
     */
    val GridCellWidth: Dp
        @Composable
        @ReadOnlyComposable
        get() = if (isHandheld) 108.dp else 160.dp

    /**
     * Whether this is a device held in the hand, which decides how big the target above is.
     *
     * Measured on the screen's shortest side, not its current width: turned sideways a phone is as
     * wide as a small tablet, and sized by width alone it would be given a tablet's large cards on
     * the one screen with no height to spare for them. The shortest side does not change when the
     * phone turns, which is the point -- and 600dp is where the platform itself puts the line.
     */
    private val isHandheld: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalConfiguration.current.smallestScreenWidthDp < TabletShortestSideDp

    /**
     * What the thing at the head of a row measures.
     *
     * One value for every list in the app -- a folder in the video library, an album, an artist, a
     * server on the network, a playlist -- because a reader moving between the tabs is looking at
     * one app, and a cover 48dp on one tab beside a tile of 56dp on the next reads as two. It is
     * also what a sheet heads itself with, so the sheet a row opens looks like the row it came from.
     *
     * Height, and width only where the thing is square: a cover and a 16:10 frame cannot share a
     * width, but they can sit on the same line. A frame asks for this height and takes whatever
     * width its shape then wants.
     *
     * 56dp is the platform's own figure for the image at the head of a list row, and two lines of
     * text above and below it come to the 72dp row the same spec asks for.
     */
    val LeadingSize: Dp = 56.dp

    /**
     * The same, for the denser lists inside a sheet -- what is playing next, and after that.
     *
     * Smaller because a queue is read as a queue: its rows are places in an order rather than
     * things to be picked out by their picture, and a sheet has a screen's height to spend on them.
     */
    val DenseLeadingSize: Dp = 48.dp

    /**
     * Between a cell's picture and the words under it. Four, not eight: the name belongs to the
     * picture above it, and at eight it starts to read as a line of its own -- which is how the
     * film folders came to sit further from their names than the music ones.
     */
    val CardTextGap: Dp = 4.dp

    val GridItemPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(VayouTheme.spacing.sm)

    /**
     * The same for a cell that is nothing but its picture.
     *
     * A card with a name under it needs the wider inset, or the words run to the edge of the cell.
     * A film in the grid has no words -- the frame is the whole card -- so that inset is only air,
     * and doubled between two cells it put 18dp between neighbouring frames: a sixth of the frame's
     * own width, which read as a grid of things kept apart rather than a sheet of them.
     */
    val GridFramePadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(VayouTheme.spacing.xs)

    /**
     * The gap between two neighbouring items, set on the container that lays them out rather than
     * drawn by each row. A row that inset its own paint would add to this instead of replacing it.
     */
    val ItemSpacing: Dp
        @Composable
        @ReadOnlyComposable
        get() = VayouTheme.spacing.xxs
}

/** Where the platform's own resource qualifiers put the line between a handset and a tablet. */
private const val TabletShortestSideDp = 600
