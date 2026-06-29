package dev.vayou.playlist

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.Sort
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouEmptyState
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.feature.videopicker.state.rememberSelectionManager

@Composable
fun FavoritesScreen(
    onNavigateUp: () -> Unit,
    onPlayVideos: (List<Uri>) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val selectionManager = rememberSelectionManager()
    val selectedCount = selectionManager.selectedVideos.size

    var sort by remember { mutableStateOf(Sort(Sort.By.TITLE, Sort.Order.ASCENDING)) }
    val sortedVideos = remember(videos, sort) { videos.sortedWith(sort.videoComparator()) }
    var showSortSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = selectionManager.isInSelectionMode) {
        selectionManager.exitSelectionMode()
    }

    VayouScaffold(
        topBar = {
            VayouTopAppBar(
                title = if (selectionManager.isInSelectionMode) "" else stringResource(R.string.video_favorites),
                navigationIcon = {
                    if (selectionManager.isInSelectionMode) {
                        SelectionNavigationIcon(
                            selectedCount = selectedCount,
                            totalCount = videos.size,
                            onExit = { selectionManager.exitSelectionMode() },
                        )
                    } else {
                        VayouIconButton(onClick = onNavigateUp) {
                            Icon(VayouIcons.ArrowBack, contentDescription = stringResource(R.string.navigate_up))
                        }
                    }
                },
                actions = {
                    if (selectionManager.isInSelectionMode) {
                        SelectionModeActions(
                            removeIcon = VayouIcons.StarFilled,
                            removeContentDescription = stringResource(R.string.remove_from_favorites),
                            selectedCount = selectedCount,
                            totalCount = videos.size,
                            onPlay = {
                                onPlayVideos(selectionManager.allSelectedVideos.map { Uri.parse(it.uriString) })
                                selectionManager.clearSelection()
                            },
                            onRemove = {
                                viewModel.removeVideos(selectionManager.allSelectedVideos.map { it.uriString })
                                selectionManager.clearSelection()
                            },
                            onToggleSelectAll = {
                                if (selectedCount != videos.size) {
                                    videos.forEach { selectionManager.selectVideo(it) }
                                } else {
                                    selectionManager.clearSelection()
                                }
                            },
                        )
                    } else {
                        LayoutToggleButton(
                            gridMode = preferences.mediaLayoutMode == MediaLayoutMode.GRID,
                            onToggle = viewModel::toggleLayoutMode,
                        )
                        SortButton(onClick = { showSortSheet = true })
                    }
                },
            )
        },
    ) {
        if (videos.isEmpty()) {
            VayouEmptyState(
                icon = VayouIcons.StarFilled,
                title = stringResource(R.string.no_favorites_yet),
            )
        } else {
            PlaylistVideoGrid(
                videos = sortedVideos,
                preferences = preferences,
                selectionManager = selectionManager,
                origin = PlaylistVideoOrigin.Favorites,
                onPlay = { uri -> onPlayVideos(listOf(uri)) },
                onRemove = { viewModel.removeVideo(it.uriString) },
            )
        }
    }

    if (showSortSheet) {
        SortSheet(sort = sort, onSortChange = { sort = it }, onDismiss = { showSortSheet = false })
    }
}
