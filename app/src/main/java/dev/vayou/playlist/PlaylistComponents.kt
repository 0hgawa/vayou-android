package dev.vayou.playlist

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.Sort
import dev.vayou.core.model.Video
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouBottomSheet
import dev.vayou.core.ui.designsystem.components.VayouBottomSheetTitle
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.feature.videopicker.composables.VideoItem
import dev.vayou.feature.videopicker.state.SelectionManager

@Composable
fun SortButton(onClick: () -> Unit) {
    VayouIconButton(onClick = onClick) {
        Icon(VayouIcons.Sort, contentDescription = stringResource(R.string.sort))
    }
}

@Composable
fun AddVideoButton(onClick: () -> Unit) {
    VayouIconButton(onClick = onClick) {
        Icon(VayouIcons.Add, contentDescription = stringResource(R.string.add_to_playlist))
    }
}

@Composable
fun LayoutToggleButton(gridMode: Boolean, onToggle: () -> Unit) {
    VayouIconButton(onClick = onToggle) {
        Icon(
            imageVector = if (gridMode) VayouIcons.ListView else VayouIcons.GridView,
            contentDescription = stringResource(if (gridMode) R.string.list else R.string.grid),
        )
    }
}

@Composable
fun SortSheet(sort: Sort, onSortChange: (Sort) -> Unit, onDismiss: () -> Unit) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        VayouBottomSheetTitle(text = stringResource(R.string.sort))
        Sort.By.entries.forEach { sortBy ->
            val isSelected = sort.by == sortBy
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        imageVector = when (sortBy) {
                            Sort.By.TITLE -> VayouIcons.Title
                            Sort.By.LENGTH -> VayouIcons.Length
                            Sort.By.DATE -> VayouIcons.Calendar
                            Sort.By.SIZE -> VayouIcons.Size
                            Sort.By.PATH -> VayouIcons.Location
                        },
                        contentDescription = null,
                        tint = if (isSelected) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                },
                text = {
                    Text(
                        text = when (sortBy) {
                            Sort.By.TITLE -> stringResource(R.string.title)
                            Sort.By.LENGTH -> stringResource(R.string.duration)
                            Sort.By.DATE -> stringResource(R.string.date)
                            Sort.By.SIZE -> stringResource(R.string.size)
                            Sort.By.PATH -> stringResource(R.string.location)
                        },
                        style = VayouTheme.typography.bodyMedium,
                        color = if (isSelected) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                    )
                },
                trailingIcon = if (isSelected) ({
                    Icon(
                        imageVector = if (sort.order == Sort.Order.ASCENDING) VayouIcons.ArrowUpward else VayouIcons.ArrowDownward,
                        contentDescription = null,
                        tint = VayouTheme.colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }) else null,
                onClick = {
                    onSortChange(
                        if (isSelected) {
                            sort.copy(order = if (sort.order == Sort.Order.ASCENDING) Sort.Order.DESCENDING else Sort.Order.ASCENDING)
                        } else {
                            sort.copy(by = sortBy)
                        },
                    )
                },
                contentPadding = PaddingValues(horizontal = VayouTheme.spacing.lg),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SelectionNavigationIcon(
    selectedCount: Int,
    totalCount: Int,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(VayouTheme.colors.surfaceContainerHigh)
            .clickable { onExit() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(VayouIcons.Close, contentDescription = stringResource(R.string.navigate_up))
        Text(
            text = stringResource(R.string.m_n_selected, selectedCount, totalCount),
            style = VayouTheme.typography.labelLarge,
        )
    }
}

enum class PlaylistVideoOrigin { Favorites, Playlist, Private }

@Composable
fun PlaylistVideoGrid(
    videos: List<Video>,
    preferences: ApplicationPreferences,
    selectionManager: SelectionManager,
    origin: PlaylistVideoOrigin,
    onPlay: (Uri) -> Unit,
    onRemove: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridMode = preferences.mediaLayoutMode == MediaLayoutMode.GRID
    val itemSpacing = MediaListLayoutDefaults.itemSpacing(preferences.mediaLayoutMode)
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = if (gridMode) GridCells.Adaptive(minSize = 130.dp) else GridCells.Fixed(1),
        contentPadding = PaddingValues(
            horizontal = MediaListLayoutDefaults.outerHorizontal(preferences.mediaLayoutMode),
            vertical = 8.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        items(videos, key = { it.uriString }) { video ->
            val removeCallback: () -> Unit = { onRemove(video) }
            VideoItem(
                video = video,
                isRecentlyPlayedVideo = false,
                preferences = preferences,
                selected = selectionManager.isVideoSelected(video),
                inSelectionMode = selectionManager.isInSelectionMode,
                onClick = {
                    if (selectionManager.isInSelectionMode) {
                        selectionManager.toggleVideoSelection(video)
                    } else {
                        onPlay(Uri.parse(video.uriString))
                    }
                },
                onLongClick = { selectionManager.toggleVideoSelection(video) },
                onRemoveFromFavoritesClick = removeCallback.takeIf { origin == PlaylistVideoOrigin.Favorites },
                onRemoveFromPlaylistClick = removeCallback.takeIf { origin == PlaylistVideoOrigin.Playlist },
                onRemoveFromPrivateClick = removeCallback.takeIf { origin == PlaylistVideoOrigin.Private },
            )
        }
    }
}

@Composable
fun AddVideoSheet(
    videos: List<Video>,
    onAdd: (Video) -> Unit,
    onDismiss: () -> Unit,
) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        VayouBottomSheetTitle(text = stringResource(R.string.add_to_playlist))
        if (videos.isEmpty()) {
            Text(
                text = stringResource(R.string.no_videos_yet),
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(videos, key = { it.uriString }) { video ->
                    AddVideoRow(video = video, onClick = { onAdd(video) })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AddVideoRow(video: Video, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(VayouTheme.colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = video.uriString,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.displayName,
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (video.formattedDuration.isNotBlank()) {
                Text(
                    text = video.formattedDuration,
                    style = VayouTheme.typography.bodySmall,
                    color = VayouTheme.colors.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = VayouIcons.Add,
            contentDescription = null,
            tint = VayouTheme.colors.accent,
            modifier = Modifier.size(VayouTheme.iconSize.sm),
        )
    }
}

@Composable
fun SelectionModeActions(
    removeIcon: ImageVector,
    removeContentDescription: String,
    selectedCount: Int,
    totalCount: Int,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onToggleSelectAll: () -> Unit,
) {
    VayouIconButton(onClick = onPlay) {
        Icon(VayouIcons.Play, contentDescription = stringResource(R.string.play))
    }
    VayouIconButton(onClick = onRemove) {
        Icon(removeIcon, contentDescription = removeContentDescription)
    }
    VayouIconButton(onClick = onToggleSelectAll) {
        Icon(
            imageVector = if (selectedCount != totalCount) VayouIcons.SelectAll else VayouIcons.DeselectAll,
            contentDescription = if (selectedCount != totalCount) stringResource(R.string.select_all) else stringResource(R.string.deselect_all),
        )
    }
}
