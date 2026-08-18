package dev.vayou.feature.network

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouEmptyState
import dev.vayou.core.ui.designsystem.components.VayouListHeader
import dev.vayou.core.ui.designsystem.components.VayouSortOption
import dev.vayou.core.ui.designsystem.components.VayouSortSheet

/** The channel lists the viewer has, with the starred channels above them. */
@Composable
internal fun PlaylistList(
    playlists: List<SavedPlaylist>,
    onOpen: (SavedPlaylist) -> Unit,
    onOpenFavourites: () -> Unit,
    onRename: (SavedPlaylist) -> Unit,
    onRemove: (SavedPlaylist) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { VayouListHeader(label = stringResource(R.string.saved_playlists)) }

        item(key = "channel_favourites") {
            NetworkRow(
                icon = { NetworkTile(VayouIcons.StarFilled) },
                title = stringResource(R.string.channel_favourites),
                onClick = onOpenFavourites,
            )
        }

        if (playlists.isEmpty()) {
            item { VayouEmptyState(VayouIcons.Tv, stringResource(R.string.no_playlists_saved)) }
        }

        items(playlists, key = { it.url }) { playlist ->
            NetworkRow(
                icon = { NetworkTile(VayouIcons.Tv) },
                title = playlist.name,
                subtitle = playlist.url,
                onClick = { onOpen(playlist) },
                trailingContent = {
                    ItemOverflowMenu(
                        name = playlist.name,
                        subtitle = playlist.url,
                        onRename = { onRename(playlist) },
                        onRemove = { onRemove(playlist) },
                        leading = { NetworkTile(VayouIcons.Tv) },
                    )
                },
            )
        }
    }
}

@Composable
internal fun ChannelFavourites(
    channels: List<PlaylistChannel>,
    searchQuery: String,
    onChannelClick: (PlaylistChannel, List<PlaylistChannel>) -> Unit,
    onToggleFavourite: (PlaylistChannel) -> Unit,
    selectedUrls: Set<String>,
    onToggleSelection: (PlaylistChannel) -> Unit,
) {
    if (channels.isEmpty()) {
        VayouEmptyState(VayouIcons.StarOutlined, stringResource(R.string.no_favourites_yet))
        return
    }
    val shown = remember(channels, searchQuery) { channels.matching(searchQuery) }
    if (shown.isEmpty()) {
        VayouEmptyState(VayouIcons.Search, stringResource(R.string.no_results_found))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(shown, key = { it.url }) { channel ->
            ChannelRow(
                channel = channel,
                isFavourite = true,
                onClick = {
                    if (selectedUrls.isEmpty()) onChannelClick(channel, shown) else onToggleSelection(channel)
                },
                onToggleFavourite = { onToggleFavourite(channel) },
                onLongClick = { onToggleSelection(channel) },
                isSelecting = selectedUrls.isNotEmpty(),
                isSelected = channel.url in selectedUrls,
            )
        }
    }
}

/**
 * One channel list, opened.
 *
 * Hundreds of channels arrive in the provider's own order, which groups them but buries any one
 * name. Sorting by name drops the group headers on purpose: an A-Z list broken into twenty
 * alphabets is not sorted in any way a reader can use.
 */
@Composable
internal fun PlaylistDetail(
    isLoading: Boolean,
    error: NetworkError?,
    channels: List<PlaylistChannel>,
    favouriteUrls: Set<String>,
    searchQuery: String,
    selectedGroup: String?,
    onChannelClick: (PlaylistChannel, List<PlaylistChannel>) -> Unit,
    onToggleFavourite: (PlaylistChannel) -> Unit,
    selectedUrls: Set<String>,
    onToggleSelection: (PlaylistChannel) -> Unit,
) {
    when {
        isLoading -> {
            Waiting()
            return
        }
        error != null -> {
            ErrorState(error)
            return
        }
        channels.isEmpty() -> {
            VayouEmptyState(VayouIcons.Video, stringResource(R.string.no_files_found))
            return
        }
    }

    var isSortedByName by rememberSaveable { mutableStateOf(false) }
    var isAscending by rememberSaveable { mutableStateOf(true) }
    var isSortSheetOpen by remember { mutableStateOf(false) }

    if (isSortSheetOpen) {
        VayouSortSheet(
            title = stringResource(R.string.sort),
            options = listOf(
                VayouSortOption(stringResource(R.string.sort_by_group), VayouIcons.Folder),
                VayouSortOption(stringResource(R.string.sort_by_name), VayouIcons.Title),
            ),
            selectedIndex = if (isSortedByName) 1 else 0,
            isAscending = isAscending,
            onSelect = { index ->
                val picked = index == 1
                if (picked == isSortedByName) isAscending = !isAscending else isSortedByName = picked
            },
            onDismiss = { isSortSheetOpen = false },
        )
    }

    val header: @Composable () -> Unit = {
        VayouListHeader(
            label = stringResource(if (isSortedByName) R.string.sort_by_name else R.string.sort_by_group),
            isAscending = isAscending,
            onClick = { isSortSheetOpen = true },
        )
    }

    // Grouped is the arrival order; anything narrowed or re-sorted is one flat list, because a
    // filtered list broken into headers is mostly headers.
    val isNarrowed = selectedGroup != null || searchQuery.isNotBlank() || isSortedByName

    if (!isNarrowed) {
        val grouped = remember(channels) { channels.groupBy { it.group.orEmpty() } }
        // Flattened in the order the groups are drawn, not the order they arrived: this is what
        // the player steps through, and "next" has to mean the row below the one just left.
        val listed = remember(grouped) { grouped.values.flatten() }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { header() }
            grouped.forEach { (group, inGroup) ->
                if (group.isNotEmpty()) {
                    item(key = "header_$group") { VayouListHeader(label = group) }
                }
                // Indexed, because one provider's list can carry the same address twice and a
                // duplicate key crashes the list rather than merely drawing it twice.
                itemsIndexed(inGroup, key = { index, _ -> "${group}_$index" }) { _, channel ->
                    ChannelRow(
                        channel = channel,
                        isFavourite = channel.url in favouriteUrls,
                        onClick = {
                            if (selectedUrls.isEmpty()) {
                                onChannelClick(channel, listed)
                            } else {
                                onToggleSelection(channel)
                            }
                        },
                        onToggleFavourite = { onToggleFavourite(channel) },
                        onLongClick = { onToggleSelection(channel) },
                        isSelecting = selectedUrls.isNotEmpty(),
                        isSelected = channel.url in selectedUrls,
                    )
                }
            }
        }
        return
    }

    val narrowed = remember(channels, selectedGroup, searchQuery) {
        channels.filter { selectedGroup == null || it.group == selectedGroup }.matching(searchQuery)
    }
    if (narrowed.isEmpty()) {
        VayouEmptyState(
            icon = if (searchQuery.isNotBlank()) VayouIcons.Search else VayouIcons.Filter,
            title = stringResource(R.string.no_results_found),
        )
        return
    }
    // Sorted once per change of what it sorts by, not once per recomposition: called inline in
    // items() it re-sorted the whole list every time anything on this screen redrew -- a channel
    // being starred, the search field gaining focus.
    val shown = remember(narrowed, isSortedByName, isAscending) {
        when {
            !isSortedByName -> narrowed
            isAscending -> narrowed.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            else -> narrowed.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { header() }
        itemsIndexed(shown, key = { index, channel -> "${channel.url}_$index" }) { _, channel ->
            ChannelRow(
                channel = channel,
                isFavourite = channel.url in favouriteUrls,
                onClick = {
                    if (selectedUrls.isEmpty()) onChannelClick(channel, shown) else onToggleSelection(channel)
                },
                onToggleFavourite = { onToggleFavourite(channel) },
                onLongClick = { onToggleSelection(channel) },
                isSelecting = selectedUrls.isNotEmpty(),
                isSelected = channel.url in selectedUrls,
            )
        }
    }
}

private fun List<PlaylistChannel>.matching(query: String): List<PlaylistChannel> =
    if (query.isBlank()) this else filter { it.name.contains(query, ignoreCase = true) }
