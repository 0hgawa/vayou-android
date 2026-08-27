package dev.vayou.feature.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.common.Utils
import dev.vayou.core.media.Song
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouActionSheet
import dev.vayou.core.ui.designsystem.components.VayouActionSheetItem
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouCancelButton
import dev.vayou.core.ui.designsystem.components.VayouConfirmButton
import dev.vayou.core.ui.designsystem.components.VayouDialog
import dev.vayou.core.ui.designsystem.components.VayouDoneButton
import dev.vayou.core.ui.designsystem.components.VayouOverflowButton
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Everything a listener can do to one track, gathered behind one button.
 *
 * The same set the video row offers, in the same order, less what a song has no equivalent of. Two
 * libraries in one app that answer the same long list of questions differently are two apps.
 *
 * "Play next" leads, unlike on a film's row: a queue is what a music player is, and slipping a track
 * into it without losing what is playing is the thing this menu exists for.
 */
internal data class SongActions(
    /**
     * Null on the player's own menu, where both would act on the track already playing: slipping it
     * in after itself, or adding a second copy of it to the end.
     */
    val onPlayNext: ((Song) -> Unit)?,
    val onAddToQueue: ((Song) -> Unit)?,
    val onShare: (Song) -> Unit,
    /**
     * Starring, where the menu is the only place to do it. Null on the player, which has the star
     * beside the title: one action, offered once.
     */
    val favourite: FavouriteAction?,
    val onAddToPlaylist: (Song) -> Unit,
    /** Non-null only inside a list the listener owns, where taking a track out means something. */
    val onRemoveFromPlaylist: ((Song) -> Unit)?,
    val onDelete: (Song) -> Unit,
    val onEditTags: (Song) -> Unit,
)

/** Whether a track is starred, and the way to change that. */
internal data class FavouriteAction(val isFavourite: (Song) -> Boolean, val toggle: (Song) -> Unit)

@Composable
internal fun SongMenuButton(
    song: Song,
    actions: SongActions,
    /**
     * The key that opens it. A row gets the narrow overflow, which sits closer to the edge than a
     * full button would; the player gets a round one on the chevron's own box, because there the
     * two keys are a pair at opposite ends of the same bar and an oval ripple beside a circular one
     * reads as a mistake.
     */
    button: @Composable (onClick: () -> Unit) -> Unit = { onClick -> VayouOverflowButton(onClick = onClick) },
) {
    var isOpen by remember { mutableStateOf(false) }
    var isShowingDetails by remember { mutableStateOf(false) }

    button { isOpen = true }

    if (isOpen) {
        val close = { isOpen = false }
        val unknownArtist = stringResource(R.string.unknown_artist)
        VayouActionSheet(
            title = song.title.ifBlank { song.fileName },
            subtitle = song.artist.ifBlank { unknownArtist },
            onDismiss = close,
            leading = {
                VayouArtwork(
                    model = song.artworkUri,
                    iconTint = VayouTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(MediaListLayoutDefaults.LeadingSize),
                )
            },
        ) {
            actions.onPlayNext?.let { playNext ->
                VayouActionSheetItem(stringResource(R.string.play_next), VayouIcons.PlayNext) {
                    close()
                    playNext(song)
                }
            }
            actions.onAddToQueue?.let { addToQueue ->
                VayouActionSheetItem(stringResource(R.string.add_to_queue), VayouIcons.ListPlus) {
                    close()
                    addToQueue(song)
                }
            }
            VayouActionSheetItem(stringResource(R.string.share), VayouIcons.Share) {
                close()
                actions.onShare(song)
            }
            actions.favourite?.let { favourite ->
                val isFavourite = favourite.isFavourite(song)
                VayouActionSheetItem(
                    text = stringResource(if (isFavourite) R.string.unfavourite else R.string.favourite),
                    icon = if (isFavourite) VayouIcons.StarFilled else VayouIcons.StarOutlined,
                ) {
                    close()
                    favourite.toggle(song)
                }
            }
            VayouActionSheetItem(stringResource(R.string.add_to_playlist), VayouIcons.Add) {
                close()
                actions.onAddToPlaylist(song)
            }
            actions.onRemoveFromPlaylist?.let { remove ->
                VayouActionSheetItem(stringResource(R.string.remove_from_playlist), VayouIcons.Close) {
                    close()
                    remove(song)
                }
            }
            // Only where the tags can actually be written. The writer reads one format, and an
            // entry that fails on a .flac is worse than no entry.
            if (song.isTaggable) {
                VayouActionSheetItem(stringResource(R.string.edit_tags), VayouIcons.Edit) {
                    close()
                    actions.onEditTags(song)
                }
            }
            VayouActionSheetItem(stringResource(R.string.details), VayouIcons.Info) {
                close()
                isShowingDetails = true
            }
            VayouActionSheetItem(stringResource(R.string.delete), VayouIcons.Delete) {
                close()
                actions.onDelete(song)
            }
        }
    }

    if (isShowingDetails) {
        SongDetailsDialog(song = song, onDismiss = { isShowingDetails = false })
    }
}

/** What the file is, as the library knows it. Only the lines that have something behind them. */
@Composable
private fun SongDetailsDialog(song: Song, onDismiss: () -> Unit) {
    VayouDialog(
        onDismissRequest = onDismiss,
        title = song.title.ifBlank { song.fileName },
        confirmButton = { VayouDoneButton(onClick = onDismiss) },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DetailSpacing),
        ) {
            DetailLine(stringResource(R.string.detail_file), song.fileName)
            if (song.artist.isNotBlank()) DetailLine(stringResource(R.string.detail_artist), song.artist)
            if (song.album.isNotBlank()) DetailLine(stringResource(R.string.detail_album), song.album)
            DetailLine(stringResource(R.string.detail_length), Utils.formatDurationMillis(song.durationMs))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = VayouTheme.typography.labelMedium, color = VayouTheme.colors.onSurfaceVariant)
        Text(text = value, style = VayouTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
    }
}

/** Deleting, behind a confirmation: it takes the file off the phone, not just out of a list. */
@Composable
internal fun SongDeleteDialog(count: Int, name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
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
                stringResource(R.string.delete_song_confirmation, name)
            } else {
                stringResource(R.string.delete_songs_confirmation, count)
            },
        )
    }
}

private val DetailSpacing = 12.dp

/** Throwing a list away. The tracks stay: a list is a way of looking at them, not where they live. */
@Composable
internal fun PlaylistDeleteDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    VayouDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.delete_playlist),
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
        Text(text = stringResource(R.string.delete_playlist_confirmation, name))
    }
}

/**
 * Whether this track's tags can be rewritten.
 *
 * MP3 alone: the tag is written into the file and the writer reads that one format. Everything else
 * is offered nothing rather than offered a button that fails.
 */
private val Song.isTaggable: Boolean
    get() = fileName.endsWith(".mp3", ignoreCase = true)
