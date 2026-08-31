package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.vayou.core.model.MediaPlaylist
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.VayouIcons

/**
 * Which list a track or a film goes into.
 *
 * "New list" sits at the top rather than the bottom: the reason anyone opens this with nothing in it
 * is to make the first one, and a button under an empty space is a button nobody finds.
 *
 * One sheet for both libraries. It was two, and the music copy was the poorer of them -- it had no
 * way to make a list at all, so a listener with none was shown an empty sheet and left there.
 */
@Composable
fun VayouPickPlaylistSheet(
    playlists: List<MediaPlaylist>,
    onPick: (MediaPlaylist) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    VayouSheet(onDismissRequest = onDismiss) {
        VayouSheetTitle(text = stringResource(R.string.add_to_playlist))
        VayouActionSheetItem(text = stringResource(R.string.new_playlist), icon = VayouIcons.Add, onClick = onNew)

        LazyColumn(modifier = Modifier.heightIn(max = VayouSheetDefaults.ListMaxHeight)) {
            items(playlists, key = { it.id }) { playlist ->
                VayouActionSheetItem(text = playlist.name, icon = VayouIcons.Playlist) {
                    onPick(playlist)
                    onDismiss()
                }
            }
        }
        Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
    }
}
