package dev.vayou.tv.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.SavedSmbServer
import dev.vayou.tv.ui.AddPlaylistDialog
import dev.vayou.tv.ui.AddServerDialog
import dev.vayou.tv.ui.ContextAction
import dev.vayou.tv.ui.ItemContextMenu
import dev.vayou.tv.ui.OpenSourceMenu
import dev.vayou.tv.ui.RenameDialog
import dev.vayou.tv.ui.TvDimensions
import dev.vayou.tv.ui.UrlInputDialog
import dev.vayou.tv.ui.VayouTvButton
import dev.vayou.tv.ui.tvLongPressClickable

private val ScreenPadding = TvDimensions.ScreenPadding
private val SectionGap = TvDimensions.SectionGap
private val SectionTitleGap = TvDimensions.SectionTitleGap
private val RowSpacing = TvDimensions.GridSpacing
private val CardWidth = 200.dp

private enum class FocusTarget { Recent, Playlists, Network, None }

@Composable
fun HomeScreen(
    onPlayUri: (String) -> Unit,
    onOpenServer: (SavedSmbServer) -> Unit,
    onConnectServer: (host: String) -> Unit,
    onOpenFolderFavorites: () -> Unit,
    onOpenPlaylist: (SavedPlaylist) -> Unit,
    onOpenIptvFavorites: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val firstCardFocus = remember { FocusRequester() }
    val anchorFocus = remember { FocusRequester() }
    var movedToCard by rememberSaveable { mutableStateOf(false) }
    // Last row the user was on; survives navigation so coming back lands on the same row.
    var lastFocusedRow by rememberSaveable { mutableStateOf<String?>(null) }
    var showOpenMenu by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var menuPlaylist by remember { mutableStateOf<SavedPlaylist?>(null) }
    var renamePlaylist by remember { mutableStateOf<SavedPlaylist?>(null) }
    val pickFile = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) onPlayUri(uri.toString())
    }
    val hasNetwork = state.servers.isNotEmpty() || state.folderFavoritesCount > 0
    val hasPlaylists = state.playlists.isNotEmpty() || state.iptvFavoritesCount > 0
    val focusTarget = when {
        lastFocusedRow == FocusTarget.Recent.name && state.recent.isNotEmpty() -> FocusTarget.Recent
        lastFocusedRow == FocusTarget.Playlists.name && hasPlaylists -> FocusTarget.Playlists
        lastFocusedRow == FocusTarget.Network.name && hasNetwork -> FocusTarget.Network
        state.recent.isNotEmpty() -> FocusTarget.Recent
        hasPlaylists -> FocusTarget.Playlists
        hasNetwork -> FocusTarget.Network
        else -> FocusTarget.None
    }

    LaunchedEffect(Unit) { runCatching { anchorFocus.requestFocus() } }
    LaunchedEffect(focusTarget) {
        if (movedToCard || focusTarget == FocusTarget.None) return@LaunchedEffect
        runCatching { firstCardFocus.requestFocus() }.onSuccess { movedToCard = true }
    }
    // Re-request focus on return — Compose loses focus during NavHost dispose; firstCardFocus
    // is already wired to the row in `lastFocusedRow`, so this lands on the right row.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (movedToCard && focusTarget != FocusTarget.None) {
            runCatching { firstCardFocus.requestFocus() }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!movedToCard) {
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(anchorFocus)
                    .focusable(),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = SectionGap),
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            item(key = "header") {
                HomeHeader(
                    onOpenClick = { showOpenMenu = true },
                    onSettingsClick = onSettingsClick,
                )
            }
        if (state.recent.isNotEmpty()) {
            item(key = "recent") {
                Section(title = "Reproduzido recentemente") {
                    VideoRow(
                        items = state.recent,
                        onClick = { onPlayUri(it.uri) },
                        firstFocusRequester = firstCardFocus,
                        onRowFocused = { lastFocusedRow = FocusTarget.Recent.name },
                    )
                }
            }
        }

        item(key = "network") {
            Section(title = "Rede") {
                NetworkRow(
                    servers = state.servers,
                    showFolderFavoritesEntry = state.folderFavoritesCount > 0,
                    onServerClick = onOpenServer,
                    onOpenFolderFavorites = onOpenFolderFavorites,
                    onAddServer = { showAddServerDialog = true },
                    firstFocusRequester = firstCardFocus.takeIf { focusTarget == FocusTarget.Network },
                    onRowFocused = { lastFocusedRow = FocusTarget.Network.name },
                )
            }
        }

        item(key = "playlists") {
            Section(title = "TV ao vivo") {
                PlaylistRow(
                    playlists = state.playlists,
                    onClick = onOpenPlaylist,
                    onLongPress = { menuPlaylist = it },
                    showFavoritesEntry = state.iptvFavoritesCount > 0,
                    onOpenFavorites = onOpenIptvFavorites,
                    onAddPlaylist = { showAddPlaylistDialog = true },
                    firstFocusRequester = firstCardFocus.takeIf { focusTarget == FocusTarget.Playlists },
                    onRowFocused = { lastFocusedRow = FocusTarget.Playlists.name },
                )
            }
        }
        }
    }

    if (showOpenMenu) {
        OpenSourceMenu(
            onDismiss = { showOpenMenu = false },
            onOpenUrl = {
                showOpenMenu = false
                showUrlDialog = true
            },
            onPickFile = {
                showOpenMenu = false
                pickFile.launch(arrayOf("video/*", "*/*"))
            },
        )
    }

    if (showUrlDialog) {
        UrlInputDialog(
            onConfirm = { url ->
                showUrlDialog = false
                onPlayUri(url)
            },
            onDismiss = { showUrlDialog = false },
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

    if (showAddServerDialog) {
        AddServerDialog(
            onDismiss = { showAddServerDialog = false },
            onConnect = { host ->
                showAddServerDialog = false
                onConnectServer(host)
            },
        )
    }

    menuPlaylist?.let { playlist ->
        ItemContextMenu(
            title = playlist.name,
            actions = listOf(
                ContextAction(
                    label = "Remover lista",
                    icon = Icons.Outlined.Delete,
                    onClick = { viewModel.removePlaylist(playlist.url) },
                ),
                ContextAction(
                    label = "Renomear",
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    onClick = { renamePlaylist = playlist },
                ),
            ),
            onDismiss = { menuPlaylist = null },
        )
    }

    renamePlaylist?.let { playlist ->
        RenameDialog(
            title = "Renomear lista",
            initialValue = playlist.name,
            onConfirm = {
                viewModel.renamePlaylist(playlist.url, it)
                renamePlaylist = null
            },
            onDismiss = { renamePlaylist = null },
        )
    }
}

@Composable
private fun HomeHeader(
    onOpenClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Vayou",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VayouTvButton(onClick = onOpenClick, icon = Icons.Outlined.Add, contentDescription = "Abrir")
            VayouTvButton(onClick = onSettingsClick, icon = Icons.Outlined.Settings, contentDescription = "Configurações")
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionTitleGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = ScreenPadding),
        )
        content()
    }
}

@Composable
private fun VideoRow(
    items: List<RecentItem>,
    onClick: (RecentItem) -> Unit,
    firstFocusRequester: FocusRequester? = null,
    onRowFocused: () -> Unit = {},
) {
    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyRow(
        state = rowState,
        modifier = Modifier.onFocusChanged { if (it.hasFocus) onRowFocused() },
        contentPadding = PaddingValues(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(RowSpacing),
    ) {
        itemsIndexed(items, key = { _, v -> v.uri }) { index, item ->
            VideoCard(
                item = item,
                onClick = { onClick(item) },
                modifier = Modifier
                    .width(CardWidth)
                    .anchorOnFocus(rowState, scope, index)
                    .then(if (index == 0 && firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier),
            )
        }
    }
}

@Composable
private fun NetworkRow(
    servers: List<SavedSmbServer>,
    showFolderFavoritesEntry: Boolean,
    onServerClick: (SavedSmbServer) -> Unit,
    onOpenFolderFavorites: () -> Unit,
    onAddServer: () -> Unit,
    firstFocusRequester: FocusRequester? = null,
    onRowFocused: () -> Unit = {},
) {
    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val serversOffset = if (showFolderFavoritesEntry) 1 else 0
    LazyRow(
        state = rowState,
        modifier = Modifier.onFocusChanged { if (it.hasFocus) onRowFocused() },
        contentPadding = PaddingValues(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(RowSpacing),
    ) {
        if (showFolderFavoritesEntry) {
            item(key = "folder-favorites") {
                LandscapeIconCard(
                    title = "Pastas favoritas",
                    icon = Icons.Filled.Star,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenFolderFavorites,
                    modifier = Modifier
                        .width(CardWidth)
                        .anchorOnFocus(rowState, scope, 0)
                        .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier),
                )
            }
        }
        itemsIndexed(servers, key = { _, s -> "server-${s.host}" }) { index, server ->
            LandscapeIconCard(
                title = server.displayName,
                icon = Icons.Outlined.Wifi,
                onClick = { onServerClick(server) },
                modifier = Modifier
                    .width(CardWidth)
                    .anchorOnFocus(rowState, scope, serversOffset + index)
                    .then(if (!showFolderFavoritesEntry && index == 0 && firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier),
            )
        }
        item(key = "add-server") {
            LandscapeIconCard(
                title = "Adicionar servidor",
                icon = Icons.Outlined.Add,
                onClick = onAddServer,
                modifier = Modifier
                    .width(CardWidth)
                    .anchorOnFocus(rowState, scope, serversOffset + servers.size),
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    playlists: List<SavedPlaylist>,
    onClick: (SavedPlaylist) -> Unit,
    onLongPress: (SavedPlaylist) -> Unit,
    showFavoritesEntry: Boolean,
    onOpenFavorites: () -> Unit,
    onAddPlaylist: () -> Unit,
    firstFocusRequester: FocusRequester? = null,
    onRowFocused: () -> Unit = {},
) {
    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val playlistsOffset = if (showFavoritesEntry) 1 else 0
    LazyRow(
        state = rowState,
        modifier = Modifier.onFocusChanged { if (it.hasFocus) onRowFocused() },
        contentPadding = PaddingValues(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(RowSpacing),
    ) {
        if (showFavoritesEntry) {
            item(key = "iptv-favorites") {
                LandscapeIconCard(
                    title = "Favoritos",
                    icon = Icons.Filled.Star,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenFavorites,
                    modifier = Modifier
                        .width(CardWidth)
                        .anchorOnFocus(rowState, scope, 0)
                        .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier),
                )
            }
        }
        itemsIndexed(playlists, key = { _, p -> "playlist-${p.url}" }) { index, playlist ->
            LandscapeIconCard(
                title = playlist.name,
                icon = Icons.Outlined.LiveTv,
                onClick = { onClick(playlist) },
                onLongPress = { onLongPress(playlist) },
                modifier = Modifier
                    .width(CardWidth)
                    .anchorOnFocus(rowState, scope, playlistsOffset + index)
                    .then(if (!showFavoritesEntry && index == 0 && firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier),
            )
        }
        item(key = "add-playlist") {
            LandscapeIconCard(
                title = "Adicionar playlist",
                icon = Icons.Outlined.Add,
                onClick = onAddPlaylist,
                modifier = Modifier
                    .width(CardWidth)
                    .anchorOnFocus(rowState, scope, playlistsOffset + playlists.size),
            )
        }
    }
}

@Composable
private fun VideoCard(
    item: RecentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = TvDimensions.FocusScale),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            contentColor = cs.onBackground,
            focusedContentColor = cs.onBackground,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(cs.surfaceVariant)
                    .border(
                        width = 2.dp,
                        color = if (isFocused) cs.border else Color.Transparent,
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (item.thumbnailModel != null) {
                    AsyncImage(
                        model = item.thumbnailModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = cs.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                }
                if (item.playedPercentage in 0.001f..0.999f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.4f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.playedPercentage)
                                .fillMaxHeight()
                                .background(cs.primary),
                        )
                    }
                }
            }
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = cs.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
            )
            Text(
                text = typeLabelFor(item.type),
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
            )
        }
    }
}

private fun typeLabelFor(type: RecentType): String = when (type) {
    RecentType.LOCAL -> "Vídeo"
    RecentType.SMB -> "SMB"
    RecentType.STREAM -> "Canal"
}

@Composable
private fun LandscapeIconCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val cs = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.tvLongPressClickable(onLongPress),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = TvDimensions.FocusScale),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            contentColor = cs.onBackground,
            focusedContentColor = cs.onBackground,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium)
                .background(cs.surfaceVariant)
                .border(
                    width = 2.dp,
                    color = if (isFocused) cs.border else Color.Transparent,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = iconTint ?: cs.onSurfaceVariant.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Snaps the focused card to the row's leftmost position so cards align across rows. */
private fun Modifier.anchorOnFocus(state: LazyListState, scope: CoroutineScope, index: Int): Modifier =
    onFocusChanged { fs ->
        if (fs.isFocused) scope.launch { state.animateScrollToItem(index, 0) }
    }

