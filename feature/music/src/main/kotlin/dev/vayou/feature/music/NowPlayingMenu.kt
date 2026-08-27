package dev.vayou.feature.music

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.vayou.core.media.Song
import dev.vayou.core.model.MediaLibrary
import dev.vayou.core.model.MediaPlaylists
import dev.vayou.core.ui.R as CoreUiR
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouNameDialog
import dev.vayou.core.ui.designsystem.components.VayouPickPlaylistSheet
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The three dots on the player, and everything they open.
 *
 * The same menu a row in the list gets, from the same [SongMenuButton]: two libraries in one app
 * that answer the same questions differently are two apps. What the player leaves out it leaves out
 * because the question is already answered here -- "play next" and "add to queue" would act on the
 * track that is playing, and there is no playlist to be taken out of, only a queue that may have
 * come from one.
 *
 * The sheets live with the button rather than in the screen: each is opened by exactly one entry of
 * this menu and by nothing else, and the screen above has no other use for four pieces of state.
 */
@Composable
internal fun NowPlayingMenu(song: Song, viewModel: MusicViewModel, playlists: MediaPlaylists) {
    val context = LocalContext.current

    var isAddingToPlaylist by remember { mutableStateOf(false) }
    var isNamingPlaylist by remember { mutableStateOf(false) }
    var isEditingTags by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // The system's own dialog, for deleting and for writing tags alike: from Android 11 an app may
    // only change what it wrote, and nothing in a music library was written by this one.
    val confirmWrite = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onWriteAnswered(it.resultCode == Activity.RESULT_OK)
    }
    viewModel.pendingWrite?.let { pending ->
        LaunchedEffect(pending) { confirmWrite.launch(IntentSenderRequest.Builder(pending.request).build()) }
    }

    val actions = remember(playlists, context) {
        SongActions(
            onPlayNext = null,
            onAddToQueue = null,
            onShare = { context.startActivity(Intent.createChooser(viewModel.shareIntent(listOf(it)), null)) },
            favourite = null,
            onAddToPlaylist = { isAddingToPlaylist = true },
            onRemoveFromPlaylist = null,
            onDelete = { isDeleting = true },
            onEditTags = { isEditingTags = true },
        )
    }

    SongMenuButton(
        song = song,
        actions = actions,
        button = { onClick ->
            VayouIconButton(onClick = onClick) {
                Icon(
                    imageVector = VayouIcons.MoreVert,
                    contentDescription = stringResource(CoreUiR.string.more_options),
                    tint = VayouTheme.colors.onSurface,
                )
            }
        },
    )

    if (isAddingToPlaylist) {
        VayouPickPlaylistSheet(
            playlists = playlists.of(MediaLibrary.Music),
            onPick = { viewModel.addToPlaylist(it.id, listOf(song)) },
            // Made and filled in one move, as in the library: a listener who opens this with no
            // lists wants the track in the one they are about to make.
            onNew = {
                isNamingPlaylist = true
                isAddingToPlaylist = false
            },
            onDismiss = { isAddingToPlaylist = false },
        )
    }

    if (isNamingPlaylist) {
        VayouNameDialog(
            title = stringResource(R.string.new_playlist),
            initialName = "",
            label = stringResource(R.string.playlist_name),
            onDismiss = { isNamingPlaylist = false },
            onDone = { name ->
                viewModel.createPlaylist(name) { id -> viewModel.addToPlaylist(id, listOf(song)) }
                isNamingPlaylist = false
            },
        )
    }

    if (isEditingTags) {
        TagEditor(
            song = song,
            isSaving = viewModel.isWritingTags,
            onDismiss = { isEditingTags = false },
            onSave = { tags, cover -> viewModel.editTags(song, tags, cover) },
        )
    }

    // Closed by the write finishing rather than by the press that started it: a write that needs
    // the system's permission takes a dialog and a moment, and a form that vanished first left
    // nothing on screen saying whether anything had been written.
    var wasWriting by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.isWritingTags) {
        if (viewModel.isWritingTags) {
            wasWriting = true
        } else if (wasWriting) {
            wasWriting = false
            isEditingTags = false
        }
    }

    if (isDeleting) {
        SongDeleteDialog(
            count = 1,
            name = song.title.ifBlank { song.fileName },
            onDismiss = { isDeleting = false },
            onConfirm = {
                viewModel.deleteSongs(listOf(song))
                isDeleting = false
            },
        )
    }
}

/**
 * The star, beside the title rather than inside the menu.
 *
 * It is the one action here a listener repeats -- the rest are done to a track once, if ever -- and
 * a thing done often should not be two taps deep. Outside the pane that slides on a track change:
 * the star belongs to what is playing now, and sliding it away with the last track would leave the
 * new one unstarred for the length of the animation.
 */
@Composable
internal fun NowPlayingStar(song: Song, viewModel: MusicViewModel, favouriteUris: List<String>) {
    val isFavourite = song.uriString in favouriteUris
    VayouIconButton(onClick = { viewModel.toggleFavourite(song) }) {
        Icon(
            imageVector = if (isFavourite) VayouIcons.StarFilled else VayouIcons.StarOutlined,
            contentDescription = stringResource(if (isFavourite) R.string.unfavourite else R.string.favourite),
            tint = if (isFavourite) VayouTheme.colors.accent else VayouTheme.colors.onSurfaceVariant,
        )
    }
}
