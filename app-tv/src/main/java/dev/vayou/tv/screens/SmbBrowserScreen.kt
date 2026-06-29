package dev.vayou.tv.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.BrowserSortBy
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.tv.ui.BrowserSortMenu
import dev.vayou.tv.ui.ContextAction
import dev.vayou.tv.ui.GridCard
import dev.vayou.tv.ui.ItemContextMenu
import dev.vayou.tv.ui.RenameDialog
import dev.vayou.tv.ui.TvDimensions
import dev.vayou.tv.ui.VayouSearchField
import dev.vayou.tv.ui.VayouTvButton

private val ScreenPadding = TvDimensions.ScreenPadding
private val GridSpacing = TvDimensions.GridSpacing
private const val GridColumns = 4

@Composable
fun SmbBrowserScreen(
    onPlay: (Uri) -> Unit,
    onBack: () -> Unit,
    viewModel: SmbBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val browserSort by viewModel.browserSort.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    LaunchedEffect(state.shareName, state.path) {
        query = ""
        searchOpen = false
    }

    BackHandler {
        when {
            searchOpen -> { searchOpen = false; query = "" }
            state.shareName != null -> viewModel.navigateUp()
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (state.status) {
            SmbStatus.Connecting -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ScreenPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Header(state = state)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Conectando…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SmbStatus.NeedsAuth -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ScreenPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Header(state = state)
                AuthForm(
                    error = state.error,
                    onSubmit = viewModel::submitCredentials,
                )
            }

            SmbStatus.Browsing -> {
                val favoritePaths by viewModel.favoritePaths.collectAsStateWithLifecycle()
                var menuItem by remember { mutableStateOf<SmbFileItem?>(null) }
                var renameItem by remember { mutableStateOf<SmbFileItem?>(null) }
                var menuShare by remember { mutableStateOf<dev.vayou.core.smb.SmbShare?>(null) }
                var renameShare by remember { mutableStateOf<dev.vayou.core.smb.SmbShare?>(null) }
                Browsing(
                    state = state,
                    favoritePaths = favoritePaths,
                    query = query,
                    searchOpen = searchOpen,
                    sort = browserSort,
                    onQueryChange = { query = it },
                    onOpenSearch = { searchOpen = true },
                    onCloseSearch = { searchOpen = false; query = "" },
                    onOpenSort = { showSortMenu = true },
                    onShareClick = viewModel::openShare,
                    onShareLongPress = { share -> menuShare = share },
                    onItemClick = { item ->
                        if (item.isDirectory) {
                            viewModel.openDirectory(item)
                        } else if (item.isVideo) {
                            val share = state.shareName ?: return@Browsing
                            val uri = Uri.parse("smb://${state.host}/$share/${item.path}".replace('\\', '/'))
                            onPlay(uri)
                        }
                    },
                    onLongPress = { item -> menuItem = item },
                    onSegmentClick = viewModel::navigateToSegment,
                    onBackToShares = { viewModel.navigateUp() },
                )

                menuItem?.let { item ->
                    val share = state.shareName ?: ""
                    val isFav = "$share|${item.path}" in favoritePaths
                    ItemContextMenu(
                        title = item.name,
                        actions = buildList {
                            add(
                                ContextAction(
                                    label = if (isFav) "Remover dos favoritos" else "Favoritar",
                                    icon = if (isFav) Icons.Outlined.Delete else Icons.Filled.Star,
                                    onClick = { viewModel.toggleFolderFavorite(item) },
                                ),
                            )
                            if (isFav) {
                                add(
                                    ContextAction(
                                        label = "Renomear",
                                        icon = Icons.Outlined.DriveFileRenameOutline,
                                        onClick = { renameItem = item },
                                    ),
                                )
                            }
                        },
                        onDismiss = { menuItem = null },
                    )
                }

                renameItem?.let { item ->
                    RenameDialog(
                        title = "Renomear pasta",
                        initialValue = item.name,
                        onConfirm = {
                            viewModel.renameFolderFavorite(item, it)
                            renameItem = null
                        },
                        onDismiss = { renameItem = null },
                    )
                }

                menuShare?.let { share ->
                    val isFav = "${share.name}|" in favoritePaths
                    ItemContextMenu(
                        title = share.name,
                        actions = buildList {
                            add(
                                ContextAction(
                                    label = if (isFav) "Remover dos favoritos" else "Favoritar",
                                    icon = if (isFav) Icons.Outlined.Delete else Icons.Filled.Star,
                                    onClick = { viewModel.toggleShareFavorite(share) },
                                ),
                            )
                            if (isFav) {
                                add(
                                    ContextAction(
                                        label = "Renomear",
                                        icon = Icons.Outlined.DriveFileRenameOutline,
                                        onClick = { renameShare = share },
                                    ),
                                )
                            }
                        },
                        onDismiss = { menuShare = null },
                    )
                }

                renameShare?.let { share ->
                    RenameDialog(
                        title = "Renomear share",
                        initialValue = share.name,
                        onConfirm = {
                            viewModel.renameShareFavorite(share, it)
                            renameShare = null
                        },
                        onDismiss = { renameShare = null },
                    )
                }
            }
        }
    }

    if (showSortMenu) {
        BrowserSortMenu(
            sort = browserSort,
            onChange = viewModel::setBrowserSort,
            onDismiss = { showSortMenu = false },
        )
    }
}

@Composable
private fun Header(state: SmbBrowserState) {
    Text(
        text = state.host,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Browsing(
    state: SmbBrowserState,
    favoritePaths: Set<String>,
    query: String,
    searchOpen: Boolean,
    sort: BrowserSort,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onOpenSort: () -> Unit,
    onShareClick: (dev.vayou.core.smb.SmbShare) -> Unit,
    onShareLongPress: (dev.vayou.core.smb.SmbShare) -> Unit,
    onItemClick: (SmbFileItem) -> Unit,
    onLongPress: (SmbFileItem) -> Unit,
    onSegmentClick: (Int) -> Unit,
    onBackToShares: () -> Unit,
) {
    val first = remember(state.shareName, state.path) { FocusRequester() }
    val inFolder = state.shareName != null
    val filteredEntries = remember(state.entries, query, sort) {
        val matched = if (query.isBlank()) state.entries
            else state.entries.filter { it.name.contains(query, ignoreCase = true) }
        val axisComparator: Comparator<SmbFileItem> = when (sort.by) {
            BrowserSortBy.NAME -> compareBy { it.name.lowercase() }
            BrowserSortBy.SIZE -> compareBy { it.size }
        }
        matched.sortedWith(if (sort.asc) axisComparator else axisComparator.reversed())
    }
    val filteredShares = remember(state.shares, query) {
        if (query.isBlank()) state.shares
        else state.shares.filter { it.name.contains(query, ignoreCase = true) }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(GridColumns),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
        contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            top = 24.dp,
            bottom = 24.dp,
        ),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                BrowsingHeader(
                    state = state,
                    query = query,
                    searchOpen = searchOpen,
                    showSort = inFolder,
                    onQueryChange = onQueryChange,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchSubmit = { runCatching { first.requestFocus() } },
                    onOpenSort = onOpenSort,
                    onSegmentClick = onSegmentClick,
                    onBackToShares = onBackToShares,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(2.dp),
                ) {
                    if (state.isLoading) IndeterminateBar(modifier = Modifier.fillMaxSize())
                }
                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        when {
            !inFolder && filteredShares.isNotEmpty() -> {
                items(filteredShares, key = { it.name }) { share ->
                    val isFirst = filteredShares.firstOrNull() === share
                    val favoriteKey = "${share.name}|"
                    GridCard(
                        title = share.name,
                        icon = Icons.Filled.Folder,
                        accent = true,
                        showFavorite = favoriteKey in favoritePaths,
                        onClick = { onShareClick(share) },
                        onMenu = { onShareLongPress(share) },
                        modifier = if (isFirst) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }
            !inFolder && state.shares.isNotEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyResults(query = query)
            }
            inFolder && state.entries.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Pasta vazia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            inFolder && filteredEntries.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyResults(query = query)
            }
            inFolder -> {
                val share = state.shareName!!
                items(filteredEntries, key = { it.path }) { entry ->
                    val isFirst = filteredEntries.firstOrNull() === entry
                    val favoriteKey = "$share|${entry.path}"
                    GridCard(
                        title = entry.name,
                        icon = if (entry.isDirectory) Icons.Filled.Folder else Icons.Outlined.PlayCircleOutline,
                        accent = entry.isDirectory,
                        showFavorite = entry.isDirectory && favoriteKey in favoritePaths,
                        onClick = { onItemClick(entry) },
                        onMenu = if (entry.isDirectory) ({ onLongPress(entry) }) else null,
                        modifier = if (isFirst) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }
        }
    }
    LaunchedEffect(state.shareName, state.path, filteredShares.size, filteredEntries.size, searchOpen) {
        if (!searchOpen) runCatching { first.requestFocus() }
    }
}

@Composable
private fun EmptyResults(query: String) {
    Text(
        text = if (query.isBlank()) "Nada por aqui" else "Nenhum resultado para \"$query\"",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BrowsingHeader(
    state: SmbBrowserState,
    query: String,
    searchOpen: Boolean,
    showSort: Boolean,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenSort: () -> Unit,
    onSegmentClick: (Int) -> Unit,
    onBackToShares: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (searchOpen) {
                VayouSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "Buscar",
                    autoFocus = true,
                    onSearch = onSearchSubmit,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (state.shareName != null) {
                BreadcrumbBar(
                    host = state.host,
                    share = state.shareName,
                    path = state.path,
                    onHostClick = onBackToShares,
                    onSegmentClick = onSegmentClick,
                )
            } else {
                Header(state = state)
            }
        }
        if (showSort) {
            VayouTvButton(
                onClick = onOpenSort,
                icon = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = "Ordenar",
            )
        }
        VayouTvButton(
            onClick = if (searchOpen) onCloseSearch else onOpenSearch,
            icon = if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
            contentDescription = if (searchOpen) "Fechar busca" else "Buscar",
        )
    }
}

@Composable
private fun IndeterminateBar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "smb-loading")
    val offset by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "smb-loading-offset",
    )
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        drawRect(color = color.copy(alpha = 0.15f))
        val barW = size.width * 0.4f
        drawRect(
            color = color,
            topLeft = Offset(offset * size.width, 0f),
            size = Size(barW, size.height),
        )
    }
}

@Composable
private fun BreadcrumbBar(
    host: String,
    share: String,
    path: String,
    onHostClick: () -> Unit,
    onSegmentClick: (Int) -> Unit,
) {
    val segments = remember(path) { path.split('\\').filter { it.isNotBlank() } }
    val scrollState = rememberScrollState()
    LaunchedEffect(path, share) { scrollState.animateScrollTo(scrollState.maxValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BreadcrumbSegment(text = host, isCurrent = false, onClick = onHostClick)
        BreadcrumbSeparator()
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
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = !isCurrent,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.small),
        scale = ClickableSurfaceDefaults.scale(focusedScale = TvDimensions.FocusScale),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = cs.surfaceVariant,
            disabledContainerColor = Color.Transparent,
            contentColor = if (isCurrent) cs.onBackground else cs.primary,
            focusedContentColor = cs.onBackground,
            disabledContentColor = cs.onBackground,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun BreadcrumbSeparator() {
    Text(
        text = "›",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun AuthForm(
    error: String?,
    onSubmit: (displayName: String, username: String, password: String, domain: String) -> Unit,
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Column(
        modifier = Modifier.width(480.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Autenticação",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        TextLine("Nome (opcional)", displayName, onChange = { displayName = it }, modifier = Modifier.focusRequester(firstFocus))
        TextLine("Usuário", username, onChange = { username = it })
        TextLine("Senha", password, onChange = { password = it }, visual = PasswordVisualTransformation())
        TextLine("Domínio (opcional)", domain, onChange = { domain = it })
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = { onSubmit(displayName, username, password, domain) },
            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.extraLarge),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = "Conectar",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun TextLine(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    visual: VisualTransformation = VisualTransformation.None,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            visualTransformation = visual,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions.Default,
            modifier = modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}
