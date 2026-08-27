package dev.vayou.feature.music

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouActionSheet
import dev.vayou.core.ui.designsystem.components.VayouActionSheetItem
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouFolderTile
import dev.vayou.core.ui.designsystem.components.VayouOverflowButton
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Everything a listener can do to a whole album, artist or folder at once.
 *
 * Only what applies to *a set of tracks*: where it goes in the queue, and where else it should be
 * listed. What a group is made of decides the rest -- an album is a tag on a file and a folder is a
 * place on disk, and neither can be renamed or thrown away as a thing in itself.
 *
 * Deliberately short of the reference app, and each absence has a reason rather than being an
 * oversight: changing a cover and editing tags mean writing to the files, which nothing in this
 * build does yet; hiding a folder needs a list of folders to leave out, which the music library has
 * no notion of; and deleting an album means deleting every file in it, which on Android 11 and up
 * is one system prompt per file -- sixty-three of them for the album this was tested on.
 */
internal data class GroupActions(
    val onPlay: (MusicGroup) -> Unit,
    val onPlayNext: (MusicGroup) -> Unit,
    val onAddToQueue: (MusicGroup) -> Unit,
    val onAddToPlaylist: (MusicGroup) -> Unit,
    val onShare: (MusicGroup) -> Unit,
    val onDelete: (MusicGroup) -> Unit,
)

/**
 * What can be done to a list the listener made, as against one the files imply.
 *
 * Null for albums, artists and folders: an album is a tag written into a file and a folder is a
 * place on disk, and renaming either here would be renaming something this app does not own.
 */
internal data class GroupOwnerActions(val onRename: (MusicGroup) -> Unit, val onDelete: (MusicGroup) -> Unit)

@Composable
internal fun GroupMenuButton(
    group: MusicGroup,
    tab: MusicTab,
    actions: GroupActions,
    ownerActions: GroupOwnerActions? = null,
) {
    var isOpen by remember { mutableStateOf(false) }

    VayouOverflowButton(onClick = { isOpen = true }, tint = VayouTheme.colors.onSurfaceVariant)

    if (!isOpen) return
    val close = { isOpen = false }
    VayouActionSheet(
        title = group.label,
        subtitle = pluralStringResource(R.plurals.n_songs, group.songs.size, group.songs.size),
        onDismiss = close,
        leading = {
            // The folder itself rather than a glyph of one, on the same tile every other leading
            // square is: a bare drawing in that slot reads as a different kind of object from the
            // covers it sits among.
            if (tab == MusicTab.Folders) {
                VayouFolderTile()
            } else {
                VayouArtwork(
                    model = group.artworkUri.takeIf { tab == MusicTab.Albums },
                    initial = group.label.initial().takeIf { tab == MusicTab.Artists },
                    modifier = Modifier.size(MediaListLayoutDefaults.LeadingSize),
                    icon = tab.sheetMark,
                )
            }
        },
    ) {
        VayouActionSheetItem(stringResource(R.string.play), VayouIcons.Play) {
            close()
            actions.onPlay(group)
        }
        VayouActionSheetItem(stringResource(R.string.play_next), VayouIcons.PlayNext) {
            close()
            actions.onPlayNext(group)
        }
        VayouActionSheetItem(stringResource(R.string.add_to_queue), VayouIcons.ListPlus) {
            close()
            actions.onAddToQueue(group)
        }
        VayouActionSheetItem(stringResource(R.string.add_to_playlist), VayouIcons.Add) {
            close()
            actions.onAddToPlaylist(group)
        }
        VayouActionSheetItem(stringResource(R.string.share), VayouIcons.Share) {
            close()
            actions.onShare(group)
        }
        // The files themselves, and only for a group that *is* the files: an album, an artist, a
        // folder. Not on a list, whether the listener made it or not -- on a made one "delete" is
        // the list itself, which is the owner's entry below, and on starred it would quietly throw
        // away the files behind a mark that was only ever a mark.
        if (tab != MusicTab.Playlists) {
            VayouActionSheetItem(stringResource(R.string.delete), VayouIcons.Delete) {
                close()
                actions.onDelete(group)
            }
        }
        ownerActions?.let { owner ->
            VayouActionSheetItem(stringResource(R.string.rename), VayouIcons.Edit) {
                close()
                owner.onRename(group)
            }
            VayouActionSheetItem(stringResource(R.string.delete), VayouIcons.Delete) {
                close()
                owner.onDelete(group)
            }
        }
    }
}

/** The same glyph the row shows, so the sheet is plainly about the row that opened it. */
private val MusicTab.sheetMark
    get() = when (this) {
        MusicTab.Artists -> VayouIcons.Artist
        MusicTab.Playlists -> VayouIcons.MusicPlaylist
        // Folders never reach here: they are drawn as a folder, not as a glyph.
        MusicTab.Folders, MusicTab.Songs, MusicTab.Albums -> VayouIcons.AudioNotesFilled
    }
