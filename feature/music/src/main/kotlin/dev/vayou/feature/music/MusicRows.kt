package dev.vayou.feature.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.common.Utils
import dev.vayou.core.media.Song
import dev.vayou.core.model.SmartPlaylist
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouArtworkRole
import dev.vayou.core.ui.designsystem.components.VayouFolderGraphic
import dev.vayou.core.ui.designsystem.components.VayouSegmentedListItem
import dev.vayou.core.ui.designsystem.components.VayouSelectionMark
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.theme.VayouTheme

/** An album, an artist or a folder, and how many tracks are under it. */
@Composable
internal fun GroupRow(
    group: MusicGroup,
    tab: MusicTab,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
    actions: GroupActions? = null,
    ownerActions: GroupOwnerActions? = null,
) {
    VayouSegmentedListItem(
        selected = isSelected,
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        onClick = onClick,
        onLongClick = onLongClick,
        trailingContent = actions
            ?.takeUnless { isSelecting }
            ?.let { { GroupMenuButton(group, tab, it, ownerActions) } },
        leadingContent = {
            // Every grouped tab reserves the same square, so the rows keep one height and the title
            // starts at the same place whichever pill is selected.
            VayouSelectionMark(selected = isSelecting && isSelected) {
                if (tab == MusicTab.Folders) {
                    // Bare, as on the folder's own screen: a folder drawn inside a rounded square
                    // reads as a card holding a folder, not as the folder itself.
                    Box(modifier = Modifier.size(GroupLeadingSize), contentAlignment = Alignment.Center) {
                        VayouFolderGraphic(width = GroupLeadingSize)
                    }
                } else {
                    VayouArtwork(
                        // Only an album owns a cover. An artist borrowing one from their tracks
                        // reads as that album rather than as the artist.
                        model = group.artworkUri.takeIf { tab == MusicTab.Albums },
                        initial = group.label.initial().takeIf { tab == MusicTab.Artists },
                        modifier = Modifier.size(GroupLeadingSize),
                        // Starred is the one list nobody made, and it is known by its star in both
                        // libraries and on the television.
                        icon = if (group.key == SmartPlaylist.Favourites) VayouIcons.StarFilled else tab.groupMark,
                        // Round for a person, square for a thing.
                        shape = if (tab == MusicTab.Artists) CircleShape else VayouTheme.shapes.medium,
                    )
                }
            }
        },
        content = {
            Text(
                text = group.label,
                style = VayouTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = pluralStringResource(R.plurals.n_songs, group.songs.size, group.songs.size),
                style = VayouTheme.typography.bodySmall,
                maxLines = 1,
            )
        },
    )
}

@Composable
internal fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
    actions: SongActions? = null,
) {
    val unknownArtist = stringResource(R.string.unknown_artist)
    VayouSegmentedListItem(
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        onClick = onClick,
        selected = isSelected,
        onLongClick = onLongClick,
        leadingContent = {
            VayouSelectionMark(selected = isSelecting && isSelected) {
                VayouArtwork(
                    model = song.artworkUri,
                    iconTint = VayouTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(SongLeadingSize),
                    icon = VayouIcons.Audio,
                )
            }
        },
        content = {
            Text(
                text = song.title.ifBlank { song.fileName },
                style = VayouTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = "${song.artist.ifBlank { unknownArtist }} · ${Utils.formatDurationMillis(song.durationMs)}",
                style = VayouTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        // The menu acts on one track; while a selection is running the toolbar owns the actions,
        // and the mark on the artwork says whether the row is picked.
        trailingContent = actions
            ?.takeUnless { isSelecting }
            ?.let { { SongMenuButton(song = song, actions = it) } },
    )
}

/**
 * One group as a card, for the grid.
 *
 * The name stays, unlike the video grid's cards. A frame of film says which film it is; an album
 * cover shared by twenty tracks, or a folder drawn the same as every other folder, does not -- take
 * the name away and the grid is a wall of identical squares.
 */
@Composable
internal fun GroupCard(
    group: MusicGroup,
    tab: MusicTab,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        selected = isSelected,
        contentPadding = MediaListLayoutDefaults.GridItemPadding,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            Column(
                // Fills the cell, or the block is only as wide as its longest child and every card
                // sits a different distance from its own left edge.
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CardTextGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VayouSelectionMark(selected = isSelecting && isSelected) {
                    if (tab == MusicTab.Folders) {
                        VayouFolderGraphic(width = CardCoverSize)
                    } else {
                        VayouArtwork(
                            model = group.artworkUri.takeIf { tab == MusicTab.Albums },
                            initial = group.label.initial().takeIf { tab == MusicTab.Artists },
                            modifier = Modifier.size(CardCoverSize),
                            icon = tab.groupMark,
                            role = VayouArtworkRole.Hero,
                            shape = if (tab == MusicTab.Artists) CircleShape else VayouTheme.shapes.medium,
                        )
                    }
                }
                Text(
                    text = group.label,
                    style = VayouTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = pluralStringResource(R.plurals.n_songs, group.songs.size, group.songs.size),
                    style = VayouTheme.typography.bodySmall,
                    color = VayouTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
    )
}

/** What a group is drawn as when it has no cover of its own. */
private val MusicTab.groupMark: ImageVector
    get() = when (this) {
        MusicTab.Folders -> VayouIcons.FolderFilled
        MusicTab.Artists -> VayouIcons.Artist
        MusicTab.Playlists -> VayouIcons.MusicPlaylist
        MusicTab.Songs, MusicTab.Albums -> VayouIcons.AudioNotesFilled
    }

/** One square for the leading visual of every grouped tab, and the same box a sheet's header uses. */
private val GroupLeadingSize = VayouSheetDefaults.LeadingSize

/** A track is one of many inside a group, and sits a step smaller than the group's own cover. */
private val SongLeadingSize = 48.dp

/** As tall as a cell is wide, less its padding: a cover in a grid is the cell. */
private val CardCoverSize = 96.dp

private val CardTextGap = 4.dp

/**
 * The letter a name is filed under, or null when it has none.
 *
 * The first character that is a letter or a digit, so a name wrapped in punctuation is filed by the
 * name rather than by the bracket. Null falls back to the glyph -- a row headed by a stray symbol
 * says less than a silhouette does.
 */
internal fun String.initial(): String? = firstOrNull { it.isLetterOrDigit() }?.uppercase()
