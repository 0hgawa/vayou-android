package dev.vayou.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.MediaPlaylist
import dev.vayou.core.model.SmartPlaylist
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouActionSheet
import dev.vayou.core.ui.designsystem.components.VayouActionSheetItem
import dev.vayou.core.ui.designsystem.components.VayouArtwork
import dev.vayou.core.ui.designsystem.components.VayouEmptyState
import dev.vayou.core.ui.designsystem.components.VayouOverflowButton
import dev.vayou.core.ui.designsystem.components.VayouSegmentedListItem
import dev.vayou.core.ui.theme.VayouTheme

/**
 * The lists the viewer keeps.
 *
 * A flat list on purpose: unlike a folder, a playlist has nothing to browse into, and the row
 * already says how much is in it.
 *
 * Favourites is pinned above the rest and carries no menu. It is always there and is never renamed
 * or deleted, which is the whole difference between a list the app derives and one somebody made.
 */
@Composable
internal fun PlaylistList(
    playlists: List<MediaPlaylist>,
    favouriteCount: Int,
    privateCount: Int,
    countOf: (MediaPlaylist) -> Int,
    onOpenFavourites: () -> Unit,
    onOpenPrivate: () -> Unit,
    onOpen: (MediaPlaylist) -> Unit,
    onRename: (MediaPlaylist) -> Unit,
    onDelete: (MediaPlaylist) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
    ) {
        item(key = SmartPlaylist.Favourites) {
            PlaylistRow(
                icon = VayouIcons.StarFilled,
                name = stringResource(R.string.favourites),
                count = favouriteCount,
                onClick = onOpenFavourites,
            )
        }

        // Pinned like Favourites, and for the same reason: it is always there and is never renamed
        // or deleted. Shown even while empty, because a folder that appears only once something is
        // in it cannot be the folder you put the first thing into.
        item(key = SmartPlaylist.Private) {
            PlaylistRow(
                icon = VayouIcons.Lock,
                name = stringResource(R.string.private_videos),
                count = privateCount,
                onClick = onOpenPrivate,
            )
        }

        if (playlists.isEmpty()) {
            item { VayouEmptyState(VayouIcons.Playlist, stringResource(R.string.no_playlists)) }
        }

        items(playlists, key = { it.id }) { playlist ->
            val count = countOf(playlist)
            PlaylistRow(
                icon = VayouIcons.Playlist,
                name = playlist.name,
                count = count,
                onClick = { onOpen(playlist) },
                trailing = {
                    var isMenuOpen by remember { mutableStateOf(false) }
                    VayouOverflowButton(onClick = { isMenuOpen = true })
                    if (isMenuOpen) {
                        VayouActionSheet(
                            title = playlist.name,
                            subtitle = pluralStringResource(R.plurals.n_videos, count, count),
                            onDismiss = { isMenuOpen = false },
                        ) {
                            VayouActionSheetItem(stringResource(R.string.rename), VayouIcons.Edit) {
                                isMenuOpen = false
                                onRename(playlist)
                            }
                            VayouActionSheetItem(stringResource(R.string.delete_playlist), VayouIcons.Delete) {
                                isMenuOpen = false
                                onDelete(playlist)
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    count: Int,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    VayouSegmentedListItem(
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        onClick = onClick,
        leadingContent = {
            VayouArtwork(model = null, modifier = Modifier.size(PlaylistTileSize), icon = icon)
        },
        content = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { SupportingLine(pluralStringResource(R.plurals.n_videos, count, count)) },
        trailingContent = trailing,
    )
}

/** The same square the network rows and the music library lead with. */
private val PlaylistTileSize = 56.dp
