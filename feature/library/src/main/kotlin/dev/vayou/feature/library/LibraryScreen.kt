package dev.vayou.feature.library

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.Folder
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.MediaLibrary
import dev.vayou.core.model.MediaPlaylist
import dev.vayou.core.model.MediaViewMode
import dev.vayou.core.model.SmartPlaylist
import dev.vayou.core.model.Sort
import dev.vayou.core.model.Video
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.LocalVayouMessages
import dev.vayou.core.ui.designsystem.components.VayouBackButton
import dev.vayou.core.ui.designsystem.components.VayouEmptyState
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouListHeader
import dev.vayou.core.ui.designsystem.components.VayouListHeaderAction
import dev.vayou.core.ui.designsystem.components.VayouNameDialog
import dev.vayou.core.ui.designsystem.components.VayouPickItem
import dev.vayou.core.ui.designsystem.components.VayouPickItemsSheet
import dev.vayou.core.ui.designsystem.components.VayouPickPlaylistSheet
import dev.vayou.core.ui.designsystem.components.VayouPillRow
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.core.ui.designsystem.components.VayouSelectionTopBar
import dev.vayou.core.ui.designsystem.components.VayouSortOption
import dev.vayou.core.ui.designsystem.components.VayouSortSheet
import dev.vayou.core.ui.designsystem.components.VayouTopAppBar
import dev.vayou.feature.player.CastButton

@Composable
fun LibraryScreen(
    onPlayVideo: (uri: String, title: String) -> Unit,
    /** The same film, ignoring where it was left. Separate because the caller builds the request. */
    onPlayFromStart: (uri: String, title: String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearching by rememberSaveable { mutableStateOf(false) }

    // In front of the library rather than inside it. Search replaces everything the library screen
    // is -- the pills, the order, the folders -- and one screen showing neither is a screen that
    // cannot say where it is.
    if (isSearching) {
        SearchScreen(onPlayVideo = onPlayVideo, onClose = { isSearching = false })
        return
    }

    LibraryContent(
        onSearch = { isSearching = true },
        uiState = uiState,
        onSelectViewMode = viewModel::selectViewMode,
        onSelectSort = viewModel::selectSort,
        onPlayVideo = onPlayVideo,
        onPlayFromStart = onPlayFromStart,
        viewModel = viewModel,
    )
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    onSearch: () -> Unit,
    onSelectViewMode: (MediaViewMode) -> Unit,
    onSelectSort: (Sort.By) -> Unit,
    onPlayVideo: (uri: String, title: String) -> Unit,
    onPlayFromStart: (uri: String, title: String) -> Unit,
    viewModel: LibraryViewModel,
) {
    val context = LocalContext.current
    val messages = LocalVayouMessages.current
    val addedTo = stringResource(R.string.added_to_playlist_message)
    val removedFrom = stringResource(R.string.removed_from_playlist_message)
    val undo = stringResource(dev.vayou.core.ui.R.string.undo)
    val hidden = stringResource(R.string.moved_to_private_message)
    val hideFailed = stringResource(R.string.move_to_private_failed)
    val deleteFailed = stringResource(R.string.delete_failed)

    // The three the shelf cannot show for itself: a film that left for the locked folder, and the
    // two that failed where nothing on screen changed to say so.
    LaunchedEffect(messages) {
        viewModel.outcomes.collect { outcome ->
            when (outcome) {
                LibraryOutcome.Hidden -> messages.show(hidden)
                LibraryOutcome.HideFailed -> messages.showProblem(hideFailed)
                LibraryOutcome.DeleteFailed -> messages.showProblem(deleteFailed)
            }
        }
    }
    val selection = rememberSelectionState()
    var sortSheetOpen by remember { mutableStateOf(false) }
    var openPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }

    /** Which folder is open, by path. Saved, so a rotation stays inside it. */
    var openFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var isNaming by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<MediaPlaylist?>(null) }
    var isAddingVideos by remember { mutableStateOf(false) }
    // True only between the tap on the locked folder and the phone's answer. Not saved: coming back
    // to the app should ask again rather than resume a half-finished unlock.
    var isUnlocking by remember { mutableStateOf(false) }
    var renamingVideo by remember { mutableStateOf<Video?>(null) }
    var deletingVideo by remember { mutableStateOf<Video?>(null) }
    var infoVideo by remember { mutableStateOf<Video?>(null) }

    /** The film waiting for a list to be picked for it. */
    /** The addresses waiting to be put in a list: one film, a whole folder, or a selection. */
    var addingToPlaylist by remember { mutableStateOf(emptyList<String>()) }

    /**
     * The films a list is being named for, as against a list made from the tab with nothing in it.
     *
     * Carried here rather than read back from [addingToPlaylist] when the name is typed: the sheet
     * that started this is gone by then, and what it held with it.
     */
    var namingForVideos by remember { mutableStateOf<List<String>?>(null) }

    /** Non-empty while a whole selection is being confirmed for deletion. */
    var deletingSelection by remember { mutableStateOf(emptyList<Video>()) }
    val ready = uiState as? LibraryUiState.Ready

    /** The list being looked at, or null once it is deleted from under us. */
    val openPlaylist = remember(ready, openPlaylistId) {
        openPlaylistId?.let { id -> ready?.playlists?.items?.firstOrNull { it.id == id } }
    }
    val isInFavourites = openPlaylistId == SmartPlaylist.Favourites
    val isInPrivate = openPlaylistId == SmartPlaylist.Private

    /**
     * The folder being looked into, or null at the root.
     *
     * Taken from the library already in hand rather than queried again: every folder arrives with
     * its own films, so opening one is a lookup, not a round trip to the database.
     */
    val openFolder = remember(ready, openFolderPath) {
        openFolderPath?.let { path -> ready?.root?.folderList?.firstOrNull { it.path == path } }
    }

    BackHandler(enabled = selection.isActive) { selection.clear() }
    BackHandler(enabled = !selection.isActive && openPlaylistId != null) { openPlaylistId = null }
    BackHandler(enabled = !selection.isActive && openFolderPath != null) { openFolderPath = null }

    if (sortSheetOpen && ready != null) {
        VayouSortSheet(
            title = stringResource(R.string.sort),
            options = SortAxes.map { VayouSortOption(stringResource(it.label), it.icon) },
            selectedIndex = SortAxes.indexOfFirst { it.by == ready.sort.by },
            isAscending = ready.sort.order == Sort.Order.ASCENDING,
            onSelect = { onSelectSort(SortAxes[it].by) },
            onDismiss = { sortSheetOpen = false },
        )
    }

    // Whatever the current view is listing, which is what "all" means for select-all and what the
    // selection acts on.
    val listed: List<Video> = when {
        ready == null -> emptyList()
        isInFavourites -> ready.videosIn(ready.playlists.favouriteUris)
        isInPrivate -> ready.privateVideos
        openPlaylist != null -> ready.videosIn(openPlaylist.itemUris)
        // Sorted here and not by the query: what came back is one folder's contents in whatever
        // order the store held them, and the header above says which order this screen is in.
        openFolder != null -> remember(openFolder, ready.sort) {
            openFolder.mediaList.sortedWith(ready.sort.videoComparator())
        }

        ready.viewMode == MediaViewMode.VIDEOS -> ready.root.mediaList
        else -> emptyList()
    }

    // Folders are listable only at the top of the Folders view: opening one shows its films, and
    // the row that led there is no longer on screen to be picked.
    val listedFolders: List<Folder> = when {
        ready == null || openFolder != null -> emptyList()
        ready.viewMode == MediaViewMode.FOLDERS -> ready.root.folderList
        else -> emptyList()
    }

    VayouScaffold(
        topBar = {
            if (selection.isActive) {
                VayouSelectionTopBar(
                    selectedCount = selection.count,
                    totalCount = listed.size + listedFolders.size,
                    onExit = selection::clear,
                    actions = {
                        val picked = selection.selectionOf(listed, listedFolders)
                        VayouIconButton(
                            onClick = {
                                picked.firstOrNull()?.let { onPlayVideo(it.uriString, it.displayName) }
                                selection.clear()
                            },
                        ) {
                            Icon(VayouIcons.Play, stringResource(R.string.play))
                        }
                        // Absent for the locked folder, whose films have no public address to send.
                        if (!isInPrivate) {
                            // The reason most selections are made: a dozen films into one list.
                            VayouIconButton(
                                onClick = {
                                    addingToPlaylist = picked.map { it.uriString }
                                    selection.clear()
                                },
                            ) {
                                Icon(VayouIcons.Add, stringResource(R.string.add_to_playlist))
                            }
                            VayouIconButton(
                                onClick = {
                                    context.startActivity(
                                        Intent.createChooser(viewModel.shareIntent(picked), null),
                                    )
                                },
                            ) {
                                Icon(VayouIcons.Share, stringResource(R.string.share))
                            }
                            VayouIconButton(onClick = { deletingSelection = picked }) {
                                Icon(VayouIcons.Delete, stringResource(R.string.delete))
                            }
                        }
                        val isAll = selection.count == listed.size + listedFolders.size
                        VayouIconButton(onClick = { selection.toggleAll(listed, listedFolders) }) {
                            Icon(
                                imageVector = if (isAll) VayouIcons.DeselectAll else VayouIcons.SelectAll,
                                contentDescription = stringResource(
                                    if (isAll) R.string.deselect_all else R.string.select_all,
                                ),
                            )
                        }
                    },
                )
                return@VayouScaffold
            }
            VayouTopAppBar(
                title = when {
                    openPlaylist != null -> openPlaylist.name
                    isInFavourites -> stringResource(R.string.favourites)
                    isInPrivate -> stringResource(R.string.private_videos)
                    openFolder != null -> openFolder.name
                    else -> stringResource(R.string.video)
                },
                navigationIcon = {
                    when {
                        openPlaylistId != null -> VayouBackButton(onClick = { openPlaylistId = null })
                        openFolderPath != null -> VayouBackButton(onClick = { openFolderPath = null })
                    }
                },
                // Cast, then search, then whatever this screen happens to offer. The first two are
                // on every screen and must not move; a button that comes and goes -- adding to a
                // list you have opened, making one you have not -- would shift them both if it came
                // first, so it goes last.
                actions = {
                    CastButton()
                    VayouIconButton(onClick = onSearch) {
                        Icon(VayouIcons.Search, stringResource(R.string.search))
                    }
                    // Inside a list, the one thing to add is what goes in it. On the tab itself, a
                    // list. Everywhere else nothing: a folder is not something anyone makes here.
                    when {
                        openPlaylist != null -> VayouIconButton(onClick = { isAddingVideos = true }) {
                            Icon(VayouIcons.Add, stringResource(R.string.add_videos))
                        }
                    }
                },
            )
        },
    ) {
        when (uiState) {
            LibraryUiState.Loading -> Unit
            is LibraryUiState.Ready -> Column(modifier = Modifier.fillMaxSize()) {
                // Hidden once a list is open: that screen belongs to the list, and the pills would
                // offer to switch out from under it.
                if (openPlaylistId == null && openFolderPath == null) {
                    VayouPillRow(
                        labels = MediaViewMode.entries.map { stringResource(it.label) },
                        selectedIndex = uiState.viewMode.ordinal,
                        onSelect = { onSelectViewMode(MediaViewMode.entries[it]) },
                    )
                }
                // The order, named where the list it orders is, and not behind an icon in the bar:
                // this way the current order is readable without opening anything. A playlist
                // keeps the order it was built in, so it has none to name.
                val isGrid = uiState.layoutMode == MediaLayoutMode.GRID
                val sortHeader: @Composable () -> Unit = {
                    VayouListHeader(
                        label = stringResource(SortAxes.first { it.by == uiState.sort.by }.label),
                        isAscending = uiState.sort.order == Sort.Order.ASCENDING,
                        onClick = { sortSheetOpen = true },
                        outerInset = MediaListLayoutDefaults.headerInset(isGrid),
                        // Beside the order rather than in the bar: both say how this list is laid
                        // out, and the answer to each is readable without opening anything.
                        trailing = {
                            VayouListHeaderAction(
                                icon = if (isGrid) VayouIcons.ListView else VayouIcons.GridView,
                                contentDescription = stringResource(
                                    if (isGrid) R.string.show_as_list else R.string.show_as_grid,
                                ),
                                onClick = viewModel::toggleLayoutMode,
                            )
                        },
                    )
                }

                // Only at the root of the library. Inside a folder or a list, "recently played"
                // would be a strip of films that are not in what is being looked at.
                val recents = uiState.recentVideos.takeIf {
                    openPlaylistId == null && openFolderPath == null && uiState.viewMode != MediaViewMode.PLAYLISTS
                }.orEmpty()

                // Everything a public film offers. What is missing from a private one is missing
                // for one reason: it has no address in MediaStore any more, so there is nothing to
                // share, rename, delete through the system, or point a list at.
                val publicActions = VideoActions(
                    onPlay = { onPlayVideo(it.uriString, it.displayName) },
                    onPlayFromStart = { onPlayFromStart(it.uriString, it.displayName) },
                    onToggleFavourite = { viewModel.toggleFavourite(it.uriString) },
                    isFavourite = { it.uriString in uiState.playlists.favouriteUris },
                    onShare = { context.startActivity(Intent.createChooser(viewModel.shareIntent(listOf(it)), null)) },
                    onAddToPlaylist = { addingToPlaylist = listOf(it.uriString) },
                    onRemoveFromPlaylist = openPlaylist?.let { list ->
                        { video: Video ->
                            viewModel.removeFromPlaylist(list.id, video.uriString)
                            // The row goes at once, so the sentence is not the news -- the way back
                            // is.
                            messages.show(removedFrom.format(list.name), undo) {
                                viewModel.addToPlaylist(list.id, listOf(video.uriString))
                            }
                        }
                    },
                    onMoveToPrivate = viewModel::moveToPrivate,
                    onRename = { renamingVideo = it },
                    onInfo = { infoVideo = it },
                    onDelete = { deletingVideo = it },
                )

                when {
                    // Inside a folder, the same list of films the Videos tab draws.
                    openFolder != null -> VideoList(
                        videos = listed,
                        actions = publicActions,
                        selection = selection,
                        isGrid = isGrid,
                        header = sortHeader,
                        emptyMessage = stringResource(R.string.no_videos),
                    )

                    isInFavourites -> VideoList(
                        videos = listed,
                        actions = publicActions,
                        selection = selection,
                        emptyMessage = stringResource(R.string.no_favourites),
                    )

                    // Its films are carried in rather than looked up: they have left the library,
                    // which is the point of the folder.
                    isInPrivate -> VideoList(
                        videos = listed,
                        actions = VideoActions(
                            onPlay = { onPlayVideo(it.uriString, it.displayName) },
                            onRestoreFromPrivate = { viewModel.restoreFromPrivate(it.uriString) },
                            onInfo = { infoVideo = it },
                        ),
                        selection = selection,
                        emptyMessage = stringResource(R.string.no_private_videos),
                    )

                    openPlaylist != null -> VideoList(
                        videos = listed,
                        actions = publicActions,
                        selection = selection,
                        emptyMessage = stringResource(R.string.playlist_empty),
                    )

                    uiState.viewMode == MediaViewMode.FOLDERS -> FolderList(
                        folders = uiState.root.folderList,
                        onOpen = { openFolderPath = it.path },
                        actions = FolderActions(
                            onPlayAll = { folder ->
                                folder.mediaList.firstOrNull()?.let { onPlayVideo(it.uriString, it.displayName) }
                            },
                            onShareAll = { folder ->
                                context.startActivity(
                                    Intent.createChooser(viewModel.shareIntent(folder.mediaList), null),
                                )
                            },
                            onAddAllToPlaylist = { folder ->
                                addingToPlaylist = folder.mediaList.map { it.uriString }
                            },
                            onDeleteAll = { deletingSelection = it.mediaList },
                        ),
                        selection = selection,
                        isGrid = isGrid,
                        header = {
                            if (recents.isNotEmpty()) {
                                RecentVideosRow(recents, isGrid) {
                                    onPlayVideo(it.uriString, it.displayName)
                                }
                            }
                            sortHeader()
                        },
                    )

                    uiState.viewMode == MediaViewMode.VIDEOS -> VideoList(
                        videos = listed,
                        actions = publicActions,
                        selection = selection,
                        isGrid = isGrid,
                        header = {
                            if (recents.isNotEmpty()) {
                                RecentVideosRow(recents, isGrid) {
                                    onPlayVideo(it.uriString, it.displayName)
                                }
                            }
                            sortHeader()
                        },
                    )

                    else -> PlaylistList(
                        playlists = uiState.playlists.of(MediaLibrary.Video),
                        favouriteCount = uiState.videosIn(uiState.playlists.favouriteUris).size,
                        privateCount = uiState.privateVideos.size,
                        // What a list still points at, not what it stores: an address outlives the
                        // file it names, so a count off the stored list reads high for ever.
                        countOf = { uiState.videosIn(it.itemUris).size },
                        onOpenFavourites = { openPlaylistId = SmartPlaylist.Favourites },
                        onOpenPrivate = { isUnlocking = true },
                        onOpen = { openPlaylistId = it.id },
                        onRename = { renaming = it },
                        onDelete = { viewModel.deletePlaylist(it.id) },
                        onNew = { isNaming = true },
                    )
                }
            }
        }
    }

    // The system's own permission dialog -- for hiding, renaming and deleting alike. Only the
    // screen can put one up, and only the viewer can answer it.
    val confirmWrite = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onWriteAnswered(it.resultCode == Activity.RESULT_OK)
    }
    viewModel.pendingWrite?.let { pending ->
        LaunchedEffect(pending) {
            confirmWrite.launch(IntentSenderRequest.Builder(pending.request).build())
        }
    }

    renamingVideo?.let { video ->
        VideoRenameDialog(
            video = video,
            onDismiss = { renamingVideo = null },
            onDone = { name ->
                viewModel.renameVideo(video, name)
                renamingVideo = null
            },
        )
    }

    deletingVideo?.let { video ->
        VideoDeleteDialog(
            count = 1,
            name = video.displayName,
            onDismiss = { deletingVideo = null },
            onConfirm = { viewModel.deleteVideos(listOf(video)) },
        )
    }

    if (deletingSelection.isNotEmpty()) {
        val picked = deletingSelection
        VideoDeleteDialog(
            count = picked.size,
            name = picked.first().displayName,
            onDismiss = { deletingSelection = emptyList() },
            onConfirm = {
                viewModel.deleteVideos(picked)
                selection.clear()
            },
        )
    }

    infoVideo?.let { video ->
        VideoInfoDialog(video = video, onDismiss = { infoVideo = null })
    }

    if (addingToPlaylist.isNotEmpty() && ready != null) {
        val films = addingToPlaylist
        VayouPickPlaylistSheet(
            playlists = ready.playlists.of(MediaLibrary.Video),
            onPick = { list ->
                viewModel.addToPlaylist(list.id, films)
                messages.show(addedTo.format(list.name))
            },
            // Made and filled in one move. It used to drop the films here and open an empty list,
            // which left the viewer to find them again in a picker -- the one thing they had
            // already said.
            onNew = {
                namingForVideos = films
                addingToPlaylist = emptyList()
            },
            onDismiss = { addingToPlaylist = emptyList() },
        )
    }

    namingForVideos?.let { films ->
        VayouNameDialog(
            title = stringResource(R.string.new_playlist),
            label = stringResource(R.string.playlist_name),
            initialName = "",
            onDismiss = { namingForVideos = null },
            onDone = { name ->
                viewModel.createPlaylist(name) { id -> viewModel.addToPlaylist(id, films) }
                namingForVideos = null
                messages.show(addedTo.format(name))
            },
        )
    }

    if (isUnlocking) {
        PrivateUnlockEffect(
            onUnlocked = {
                isUnlocking = false
                openPlaylistId = SmartPlaylist.Private
            },
            onCancelled = { isUnlocking = false },
        )
    }

    if (isNaming) {
        VayouNameDialog(
            title = stringResource(R.string.new_playlist),
            label = stringResource(R.string.playlist_name),
            initialName = "",
            onDismiss = { isNaming = false },
            onDone = { name ->
                isNaming = false
                // Straight into it with the picker up: made and dropped back on a list of lists is
                // the dead end this avoids.
                viewModel.createPlaylist(name) { id ->
                    openPlaylistId = id
                    isAddingVideos = true
                }
            },
        )
    }

    renaming?.let { playlist ->
        VayouNameDialog(
            label = stringResource(R.string.playlist_name),
            title = stringResource(R.string.rename),
            initialName = playlist.name,
            onDismiss = { renaming = null },
            onDone = { name ->
                viewModel.renamePlaylist(playlist.id, name)
                renaming = null
            },
        )
    }

    if (isAddingVideos && ready != null) {
        val target = openPlaylist
        if (target == null) {
            isAddingVideos = false
        } else {
            VayouPickItemsSheet(
                title = stringResource(R.string.add_videos),
                emptyIcon = VayouIcons.Video,
                emptyTitle = stringResource(R.string.no_videos),
                confirmLabel = { pluralStringResource(R.plurals.n_videos, it, it) },
                items = remember(ready, target) {
                    val already = target.itemUris.toHashSet()
                    ready.root.mediaList
                        .filterNot { it.uriString in already }
                        .map { VayouPickItem(it.uriString, it.displayName, it.formattedDuration) }
                },
                onConfirm = { viewModel.addToPlaylist(target.id, it) },
                onDismiss = { isAddingVideos = false },
            )
        }
    }
}

/**
 * The axes offered, in the order they are offered. A list and not the enum's own order, because the
 * enum is a storage format and this is a menu: what belongs at the top is what gets picked most.
 */
private val SortAxes = listOf(
    SortAxis(Sort.By.TITLE, R.string.sort_by_title, VayouIcons.Title),
    SortAxis(Sort.By.DATE, R.string.sort_by_date, VayouIcons.Calendar),
    SortAxis(Sort.By.LENGTH, R.string.sort_by_length, VayouIcons.Length),
    SortAxis(Sort.By.SIZE, R.string.sort_by_size, VayouIcons.Size),
    SortAxis(Sort.By.PATH, R.string.sort_by_path, VayouIcons.Location),
)

private data class SortAxis(val by: Sort.By, @param:StringRes val label: Int, val icon: ImageVector)

@Composable
private fun FolderList(
    folders: List<Folder>,
    onOpen: (Folder) -> Unit,
    actions: FolderActions,
    selection: SelectionState,
    isGrid: Boolean,
    header: @Composable () -> Unit,
) {
    MediaGrid(
        isGrid = isGrid,
        header = header,
        isEmpty = folders.isEmpty(),
        empty = { VayouEmptyState(icon = VayouIcons.Folder, title = stringResource(R.string.no_folders)) },
    ) {
        items(folders, key = { it.path }) { folder ->
            // While marking, a tap marks: opening a folder would leave the selection behind on a
            // screen that no longer lists what is in it.
            val onClick = { if (selection.isActive) selection.toggle(folder) else onOpen(folder) }
            if (isGrid) {
                FolderCard(
                    folder = folder,
                    onClick = onClick,
                    onLongClick = { selection.toggle(folder) },
                    isSelecting = selection.isActive,
                    isSelected = selection.isMarked(folder),
                )
            } else {
                FolderRow(
                    folder = folder,
                    onClick = onClick,
                    onLongClick = { selection.toggle(folder) },
                    isSelecting = selection.isActive,
                    isSelected = selection.isMarked(folder),
                    actions = actions,
                )
            }
        }
    }
}

/**
 * The one container both layouts are laid out by.
 *
 * A grid of one column rather than a column: the two would otherwise be two scrollers with two
 * scroll positions, and switching between them would jump the reader to the top. `LazyVerticalGrid`
 * at a single column is a `LazyColumn` -- same measure, same recycling -- and the header spans
 * whatever the column count turns out to be.
 */
@Composable
private fun MediaGrid(
    isGrid: Boolean,
    header: @Composable () -> Unit,
    isEmpty: Boolean,
    empty: @Composable () -> Unit,
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        // Sized, not counted: the grid fits as many cells of this width as the window allows, so a
        // card is the same size in the hand upright, sideways and on a tablet.
        columns = if (isGrid) GridCells.Adaptive(MediaListLayoutDefaults.GridCellWidth) else GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        // Half the margin here and half on each cell, so a card lands on the same line a row did.
        contentPadding = PaddingValues(horizontal = if (isGrid) MediaListLayoutDefaults.GridOuterInset else 0.dp),
        verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
        // The same gap sideways, as the original lays it. Without it the columns were 16dp apart
        // against the rows' 18 -- and, since a cell's padding is inside its background, two picked
        // plates in one line met with nothing between them and read as a single block.
        horizontalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
    ) {
        // Stacked, not laid loose in the slot: a grid item is one box, and two composables handed
        // to it draw over each other -- which is what put the recents strip on top of the order.
        item(span = { GridItemSpan(maxLineSpan) }) { Column { header() } }
        if (isEmpty) {
            item(span = { GridItemSpan(maxLineSpan) }) { empty() }
        } else {
            content()
        }
    }
}

@Composable
private fun VideoList(
    videos: List<Video>,
    actions: VideoActions,
    selection: SelectionState,
    isGrid: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    emptyMessage: String? = null,
) {
    MediaGrid(
        isGrid = isGrid,
        header = header ?: {},
        isEmpty = videos.isEmpty(),
        empty = { VayouEmptyState(VayouIcons.Video, emptyMessage ?: stringResource(R.string.no_videos)) },
    ) {
        items(videos, key = { it.uriString }) { video ->
            // A long press starts marking; once it has, an ordinary tap marks too. Anything else
            // would mean holding every row after the first.
            val onClick = { if (selection.isActive) selection.toggle(video) else actions.onPlay(video) }
            if (isGrid) {
                VideoCard(
                    video = video,
                    onClick = onClick,
                    onLongClick = { selection.toggle(video) },
                    isSelecting = selection.isActive,
                    isSelected = selection.isMarked(video),
                )
            } else {
                VideoRow(
                    video = video,
                    onClick = onClick,
                    onLongClick = { selection.toggle(video) },
                    isSelecting = selection.isActive,
                    isSelected = selection.isMarked(video),
                    actions = actions,
                )
            }
        }
    }
}

/** How a view mode is named belongs here and not on the enum: `core:model` is plain Kotlin and
 *  holds no resources, and a name is a thing that has a language. */
@get:StringRes
private val MediaViewMode.label: Int
    get() = when (this) {
        MediaViewMode.FOLDERS -> R.string.folders
        MediaViewMode.VIDEOS -> R.string.videos
        MediaViewMode.PLAYLISTS -> R.string.playlists
    }
