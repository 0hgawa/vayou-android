package dev.vayou.feature.music

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.vayou.core.common.audioPermission
import dev.vayou.core.media.MusicSort
import dev.vayou.core.media.Song
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.MediaLibrary
import dev.vayou.core.model.SmartPlaylist
import dev.vayou.core.player.ui.addToQueue
import dev.vayou.core.player.ui.playNext
import dev.vayou.core.player.ui.rememberMusicController
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.LocalVayouMessages
import dev.vayou.core.ui.designsystem.components.VayouBackButton
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
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
import dev.vayou.core.ui.designsystem.components.VayouSearchField
import dev.vayou.core.ui.designsystem.components.VayouSelectionTopBar
import dev.vayou.core.ui.designsystem.components.VayouSortOption
import dev.vayou.core.ui.designsystem.components.VayouSortSheet
import dev.vayou.core.ui.designsystem.components.VayouTopAppBar
import kotlinx.coroutines.launch

/**
 * The music on this device.
 *
 * Five ways into the same scan, as pills over a pager. Nothing here re-queries MediaStore: the
 * grouping keys already ride on each track, so a tab change is a regroup in memory.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicScreen(onPlaySong: (Song, List<Song>) -> Unit, viewModel: MusicViewModel = hiltViewModel()) {
    val permission = rememberPermissionState(audioPermission)
    val isGranted = permission.status.isGranted

    LaunchedEffect(Unit) {
        if (!isGranted) permission.launchPermissionRequest()
    }
    LaunchedEffect(isGranted) {
        if (isGranted) viewModel.load()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val isAscending by viewModel.isAscending.collectAsStateWithLifecycle()
    val isGrid = viewModel.layoutMode.collectAsStateWithLifecycle().value == MediaLayoutMode.GRID

    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(MusicTab.Songs) }
    var openGroupKey by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSortSheetOpen by remember { mutableStateOf(false) }
    var selectedIds by rememberSaveable(saver = SelectedIdsSaver) { mutableStateOf(emptySet<Long>()) }
    var selectedGroups by rememberSaveable(saver = SelectedKeysSaver) { mutableStateOf(emptySet<String>()) }
    val searchFocus = remember { FocusRequester() }

    val context = LocalContext.current
    // Connected only while this screen is up, and released on stop -- the same helper the mini
    // controller uses. "Play next" is the one thing here that touches what is playing.
    val controller = rememberMusicController()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var addingToPlaylist by remember { mutableStateOf(emptyList<Song>()) }

    /**
     * The tracks a list is being named for, as against a list made from the tab with nothing in it.
     *
     * They are carried here rather than read back from [addingToPlaylist] when the name is typed:
     * the sheet that started this is dismissed on the way, and reading it afterwards read an empty
     * list -- the list was made and nothing went into it.
     */
    var namingForTracks by remember { mutableStateOf<List<Song>?>(null) }

    /** The track whose tags are being corrected, over the whole screen. */
    var editingTags by remember { mutableStateOf<Song?>(null) }
    var namingPlaylist by remember { mutableStateOf<MusicGroup?>(null) }
    var isCreatingPlaylist by remember { mutableStateOf(false) }
    var deletingPlaylist by remember { mutableStateOf<MusicGroup?>(null) }
    var isAddingTracks by remember { mutableStateOf(false) }
    var deletingSongs by remember { mutableStateOf(emptyList<Song>()) }

    val messages = LocalVayouMessages.current
    val playingNext = stringResource(R.string.playing_next)
    val addedToQueue = stringResource(R.string.added_to_queue_message)
    val undo = stringResource(dev.vayou.core.ui.R.string.undo)
    val removedFrom = stringResource(R.string.removed_from_playlist_message)
    val addedTo = stringResource(R.string.added_to_playlist_message)

    // The same five, for a whole album, artist or folder. A group is a set of tracks, and every
    // one of these is an action on a set.
    val groupActions = remember(controller) {
        GroupActions(
            onPlay = { group -> group.songs.firstOrNull()?.let { onPlaySong(it, group.songs) } },
            onPlayNext = { group ->
                controller?.playNext(group.songs)
                messages.show(playingNext)
            },
            onAddToQueue = { group ->
                controller?.addToQueue(group.songs)
                messages.show(addedToQueue)
            },
            onAddToPlaylist = { group -> addingToPlaylist = group.songs },
            onShare = { group ->
                context.startActivity(Intent.createChooser(viewModel.shareIntent(group.songs), null))
            },
            onDelete = { group -> deletingSongs = group.songs },
        )
    }
    // Only a playlist can be renamed or thrown away: an album is a tag on a file and a folder is a
    // place on disk, and neither is a thing the listener made.
    val playlistActions = if (tab == MusicTab.Playlists) {
        GroupOwnerActions(onRename = { namingPlaylist = it }, onDelete = { deletingPlaylist = it })
    } else {
        null
    }

    // The system's own dialog -- for deleting and for writing tags alike. Only a screen can put
    // one up, and only the listener can answer it.
    val confirmWrite = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onWriteAnswered(it.resultCode == Activity.RESULT_OK)
    }
    viewModel.pendingWrite?.let { pending ->
        LaunchedEffect(pending) {
            confirmWrite.launch(IntentSenderRequest.Builder(pending.request).build())
        }
    }

    val unknown = stringResource(R.string.unknown)
    val allSongs = (uiState as? MusicUiState.Success)?.songs.orEmpty()
    val musicPlaylists = playlists.of(MediaLibrary.Music)
    val favouritesLabel = stringResource(R.string.favourites)
    val favouriteSongs = remember(allSongs, playlists) {
        // Resolved once here rather than per row: the store keeps addresses, and a list of them
        // matched against the library on every recomposition would be a scan per frame.
        val starred = playlists.favouriteUris
        allSongs.filter { it.uriString in starred }
    }
    val groups = remember(allSongs, tab, unknown, musicPlaylists) {
        if (tab == MusicTab.Playlists) {
            listOf(favouritesGroup(favouriteSongs, favouritesLabel)) + playlistGroups(musicPlaylists, allSongs)
        } else {
            groupSongs(allSongs, tab, unknown)
        }
    }
    val openGroup = remember(groups, openGroupKey) { groups.firstOrNull { it.key == openGroupKey } }

    // Everything the row's menu can do. Built once here rather than per row: five hundred rows
    // would otherwise each hold their own copy of six lambdas.
    val songActions = remember(playlists, controller, messages) {
        SongActions(
            // Said out loud, because none of this shows on the screen the listener is looking at:
            // the queue is behind the player, and a track dropped into it silently reads as a
            // button that did nothing.
            onPlayNext = { song ->
                controller?.playNext(listOf(song))
                messages.show(playingNext)
            },
            onAddToQueue = { song ->
                controller?.addToQueue(listOf(song))
                messages.show(addedToQueue)
            },
            onShare = { song ->
                context.startActivity(Intent.createChooser(viewModel.shareIntent(listOf(song)), null))
            },
            onToggleFavourite = viewModel::toggleFavourite,
            isFavourite = { it.uriString in playlists.favouriteUris },
            onAddToPlaylist = { addingToPlaylist = listOf(it) },
            // Not inside starred: there is no list to take the track out of, and the star above
            // this row already does the only thing that means anything there.
            onRemoveFromPlaylist = openGroup
                ?.takeIf { tab == MusicTab.Playlists && it.key != SmartPlaylist.Favourites }
                ?.let { group ->
                    { song: Song ->
                        viewModel.removeFromPlaylist(group.key, song.uriString)
                        // The row goes at once, so the sentence is not the news -- the way back is.
                        messages.show(removedFrom.format(group.label), undo) {
                            viewModel.addToPlaylist(group.key, listOf(song))
                        }
                    }
                },
            onDelete = { deletingSongs = listOf(it) },
            onEditTags = { editingTags = it },
        )
    }

    val query = searchQuery.takeIf { isSearchOpen && it.isNotBlank() }
    val listedSongs: List<Song> = when {
        openGroup != null -> openGroup.songs.matching(query)
        tab == MusicTab.Songs -> allSongs.matching(query)
        else -> emptyList()
    }
    // The groups on this tab, which is what "all" means while a list of them is on screen. Empty
    // inside an opened group, where the rows are tracks and the group that led there is gone.
    val listedGroups: List<MusicGroup> = if (openGroup != null) {
        emptyList()
    } else {
        remember(groups, query) { groups.filter { query == null || it.label.contains(query, ignoreCase = true) } }
    }

    val isSelecting = selectedIds.isNotEmpty() || selectedGroups.isNotEmpty()

    /**
     * What the toolbar acts on: the tracks picked one by one, and everything inside the groups
     * picked whole. Distinct, because a track reachable both ways would be shared or deleted twice.
     */
    val pickedSongs = remember(allSongs, musicPlaylists, favouriteSongs, selectedIds, selectedGroups) {
        // Resolved against the whole library rather than against what this tab happens to be
        // showing: the marks outlive the tab, and reading them off the current page would act on
        // the half of them that is still on screen.
        //
        // The groups are only rebuilt when one of them is actually marked, which is the rarer half
        // of selecting and the only case that cannot be answered from the track list alone.
        val fromGroups = if (selectedGroups.isEmpty()) {
            emptyList()
        } else {
            val everyGroup = MusicTab.entries.flatMap { entry ->
                when (entry) {
                    MusicTab.Songs -> emptyList()
                    MusicTab.Playlists ->
                        playlistGroups(musicPlaylists, allSongs) + favouritesGroup(favouriteSongs, favouritesLabel)

                    else -> groupSongs(allSongs, entry, unknown)
                }
            }
            everyGroup.filter { it.key in selectedGroups }.flatMap { it.songs }
        }
        (allSongs.filter { it.id in selectedIds } + fromGroups).distinctBy { it.id }
    }
    val toggleSelection = { song: Song ->
        selectedIds = if (song.id in selectedIds) selectedIds - song.id else selectedIds + song.id
    }
    val toggleGroupSelection = { group: MusicGroup ->
        selectedGroups = if (group.key in selectedGroups) {
            selectedGroups - group.key
        } else {
            selectedGroups + group.key
        }
    }
    val closeSearch = {
        isSearchOpen = false
        searchQuery = ""
    }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) searchFocus.requestFocus()
    }
    // A query typed on one tab means nothing on the next, and carried over it hides what arrived.
    // The selection is not cleared with it: marking three tracks here and a whole album there is
    // the ordinary way to fill a list, and the bar at the top says all along how many are held.
    LaunchedEffect(tab, openGroupKey) { closeSearch() }

    BackHandler(enabled = isSelecting) {
        selectedIds = emptySet()
        selectedGroups = emptySet()
    }
    BackHandler(enabled = !isSelecting && (isSearchOpen || openGroup != null)) {
        if (isSearchOpen) closeSearch() else openGroupKey = null
    }

    VayouScaffold(
        topBar = {
            if (isSelecting) {
                val exitSelection = {
                    selectedIds = emptySet()
                    selectedGroups = emptySet()
                }
                VayouSelectionTopBar(
                    // Rows picked, not files acted on: a group counts once however much is in it.
                    selectedCount = selectedIds.size + selectedGroups.size,
                    totalCount = listedSongs.size + listedGroups.size,
                    onExit = exitSelection,
                    actions = {
                        VayouIconButton(
                            onClick = {
                                // The queue is what was picked, starting at the first of them.
                                pickedSongs.firstOrNull()?.let { onPlaySong(it, pickedSongs) }
                                exitSelection()
                            },
                        ) {
                            Icon(VayouIcons.Play, stringResource(R.string.play))
                        }
                        // The reason anyone marks a dozen tracks in the first place. It was on the
                        // row's own menu and not here, which left picking twelve and then adding
                        // them one at a time as the way to fill a list.
                        VayouIconButton(
                            onClick = {
                                addingToPlaylist = pickedSongs
                                exitSelection()
                            },
                        ) {
                            Icon(VayouIcons.Add, stringResource(R.string.add_to_playlist))
                        }
                        VayouIconButton(
                            onClick = {
                                context.startActivity(
                                    Intent.createChooser(viewModel.shareIntent(pickedSongs), null),
                                )
                            },
                        ) {
                            Icon(VayouIcons.Share, stringResource(R.string.share))
                        }
                        VayouIconButton(onClick = { deletingSongs = pickedSongs }) {
                            Icon(VayouIcons.Delete, stringResource(R.string.delete))
                        }
                        // "All" is what this tab is showing, and so is what the second press
                        // gives back: marks made on another tab are not this button's to undo.
                        val isAll = (listedSongs.isNotEmpty() || listedGroups.isNotEmpty()) &&
                            listedSongs.all { it.id in selectedIds } &&
                            listedGroups.all { it.key in selectedGroups }
                        VayouIconButton(
                            onClick = {
                                if (isAll) {
                                    selectedIds -= listedSongs.mapTo(mutableSetOf()) { it.id }
                                    selectedGroups -= listedGroups.mapTo(mutableSetOf()) { it.key }
                                } else {
                                    selectedIds += listedSongs.map { it.id }
                                    selectedGroups += listedGroups.map { it.key }
                                }
                            },
                        ) {
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
                title = {
                    if (isSearchOpen) {
                        VayouSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = stringResource(R.string.search),
                            focusRequester = searchFocus,
                        )
                    } else {
                        androidx.compose.material3.Text(
                            text = openGroup?.label ?: stringResource(R.string.audio),
                        )
                    }
                },
                navigationIcon = {
                    if (openGroup != null) {
                        VayouBackButton(onClick = { if (isSearchOpen) closeSearch() else openGroupKey = null })
                    }
                },
                actions = {
                    // Search first and the conditional after it, as on the video side: the fixed
                    // button keeps its corner whether or not this tab has anything to add.
                    VayouIconButton(onClick = { if (isSearchOpen) closeSearch() else isSearchOpen = true }) {
                        Icon(
                            imageVector = if (isSearchOpen) VayouIcons.Close else VayouIcons.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                    // Inside a list, the one thing to add is what goes in it. On the tab itself, a
                    // list. Nowhere else: an album is not something anyone makes here.
                    when {
                        openGroup != null && tab == MusicTab.Playlists ->
                            VayouIconButton(onClick = { isAddingTracks = true }) {
                                Icon(VayouIcons.Add, stringResource(R.string.add_tracks))
                            }

                        openGroup == null && tab == MusicTab.Playlists ->
                            VayouIconButton(onClick = { isCreatingPlaylist = true }) {
                                Icon(VayouIcons.Add, stringResource(R.string.new_playlist))
                            }
                    }
                },
            )
        },
    ) {
        if (!isGranted) {
            VayouEmptyState(VayouIcons.Audio, stringResource(R.string.audio_permission_needed))
            return@VayouScaffold
        }

        val pager = rememberPagerState(initialPage = tab.ordinal) { MusicTab.entries.size }
        // The tab follows the swipe once it lands. Half a gesture is not a tab change.
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage }.collect { page -> tab = MusicTab.entries[page] }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Hidden once a group is open: that list is the group's, and the pills would offer to
            // switch out from under it.
            if (openGroup == null) {
                VayouPillRow(
                    labels = MusicTab.entries.map { stringResource(it.label) },
                    // The page the swipe is closest to, so the pill travels with the finger.
                    selectedIndex = pager.currentPage,
                    onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                )
            }

            if (openGroup != null) {
                TrackList(
                    uiState = uiState,
                    songs = listedSongs,
                    sortLabel = stringResource(sort.label),
                    isAscending = isAscending,
                    onOpenSort = { isSortSheetOpen = true },
                    onPlaySong = { song -> onPlaySong(song, openGroup.songs) },
                    selectedIds = selectedIds,
                    onToggleSelection = toggleSelection,
                    emptyTitle = stringResource(R.string.playlist_empty),
                    isSearching = query != null,
                    actions = songActions,
                )
                return@Column
            }

            // Each page keeps its own scroll, so a tab returned to is where it was left.
            HorizontalPager(state = pager) { page ->
                when (val pageTab = MusicTab.entries[page]) {
                    MusicTab.Songs -> {
                        val shown = remember(allSongs, query) { allSongs.matching(query) }
                        TrackList(
                            uiState = uiState,
                            songs = shown,
                            sortLabel = stringResource(sort.label),
                            isAscending = isAscending,
                            onOpenSort = { isSortSheetOpen = true },
                            onPlaySong = { song -> onPlaySong(song, shown) },
                            selectedIds = selectedIds,
                            onToggleSelection = toggleSelection,
                            emptyTitle = stringResource(R.string.no_music_found),
                            isSearching = query != null,
                            actions = songActions,
                        )
                    }

                    else -> GroupList(
                        uiState = uiState,
                        groups = remember(
                            allSongs,
                            pageTab,
                            unknown,
                            query,
                            musicPlaylists,
                            favouriteSongs,
                            isAscending,
                        ) {
                            val all = if (pageTab == MusicTab.Playlists) {
                                playlistGroups(musicPlaylists, allSongs)
                            } else {
                                groupSongs(allSongs, pageTab, unknown)
                            }
                            val shown = all.filter { query == null || it.label.contains(query, true) }
                            val ordered = if (isAscending) shown else shown.reversed()
                            // Starred stays at the head whichever way the rest runs: it is not one
                            // of the made lists and does not take its turn among them.
                            if (pageTab == MusicTab.Playlists && query == null) {
                                listOf(favouritesGroup(favouriteSongs, favouritesLabel)) + ordered
                            } else {
                                ordered
                            }
                        },
                        emptyTitle = stringResource(
                            if (pageTab == MusicTab.Playlists) R.string.no_playlists else R.string.no_music_found,
                        ),
                        isSearching = query != null,
                        isGrid = isGrid,
                        header = {
                            // The same row the track list carries, so the switch is in one corner
                            // throughout. A group list is ordered by name and nothing else, so the
                            // label says so and the tap flips the direction rather than opening a
                            // sheet of axes that would not apply.
                            VayouListHeader(
                                label = stringResource(R.string.sort_by_name),
                                isAscending = isAscending,
                                onClick = viewModel::toggleAscending,
                                outerInset = MediaListLayoutDefaults.headerInset(isGrid),
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
                        },
                        tab = pageTab,
                        selectedKeys = selectedGroups,
                        onToggleSelection = toggleGroupSelection,
                        ownerActions = playlistActions,
                        onOpenGroup = { openGroupKey = it.key },
                        actions = groupActions,
                    )
                }
            }
        }
    }

    if (addingToPlaylist.isNotEmpty()) {
        val songs = addingToPlaylist
        VayouPickPlaylistSheet(
            playlists = playlists.of(MediaLibrary.Music),
            onPick = { list ->
                viewModel.addToPlaylist(list.id, songs)
                messages.show(addedTo.format(list.name))
            },
            // Made and filled in one move: a listener who opens this with no lists wants the
            // tracks in the one they are about to make, not to make it and start again.
            onNew = {
                namingForTracks = songs
                addingToPlaylist = emptyList()
            },
            onDismiss = { addingToPlaylist = emptyList() },
        )
    }

    namingForTracks?.let { songs ->
        VayouNameDialog(
            title = stringResource(R.string.new_playlist),
            initialName = "",
            label = stringResource(R.string.playlist_name),
            onDismiss = { namingForTracks = null },
            onDone = { name ->
                viewModel.createPlaylist(name) { viewModel.addToPlaylist(it, songs) }
                namingForTracks = null
                messages.show(addedTo.format(name))
            },
        )
    }

    // Written, refused or failed: all three used to end the same way, with the form closing and
    // nothing said. A tag write is the one action here whose result is invisible even when it works,
    // since the library reads the file again on its own time.
    val tagsSaved = stringResource(R.string.tags_saved)
    val tagsFailed = stringResource(R.string.tags_not_saved)
    val deleteFailed = stringResource(R.string.delete_failed)
    LaunchedEffect(messages) {
        viewModel.outcomes.collect { outcome ->
            when (outcome) {
                MusicOutcome.TagsWritten -> messages.show(tagsSaved)
                MusicOutcome.TagsFailed -> messages.showProblem(tagsFailed)
                MusicOutcome.DeleteFailed -> messages.showProblem(deleteFailed)
            }
        }
    }

    editingTags?.let { song ->
        TagEditor(
            song = song,
            isSaving = viewModel.isWritingTags,
            onDismiss = { editingTags = null },
            onSave = { tags, cover -> viewModel.editTags(song, tags, cover) },
        )
    }

    // Closed by the write finishing rather than by the press that started it. A write that needs
    // the system's permission takes a dialog and a moment, and a form that vanished first left
    // nothing on screen saying whether anything had been written.
    var wasWriting by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel.isWritingTags) {
        if (viewModel.isWritingTags) {
            wasWriting = true
        } else if (wasWriting) {
            wasWriting = false
            editingTags = null
        }
    }

    if (deletingSongs.isNotEmpty()) {
        val doomed = deletingSongs
        SongDeleteDialog(
            count = doomed.size,
            name = doomed.first().title.ifBlank { doomed.first().fileName },
            onDismiss = { deletingSongs = emptyList() },
            onConfirm = {
                doomed.forEach(viewModel::deleteSong)
                selectedIds = emptySet()
                selectedGroups = emptySet()
            },
        )
    }

    if (isCreatingPlaylist) {
        VayouNameDialog(
            title = stringResource(R.string.new_playlist),
            initialName = "",
            label = stringResource(R.string.playlist_name),
            onDismiss = { isCreatingPlaylist = false },
            onDone = { name ->
                viewModel.createPlaylist(name) { openGroupKey = it }
                isCreatingPlaylist = false
            },
        )
    }

    namingPlaylist?.let { group ->
        VayouNameDialog(
            title = stringResource(R.string.rename),
            initialName = group.label,
            label = stringResource(R.string.playlist_name),
            onDismiss = { namingPlaylist = null },
            onDone = {
                viewModel.renamePlaylist(group.key, it)
                namingPlaylist = null
            },
        )
    }

    deletingPlaylist?.let { group ->
        PlaylistDeleteDialog(
            name = group.label,
            onDismiss = { deletingPlaylist = null },
            onConfirm = {
                viewModel.deletePlaylist(group.key)
                if (openGroupKey == group.key) openGroupKey = null
            },
        )
    }

    if (isAddingTracks) {
        val target = openGroup
        if (target == null) {
            isAddingTracks = false
        } else {
            VayouPickItemsSheet(
                title = stringResource(R.string.add_tracks),
                items = remember(allSongs, target) {
                    val already = target.songs.mapTo(HashSet()) { it.uriString }
                    allSongs
                        .filterNot { it.uriString in already }
                        .map { VayouPickItem(it.uriString, it.title.ifBlank { it.fileName }, it.artist) }
                },
                emptyIcon = VayouIcons.Audio,
                emptyTitle = stringResource(R.string.no_music_found),
                confirmLabel = { pluralStringResource(R.plurals.n_songs, it, it) },
                onConfirm = { uris -> viewModel.addToPlaylist(target.key, allSongs.filter { it.uriString in uris }) },
                onDismiss = { isAddingTracks = false },
            )
        }
    }

    if (isSortSheetOpen) {
        VayouSortSheet(
            title = stringResource(R.string.sort),
            options = MusicSort.entries.map { VayouSortOption(stringResource(it.label), it.icon) },
            selectedIndex = sort.ordinal,
            isAscending = isAscending,
            onSelect = { viewModel.selectSort(MusicSort.entries[it]) },
            onDismiss = { isSortSheetOpen = false },
        )
    }
}

@Composable
private fun TrackList(
    uiState: MusicUiState,
    songs: List<Song>,
    sortLabel: String,
    isAscending: Boolean,
    onOpenSort: () -> Unit,
    onPlaySong: (Song) -> Unit,
    selectedIds: Set<Long>,
    onToggleSelection: (Song) -> Unit,
    /** What this list has none of yet, in its own words. */
    emptyTitle: String,
    /** So an empty list can say whether it is empty or merely filtered. */
    isSearching: Boolean,
    actions: SongActions,
) {
    val isSelecting = selectedIds.isNotEmpty()
    if (uiState.isNotReady()) return

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
    ) {
        // The row naming the order lives in the list rather than in the bar, so the current order
        // is readable without opening anything -- the same control the video library uses.
        item { VayouListHeader(label = sortLabel, isAscending = isAscending, onClick = onOpenSort) }
        items(songs, key = { it.id }) { song ->
            SongRow(
                song = song,
                // While marking, a tap marks: starting a track would replace the screen the
                // selection was being built on.
                onClick = { if (isSelecting) onToggleSelection(song) else onPlaySong(song) },
                onLongClick = { onToggleSelection(song) },
                isSelecting = isSelecting,
                isSelected = song.id in selectedIds,
                actions = actions,
            )
        }
        if (songs.isEmpty()) {
            item {
                // A list nobody has put anything in yet is not a search that found nothing, and a
                // playlist just made is the first thing in this app that can honestly be empty.
                VayouEmptyState(
                    icon = if (isSearching) VayouIcons.Search else VayouIcons.MusicPlaylist,
                    title = if (isSearching) stringResource(R.string.no_results_found) else emptyTitle,
                )
            }
        }
    }
}

@Composable
private fun GroupList(
    uiState: MusicUiState,
    groups: List<MusicGroup>,
    tab: MusicTab,
    selectedKeys: Set<String>,
    onToggleSelection: (MusicGroup) -> Unit,
    onOpenGroup: (MusicGroup) -> Unit,
    actions: GroupActions,
    /** What the tab has none of yet, said in its own words -- "no lists" is not "no music". */
    emptyTitle: String,
    /** So an empty list can say whether it is empty or merely filtered. */
    isSearching: Boolean,
    isGrid: Boolean,
    header: @Composable () -> Unit,
    ownerActions: GroupOwnerActions? = null,
) {
    val isSelecting = selectedKeys.isNotEmpty()
    if (uiState.isNotReady()) return

    // A grid of one column rather than a column, for the reason the video library gives: the two
    // would be two scrollers with two scroll positions, and switching between them would jump the
    // reader to the top.
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isGrid) GridColumns else 1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = if (isGrid) MediaListLayoutDefaults.GridOuterInset else 0.dp),
        verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
        horizontalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }

        if (groups.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // A search that found nothing and a tab that holds nothing are different facts, and
                // the Playlists tab is the first here that can honestly be empty.
                VayouEmptyState(
                    icon = if (isSearching) VayouIcons.Search else VayouIcons.MusicPlaylist,
                    title = if (isSearching) stringResource(R.string.no_results_found) else emptyTitle,
                )
            }
            return@LazyVerticalGrid
        }

        items(groups, key = { it.key }) { group ->
            // While marking, a tap marks: opening the group would leave the selection behind on a
            // screen that no longer lists what is in it.
            val onClick = { if (isSelecting) onToggleSelection(group) else onOpenGroup(group) }
            if (isGrid) {
                GroupCard(
                    group = group,
                    tab = tab,
                    onClick = onClick,
                    onLongClick = { onToggleSelection(group) },
                    isSelecting = isSelecting,
                    isSelected = group.key in selectedKeys,
                )
            } else {
                GroupRow(
                    group = group,
                    tab = tab,
                    onClick = onClick,
                    onLongClick = { onToggleSelection(group) },
                    isSelecting = isSelecting,
                    isSelected = group.key in selectedKeys,
                    actions = actions,
                    // Starred is not a list the listener made, so it is not one they can rename or
                    // throw away -- the film library leaves its own smart lists without a menu for
                    // the same reason.
                    ownerActions = ownerActions?.takeIf { group.key != SmartPlaylist.Favourites },
                )
            }
        }
    }
}

/** Three across, as the video grid is: the same phone and the same size of thing to look at. */
private const val GridColumns = 3

/** Draws the waiting and the empty states, and says whether the caller still has work to do. */
@Composable
private fun MusicUiState.isNotReady(): Boolean = when (this) {
    MusicUiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VayouCircularProgress()
        }
        true
    }

    MusicUiState.Empty -> {
        VayouEmptyState(VayouIcons.Audio, stringResource(R.string.no_music_found))
        true
    }

    is MusicUiState.Success -> false
}

private fun List<Song>.matching(query: String?): List<Song> = when (query) {
    null -> this
    else -> filter {
        it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
    }
}

/** How each axis is named here. The comparators are shared; the words are this screen's. */
private val MusicSort.label
    get() = when (this) {
        MusicSort.Title -> R.string.sort_by_title
        MusicSort.Artist -> R.string.sort_by_artist
        MusicSort.Album -> R.string.sort_by_album
        MusicSort.Duration -> R.string.sort_by_duration
        MusicSort.DateAdded -> R.string.sort_by_date
    }

private val MusicSort.icon
    get() = when (this) {
        MusicSort.Title -> VayouIcons.Title
        MusicSort.Artist -> VayouIcons.Artist
        MusicSort.Album -> VayouIcons.AudioNotes
        MusicSort.Duration -> VayouIcons.Timer
        MusicSort.DateAdded -> VayouIcons.Calendar
    }

/** Saves the selection across a rotation. A [Set] is not bundle-storable; a list of ids is. */
private val SelectedIdsSaver = Saver<MutableState<Set<Long>>, List<Long>>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)

/** The group half of the same selection. Keys, for the reason the ids above are ids. */
private val SelectedKeysSaver = Saver<MutableState<Set<String>>, List<String>>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)
