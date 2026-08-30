package dev.vayou.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Video
import dev.vayou.core.ui.asFileSize
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.components.VayouFolderGraphic
import dev.vayou.core.ui.designsystem.components.VayouListHeader
import dev.vayou.core.ui.designsystem.components.VayouMediaThumbnail
import dev.vayou.core.ui.designsystem.components.VayouSegmentedListItem
import dev.vayou.core.ui.designsystem.components.VayouSelectionMark
import dev.vayou.core.ui.theme.VayouTheme

/**
 * One film, wherever it is being listed.
 *
 * Shared by the library and by search so the two cannot drift: a result that looked different from
 * the same file in the list would read as a different file.
 *
 * While [isSelecting], the tap marks instead of playing and the trailing slot empties: the mark on
 * the thumbnail already says whether the row is picked, and the toolbar over the list owns every
 * action, so a menu button there would open a sheet for one file in the middle of choosing several.
 */
@Composable
internal fun VideoRow(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
    actions: VideoActions? = null,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        selected = isSelected,
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = {
            VayouSelectionMark(selected = isSelecting && isSelected) {
                VayouMediaThumbnail(
                    model = video.uriString,
                    duration = video.formattedDuration,
                    playedFraction = video.playedPercentage,
                    // Asked for by height, as the sheet this row opens asks: a frame given a
                    // width of its own came out 60dp tall beside every other row's 56, and the
                    // sheet then headed itself with a smaller frame than the row that opened it.
                    modifier = Modifier.height(MediaListLayoutDefaults.LeadingSize),
                )
            }
        },
        content = { OneLine(video.displayName) },
        supportingContent = { SupportingLine("${video.height}p · ${video.size.asFileSize()}") },
        trailingContent = when {
            isSelecting -> null

            actions?.hasAny == true -> {
                { VideoMenuButton(video = video, actions = actions) }
            }

            else -> null
        },
    )
}

/** One folder, as a row. */
@Composable
internal fun FolderRow(
    folder: Folder,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
    supporting: String? = null,
    actions: FolderActions? = null,
) {
    VayouSegmentedListItem(
        selected = isSelected,
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = {
            VayouSelectionMark(selected = isSelecting && isSelected) { FolderGraphic() }
        },
        content = { OneLine(folder.name) },
        supportingContent = {
            SupportingLine(
                supporting ?: buildString {
                    // Counted in words, not as a bare number: "1" beside a size reads as part of
                    // the size, and there is nothing on the row to say what is being counted.
                    append(pluralStringResource(R.plurals.n_videos, folder.mediaList.size, folder.mediaList.size))
                    append(" · ")
                    append(folder.mediaSize.asFileSize())
                },
            )
        },
        trailingContent = actions
            ?.takeUnless { isSelecting }
            ?.let { { FolderMenuButton(folder = folder, actions = it) } },
    )
}

@Composable
internal fun FolderGraphic() {
    VayouFolderGraphic(modifier = Modifier.width(MediaListLayoutDefaults.LeadingSize))
}

/** A line of a row: one line, cut with an ellipsis. Every row here wants exactly this. */
@Composable
internal fun OneLine(text: String) {
    Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * The line under a row's name: what the file is, rather than which file it is.
 *
 * A step below the body size the row would otherwise inherit. This is a footnote -- a resolution, a
 * size, a count -- and at the same size as the name above it the two read as one wrapped sentence
 * and the eye has to pick out which half is the title.
 */
@Composable
internal fun SupportingLine(text: String) {
    Text(
        text = text,
        style = VayouTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The same box a sheet's header uses, so a row and the sheet it opens are one size. */
/**
 * One film as a card, for the grid.
 *
 * The frame and nothing else. A grid is chosen to be scanned by picture -- that is the whole reason
 * to leave the list -- and at a third of a phone's width a filename is a row of ellipsis that says
 * less than the frame above it. The name is a tap away in the list, and on the player.
 */
@Composable
internal fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        selected = isSelected,
        contentPadding = MediaListLayoutDefaults.GridFramePadding,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            VayouSelectionMark(selected = isSelecting && isSelected) {
                VayouMediaThumbnail(
                    model = video.uriString,
                    duration = video.formattedDuration,
                    playedFraction = video.playedPercentage,
                )
            }
        },
    )
}

/** One folder as a card. Cards carry no menu -- there is no trailing slot to hang one in. */
@Composable
internal fun FolderCard(
    folder: Folder,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        selected = isSelected,
        contentPadding = MediaListLayoutDefaults.GridItemPadding,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            Column(
                // Fills the cell, or the block is only as wide as the longer of the folder and its
                // name -- and pinned to the start of a cell it does not fill, which is what put
                // four folders at four different distances from their own left edges.
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.CardTextGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VayouSelectionMark(selected = isSelecting && isSelected) {
                    VayouFolderGraphic(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = folder.name,
                    style = VayouTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                // What the row says under the name, said here too: a folder is worth opening or not
                // by how much is in it, and the grid was the one place that did not answer.
                Text(
                    text = pluralStringResource(R.plurals.n_videos, folder.mediaList.size, folder.mediaList.size),
                    style = VayouTheme.typography.bodySmall,
                    color = VayouTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
    )
}

/**
 * What was last watched, along the top of the library.
 *
 * A row of frames rather than a list of names: this is for picking up where you left off, and the
 * one thing that says which film that is is the picture. Sideways so it costs one strip of height
 * rather than a screen of it -- what is under it is still the library.
 */
@Composable
internal fun RecentVideosRow(videos: List<Video>, isGrid: Boolean, onPlay: (Video) -> Unit) {
    Column {
        // The same line the sort row uses, not a heavier one: this introduces a strip that is not
        // the point of the screen, and at title weight the quietest thing above the list would be
        // the loudest.
        //
        // Both of these give back what the grid's container already inset, for the same reason the
        // sort row does: this strip spans the whole width and sees the container's margin, so on a
        // fixed inset it moved sideways every time the switch was pressed.
        VayouListHeader(
            label = stringResource(R.string.recently_played),
            outerInset = MediaListLayoutDefaults.headerInset(isGrid),
        )
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = VayouTheme.spacing.lg - if (isGrid) MediaListLayoutDefaults.GridOuterInset else 0.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
        ) {
            items(videos, key = { it.uriString }) { video ->
                VayouMediaThumbnail(
                    model = video.uriString,
                    duration = video.formattedDuration,
                    playedFraction = video.playedPercentage,
                    modifier = Modifier
                        .width(RecentCardWidth)
                        // Clipped before the click, so the ripple stops at the frame's corners
                        // rather than filling the square the card occupies.
                        .clip(VayouTheme.shapes.small)
                        .clickable { onPlay(video) },
                )
            }
        }
    }
}

/** Wide enough that the frame reads at a glance, narrow enough that a third card peeks in. */
private val RecentCardWidth = 140.dp
