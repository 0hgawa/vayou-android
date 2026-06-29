package dev.vayou.feature.videopicker.screens.mediapicker

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.vayou.core.ui.designsystem.components.VayouBottomSheet
import dev.vayou.core.ui.designsystem.components.VayouBottomSheetTitle
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dev.vayou.core.common.storagePermission
import dev.vayou.core.media.services.MediaService
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Folder
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.MediaViewMode
import dev.vayou.core.model.Playlist
import dev.vayou.core.model.Sort
import dev.vayou.core.model.Video
import dev.vayou.core.ui.R
import dev.vayou.core.ui.base.DataState
import dev.vayou.core.ui.components.CancelButton
import dev.vayou.core.ui.components.DoneButton
import dev.vayou.core.ui.components.VayouDialog
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.composables.PermissionMissingView
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.extensions.copy
import dev.vayou.core.ui.preview.DayNightPreview
import dev.vayou.core.ui.preview.VideoPickerPreviewParameterProvider
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.feature.videopicker.composables.CenterCircularProgressBar
import dev.vayou.feature.videopicker.composables.InfoChip
import dev.vayou.feature.videopicker.composables.MediaView
import dev.vayou.feature.videopicker.composables.NoVideosFound
import dev.vayou.core.ui.components.RenameDialog
import dev.vayou.feature.videopicker.extensions.name
import dev.vayou.feature.videopicker.composables.VideoInfoDialog
import dev.vayou.feature.videopicker.state.SelectedFolder
import dev.vayou.feature.videopicker.state.SelectedVideo
import dev.vayou.feature.videopicker.state.rememberSelectionManager
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenu

@Composable
fun MediaPickerRoute(
    viewModel: MediaPickerViewModel = hiltViewModel(),
    onPlayVideo: (uri: Uri) -> Unit,
    onPlayVideos: (uris: List<Uri>) -> Unit,
    onFolderClick: (folderPath: String) -> Unit,
    onSearchClick: () -> Unit,
    onNavigateUp: () -> Unit,
    topBarActions: @Composable () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favoriteUris by viewModel.favoriteUris.collectAsStateWithLifecycle()
    val privateUris by viewModel.privateUris.collectAsStateWithLifecycle()

    MediaPickerScreen(
        uiState = uiState,
        playlists = playlists,
        favoriteUris = favoriteUris,
        privateUris = privateUris,
        onPlayVideo = onPlayVideo,
        onPlayVideos = onPlayVideos,
        onNavigateUp = onNavigateUp,
        onFolderClick = onFolderClick,
        onSearchClick = onSearchClick,
        onEvent = viewModel::onEvent,
        topBarActions = topBarActions,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
internal fun MediaPickerScreen(
    uiState: MediaPickerUiState,
    playlists: List<Playlist> = emptyList(),
    favoriteUris: Set<String> = emptySet(),
    privateUris: Set<String> = emptySet(),
    onNavigateUp: () -> Unit = {},
    onPlayVideo: (Uri) -> Unit = {},
    onPlayVideos: (List<Uri>) -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onEvent: (MediaPickerUiEvent) -> Unit = {},
    topBarActions: @Composable () -> Unit = {},
) {
    val selectionManager = rememberSelectionManager()
    val permissionState = rememberPermissionState(permission = storagePermission)
    val lazyGridState = rememberLazyGridState()
    var showRenameActionFor: Video? by rememberSaveable { mutableStateOf(null) }
    var showInfoActionFor: Video? by rememberSaveable { mutableStateOf(null) }
    var showAddToPlaylistFor: List<String>? by remember { mutableStateOf(null) }
    var showDeleteVideosConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSortSheet by rememberSaveable { mutableStateOf(false) }

    val selectedItemsSize = selectionManager.selectedFolders.size + selectionManager.selectedVideos.size
    val totalItemsSize = (uiState.mediaDataState as? DataState.Success)?.value?.run { folderList.size + mediaList.size } ?: 0

    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = (uiState.folderName ?: stringResource(R.string.app_name)).takeIf { !selectionManager.isInSelectionMode } ?: "",
                fontWeight = FontWeight.Bold.takeIf { uiState.folderName == null },
                navigationIcon = {
                    if (selectionManager.isInSelectionMode) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(VayouTheme.colors.surfaceContainerHigh)
                                .clickable { selectionManager.exitSelectionMode() }
                                .padding(8.dp)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = VayouIcons.Close,
                                contentDescription = stringResource(id = R.string.navigate_up),
                            )
                            Text(
                                text = stringResource(R.string.m_n_selected, selectedItemsSize, totalItemsSize),
                                style = VayouTheme.typography.labelLarge,
                            )
                        }
                    } else if (uiState.folderName != null) {
                        VayouIconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = VayouIcons.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigate_up),
                            )
                        }
                    }
                },
                actions = {
                    if (selectionManager.isInSelectionMode) {
                        val hasVideos = selectionManager.allSelectedVideos.isNotEmpty()
                        if (hasVideos) {
                            VayouIconButton(onClick = {
                                onPlayVideos(selectionManager.allSelectedVideos.map { it.uriString.toUri() })
                                selectionManager.clearSelection()
                            }) {
                                Icon(imageVector = VayouIcons.Play, contentDescription = stringResource(R.string.play))
                            }
                            VayouIconButton(onClick = {
                                onEvent(MediaPickerUiEvent.ShareVideos(selectionManager.allSelectedVideos.map { it.uriString }))
                            }) {
                                Icon(imageVector = VayouIcons.Share, contentDescription = stringResource(R.string.share))
                            }
                            VayouIconButton(onClick = {
                                if (MediaService.willSystemAsksForDeleteConfirmation()) {
                                    onEvent(MediaPickerUiEvent.DeleteVideos(selectionManager.allSelectedVideos.map { it.uriString }))
                                    selectionManager.clearSelection()
                                } else {
                                    showDeleteVideosConfirmation = true
                                }
                            }) {
                                Icon(imageVector = VayouIcons.Delete, contentDescription = stringResource(R.string.delete))
                            }
                            var selectionOverflowExpanded by remember { mutableStateOf(false) }
                            Box {
                                VayouIconButton(onClick = { selectionOverflowExpanded = true }) {
                                    Icon(imageVector = VayouIcons.MoreVert, contentDescription = null)
                                }
                                VayouDropdownMenu(
                                    expanded = selectionOverflowExpanded,
                                    onDismissRequest = { selectionOverflowExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.add_to_playlist)) },
                                        leadingIcon = { Icon(VayouIcons.SaveToPlaylist, contentDescription = null) },
                                        onClick = {
                                            selectionOverflowExpanded = false
                                            showAddToPlaylistFor = selectionManager.allSelectedVideos.map { it.uriString }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.move_to_private)) },
                                        leadingIcon = { Icon(VayouIcons.Lock, contentDescription = null) },
                                        onClick = {
                                            selectionOverflowExpanded = false
                                            onEvent(MediaPickerUiEvent.MoveVideosToPrivate(selectionManager.allSelectedVideos.map { it.uriString }))
                                            selectionManager.clearSelection()
                                        },
                                    )
                                }
                            }
                        }
                        VayouIconButton(
                            onClick = {
                                if (selectedItemsSize != totalItemsSize) {
                                    (uiState.mediaDataState as? DataState.Success)?.value?.let { folder ->
                                        folder.folderList.forEach { selectionManager.selectFolder(it) }
                                        folder.mediaList.forEach { selectionManager.selectVideo(it) }
                                    }
                                } else {
                                    selectionManager.clearSelection()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (selectedItemsSize != totalItemsSize) VayouIcons.SelectAll else VayouIcons.DeselectAll,
                                contentDescription = if (selectedItemsSize != totalItemsSize) stringResource(R.string.select_all) else stringResource(R.string.deselect_all),
                            )
                        }
                    } else {
                        topBarActions()
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = VayouIcons.Search,
                                contentDescription = stringResource(id = R.string.search),
                            )
                        }
                        var overflowExpanded by remember { mutableStateOf(false) }
                        var menuPrefs by remember { mutableStateOf(uiState.preferences) }
                        if (overflowExpanded) menuPrefs = uiState.preferences
                        Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(
                                    imageVector = VayouIcons.MoreVert,
                                    contentDescription = stringResource(id = R.string.menu),
                                )
                            }
                            VayouDropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false },
                            ) {
                                val dividerColor = VayouTheme.colors.outlineVariant.copy(alpha = 0.3f)
                                MediaLayoutMode.entries.forEach { mode ->
                                    val selected = menuPrefs.mediaLayoutMode == mode
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when (mode) {
                                                    MediaLayoutMode.LIST -> VayouIcons.ListView
                                                    MediaLayoutMode.GRID -> VayouIcons.GridView
                                                },
                                                contentDescription = null,
                                                tint = VayouTheme.colors.onSurface,
                                                modifier = Modifier.size(VayouTheme.iconSize.sm),
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = when (mode) {
                                                    MediaLayoutMode.LIST -> stringResource(R.string.list)
                                                    MediaLayoutMode.GRID -> stringResource(R.string.grid)
                                                },
                                                style = VayouTheme.typography.bodyMedium,
                                            )
                                        },
                                        trailingIcon = if (selected) ({
                                            Icon(
                                                imageVector = VayouIcons.Check,
                                                contentDescription = null,
                                                tint = VayouTheme.colors.accent,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }) else null,
                                        onClick = {
                                            onEvent(MediaPickerUiEvent.UpdateMenu(uiState.preferences.copy(mediaLayoutMode = mode)))
                                            overflowExpanded = false
                                        },
                                    )
                                }

                                if (uiState.folderName == null) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = dividerColor)

                                    MediaViewMode.entries.forEach { mode ->
                                        val selected = menuPrefs.mediaViewMode == mode
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = when (mode) {
                                                        MediaViewMode.FOLDERS -> VayouIcons.Folder
                                                        MediaViewMode.VIDEOS -> VayouIcons.Video
                                                    },
                                                    contentDescription = null,
                                                    tint = VayouTheme.colors.onSurface,
                                                    modifier = Modifier.size(VayouTheme.iconSize.sm),
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = mode.name(),
                                                    style = VayouTheme.typography.bodyMedium,
                                                )
                                            },
                                            trailingIcon = if (selected) ({
                                                Icon(
                                                    imageVector = VayouIcons.Check,
                                                    contentDescription = null,
                                                    tint = VayouTheme.colors.accent,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }) else null,
                                            onClick = {
                                                onEvent(MediaPickerUiEvent.UpdateMenu(uiState.preferences.copy(mediaViewMode = mode)))
                                                overflowExpanded = false
                                            },
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = dividerColor)

                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = VayouIcons.Sort,
                                            contentDescription = null,
                                            tint = VayouTheme.colors.onSurface,
                                            modifier = Modifier.size(VayouTheme.iconSize.sm),
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = stringResource(R.string.sort),
                                            style = VayouTheme.typography.bodyMedium,
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        showSortSheet = true
                                    },
                                )

                                if (uiState.folderName == null) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = dividerColor)

                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = VayouIcons.History,
                                                contentDescription = null,
                                                tint = VayouTheme.colors.onSurface,
                                                modifier = Modifier.size(VayouTheme.iconSize.sm),
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = stringResource(R.string.show_recent_videos),
                                                style = VayouTheme.typography.bodyMedium,
                                            )
                                        },
                                        trailingIcon = if (menuPrefs.showRecentVideos) ({
                                            Icon(
                                                imageVector = VayouIcons.Check,
                                                contentDescription = null,
                                                tint = VayouTheme.colors.accent,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }) else null,
                                        onClick = {
                                            onEvent(MediaPickerUiEvent.UpdateMenu(uiState.preferences.copy(showRecentVideos = !uiState.preferences.showRecentVideos)))
                                            overflowExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        containerColor = VayouTheme.colors.surface,
    ) { scaffoldPadding ->
        when (uiState.mediaDataState) {
            is DataState.Error -> {
            }

            is DataState.Loading -> {
                CenterCircularProgressBar(modifier = Modifier.padding(scaffoldPadding))
            }

            is DataState.Success -> {
                PullToRefreshBox(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = scaffoldPadding.calculateTopPadding())
                        .padding(start = scaffoldPadding.calculateStartPadding(LocalLayoutDirection.current))
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(VayouTheme.colors.background),
                    isRefreshing = uiState.refreshing,
                    onRefresh = { onEvent(MediaPickerUiEvent.Refresh) },
                ) {
                    val updatedScaffoldPadding = scaffoldPadding.copy(top = 0.dp, start = 0.dp)
                    PermissionMissingView(
                        isGranted = permissionState.status.isGranted,
                        showRationale = permissionState.status.shouldShowRationale,
                        permission = permissionState.permission,
                        launchPermissionRequest = { permissionState.launchPermissionRequest() },
                    ) {
                        val rootFolder = uiState.mediaDataState.value
                        if (rootFolder == null || rootFolder.folderList.isEmpty() && rootFolder.mediaList.isEmpty()) {
                            NoVideosFound(contentPadding = updatedScaffoldPadding)
                            return@PermissionMissingView
                        }

                        val isRoot = uiState.folderName == null
                        val recentVideos = remember(rootFolder, uiState.preferences.showRecentVideos, isRoot) {
                            if (!isRoot || !uiState.preferences.showRecentVideos) emptyList()
                            else rootFolder.allMediaList
                                .filter { it.lastPlayedAt != null }
                                .sortedByDescending { it.lastPlayedAt!!.time }
                                .take(20)
                        }

                        MediaView(
                            rootFolder = rootFolder,
                            preferences = uiState.preferences,
                            onFolderClick = onFolderClick,
                            onVideoClick = { onPlayVideo(it) },
                            selectionManager = selectionManager,
                            lazyGridState = lazyGridState,
                            contentPadding = updatedScaffoldPadding,
                            onVideoLoaded = { onEvent(MediaPickerUiEvent.AddToSync(it)) },
                            header = if (recentVideos.isNotEmpty()) ({
                                RecentVideosSection(
                                    videos = recentVideos,
                                    onVideoClick = onPlayVideo,
                                )
                            }) else null,
                            onFolderPlayClick = { folder ->
                                val uris = folder.mediaList.map { it.uriString.toUri() }
                                if (uris.isNotEmpty()) onPlayVideos(uris)
                            },
                            onFolderShareClick = { folder ->
                                onEvent(MediaPickerUiEvent.ShareVideos(folder.mediaList.map { it.uriString }))
                            },
                            onFolderDeleteClick = { folder ->
                                folder.mediaList.forEach { selectionManager.toggleVideoSelection(it) }
                                if (MediaService.willSystemAsksForDeleteConfirmation()) {
                                    onEvent(MediaPickerUiEvent.DeleteVideos(folder.mediaList.map { it.uriString }))
                                    selectionManager.clearSelection()
                                } else {
                                    showDeleteVideosConfirmation = true
                                }
                            },
                            onVideoPlayClick = { video -> onPlayVideo(video.uriString.toUri()) },
                            onVideoRenameClick = { video -> showRenameActionFor = video },
                            onVideoInfoClick = { video ->
                                showInfoActionFor = video
                            },
                            onVideoShareClick = { video ->
                                onEvent(MediaPickerUiEvent.ShareVideos(listOf(video.uriString)))
                            },
                            onVideoDeleteClick = { video ->
                                selectionManager.toggleVideoSelection(video)
                                if (MediaService.willSystemAsksForDeleteConfirmation()) {
                                    onEvent(MediaPickerUiEvent.DeleteVideos(listOf(video.uriString)))
                                    selectionManager.clearSelection()
                                } else {
                                    showDeleteVideosConfirmation = true
                                }
                            },
                            onVideoAddToPlaylistClick = { video -> showAddToPlaylistFor = listOf(video.uriString) },
                            onVideoAddToFavoritesClick = { video -> onEvent(MediaPickerUiEvent.ToggleFavorite(video.uriString)) },
                            onVideoRemoveFromFavoritesClick = { video -> onEvent(MediaPickerUiEvent.ToggleFavorite(video.uriString)) },
                            onVideoMoveToPrivateClick = { video -> onEvent(MediaPickerUiEvent.MoveToPrivate(video)) },
                            favoriteUris = favoriteUris,
                            privateUris = privateUris,
                        )
                    }
                }
            }
        }
    }

    BackHandler(enabled = selectionManager.isInSelectionMode) {
        selectionManager.exitSelectionMode()
    }

    showRenameActionFor?.let { video ->
        RenameDialog(
            name = video.displayName,
            onDismiss = { showRenameActionFor = null },
            onDone = {
                onEvent(MediaPickerUiEvent.RenameVideo(video.uriString.toUri(), it))
                showRenameActionFor = null
                selectionManager.clearSelection()
            },
        )
    }

    showInfoActionFor?.let { video ->
        VideoInfoDialog(
            video = video,
            onDismiss = { showInfoActionFor = null },
        )
    }

    showAddToPlaylistFor?.let { uris ->
        AddToPlaylistSheet(
            playlists = playlists,
            onPlaylistSelected = { playlistId ->
                onEvent(MediaPickerUiEvent.AddVideosToPlaylist(playlistId, uris))
                showAddToPlaylistFor = null
                selectionManager.clearSelection()
            },
            onCreatePlaylist = { name ->
                onEvent(MediaPickerUiEvent.CreatePlaylistAndAddVideos(name, uris))
                selectionManager.clearSelection()
            },
            onDismiss = { showAddToPlaylistFor = null },
        )
    }

    if (showDeleteVideosConfirmation) {
        DeleteConfirmationDialog(
            selectedVideos = selectionManager.selectedVideos,
            selectedFolders = selectionManager.selectedFolders,
            onConfirm = {
                onEvent(MediaPickerUiEvent.DeleteVideos(selectionManager.allSelectedVideos.map { it.uriString }))
                selectionManager.clearSelection()
                showDeleteVideosConfirmation = false
            },
            onCancel = { showDeleteVideosConfirmation = false },
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            preferences = uiState.preferences,
            onEvent = onEvent,
            onDismiss = { showSortSheet = false },
        )
    }

}

@Composable
private fun SortBottomSheet(
    preferences: ApplicationPreferences,
    onEvent: (MediaPickerUiEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        VayouBottomSheetTitle(text = stringResource(R.string.sort))
        Sort.By.entries.forEach { sortBy ->
            val isSelected = preferences.sortBy == sortBy
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
                        imageVector = if (preferences.sortOrder == Sort.Order.ASCENDING) VayouIcons.ArrowUpward else VayouIcons.ArrowDownward,
                        contentDescription = null,
                        tint = VayouTheme.colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }) else null,
                onClick = {
                    val newPrefs = if (isSelected) {
                        val newOrder = if (preferences.sortOrder == Sort.Order.ASCENDING) Sort.Order.DESCENDING else Sort.Order.ASCENDING
                        preferences.copy(sortOrder = newOrder)
                    } else {
                        preferences.copy(sortBy = sortBy)
                    }
                    onEvent(MediaPickerUiEvent.UpdateMenu(newPrefs))
                },
                contentPadding = PaddingValues(horizontal = VayouTheme.spacing.lg),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DeleteConfirmationDialog(
    modifier: Modifier = Modifier,
    selectedVideos: Set<SelectedVideo>,
    selectedFolders: Set<SelectedFolder>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    VayouDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = when {
                    selectedVideos.isEmpty() -> when (selectedFolders.size) {
                        1 -> stringResource(R.string.delete_one_folder)
                        else -> stringResource(R.string.delete_folders, selectedFolders.size)
                    }

                    selectedFolders.isEmpty() -> when (selectedVideos.size) {
                        1 -> stringResource(R.string.delete_one_video)
                        else -> stringResource(R.string.delete_videos, selectedVideos.size)
                    }

                    else -> stringResource(R.string.delete_items, selectedFolders.size + selectedVideos.size)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = modifier,
            ) {
                Text(text = stringResource(R.string.delete))
            }
        },
        dismissButton = { CancelButton(onClick = onCancel) },
        modifier = modifier,
        content = {
            Text(
                text = if ((selectedFolders.size + selectedVideos.size) == 1) {
                    stringResource(R.string.delete_item_info)
                } else {
                    stringResource(R.string.delete_items_info)
                },
                style = VayouTheme.typography.titleSmall,
            )
        },
    )
}

@Composable
private fun RecentVideosSection(
    videos: List<Video>,
    onVideoClick: (Uri) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.recently_played),
            style = VayouTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = VayouTheme.colors.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = VayouTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
        ) {
            items(videos, key = { it.uriString }) { video ->
                RecentVideoCard(
                    video = video,
                    onClick = { onVideoClick(video.uriString.toUri()) },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_section_library),
            style = VayouTheme.typography.labelMedium,
            color = VayouTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun RecentVideoCard(
    video: Video,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(VayouTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(VayouTheme.shapes.small)
                .background(VayouTheme.colors.surfaceContainer),
        ) {
            Icon(
                imageVector = VayouIcons.Video,
                contentDescription = null,
                tint = VayouTheme.colors.surfaceContainerHighest,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.4f),
            )
            AsyncImage(
                model = remember(video.uriString) {
                    ImageRequest.Builder(context).data(video.uriString).crossfade(true).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            InfoChip(
                text = video.formattedDuration,
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.BottomEnd),
                backgroundColor = VayouTheme.colors.scrim.copy(alpha = 0.6f),
                contentColor = Color.White,
                shape = VayouTheme.shapes.extraSmall,
            )
            if (video.playedPercentage > 0) {
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(VayouTheme.colors.surfaceContainerHigh))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(video.playedPercentage)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(VayouTheme.colors.accent),
                    )
                }
            }
        }
    }
}

@PreviewScreenSizes
@PreviewLightDark
@Composable
private fun MediaPickerScreenPreview(
    @PreviewParameter(VideoPickerPreviewParameterProvider::class)
    videos: List<Video>,
) {
    VayouPlayerTheme {
        MediaPickerScreen(
            uiState = MediaPickerUiState(
                folderName = null,
                mediaDataState = DataState.Success(
                    value = Folder(
                        name = "Root Folder",
                        path = "/root",
                        dateModified = System.currentTimeMillis(),
                        folderList = listOf(
                            Folder(name = "Folder 1", path = "/root/folder1", dateModified = System.currentTimeMillis()),
                            Folder(name = "Folder 2", path = "/root/folder2", dateModified = System.currentTimeMillis()),
                        ),
                        mediaList = videos,
                    ),
                ),
                preferences = ApplicationPreferences().copy(
                    mediaViewMode = MediaViewMode.FOLDERS,
                    mediaLayoutMode = MediaLayoutMode.GRID,
                ),
            ),
        )
    }
}


@DayNightPreview
@Composable
private fun MediaPickerNoVideosFoundPreview() {
    VayouPlayerTheme {
        Surface {
            MediaPickerScreen(
                uiState = MediaPickerUiState(
                    folderName = null,
                    mediaDataState = DataState.Success(null),
                    preferences = ApplicationPreferences(),
                ),
            )
        }
    }
}

@DayNightPreview
@Composable
private fun MediaPickerLoadingPreview() {
    VayouPlayerTheme {
        Surface {
            MediaPickerScreen(
                uiState = MediaPickerUiState(
                    folderName = null,
                    mediaDataState = DataState.Loading,
                    preferences = ApplicationPreferences(),
                ),
            )
        }
    }
}
