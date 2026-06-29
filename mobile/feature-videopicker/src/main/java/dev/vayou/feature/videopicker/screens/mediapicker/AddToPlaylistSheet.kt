package dev.vayou.feature.videopicker.screens.mediapicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.Playlist
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.NameInputDialog
import dev.vayou.core.ui.components.PlaylistThumbnail
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouBottomSheet
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onPlaylistSelected: (playlistId: String) -> Unit,
    onCreatePlaylist: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    VayouBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.add_to_playlist),
            style = VayouTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.new_playlist), style = VayouTheme.typography.bodyMedium)
            },
            leadingContent = {
                Icon(
                    imageVector = VayouIcons.SaveToPlaylist,
                    contentDescription = null,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable { showCreateDialog = true },
        )
        if (playlists.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = {
                            Text(playlist.name, style = VayouTheme.typography.bodyMedium)
                        },
                        supportingContent = {
                            Text(
                                text = pluralStringResource(R.plurals.videos_count, playlist.itemCount, playlist.itemCount),
                                style = VayouTheme.typography.labelSmall,
                                color = VayouTheme.colors.onSurfaceVariant,
                            )
                        },
                        leadingContent = {
                            PlaylistThumbnail(
                                thumbnailUris = playlist.thumbnailUris,
                                modifier = Modifier.width(56.dp),
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onPlaylistSelected(playlist.id) },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        NameInputDialog(
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                onCreatePlaylist(name)
                onDismiss()
            },
            onDismiss = { showCreateDialog = false },
        )
    }
}
