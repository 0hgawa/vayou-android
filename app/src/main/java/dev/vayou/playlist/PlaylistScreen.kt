package dev.vayou.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.Playlist
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.CancelButton
import dev.vayou.core.ui.components.NameInputDialog
import dev.vayou.core.ui.components.PlaylistThumbnail
import dev.vayou.core.ui.components.RenameDialog
import dev.vayou.core.ui.components.VayouDialog
import dev.vayou.core.ui.components.VayouEmptyState
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouSegmentedListItem
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenu
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenuItem
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.feature.videopicker.composables.selectionTrailingFor

private val PrivateIconTint = Color(0xFF7C5CBF)

@Composable
fun PlaylistScreen(
    onPlaylistClick: (id: String) -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onPrivateClick: () -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val mode = preferences.mediaLayoutMode
    val gridMode = mode == MediaLayoutMode.GRID
    val freeSpanPadding = MediaListLayoutDefaults.freeSpanHorizontal(mode)
    val itemSpacing = MediaListLayoutDefaults.itemSpacing(mode)
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showBulkDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val inSelectionMode = selectedIds.isNotEmpty()
    val selectedCount = selectedIds.size
    val allSelected = selectedCount == playlists.size

    val toggleSelection: (String) -> Unit = { id ->
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    BackHandler(enabled = inSelectionMode) {
        selectedIds = emptySet()
    }

    VayouScaffold(
        topBar = {
            VayouTopAppBar(
                title = if (inSelectionMode) "" else stringResource(R.string.playlists),
                navigationIcon = {
                    if (inSelectionMode) {
                        SelectionNavigationIcon(
                            selectedCount = selectedCount,
                            totalCount = playlists.size,
                            onExit = { selectedIds = emptySet() },
                        )
                    }
                },
                actions = {
                    if (inSelectionMode) {
                        VayouIconButton(onClick = {
                            selectedIds = if (allSelected) emptySet() else playlists.mapTo(mutableSetOf()) { it.id }
                        }) {
                            Icon(
                                imageVector = if (allSelected) VayouIcons.DeselectAll else VayouIcons.SelectAll,
                                contentDescription = stringResource(if (allSelected) R.string.deselect_all else R.string.select_all),
                            )
                        }
                        VayouIconButton(onClick = { showBulkDeleteDialog = true }) {
                            Icon(VayouIcons.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    } else {
                        LayoutToggleButton(gridMode = gridMode, onToggle = viewModel::toggleLayoutMode)
                        VayouIconButton(onClick = { showCreateDialog = true }) {
                            Icon(VayouIcons.Add, contentDescription = stringResource(R.string.add_playlist))
                        }
                    }
                },
            )
        },
    ) {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = if (gridMode) GridCells.Adaptive(minSize = 150.dp) else GridCells.Fixed(1),
            contentPadding = PaddingValues(
                horizontal = MediaListLayoutDefaults.outerHorizontal(mode),
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                SpecialCollectionsHeader(
                    onFavoritesClick = onFavoritesClick,
                    onPrivateClick = onPrivateClick,
                    modifier = Modifier.padding(
                        start = freeSpanPadding,
                        end = freeSpanPadding,
                        bottom = 12.dp,
                    ),
                )
            }
            if (playlists.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    VayouEmptyState(
                        icon = VayouIcons.PlaylistFilled,
                        title = stringResource(R.string.no_playlists_yet),
                        modifier = Modifier.heightIn(min = 280.dp),
                    )
                }
            } else {
                item(key = "section_label", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.saved_playlists),
                        style = VayouTheme.typography.labelMedium,
                        color = VayouTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = freeSpanPadding, bottom = 4.dp),
                    )
                }
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistEntry(
                        thumbnailUris = playlist.thumbnailUris,
                        title = playlist.name,
                        subtitle = pluralStringResource(R.plurals.videos_count, playlist.itemCount, playlist.itemCount),
                        preferences = preferences,
                        selected = playlist.id in selectedIds,
                        inSelectionMode = inSelectionMode,
                        onClick = {
                            if (inSelectionMode) toggleSelection(playlist.id) else onPlaylistClick(playlist.id)
                        },
                        onLongClick = { toggleSelection(playlist.id) },
                        onRenameClick = { renameTarget = playlist },
                        onDeleteClick = { deleteTarget = playlist },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        NameInputDialog(
            title = stringResource(R.string.add_playlist),
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    renameTarget?.let { playlist ->
        RenameDialog(
            name = playlist.name,
            onDone = { name ->
                viewModel.renamePlaylist(playlist.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { playlist ->
        DeletePlaylistDialog(
            name = playlist.name,
            onConfirm = {
                viewModel.deletePlaylist(playlist.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showBulkDeleteDialog) {
        DeletePlaylistsDialog(
            count = selectedCount,
            onConfirm = {
                viewModel.deletePlaylists(selectedIds)
                selectedIds = emptySet()
                showBulkDeleteDialog = false
            },
            onDismiss = { showBulkDeleteDialog = false },
        )
    }
}

@Composable
private fun SpecialCollectionsHeader(
    onFavoritesClick: () -> Unit,
    onPrivateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpecialCollectionPill(
            icon = VayouIcons.StarFilled,
            iconTint = VayouTheme.colors.accent,
            title = stringResource(R.string.video_favorites),
            onClick = onFavoritesClick,
            modifier = Modifier.weight(1f),
        )
        SpecialCollectionPill(
            icon = VayouIcons.Lock,
            iconTint = PrivateIconTint,
            title = stringResource(R.string.private_section),
            onClick = onPrivateClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SpecialCollectionPill(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .clip(VayouTheme.shapes.medium)
            .background(VayouTheme.colors.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = title,
            style = VayouTheme.typography.titleSmall,
            color = VayouTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaylistEntry(
    thumbnailUris: List<String>,
    title: String,
    subtitle: String,
    preferences: ApplicationPreferences,
    selected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (preferences.mediaLayoutMode) {
        MediaLayoutMode.LIST -> PlaylistEntryRow(
            thumbnailUris = thumbnailUris,
            title = title,
            subtitle = subtitle,
            selected = selected,
            inSelectionMode = inSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
            modifier = modifier,
        )
        MediaLayoutMode.GRID -> PlaylistEntryCard(
            thumbnailUris = thumbnailUris,
            title = title,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlaylistEntryCard(
    thumbnailUris: List<String>,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        selected = selected,
        containerColor = Color.Transparent,
        contentPadding = MediaListLayoutDefaults.GridItemPadding,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlaylistThumbnail(
                    thumbnailUris = thumbnailUris,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f),
                )
                Text(
                    text = title,
                    style = VayouTheme.typography.titleSmall,
                    color = VayouTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

@Composable
private fun PlaylistEntryRow(
    thumbnailUris: List<String>,
    title: String,
    subtitle: String,
    selected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier,
) {
    VayouSegmentedListItem(
        modifier = modifier,
        selected = selected,
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        leadingContent = {
            PlaylistThumbnail(
                thumbnailUris = thumbnailUris,
                modifier = Modifier
                    .width(min(130.dp, LocalConfiguration.current.screenWidthDp.dp * 0.32f))
                    .aspectRatio(16f / 10f),
            )
        },
        content = {
            Text(
                text = title,
                style = VayouTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = VayouTheme.typography.bodySmall,
                color = VayouTheme.colors.onSurfaceVariant,
            )
        },
        trailingContent = when {
            inSelectionMode -> selectionTrailingFor(inSelectionMode, selected)
            else -> {
                { PlaylistEntryMenu(onRenameClick = onRenameClick, onDeleteClick = onDeleteClick) }
            }
        },
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
private fun PlaylistEntryMenu(
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        VayouIconButton(onClick = { expanded = true }, modifier = Modifier.width(36.dp)) {
            Icon(imageVector = VayouIcons.MoreVert, contentDescription = null)
        }
        VayouDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            VayouDropdownMenuItem(
                text = stringResource(R.string.rename),
                icon = VayouIcons.Edit,
                onClick = { expanded = false; onRenameClick() },
            )
            VayouDropdownMenuItem(
                text = stringResource(R.string.delete),
                icon = VayouIcons.Delete,
                onClick = { expanded = false; onDeleteClick() },
            )
        }
    }
}

@Composable
private fun DeletePlaylistDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    VayouDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_playlist)) },
        content = { Text(stringResource(R.string.delete_playlist_confirm, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
}

@Composable
private fun DeletePlaylistsDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    VayouDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_playlist)) },
        content = {
            Text(pluralStringResource(R.plurals.delete_n_playlists_confirm, count, count))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
}
