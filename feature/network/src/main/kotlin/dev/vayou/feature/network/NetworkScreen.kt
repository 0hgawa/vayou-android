package dev.vayou.feature.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.IptvCountry
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.smb.mergeNetworkServers
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouBackButton
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouPillRow
import dev.vayou.core.ui.designsystem.components.VayouSearchField
import dev.vayou.core.ui.designsystem.components.VayouSelectionTopBar
import dev.vayou.core.ui.designsystem.components.VayouTopAppBar
import dev.vayou.core.ui.theme.VayouTheme
import kotlinx.coroutines.launch

/**
 * Everything that is not on this phone.
 *
 * Two tabs at the root -- the shares on this network, and the channel lists -- and a stack of
 * screens above each. They are one destination rather than two, because both answer the same
 * question and share the favourites, the search and the add button that serve them.
 */
@Composable
fun NetworkScreen(
    onPlay: (uri: String, title: String, subtitles: List<String>) -> Unit,
    /** The same file, ignoring where it was left. The menu item exists to override that. */
    onPlayFromStart: (uri: String, title: String, subtitles: List<String>) -> Unit,
    /**
     * A track on a share, which opens the music player rather than the video one.
     *
     * No subtitles and no queue: a share is browsed a file at a time, and what is around a track
     * in a folder is not an album.
     */
    onPlayAudio: (uri: String, title: String) -> Unit,
    /**
     * A channel, and the channels listed beside it.
     *
     * The neighbours are what the player's previous and next act on: changing channel is the thing
     * a viewer does most, and leaving the player to do it is leaving the picture.
     */
    onPlayChannel: (uri: String, title: String, queue: List<String>, queueTitles: List<String>) -> Unit,
    onBackAtRoot: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favouriteChannels by viewModel.favouriteChannels.collectAsStateWithLifecycle()
    val favouriteChannelUrls by viewModel.favouriteChannelUrls.collectAsStateWithLifecycle()
    val favouriteFolders by viewModel.favouriteFolders.collectAsStateWithLifecycle()
    val browserSort by viewModel.browserSort.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(ServersTab) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedChannels by rememberSaveable(saver = SelectedUrlsSaver) { mutableStateOf(emptySet<String>()) }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var renamingFolder by remember { mutableStateOf<FavoriteFolder?>(null) }
    var renamingPlaylist by remember { mutableStateOf<SavedPlaylist?>(null) }
    var detailsFor by remember { mutableStateOf<SmbFileItem?>(null) }
    var isAddServerOpen by remember { mutableStateOf(false) }
    var isAddPlaylistOpen by remember { mutableStateOf(false) }
    var isSortSheetOpen by remember { mutableStateOf(false) }
    var isCountrySheetOpen by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }

    val isSearchable = uiState.screen in SearchableScreens
    val closeSearch = {
        isSearchOpen = false
        searchQuery = ""
    }

    val groups = remember(uiState.channels) {
        uiState.channels.mapNotNull { it.group?.takeIf(String::isNotBlank) }.distinct().sorted()
    }
    val countryCode = remember(uiState.playlistUrl) { uiState.playlistUrl?.iptvCountryCode }
    val isCountryList = remember(uiState.playlistUrl) { uiState.playlistUrl?.isIptvOrg == true }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) searchFocus.requestFocus()
    }
    LaunchedEffect(uiState.screen) {
        if (!isSearchable) closeSearch()
        if (uiState.screen != NetworkScreen.Playlist) selectedGroup = null
    }
    // A new folder is a new list, and a query carried into it hides most of what just arrived.
    LaunchedEffect(uiState.path) {
        if (uiState.screen == NetworkScreen.FileBrowser) closeSearch()
    }

    // Whichever channel list is up. Nothing else on this screen is pickable: a share's files are
    // read over a connection that offers no way to act on several at once, and a saved server is a
    // single thing with a menu of its own.
    val listedChannels: List<PlaylistChannel> = when (uiState.screen) {
        NetworkScreen.Playlist -> uiState.channels
        NetworkScreen.ChannelFavourites -> favouriteChannels
        else -> emptyList()
    }
    val pickedChannels = remember(listedChannels, selectedChannels) {
        listedChannels.filter { it.url in selectedChannels }
    }
    val isSelecting = selectedChannels.isNotEmpty()
    val toggleChannelSelection = { channel: PlaylistChannel ->
        selectedChannels = if (channel.url in selectedChannels) {
            selectedChannels - channel.url
        } else {
            selectedChannels + channel.url
        }
    }

    // A selection belongs to the list it was made in; leaving that list leaves it behind.
    LaunchedEffect(uiState.screen) { selectedChannels = emptySet() }

    BackHandler(enabled = isSelecting) { selectedChannels = emptySet() }
    BackHandler(enabled = !isSelecting) {
        when {
            isSearchOpen -> closeSearch()
            !viewModel.navigateUp() -> onBackAtRoot()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VayouTheme.colors.background),
    ) {
        if (isSelecting) {
            val isAllStarred = pickedChannels.all { it.url in favouriteChannelUrls }
            VayouSelectionTopBar(
                selectedCount = selectedChannels.size,
                totalCount = listedChannels.size,
                onExit = { selectedChannels = emptySet() },
                actions = {
                    VayouIconButton(
                        onClick = {
                            // The picked channels become the running order, starting at the first.
                            pickedChannels.firstOrNull()?.let { first ->
                                onPlayChannel(
                                    first.url,
                                    first.name,
                                    pickedChannels.map { it.url },
                                    pickedChannels.map { it.name },
                                )
                            }
                            selectedChannels = emptySet()
                        },
                    ) {
                        Icon(VayouIcons.Play, stringResource(R.string.play))
                    }
                    // One button that starres or unstars, by what the selection already is: with a
                    // mixed set the useful move is to bring it into agreement, and starring does
                    // that. Only once every one is starred does the button offer to undo it.
                    VayouIconButton(
                        onClick = {
                            viewModel.setChannelsFavourite(pickedChannels, !isAllStarred)
                            selectedChannels = emptySet()
                        },
                    ) {
                        Icon(
                            imageVector = if (isAllStarred) VayouIcons.StarFilled else VayouIcons.StarOutlined,
                            contentDescription = stringResource(
                                if (isAllStarred) R.string.unfavourite else R.string.favourite,
                            ),
                        )
                    }
                    // Only where there is a list to be taken out of. A channel in a provider's
                    // index cannot be removed from it -- that list is not ours -- so offering the
                    // action there would be a button that lies.
                    if (uiState.screen == NetworkScreen.ChannelFavourites) {
                        VayouIconButton(
                            onClick = {
                                viewModel.setChannelsFavourite(pickedChannels, false)
                                selectedChannels = emptySet()
                            },
                        ) {
                            Icon(VayouIcons.Delete, stringResource(R.string.remove))
                        }
                    }
                    val isAll = selectedChannels.size == listedChannels.size
                    VayouIconButton(
                        onClick = {
                            selectedChannels = if (isAll) {
                                emptySet()
                            } else {
                                listedChannels.mapTo(mutableSetOf()) { it.url }
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
        } else {
            VayouTopAppBar(
                title = {
                    if (isSearchable && isSearchOpen) {
                        VayouSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = stringResource(R.string.search),
                            focusRequester = searchFocus,
                        )
                    } else {
                        Text(
                            text = uiState.title(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.screen != NetworkScreen.ServerList) {
                        VayouBackButton(
                            onClick = { if (isSearchOpen) closeSearch() else viewModel.navigateUp() },
                        )
                    }
                },
                actions = {
                    if (uiState.screen == NetworkScreen.Playlist) {
                        when {
                            isCountryList -> VayouIconButton(onClick = { isCountrySheetOpen = true }) {
                                Icon(
                                    imageVector = VayouIcons.Filter,
                                    contentDescription = stringResource(R.string.country),
                                    tint = VayouTheme.colors.onSurface,
                                )
                            }

                            groups.isNotEmpty() -> GroupFilterButton(
                                groups = groups,
                                selected = selectedGroup,
                                onSelect = { selectedGroup = it },
                            )
                        }
                    }

                    if (isSearchable) {
                        VayouIconButton(onClick = { if (isSearchOpen) closeSearch() else isSearchOpen = true }) {
                            Icon(
                                imageVector = if (isSearchOpen) VayouIcons.Close else VayouIcons.Search,
                                contentDescription = stringResource(R.string.search),
                            )
                        }
                    }
                },
            )
        }

        val pager = rememberPagerState(initialPage = tab) { TabCount }
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage }.collect { tab = it }
        }

        if (uiState.screen == NetworkScreen.ServerList) {
            VayouPillRow(
                labels = listOf(stringResource(R.string.servers), stringResource(R.string.streams)),
                // The page the swipe is closest to, so the pill travels with the finger.
                selectedIndex = pager.currentPage,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
            )
        }

        when (uiState.screen) {
            NetworkScreen.Connecting -> Waiting()

            NetworkScreen.Auth -> AuthForm(
                host = uiState.host.orEmpty(),
                isLoading = uiState.isLoading,
                error = uiState.error,
                onSubmit = viewModel::submitCredentials,
            )

            NetworkScreen.ShareList -> ShareList(
                host = uiState.host.orEmpty(),
                isLoading = uiState.isLoading,
                error = uiState.error,
                shares = uiState.shares,
                favouritedShares = remember(favouriteFolders, uiState.host) {
                    favouriteFolders.asSequence()
                        .filter { it.host == uiState.host && it.path.isEmpty() }
                        .mapTo(mutableSetOf()) { it.share }
                },
                onShareClick = viewModel::openShare,
                onToggleFavourite = viewModel::toggleShareFavourite,
            )

            NetworkScreen.FileBrowser -> FileBrowser(
                share = uiState.share.orEmpty(),
                path = uiState.path,
                isLoading = uiState.isLoading,
                error = uiState.error,
                files = uiState.files,
                searchQuery = searchQuery,
                sort = browserSort,
                favouritedPaths = remember(favouriteFolders, uiState.host, uiState.share) {
                    favouriteFolders.asSequence()
                        .filter { it.host == uiState.host && it.share == uiState.share }
                        .mapTo(mutableSetOf()) { it.path }
                },
                onOpenAncestor = viewModel::openAncestor,
                onOpenSort = { isSortSheetOpen = true },
                onOpenDirectory = viewModel::openDirectory,
                onToggleFolderFavourite = viewModel::toggleFolderFavourite,
                onPlayVideo = { item -> scope.launch { viewModel.play(item, onPlay) } },
                onPlayAudio = { item ->
                    scope.launch { viewModel.play(item) { uri, title, _ -> onPlayAudio(uri, title) } }
                },
                onPlayFromStart = { item -> scope.launch { viewModel.play(item, onPlayFromStart) } },
                onShowDetails = { detailsFor = it },
            )

            NetworkScreen.Playlist -> PlaylistDetail(
                isLoading = uiState.isLoading,
                error = uiState.error,
                channels = uiState.channels,
                favouriteUrls = favouriteChannelUrls,
                searchQuery = searchQuery,
                selectedGroup = selectedGroup,
                onChannelClick = { channel, listed ->
                    val nearby = channelsAround(channel, listed)
                    onPlayChannel(channel.url, channel.name, nearby.map { it.url }, nearby.map { it.name })
                },
                onToggleFavourite = viewModel::toggleChannelFavourite,
                selectedUrls = selectedChannels,
                onToggleSelection = toggleChannelSelection,
            )

            NetworkScreen.ChannelFavourites -> ChannelFavourites(
                channels = favouriteChannels,
                searchQuery = searchQuery,
                onChannelClick = { channel, listed ->
                    val nearby = channelsAround(channel, listed)
                    onPlayChannel(channel.url, channel.name, nearby.map { it.url }, nearby.map { it.name })
                },
                onToggleFavourite = viewModel::toggleChannelFavourite,
                selectedUrls = selectedChannels,
                onToggleSelection = toggleChannelSelection,
            )

            NetworkScreen.FolderFavourites -> FolderFavourites(
                favourites = favouriteFolders,
                error = uiState.error,
                onOpen = viewModel::openFavouriteFolder,
                onRename = { renamingFolder = it },
                onRemove = viewModel::removeFolderFavourite,
            )

            // The only screen here with a neighbour: everything above is somewhere you arrived at,
            // with a way back rather than a sibling.
            NetworkScreen.ServerList -> HorizontalPager(state = pager) { page ->
                if (page == StreamsTab) {
                    PlaylistList(
                        playlists = uiState.savedPlaylists,
                        onOpen = viewModel::openPlaylist,
                        onOpenFavourites = viewModel::openChannelFavourites,
                        onRename = { renamingPlaylist = it },
                        onRemove = { viewModel.removePlaylist(it.url) },
                        onAdd = { isAddPlaylistOpen = true },
                    )
                } else {
                    ServerList(
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        servers = remember(uiState.savedServers, uiState.discoveredServers) {
                            mergeNetworkServers(uiState.savedServers, uiState.discoveredServers)
                        },
                        favouriteFolderCount = favouriteFolders.size,
                        onServerClick = { viewModel.connectTo(it.host) },
                        onEditServer = { viewModel.editServer(it.host, it.displayName) },
                        onForgetServer = { viewModel.forgetServer(it.host) },
                        onOpenFolderFavourites = viewModel::openFolderFavourites,
                        onAdd = { isAddServerOpen = true },
                        onScan = { viewModel.scan(force = true) },
                    )
                }
            }
        }
    }

    if (isAddServerOpen) {
        AddServerDialog(
            onDismiss = { isAddServerOpen = false },
            onConnect = {
                isAddServerOpen = false
                viewModel.connectTo(it)
            },
        )
    }

    if (isAddPlaylistOpen) {
        AddPlaylistDialog(
            onDismiss = { isAddPlaylistOpen = false },
            onAdd = { name, url ->
                isAddPlaylistOpen = false
                viewModel.addPlaylist(name, url)
            },
        )
    }

    renamingPlaylist?.let { playlist ->
        RenameDialog(
            name = playlist.name,
            onDismiss = { renamingPlaylist = null },
            onDone = { newName ->
                viewModel.renamePlaylist(playlist.url, newName)
                renamingPlaylist = null
            },
        )
    }

    renamingFolder?.let { favourite ->
        RenameDialog(
            name = favourite.displayName,
            onDismiss = { renamingFolder = null },
            onDone = { newName ->
                viewModel.renameFolderFavourite(favourite, newName)
                renamingFolder = null
            },
        )
    }

    detailsFor?.let { item ->
        FileDetailsDialog(
            item = item,
            host = uiState.host,
            share = uiState.share,
            onDismiss = { detailsFor = null },
        )
    }

    if (isSortSheetOpen) {
        BrowserSortSheet(
            sort = browserSort,
            onChange = viewModel::setBrowserSort,
            onDismiss = { isSortSheetOpen = false },
        )
    }

    if (isCountrySheetOpen) {
        CountrySheet(
            currentCode = countryCode,
            onSelect = { code ->
                viewModel.switchCountry(code)
                isCountrySheetOpen = false
            },
            onDismiss = { isCountrySheetOpen = false },
        )
    }

    uiState.editingServer?.let { editing ->
        EditServerDialog(
            editing = editing,
            onDismiss = viewModel::dismissEditServer,
            onSave = viewModel::saveEditedServer,
        )
    }
}

/**
 * The address only means anything once the share is connected and the file is open for reading, so
 * the handshake happens here rather than in the player.
 */
private suspend fun NetworkViewModel.play(
    item: SmbFileItem,
    onPlay: (uri: String, title: String, subtitles: List<String>) -> Unit,
) {
    val uris = streamingUris(item) ?: return
    onPlay(uris.media.toString(), item.name, uris.subtitles.map(Any::toString))
}

@Composable
private fun NetworkUiState.title(): String = when (screen) {
    NetworkScreen.ServerList -> stringResource(R.string.network)
    NetworkScreen.Playlist -> playlistName ?: stringResource(R.string.network)
    NetworkScreen.ChannelFavourites -> stringResource(R.string.channel_favourites)
    NetworkScreen.FolderFavourites -> stringResource(R.string.folder_favourites)
    // The server names the three screens that are about the server itself: reaching it, being asked
    // for credentials, and the list of what it shares.
    NetworkScreen.Connecting, NetworkScreen.Auth, NetworkScreen.ShareList ->
        host ?: stringResource(R.string.network)
    // Inside a share the title is where you are, like every other folder in the app. The host as the
    // title of all five screens left the bar saying the same thing six levels down; the trail back
    // up is what the breadcrumb under it is for. The share names its own root, which has no folder
    // to be named after.
    NetworkScreen.FileBrowser -> path.substringAfterLast('\\').ifBlank { share } ?: stringResource(R.string.network)
}

/** Seeded by the app rather than added by the viewer, and so swappable for another country's. */
private val String.isIptvOrg: Boolean
    get() = this == IptvCountry.GlobalUrl || startsWith(IptvCountry.CountryPrefix)

private val String.iptvCountryCode: String?
    get() = removePrefix(IptvCountry.CountryPrefix)
        .removeSuffix(".m3u")
        .takeIf { isIptvOrg && it.length == CountryCodeLength }

private val SearchableScreens = setOf(
    NetworkScreen.Playlist,
    NetworkScreen.ChannelFavourites,
    NetworkScreen.FileBrowser,
)

/**
 * The channels either side of [channel], for the player to step through.
 *
 * Windowed and not the whole list, because the whole list is what crashed the app: an intent travels
 * over Binder, whose transaction buffer is about a megabyte, and a provider's index runs to tens of
 * thousands of channels -- two and a half megabytes of addresses and names, and the system kills the
 * process rather than delivering it.
 *
 * [ZapReach] either side is far more than anyone holds the button through, and costs tens of
 * kilobytes. What it means is that stepping past the edge of the window stops rather than carrying
 * on into the rest of the list -- the same thing that happens at the end of a folder of films.
 */
private fun channelsAround(channel: PlaylistChannel, listed: List<PlaylistChannel>): List<PlaylistChannel> {
    val at = listed.indexOf(channel)
    if (at < 0) return listOf(channel)
    return listed.subList((at - ZapReach).coerceAtLeast(0), (at + ZapReach + 1).coerceAtMost(listed.size))
}

/** How many channels either side travel with the one picked. */
private const val ZapReach = 100

private const val ServersTab = 0

private const val StreamsTab = 1

private const val TabCount = 2

private const val CountryCodeLength = 2

/** Saves the channel selection across a rotation. Addresses, as everywhere else a selection is kept. */
private val SelectedUrlsSaver = Saver<MutableState<Set<String>>, List<String>>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)
