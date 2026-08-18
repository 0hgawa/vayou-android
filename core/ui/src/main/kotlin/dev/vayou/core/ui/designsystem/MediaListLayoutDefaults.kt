package dev.vayou.core.ui.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
    val GridItemPadding: PaddingValues
        @Composable
        @ReadOnlyComposable
        get() = PaddingValues(VayouTheme.spacing.sm)

    /**
     * The gap between two neighbouring items, set on the container that lays them out rather than
     * drawn by each row. A row that inset its own paint would add to this instead of replacing it.
     */
    val ItemSpacing: Dp
        @Composable
        @ReadOnlyComposable
        get() = VayouTheme.spacing.xxs
}
