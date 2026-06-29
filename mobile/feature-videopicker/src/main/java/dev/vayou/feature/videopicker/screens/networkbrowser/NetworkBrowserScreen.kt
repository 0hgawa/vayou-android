package dev.vayou.feature.videopicker.screens.networkbrowser

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.BrowserSortBy
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.IptvCountries
import dev.vayou.core.smb.IptvCountry
import dev.vayou.core.smb.NetworkServerEntry
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.smb.SmbShare
import dev.vayou.core.smb.mergeNetworkServers
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.CancelButton
import dev.vayou.core.ui.components.DoneButton
import dev.vayou.core.ui.components.RenameDialog
import dev.vayou.core.ui.components.VayouDialog
import dev.vayou.core.ui.components.VayouEmptyState
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouButton
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.components.VayouBottomSheet
import dev.vayou.core.ui.designsystem.components.VayouBottomSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenu
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenuItem
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouTabRow
import androidx.compose.material3.OutlinedTextField
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NetworkBrowserScreen(
    onPlayVideo: (Uri, List<Uri>) -> Unit,
    onPlayVideoFromStart: (Uri, List<Uri>) -> Unit,
    onBackAtRoot: () -> Unit,
    viewModel: NetworkBrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteUrls by viewModel.favoriteUrls.collectAsStateWithLifecycle()
    val favoriteChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
    val folderFavorites by viewModel.folderFavorites.collectAsStateWithLifecycle()
    val browserSort by viewModel.browserSort.collectAsStateWithLifecycle()
    var renamingFolderFavorite by remember { mutableStateOf<FavoriteFolder?>(null) }
    var detailsItem by remember { mutableStateOf<SmbFileItem?>(null) }
    var showBrowserSortSheet by remember { mutableStateOf(false) }
    var showCountrySheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<SavedPlaylist?>(null) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    val searchFocusRequester = remember { FocusRequester() }
    val isSearchableScreen = uiState.screen == NetworkScreen.PlaylistDetail ||
        uiState.screen == NetworkScreen.IptvFavorites ||
        uiState.screen == NetworkScreen.FileBrowser
    val playlistGroups = remember(uiState.playlistChannels) {
        uiState.playlistChannels.mapNotNull { it.group?.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()
    }
    val isIptvOrgCountry = remember(uiState.currentPlaylistUrl) {
        val url = uiState.currentPlaylistUrl
        url != null && (url.startsWith(IptvCountry.COUNTRY_PREFIX) || url == IptvCountry.GLOBAL_URL)
    }
    val currentCountryCode = remember(uiState.currentPlaylistUrl) {
        val url = uiState.currentPlaylistUrl ?: return@remember null
        if (url == IptvCountry.GLOBAL_URL) null
        else url.removePrefix(IptvCountry.COUNTRY_PREFIX).removeSuffix(".m3u").takeIf { it.length == 2 }
    }
    val closeSearch = {
        isSearchActive = false
        searchQuery = ""
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) searchFocusRequester.requestFocus()
    }

    LaunchedEffect(uiState.screen) {
        if (!isSearchableScreen) closeSearch()
        if (uiState.screen != NetworkScreen.PlaylistDetail) selectedGroup = null
    }

    LaunchedEffect(uiState.currentPath) {
        if (uiState.screen == NetworkScreen.FileBrowser) closeSearch()
    }

    BackHandler {
        if (isSearchActive) closeSearch()
        else if (!viewModel.navigateUp()) onBackAtRoot()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VayouTheme.colors.background),
    ) {
        VayouTopAppBar(
            title = {
                if (isSearchableScreen && isSearchActive) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = VayouTheme.typography.titleLarge.copy(color = VayouTheme.colors.onSurface),
                        cursorBrush = SolidColor(VayouTheme.colors.accent),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    stringResource(R.string.search),
                                    style = VayouTheme.typography.titleLarge,
                                    color = VayouTheme.colors.onSurfaceVariant,
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester),
                    )
                } else {
                    val title = when (uiState.screen) {
                        NetworkScreen.ServerList -> stringResource(R.string.network)
                        NetworkScreen.PlaylistDetail -> uiState.currentPlaylistName ?: stringResource(R.string.network)
                        NetworkScreen.IptvFavorites -> stringResource(R.string.channel_favorites)
                        NetworkScreen.FolderFavorites -> stringResource(R.string.folder_favorites)
                        NetworkScreen.Connecting, NetworkScreen.Auth,
                        NetworkScreen.ShareList, NetworkScreen.FileBrowser ->
                            uiState.currentHost ?: stringResource(R.string.network)
                    }
                    Text(title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            },
            navigationIcon = {
                if (uiState.screen != NetworkScreen.ServerList) {
                    VayouIconButton(onClick = {
                        if (isSearchActive) closeSearch() else viewModel.navigateUp()
                    }) {
                        Icon(VayouIcons.ArrowBack, contentDescription = stringResource(R.string.navigate_up))
                    }
                }
            },
            actions = {
                if (uiState.screen == NetworkScreen.PlaylistDetail) {
                    when {
                        isIptvOrgCountry -> VayouIconButton(onClick = { showCountrySheet = true }) {
                            Icon(
                                imageVector = VayouIcons.Filter,
                                contentDescription = stringResource(R.string.filter_by_group),
                                tint = VayouTheme.colors.onSurface,
                            )
                        }
                        playlistGroups.isNotEmpty() -> GroupFilterButton(
                            groups = playlistGroups,
                            selected = selectedGroup,
                            onSelect = { selectedGroup = it },
                        )
                    }
                }
                if (uiState.screen == NetworkScreen.FileBrowser) {
                    VayouIconButton(onClick = { showBrowserSortSheet = true }) {
                        Icon(VayouIcons.Sort, contentDescription = stringResource(R.string.sort))
                    }
                }
                if (isSearchableScreen) {
                    VayouIconButton(onClick = {
                        if (isSearchActive) closeSearch() else isSearchActive = true
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) VayouIcons.Close else VayouIcons.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                }
            },
        )

        if (uiState.screen == NetworkScreen.ServerList) {
            VayouTabRow(
                selectedIndex = selectedTab,
                tabs = listOf(stringResource(R.string.servers), stringResource(R.string.streams)),
                onTabSelected = { selectedTab = it },
            )
        }

        when {
            uiState.screen == NetworkScreen.PlaylistDetail -> PlaylistDetailContent(
                isLoading = uiState.isLoading,
                channels = uiState.playlistChannels,
                favoriteUrls = favoriteUrls,
                error = uiState.error,
                searchQuery = searchQuery,
                selectedGroup = selectedGroup,
                onChannelClick = { onPlayVideo(Uri.parse(it.url), emptyList()) },
                onToggleFavorite = viewModel::toggleFavorite,
            )

            uiState.screen == NetworkScreen.IptvFavorites -> FavoritesContent(
                channels = favoriteChannels,
                searchQuery = searchQuery,
                onChannelClick = { onPlayVideo(Uri.parse(it.url), emptyList()) },
                onToggleFavorite = viewModel::toggleFavorite,
            )

            uiState.screen == NetworkScreen.FolderFavorites -> FolderFavoritesContent(
                favorites = folderFavorites,
                onFavoriteClick = viewModel::openFavoriteFolder,
                onRename = { renamingFolderFavorite = it },
                onRemove = viewModel::removeFolderFavorite,
            )

            uiState.screen == NetworkScreen.Connecting -> ConnectingContent()

            uiState.screen == NetworkScreen.Auth -> AuthContent(
                host = uiState.currentHost ?: "",
                isLoading = uiState.isLoading,
                error = uiState.error,
                onSubmit = { user, pass, name -> viewModel.onCredentialsSubmit(user, pass, name) },
            )

            uiState.screen == NetworkScreen.ShareList -> ShareListContent(
                host = uiState.currentHost ?: "",
                isLoading = uiState.isLoading,
                shares = uiState.shares,
                error = uiState.error,
                favoritedShares = remember(folderFavorites, uiState.currentHost) {
                    folderFavorites.asSequence()
                        .filter { it.host == uiState.currentHost && it.path.isEmpty() }
                        .mapTo(mutableSetOf()) { it.share }
                },
                onShareClick = { viewModel.connectToShare(it.name) },
                onToggleShareFavorite = viewModel::toggleShareFavorite,
            )

            uiState.screen == NetworkScreen.FileBrowser -> FileBrowserContent(
                path = uiState.currentPath,
                share = uiState.currentShare ?: "",
                isLoading = uiState.isLoading,
                files = uiState.files,
                error = uiState.error,
                searchQuery = searchQuery,
                sort = browserSort,
                onNavigateToSegment = viewModel::navigateToSegment,
                favoritedPaths = remember(folderFavorites, uiState.currentHost, uiState.currentShare) {
                    folderFavorites.asSequence()
                        .filter { it.host == uiState.currentHost && it.share == uiState.currentShare }
                        .mapTo(mutableSetOf()) { it.path }
                },
                onDirectoryClick = { viewModel.navigateInto(it) },
                onToggleFolderFavorite = viewModel::toggleFolderFavorite,
                onVideoClick = { item ->
                    scope.launch {
                        val (uri, subs) = viewModel.startStreaming(item)
                        if (uri != Uri.EMPTY) onPlayVideo(uri, subs)
                    }
                },
                onPlayVideoFromStart = { item ->
                    scope.launch {
                        val (uri, subs) = viewModel.startStreaming(item)
                        if (uri != Uri.EMPTY) onPlayVideoFromStart(uri, subs)
                    }
                },
                onShowVideoDetails = { detailsItem = it },
            )

            selectedTab == 1 -> PlaylistsTabContent(
                playlists = uiState.savedPlaylists,
                onPlaylistClick = { viewModel.openPlaylist(it) },
                onOpenFavorites = viewModel::openFavorites,
                onAddClick = { showAddPlaylistDialog = true },
                onEditPlaylist = { editingPlaylist = it },
                onRemovePlaylist = { viewModel.removePlaylist(it.url) },
            )

            else -> ServerListContent(
                isLoading = uiState.isLoading,
                error = uiState.error,
                servers = remember(uiState.savedServers, uiState.discoveredServers) {
                    mergeNetworkServers(uiState.savedServers, uiState.discoveredServers)
                },
                onServerClick = { viewModel.connectToServer(it.host) },
                onEditServer = { viewModel.openEditServer(it.host, it.displayName) },
                onRemoveServer = { viewModel.removeServer(it.host) },
                onRefresh = { viewModel.startDiscovery(forceRefresh = true) },
                onAddServerClick = { showAddServerDialog = true },
                onOpenFolderFavorites = viewModel::openFolderFavorites,
            )
        }
    }

    if (showAddServerDialog) {
        AddServerDialog(
            onDismiss = { showAddServerDialog = false },
            onConnect = {
                showAddServerDialog = false
                viewModel.connectToServer(it)
            },
        )
    }

    if (showAddPlaylistDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddPlaylistDialog = false },
            onAdd = { name, url ->
                showAddPlaylistDialog = false
                viewModel.addPlaylist(name, url)
            },
        )
    }

    editingPlaylist?.let { playlist ->
        RenameDialog(
            name = playlist.name,
            onDismiss = { editingPlaylist = null },
            onDone = { newName ->
                viewModel.renamePlaylist(playlist.url, newName)
                editingPlaylist = null
            },
        )
    }

    renamingFolderFavorite?.let { favorite ->
        RenameDialog(
            name = favorite.displayName,
            onDismiss = { renamingFolderFavorite = null },
            onDone = { newName ->
                viewModel.renameFolderFavorite(favorite, newName)
                renamingFolderFavorite = null
            },
        )
    }

    detailsItem?.let { item ->
        VideoDetailsDialog(
            item = item,
            host = uiState.currentHost,
            share = uiState.currentShare,
            onDismiss = { detailsItem = null },
        )
    }

    if (showBrowserSortSheet) {
        BrowserSortSheet(
            sort = browserSort,
            onChange = viewModel::setBrowserSort,
            onDismiss = { showBrowserSortSheet = false },
        )
    }

    if (showCountrySheet) {
        CountryFilterSheet(
            currentCode = currentCountryCode,
            onSelect = { code ->
                viewModel.switchIptvCountry(code)
                showCountrySheet = false
            },
            onDismiss = { showCountrySheet = false },
        )
    }

    uiState.editingServer?.let { editing ->
        EditServerDialog(
            editing = editing,
            onDismiss = { viewModel.dismissEditServer() },
            onSave = { displayName, username, password, domain ->
                viewModel.saveEditServer(displayName, username, password, domain)
            },
        )
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<SavedPlaylist>,
    onPlaylistClick: (SavedPlaylist) -> Unit,
    onOpenFavorites: () -> Unit,
    onAddClick: () -> Unit,
    onEditPlaylist: (SavedPlaylist) -> Unit,
    onRemovePlaylist: (SavedPlaylist) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            SectionHeader(
                title = stringResource(R.string.saved_playlists),
                topPadding = 20.dp,
                trailingContent = {
                    VayouIconButton(onClick = onAddClick) {
                        Icon(VayouIcons.Add, contentDescription = stringResource(R.string.add_playlist), modifier = Modifier.size(VayouTheme.iconSize.sm))
                    }
                },
            )
        }

        item(key = "favorites_entry") {
            NetworkListItem(
                icon = {
                    IconContainer {
                        Icon(
                            VayouIcons.StarFilled,
                            contentDescription = null,
                            tint = VayouTheme.colors.accent,
                            modifier = Modifier.size(VayouTheme.iconSize.lg),
                        )
                    }
                },
                title = stringResource(R.string.channel_favorites),
                onClick = onOpenFavorites,
            )
        }

        if (playlists.isEmpty()) {
            item {
                VayouEmptyState(
                    icon = VayouIcons.Movie,
                    title = stringResource(R.string.no_playlists_saved),
                )
            }
        }

        items(playlists, key = { it.url }) { playlist ->
            NetworkListItem(
                icon = { IconContainer { Icon(VayouIcons.Movie, contentDescription = null, tint = VayouTheme.colors.accent, modifier = Modifier.size(VayouTheme.iconSize.lg)) } },
                title = playlist.name,
                subtitle = playlist.url,
                onClick = { onPlaylistClick(playlist) },
                trailingContent = {
                    ItemOverflowMenu(
                        onEdit = { onEditPlaylist(playlist) },
                        onDelete = { onRemovePlaylist(playlist) },
                    )
                },
            )
        }
    }
}

@Composable
private fun FavoritesContent(
    channels: List<PlaylistChannel>,
    searchQuery: String,
    onChannelClick: (PlaylistChannel) -> Unit,
    onToggleFavorite: (PlaylistChannel) -> Unit,
) {
    if (channels.isEmpty()) {
        VayouEmptyState(
            icon = VayouIcons.StarFilled,
            title = stringResource(R.string.no_favorites_yet),
        )
        return
    }
    val filtered = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) channels
        else channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    if (filtered.isEmpty()) {
        VayouEmptyState(
            icon = VayouIcons.Search,
            title = stringResource(R.string.no_results_found),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(filtered, key = { it.url }) { channel ->
            ChannelItem(
                channel = channel,
                isFavorite = true,
                onClick = { onChannelClick(channel) },
                onToggleFavorite = { onToggleFavorite(channel) },
            )
        }
    }
}

@Composable
private fun ServerListContent(
    isLoading: Boolean,
    error: String?,
    servers: List<NetworkServerEntry>,
    onServerClick: (NetworkServerEntry) -> Unit,
    onEditServer: (NetworkServerEntry) -> Unit,
    onRemoveServer: (NetworkServerEntry) -> Unit,
    onRefresh: () -> Unit,
    onAddServerClick: () -> Unit,
    onOpenFolderFavorites: () -> Unit,
) {
    val offlineLabel = stringResource(R.string.offline)
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (error != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(VayouIcons.Priority, contentDescription = null, tint = VayouTheme.colors.error, modifier = Modifier.size(VayouTheme.iconSize.sm))
                    Text(error, style = VayouTheme.typography.bodyMedium, color = VayouTheme.colors.error)
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.servers),
                topPadding = 20.dp,
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (isLoading) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                VayouCircularProgress(size = 20.dp, strokeWidth = 2.dp)
                            }
                        } else {
                            VayouIconButton(onClick = onRefresh) {
                                Icon(VayouIcons.Update, contentDescription = null, modifier = Modifier.size(VayouTheme.iconSize.sm))
                            }
                        }
                        VayouIconButton(onClick = onAddServerClick) {
                            Icon(VayouIcons.Add, contentDescription = stringResource(R.string.connect_to_server), modifier = Modifier.size(VayouTheme.iconSize.sm))
                        }
                    }
                },
            )
        }

        item(key = "folder_favorites_entry") {
            NetworkListItem(
                icon = {
                    IconContainer {
                        Icon(
                            VayouIcons.StarFilled,
                            contentDescription = null,
                            tint = VayouTheme.colors.accent,
                            modifier = Modifier.size(VayouTheme.iconSize.lg),
                        )
                    }
                },
                title = stringResource(R.string.folder_favorites),
                onClick = onOpenFolderFavorites,
            )
        }

        if (servers.isEmpty() && !isLoading) {
            item {
                VayouEmptyState(
                    icon = VayouIcons.Wifi,
                    title = stringResource(R.string.no_servers_found),
                )
            }
        }

        items(servers, key = { it.host }) { entry ->
            val subtitle = listOfNotNull(
                entry.host.takeIf { it != entry.displayName },
                offlineLabel.takeIf { entry.isSaved && !entry.isOnline },
            ).joinToString(" · ").ifEmpty { null }
            NetworkListItem(
                icon = {
                    IconContainer {
                        Icon(
                            imageVector = if (entry.isSaved) VayouIcons.Network else VayouIcons.Wifi,
                            contentDescription = null,
                            tint = if (entry.isOnline) VayouTheme.colors.accent
                                else VayouTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(VayouTheme.iconSize.lg),
                        )
                    }
                },
                title = entry.displayName,
                subtitle = subtitle,
                onClick = { onServerClick(entry) },
                trailingContent = if (entry.isSaved) {
                    {
                        ItemOverflowMenu(
                            onEdit = { onEditServer(entry) },
                            onDelete = { onRemoveServer(entry) },
                        )
                    }
                } else null,
            )
        }
    }
}

@Composable
private fun ConnectingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        VayouCircularProgress()
    }
}

@Composable
private fun AuthContent(
    host: String,
    isLoading: Boolean,
    error: String?,
    onSubmit: (username: String, password: String, displayName: String) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.authentication_required), style = VayouTheme.typography.titleMedium, color = VayouTheme.colors.onSurface)
        Text(host, style = VayouTheme.typography.bodyMedium, color = VayouTheme.colors.onSurfaceVariant)

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(R.string.server_name_optional)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            visualTransformation = PasswordVisualTransformation(),
        )

        if (error != null) {
            Text(error, style = VayouTheme.typography.bodySmall, color = VayouTheme.colors.error)
        }

        VayouButton(
            onClick = { onSubmit(username, password, displayName) },
            enabled = !isLoading && username.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) {
                VayouCircularProgress(size = 18.dp, strokeWidth = 2.dp, color = VayouTheme.colors.onAccent)
            } else {
                Text(stringResource(R.string.connect), style = VayouTheme.typography.labelLarge)
            }
        }
    }
}


@Composable
private fun ShareListContent(
    host: String,
    isLoading: Boolean,
    shares: List<SmbShare>,
    error: String?,
    favoritedShares: Set<String>,
    onShareClick: (SmbShare) -> Unit,
    onToggleShareFavorite: (SmbShare) -> Unit,
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VayouCircularProgress()
        }
        return
    }

    if (error != null) {
        VayouEmptyState(
            icon = VayouIcons.Priority,
            title = error,
            iconTint = VayouTheme.colors.error,
        )
        return
    }

    if (shares.isEmpty()) {
        VayouEmptyState(
            icon = VayouIcons.Folder,
            title = stringResource(R.string.no_shares_found),
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SectionHeader(host) }
        items(shares, key = { it.name }) { share ->
            NetworkListItem(
                icon = { IconContainer { Icon(VayouIcons.FolderFilled, contentDescription = null, tint = VayouTheme.colors.accent, modifier = Modifier.size(VayouTheme.iconSize.lg)) } },
                title = share.name,
                onClick = { onShareClick(share) },
                trailingContent = {
                    FolderFavoriteMenu(
                        isFavorited = share.name in favoritedShares,
                        onToggle = { onToggleShareFavorite(share) },
                    )
                },
            )
        }
    }
}

@Composable
private fun FileBrowserContent(
    path: String,
    share: String,
    isLoading: Boolean,
    files: List<SmbFileItem>,
    error: String?,
    searchQuery: String,
    sort: BrowserSort,
    favoritedPaths: Set<String>,
    onNavigateToSegment: (Int) -> Unit,
    onDirectoryClick: (SmbFileItem) -> Unit,
    onToggleFolderFavorite: (SmbFileItem) -> Unit,
    onVideoClick: (SmbFileItem) -> Unit,
    onPlayVideoFromStart: (SmbFileItem) -> Unit,
    onShowVideoDetails: (SmbFileItem) -> Unit,
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VayouCircularProgress()
        }
        return
    }

    if (error != null) {
        VayouEmptyState(
            icon = VayouIcons.Priority,
            title = error,
            iconTint = VayouTheme.colors.error,
        )
        return
    }

    if (files.isEmpty()) {
        VayouEmptyState(
            icon = VayouIcons.FolderOff,
            title = stringResource(R.string.no_files_found),
        )
        return
    }

    val filtered = remember(files, searchQuery, sort) {
        val matched = if (searchQuery.isBlank()) files
            else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
        val axisComparator: Comparator<SmbFileItem> = when (sort.by) {
            BrowserSortBy.NAME -> compareBy { it.name.lowercase() }
            BrowserSortBy.SIZE -> compareBy { it.size }
        }
        matched.sortedWith(if (sort.asc) axisComparator else axisComparator.reversed())
    }
    if (filtered.isEmpty()) {
        VayouEmptyState(
            icon = VayouIcons.Search,
            title = stringResource(R.string.no_results_found),
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (share.isNotEmpty()) {
            item {
                BreadcrumbBar(
                    share = share,
                    path = path,
                    onSegmentClick = onNavigateToSegment,
                )
            }
        }
        items(filtered, key = { it.path }) { file ->
            NetworkListItem(
                icon = {
                    IconContainer {
                        Icon(
                            imageVector = if (file.isDirectory) VayouIcons.FolderFilled else VayouIcons.Video,
                            contentDescription = null,
                            tint = if (file.isDirectory) VayouTheme.colors.accent else VayouTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(VayouTheme.iconSize.lg),
                        )
                    }
                },
                title = file.name,
                subtitle = if (!file.isDirectory && file.size > 0) formatSize(file.size) else null,
                onClick = { if (file.isDirectory) onDirectoryClick(file) else if (file.isVideo) onVideoClick(file) },
                trailingContent = when {
                    file.isDirectory -> {
                        {
                            FolderFavoriteMenu(
                                isFavorited = file.path in favoritedPaths,
                                onToggle = { onToggleFolderFavorite(file) },
                            )
                        }
                    }
                    file.isVideo -> {
                        {
                            VideoActionsMenu(
                                onPlayFromStart = { onPlayVideoFromStart(file) },
                                onShowDetails = { onShowVideoDetails(file) },
                            )
                        }
                    }
                    else -> null
                },
            )
        }
    }
}

@Composable
private fun BrowserSortSheet(
    sort: BrowserSort,
    onChange: (BrowserSort) -> Unit,
    onDismiss: () -> Unit,
) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        VayouBottomSheetTitle(text = stringResource(R.string.sort))
        BrowserSortBy.entries.forEach { axis ->
            val isSelected = sort.by == axis
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        imageVector = when (axis) {
                            BrowserSortBy.NAME -> VayouIcons.Title
                            BrowserSortBy.SIZE -> VayouIcons.Size
                        },
                        contentDescription = null,
                        tint = if (isSelected) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                },
                text = {
                    Text(
                        text = when (axis) {
                            BrowserSortBy.NAME -> stringResource(R.string.name)
                            BrowserSortBy.SIZE -> stringResource(R.string.size)
                        },
                        style = VayouTheme.typography.bodyMedium,
                        color = if (isSelected) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                    )
                },
                trailingIcon = if (isSelected) ({
                    Icon(
                        imageVector = if (sort.asc) VayouIcons.ArrowUpward else VayouIcons.ArrowDownward,
                        contentDescription = null,
                        tint = VayouTheme.colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }) else null,
                onClick = {
                    onChange(
                        if (isSelected) sort.copy(asc = !sort.asc)
                        else sort.copy(by = axis),
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
private fun BreadcrumbBar(
    share: String,
    path: String,
    onSegmentClick: (Int) -> Unit,
) {
    val segments = remember(path) { path.split('\\').filter { it.isNotBlank() } }
    val scrollState = rememberScrollState()
    LaunchedEffect(path) { scrollState.animateScrollTo(scrollState.maxValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BreadcrumbSegment(
            text = share,
            isCurrent = segments.isEmpty(),
            onClick = { onSegmentClick(-1) },
        )
        segments.forEachIndexed { index, segment ->
            BreadcrumbSeparator()
            BreadcrumbSegment(
                text = segment,
                isCurrent = index == segments.lastIndex,
                onClick = { onSegmentClick(index) },
            )
        }
    }
}

@Composable
private fun BreadcrumbSegment(
    text: String,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = VayouTheme.typography.labelMedium,
        color = if (isCurrent) VayouTheme.colors.onSurface else VayouTheme.colors.accent,
        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun BreadcrumbSeparator() {
    Text(
        text = "›",
        style = VayouTheme.typography.labelMedium,
        color = VayouTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun FolderFavoriteMenu(
    isFavorited: Boolean,
    onToggle: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        VayouIconButton(onClick = { expanded = true }) {
            Icon(VayouIcons.MoreVert, contentDescription = null, tint = VayouTheme.colors.onSurfaceVariant)
        }
        VayouDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VayouDropdownMenuItem(
                text = stringResource(if (isFavorited) R.string.remove_from_favorites else R.string.add_to_favorites),
                icon = VayouIcons.StarFilled,
                contentColor = if (isFavorited) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                onClick = { expanded = false; onToggle() },
            )
        }
    }
}

@Composable
private fun VideoActionsMenu(
    onPlayFromStart: () -> Unit,
    onShowDetails: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        VayouIconButton(onClick = { expanded = true }) {
            Icon(VayouIcons.MoreVert, contentDescription = null, tint = VayouTheme.colors.onSurfaceVariant)
        }
        VayouDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VayouDropdownMenuItem(
                text = stringResource(R.string.play_from_start),
                icon = VayouIcons.Play,
                onClick = { expanded = false; onPlayFromStart() },
            )
            VayouDropdownMenuItem(
                text = stringResource(R.string.details),
                icon = VayouIcons.Info,
                onClick = { expanded = false; onShowDetails() },
            )
        }
    }
}

@Composable
private fun VideoDetailsDialog(
    item: SmbFileItem,
    host: String?,
    share: String?,
    onDismiss: () -> Unit,
) {
    val location = listOfNotNull(host, share, item.path.replace('\\', '/').ifBlank { null })
        .joinToString("/")
    VayouDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, style = VayouTheme.typography.titleMedium, color = VayouTheme.colors.onSurface) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.size > 0) DetailRow(stringResource(R.string.size), formatSize(item.size))
                DetailRow(stringResource(R.string.location), location)
            }
        },
        confirmButton = { DoneButton(onClick = onDismiss) },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = VayouTheme.typography.labelMedium, color = VayouTheme.colors.onSurfaceVariant)
        Text(value, style = VayouTheme.typography.bodyMedium, color = VayouTheme.colors.onSurface)
    }
}

@Composable
private fun FolderFavoritesContent(
    favorites: List<FavoriteFolder>,
    onFavoriteClick: (FavoriteFolder) -> Unit,
    onRename: (FavoriteFolder) -> Unit,
    onRemove: (FavoriteFolder) -> Unit,
) {
    if (favorites.isEmpty()) {
        VayouEmptyState(
            icon = VayouIcons.StarFilled,
            title = stringResource(R.string.no_favorites_yet),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(favorites, key = { "${it.host}|${it.share}|${it.path}" }) { favorite ->
            NetworkListItem(
                icon = {
                    IconContainer {
                        Icon(
                            VayouIcons.FolderFilled,
                            contentDescription = null,
                            tint = VayouTheme.colors.accent,
                            modifier = Modifier.size(VayouTheme.iconSize.lg),
                        )
                    }
                },
                title = favorite.displayName,
                subtitle = "${favorite.host} · ${favorite.share}/${favorite.path.replace('\\', '/')}".trimEnd('/'),
                onClick = { onFavoriteClick(favorite) },
                trailingContent = {
                    ItemOverflowMenu(
                        onEdit = { onRename(favorite) },
                        onDelete = { onRemove(favorite) },
                    )
                },
            )
        }
    }
}

@Composable
private fun PlaylistDetailContent(
    isLoading: Boolean,
    channels: List<PlaylistChannel>,
    favoriteUrls: Set<String>,
    error: String?,
    searchQuery: String,
    selectedGroup: String?,
    onChannelClick: (PlaylistChannel) -> Unit,
    onToggleFavorite: (PlaylistChannel) -> Unit,
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VayouCircularProgress()
        }
        return
    }

    if (error != null) {
        VayouEmptyState(
            icon = VayouIcons.Priority,
            title = error,
            iconTint = VayouTheme.colors.error,
        )
        return
    }

    if (channels.isEmpty()) {
        VayouEmptyState(
            icon = VayouIcons.Movie,
            title = stringResource(R.string.no_files_found),
        )
        return
    }

    val isFiltered = selectedGroup != null || searchQuery.isNotBlank()

    if (isFiltered) {
        val filtered = remember(channels, selectedGroup, searchQuery) {
            val byGroup = if (selectedGroup == null) channels
                else channels.filter { it.group == selectedGroup }
            if (searchQuery.isBlank()) byGroup
            else byGroup.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        if (filtered.isEmpty()) {
            VayouEmptyState(
                icon = if (searchQuery.isNotBlank()) VayouIcons.Search else VayouIcons.Filter,
                title = stringResource(R.string.no_results_found),
            )
            return
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.url }) { channel ->
                ChannelItem(
                    channel = channel,
                    isFavorite = channel.url in favoriteUrls,
                    onClick = { onChannelClick(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                )
            }
        }
        return
    }

    val grouped = remember(channels) {
        channels.groupBy { it.group ?: "" }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (group, groupChannels) ->
            if (group.isNotEmpty()) {
                item(key = "header_$group") {
                    SectionHeader(group)
                }
            }
            itemsIndexed(groupChannels, key = { index, _ -> "${group}_$index" }) { _, channel ->
                ChannelItem(
                    channel = channel,
                    isFavorite = channel.url in favoriteUrls,
                    onClick = { onChannelClick(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) },
                )
            }
        }
    }
}

@Composable
private fun ChannelItem(
    channel: PlaylistChannel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChannelLogoBox(logo = channel.logo)
        Text(
            text = channel.name,
            style = VayouTheme.typography.titleSmall,
            color = VayouTheme.colors.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        VayouIconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = VayouIcons.StarFilled,
                contentDescription = null,
                tint = if (isFavorite) VayouTheme.colors.accent else VayouTheme.colors.onSurface.copy(alpha = 0.15f),
                modifier = Modifier.size(VayouTheme.iconSize.sm),
            )
        }
    }
}

@Composable
private fun ChannelLogoBox(logo: String?) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .aspectRatio(4f / 3f)
            .clip(VayouTheme.shapes.small)
            .background(VayouTheme.colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = VayouIcons.Play,
                contentDescription = null,
                tint = VayouTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun IconContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(VayouTheme.shapes.medium)
            .background(VayouTheme.colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun NetworkListItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = VayouTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = VayouTheme.colors.onSurface, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, style = VayouTheme.typography.bodySmall, color = VayouTheme.colors.onSurfaceVariant, maxLines = 1)
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
private fun SectionHeader(
    title: String,
    topPadding: Dp = 12.dp,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = VayouTheme.typography.labelMedium,
            color = VayouTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
        trailingContent?.invoke()
    }
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
) {
    var address by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    VayouDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connect_to_server)) },
        content = {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.server_address)) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            DoneButton(
                enabled = address.isNotBlank(),
                onClick = { onConnect(address.trim()) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
    LaunchedEffect(Unit) {
        delay(200.milliseconds)
        focusRequester.requestFocus()
    }
}

@Composable
private fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    VayouDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_playlist)) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.playlist_url)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
        },
        confirmButton = {
            DoneButton(
                enabled = url.isNotBlank(),
                onClick = { onAdd(name.trim(), url.trim()) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
    LaunchedEffect(Unit) {
        delay(200.milliseconds)
        focusRequester.requestFocus()
    }
}

@Composable
private fun GroupFilterButton(
    groups: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        VayouIconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = VayouIcons.Filter,
                contentDescription = stringResource(R.string.filter_by_group),
                tint = if (selected != null) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
            )
        }
        VayouDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                FilterMenuItem(
                    text = stringResource(R.string.all_groups),
                    isSelected = selected == null,
                    onClick = { expanded = false; onSelect(null) },
                )
                groups.forEach { group ->
                    FilterMenuItem(
                        text = group,
                        isSelected = selected == group,
                        onClick = { expanded = false; onSelect(group) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryFilterSheet(
    currentCode: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        VayouBottomSheetTitle(text = "País")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            IptvCountries.forEach { country ->
                FilterSheetRow(
                    text = country.name,
                    isSelected = (country.code ?: "") == (currentCode ?: ""),
                    onClick = { onSelect(country.code) },
                )
            }
        }
    }
}

@Composable
private fun FilterMenuItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.DropdownMenuItem(
        modifier = Modifier.fillMaxWidth(),
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    style = VayouTheme.typography.labelLarge,
                    color = if (isSelected) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                    Icon(
                        imageVector = VayouIcons.Check,
                        contentDescription = null,
                        tint = VayouTheme.colors.accent,
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                }
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun FilterSheetRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = VayouTheme.typography.bodyLarge,
            color = if (isSelected) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = VayouIcons.Check,
                contentDescription = null,
                tint = VayouTheme.colors.accent,
                modifier = Modifier.size(VayouTheme.iconSize.sm),
            )
        }
    }
}

@Composable
private fun ItemOverflowMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        VayouIconButton(onClick = { expanded = true }) {
            Icon(VayouIcons.MoreVert, contentDescription = null, tint = VayouTheme.colors.onSurfaceVariant)
        }
        VayouDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VayouDropdownMenuItem(
                text = stringResource(R.string.rename),
                icon = VayouIcons.Edit,
                onClick = { expanded = false; onEdit() },
            )
            VayouDropdownMenuItem(
                text = stringResource(R.string.remove),
                icon = VayouIcons.Delete,
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}

@Composable
private fun EditServerDialog(
    editing: EditServerState,
    onDismiss: () -> Unit,
    onSave: (displayName: String, username: String, password: String, domain: String) -> Unit,
) {
    var displayName by remember { mutableStateOf(editing.displayName) }
    var username by remember { mutableStateOf(editing.username) }
    var password by remember { mutableStateOf(editing.password) }
    VayouDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_server)) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(editing.host, style = VayouTheme.typography.bodySmall, color = VayouTheme.colors.onSurfaceVariant)
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.server_name_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = {
            DoneButton(
                enabled = true,
                onClick = { onSave(displayName.trim(), username.trim(), password, editing.domain) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismiss) },
    )
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}
