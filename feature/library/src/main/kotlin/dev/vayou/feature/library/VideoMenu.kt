package dev.vayou.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Video
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouActionSheet
import dev.vayou.core.ui.designsystem.components.VayouActionSheetItem
import dev.vayou.core.ui.designsystem.components.VayouCancelButton
import dev.vayou.core.ui.designsystem.components.VayouConfirmButton
import dev.vayou.core.ui.designsystem.components.VayouDialog
import dev.vayou.core.ui.designsystem.components.VayouDoneButton
import dev.vayou.core.ui.designsystem.components.VayouFolderTile
import dev.vayou.core.ui.designsystem.components.VayouMediaThumbnail
import dev.vayou.core.ui.designsystem.components.VayouOverflowButton
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouTextField
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Everything a viewer can do to one film, gathered behind one button.
 *
 * Each action is nullable and each is left out where it would make no sense: a film already in the
 * locked folder cannot be put into it, one outside a list cannot be taken out of it, and a private
 * film has no public address to share or rename. A row whose every action is absent shows no button
 * rather than an empty sheet.
 */
internal data class VideoActions(
    /** Not in the menu -- the row's own tap. Here because the list calls it. */
    val onPlay: (Video) -> Unit,
    /** Null for a film that was never left halfway: there is nothing to ignore. */
    val onPlayFromStart: ((Video) -> Unit)? = null,
    val onToggleFavourite: ((Video) -> Unit)? = null,
    val isFavourite: (Video) -> Boolean = { false },
    val onShare: ((Video) -> Unit)? = null,
    val onAddToPlaylist: ((Video) -> Unit)? = null,
    val onRemoveFromPlaylist: ((Video) -> Unit)? = null,
    val onMoveToPrivate: ((Video) -> Unit)? = null,
    val onRestoreFromPrivate: ((Video) -> Unit)? = null,
    val onRename: ((Video) -> Unit)? = null,
    val onInfo: ((Video) -> Unit)? = null,
    val onDelete: ((Video) -> Unit)? = null,
) {
    val hasAny: Boolean = listOf(
        onToggleFavourite,
        onShare,
        onAddToPlaylist,
        onRemoveFromPlaylist,
        onMoveToPrivate,
        onRestoreFromPrivate,
        onRename,
        onInfo,
        onDelete,
    ).any { it != null }
}

@Composable
internal fun VideoMenuButton(video: Video, actions: VideoActions) {
    var isOpen by remember { mutableStateOf(false) }
    VayouOverflowButton(onClick = { isOpen = true })
    if (!isOpen) return

    val close = { isOpen = false }
    VayouActionSheet(
        title = video.displayName,
        subtitle = video.formattedDuration,
        onDismiss = close,
        // The frame the row was showing, carried into the sheet. A name alone leaves the viewer
        // checking they opened the menu for the film they meant; the picture answers that at a
        // glance, and it is already decoded and in the cache from the row behind.
        leading = {
            VayouMediaThumbnail(
                model = video.uriString,
                duration = video.formattedDuration,
                // Height, not width: a 16:10 frame and a square cover cannot share a width,
                // and what makes two sheets look alike is that their headers are one height.
                modifier = Modifier.height(VayouSheetDefaults.LeadingSize),
            )
        },
    ) {
        // No "Play" here. The row itself plays on a tap, so a first entry that does the same thing is
        // the one the eye reads first and the one that teaches nothing -- it only pushes share and
        // delete further down. Every app whose rows play on tap leaves it out for that reason.
        //
        // Ordered by how often each is reached for rather than by what it does to the file: share is
        // everyday, delete is the once-a-month one that must not sit under a wandering thumb.
        // First, and only for a film left part-watched. On any other it is what the row's own tap
        // already does, and a menu offering what you just did is a menu that makes you wonder what
        // the difference was.
        actions.onPlayFromStart?.takeIf { video.playbackPosition > 0L }?.let { fromStart ->
            VayouActionSheetItem(stringResource(R.string.play_from_start), VayouIcons.Replay) {
                close()
                fromStart(video)
            }
        }
        actions.onShare?.let { share ->
            VayouActionSheetItem(stringResource(R.string.share), VayouIcons.Share) {
                close()
                share(video)
            }
        }
        actions.onToggleFavourite?.let { toggle ->
            val isFavourite = actions.isFavourite(video)
            VayouActionSheetItem(
                text = stringResource(if (isFavourite) R.string.unfavourite else R.string.favourite),
                icon = if (isFavourite) VayouIcons.StarFilled else VayouIcons.StarOutlined,
            ) {
                close()
                toggle(video)
            }
        }
        actions.onAddToPlaylist?.let { add ->
            VayouActionSheetItem(stringResource(R.string.add_to_playlist), VayouIcons.Add) {
                close()
                add(video)
            }
        }
        actions.onRemoveFromPlaylist?.let { remove ->
            VayouActionSheetItem(stringResource(R.string.remove_from_playlist), VayouIcons.Close) {
                close()
                remove(video)
            }
        }
        actions.onMoveToPrivate?.let { move ->
            VayouActionSheetItem(stringResource(R.string.move_to_private), VayouIcons.Lock) {
                close()
                move(video)
            }
        }
        actions.onRestoreFromPrivate?.let { restore ->
            VayouActionSheetItem(stringResource(R.string.restore_from_private), VayouIcons.Lock) {
                close()
                restore(video)
            }
        }
        actions.onRename?.let { rename ->
            VayouActionSheetItem(stringResource(R.string.rename), VayouIcons.Edit) {
                close()
                rename(video)
            }
        }
        actions.onInfo?.let { info ->
            VayouActionSheetItem(stringResource(R.string.info), VayouIcons.Info) {
                close()
                info(video)
            }
        }
        actions.onDelete?.let { delete ->
            VayouActionSheetItem(stringResource(R.string.delete), VayouIcons.Delete) {
                close()
                delete(video)
            }
        }
    }
}

/**
 * Renaming, with the extension left out of the field and put back on the way out.
 *
 * A viewer renaming a film means the name, not the ".mp4" -- and one deleted by accident leaves a
 * file no player will open.
 */
@Composable
internal fun VideoRenameDialog(video: Video, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val extension = video.nameWithExtension.substringAfterLast(".", "")
    var name by remember { mutableStateOf(video.displayName) }

    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.rename),
        confirmButton = {
            VayouDoneButton(
                enabled = name.isNotBlank(),
                onClick = { onDone(if (extension.isEmpty()) name.trim() else "${name.trim()}.$extension") },
            )
        },
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        VayouTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.name),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }
}

/**
 * Deleting, behind a confirmation, because it takes the file off the phone and not just off a list.
 *
 * One film is named; several are counted. "3 videos will be removed" is what a viewer can check
 * against what they marked, where three names in a row is a paragraph nobody reads.
 */
@Composable
internal fun VideoDeleteDialog(count: Int, name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.delete),
        confirmButton = {
            VayouConfirmButton(
                text = stringResource(R.string.delete),
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            )
        },
        dismissButton = { VayouCancelButton(onClick = onDismiss) },
    ) {
        Text(
            text = if (count == 1) {
                stringResource(R.string.delete_video_confirmation, name)
            } else {
                stringResource(R.string.delete_videos_confirmation, count)
            },
        )
    }
}

/**
 * What the file is, as the library knows it.
 *
 * Only the lines that have something behind them: a film whose streams have not been read yet has no
 * codec to name, and a row reading "Codec: —" says less than no row at all.
 */
@Composable
internal fun VideoInfoDialog(video: Video, onDismiss: () -> Unit) {
    VayouDialog(
        onDismissRequest = onDismiss,
        title = video.displayName,
        confirmButton = { VayouDoneButton(onClick = onDismiss) },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(InfoLineSpacing),
        ) {
            InfoLine(stringResource(R.string.info_file), video.nameWithExtension)
            if (video.parentPath.isNotBlank()) InfoLine(stringResource(R.string.info_location), video.parentPath)
            InfoLine(stringResource(R.string.info_size), video.formattedFileSize)
            InfoLine(stringResource(R.string.info_duration), video.formattedDuration)
            InfoLine(stringResource(R.string.info_resolution), "${video.width} × ${video.height}")
            video.format?.let { InfoLine(stringResource(R.string.info_format), it) }
            video.videoStream?.let { stream ->
                InfoLine(stringResource(R.string.info_video_codec), stream.codecName)
            }
            video.audioStreams.forEachIndexed { index, stream ->
                InfoLine(stringResource(R.string.info_audio_codec, index + 1), stream.codecName)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = VayouTheme.typography.labelMedium,
            color = VayouTheme.colors.onSurfaceVariant,
        )
        Text(text = value, style = VayouTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
    }
}

private val InfoLineSpacing = 12.dp

/** Smaller than the row's own: the sheet's title is what is being read here, not the picture. */
/**
 * What a viewer can do to a whole folder.
 *
 * "Play" belongs here, unlike on a film's row: tapping a folder *opens* it, so playing everything in
 * it is a different thing and has nowhere else to be asked for.
 */
internal data class FolderActions(
    val onPlayAll: (Folder) -> Unit,
    val onAddAllToPlaylist: (Folder) -> Unit,
    val onShareAll: (Folder) -> Unit,
    val onDeleteAll: (Folder) -> Unit,
)

@Composable
internal fun FolderMenuButton(folder: Folder, actions: FolderActions) {
    var isOpen by remember { mutableStateOf(false) }
    VayouOverflowButton(onClick = { isOpen = true })
    if (!isOpen) return

    val close = { isOpen = false }
    VayouActionSheet(
        title = folder.name,
        subtitle = pluralStringResource(R.plurals.n_videos, folder.mediaList.size, folder.mediaList.size),
        onDismiss = close,
        leading = { VayouFolderTile() },
    ) {
        VayouActionSheetItem(stringResource(R.string.play_all), VayouIcons.Play) {
            close()
            actions.onPlayAll(folder)
        }
        VayouActionSheetItem(stringResource(R.string.add_to_playlist), VayouIcons.Add) {
            close()
            actions.onAddAllToPlaylist(folder)
        }
        VayouActionSheetItem(stringResource(R.string.share), VayouIcons.Share) {
            close()
            actions.onShareAll(folder)
        }
        VayouActionSheetItem(stringResource(R.string.delete), VayouIcons.Delete) {
            close()
            actions.onDeleteAll(folder)
        }
    }
}
