package dev.vayou.tv.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.vayou.core.smb.IptvCountries
import dev.vayou.core.smb.IptvCountry
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.tv.ui.ChannelCard
import dev.vayou.tv.ui.ChannelCardHeight
import dev.vayou.tv.ui.ContextAction
import dev.vayou.tv.ui.FilterPanel
import dev.vayou.tv.ui.ItemContextMenu
import dev.vayou.tv.ui.RenameDialog
import dev.vayou.tv.ui.TvDimensions
import dev.vayou.tv.ui.VayouSearchField
import dev.vayou.tv.ui.VayouTvButton

private val ScreenPadding = TvDimensions.ScreenPadding
private val GridSpacing = TvDimensions.GridSpacing
private const val GridColumns = 3

@Composable
fun PlaylistDetailScreen(
    onPlay: (PlaylistChannel) -> Unit,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favoriteUrls by viewModel.favoriteUrls.collectAsStateWithLifecycle()
    var menuChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    var renameChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    val isIptvOrgCountry = remember(state.url) {
        state.url.startsWith(IptvCountry.COUNTRY_PREFIX) || state.url == IptvCountry.GLOBAL_URL
    }
    val currentCountry = remember(state.url, isIptvOrgCountry) {
        if (!isIptvOrgCountry) return@remember null
        val code = if (state.url == IptvCountry.GLOBAL_URL) null
                   else state.url.removePrefix(IptvCountry.COUNTRY_PREFIX).removeSuffix(".m3u").takeIf { it.length == 2 }
        IptvCountries.firstOrNull { it.code == code }
    }
    val countryOptions = remember { IptvCountries.filter { it.code != null }.map { it.name } }
    val groups = remember(state.channels) {
        state.channels.asSequence()
            .mapNotNull { it.group?.takeIf { g -> g.isNotBlank() } }
            .distinct()
            .sorted()
            .toList()
    }
    LaunchedEffect(state.url) { selectedGroup = null }

    val filtered = remember(state.channels, query, selectedGroup) {
        state.channels.asSequence()
            .filter { selectedGroup == null || it.group == selectedGroup }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .toList()
    }

    var searchOpen by remember { mutableStateOf(false) }
    BackHandler {
        when {
            showFilterPanel -> showFilterPanel = false
            searchOpen -> {
                searchOpen = false
                query = ""
            }
            else -> onBack()
        }
    }

    val firstCardFocus = remember { FocusRequester() }
    val compact = showFilterPanel
    val gridWeight by animateFloatAsState(
        targetValue = if (compact) 0.62f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "grid-weight",
    )
    val panelWeight by animateFloatAsState(
        targetValue = if (compact) 0.38f else 0.0001f,
        animationSpec = tween(durationMillis = 250),
        label = "panel-weight",
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val activeColumns = if (compact) 2 else GridColumns
        LazyVerticalGrid(
            columns = GridCells.Fixed(activeColumns),
            modifier = Modifier
                .fillMaxHeight()
                .weight(gridWeight),
            horizontalArrangement = Arrangement.spacedBy(GridSpacing),
            verticalArrangement = Arrangement.spacedBy(GridSpacing),
            contentPadding = PaddingValues(
                start = ScreenPadding,
                end = if (compact) 24.dp else ScreenPadding,
                top = 24.dp,
                bottom = 24.dp,
            ),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                PlaylistHeader(
                    titleName = state.name,
                    searchOpen = searchOpen,
                    query = query,
                    onQueryChange = { query = it },
                    onOpenSearch = { searchOpen = true },
                    onSearchSubmit = { runCatching { firstCardFocus.requestFocus() } },
                    isIptvOrgCountry = isIptvOrgCountry,
                    currentCountryName = currentCountry?.name,
                    hasGroups = groups.isNotEmpty(),
                    selectedGroup = selectedGroup,
                    onOpenFilter = { showFilterPanel = true },
                )
            }
            when {
                state.isLoading -> items(activeColumns * 3) { SkeletonBox() }
                state.error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = state.error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                state.channels.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Nenhum canal encontrado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                filtered.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Nenhum canal corresponde à busca",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> items(filtered, key = { it.url }) { channel ->
                    val isFirst = filtered.firstOrNull() === channel
                    ChannelCard(
                        channel = channel,
                        isFavorite = channel.url in favoriteUrls,
                        onClick = { onPlay(channel) },
                        onLongPress = { menuChannel = channel },
                        modifier = if (isFirst) Modifier.focusRequester(firstCardFocus) else Modifier,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(panelWeight)
                .padding(end = if (compact) 24.dp else 0.dp, top = 24.dp, bottom = 24.dp),
        ) {
            if (compact) {
                if (isIptvOrgCountry) {
                    FilterPanel(
                        title = "Escolha o país",
                        options = countryOptions,
                        selected = currentCountry?.name?.takeIf { currentCountry.code != null },
                        allLabel = "Internacional",
                        onSelect = { selectedName ->
                            val code = IptvCountries.firstOrNull { it.name == selectedName }?.code
                            viewModel.switchIptvCountry(code)
                            query = ""
                        },
                        onDismiss = { showFilterPanel = false },
                    )
                } else if (groups.isNotEmpty()) {
                    FilterPanel(
                        title = "Filtrar por grupo",
                        options = groups,
                        selected = selectedGroup,
                        allLabel = "Todos",
                        onSelect = { selectedGroup = it },
                        onDismiss = { showFilterPanel = false },
                    )
                }
            }
        }
    }
    LaunchedEffect(filtered.size, compact, searchOpen) {
        if (filtered.isNotEmpty() && !compact && !searchOpen) {
            runCatching { firstCardFocus.requestFocus() }
        }
    }

    menuChannel?.let { channel ->
        val isFav = channel.url in favoriteUrls
        ItemContextMenu(
            title = channel.name,
            actions = listOf(
                ContextAction(
                    label = if (isFav) "Remover dos favoritos" else "Favoritar",
                    icon = if (isFav) Icons.Outlined.Delete else Icons.Filled.Star,
                    onClick = { viewModel.toggleFavorite(channel) },
                ),
                ContextAction(
                    label = "Renomear",
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    onClick = { renameChannel = channel },
                ),
            ),
            onDismiss = { menuChannel = null },
        )
    }

    renameChannel?.let { channel ->
        RenameDialog(
            title = "Renomear canal",
            initialValue = channel.name,
            onConfirm = {
                viewModel.renameChannel(channel, it)
                renameChannel = null
            },
            onDismiss = { renameChannel = null },
        )
    }

}

@Composable
private fun PlaylistHeader(
    titleName: String,
    searchOpen: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onSearchSubmit: () -> Unit,
    isIptvOrgCountry: Boolean,
    currentCountryName: String?,
    hasGroups: Boolean,
    selectedGroup: String?,
    onOpenFilter: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (searchOpen) {
            VayouSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Buscar canal",
                autoFocus = true,
                onSearch = onSearchSubmit,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = titleName.ifBlank { "TV ao vivo" },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when {
                isIptvOrgCountry -> VayouTvButton(
                    onClick = onOpenFilter,
                    icon = Icons.Outlined.FilterList,
                    label = currentCountryName ?: "País",
                    contentDescription = "Escolher país",
                )
                hasGroups -> VayouTvButton(
                    onClick = onOpenFilter,
                    icon = Icons.Outlined.FilterList,
                    label = selectedGroup ?: "Grupo",
                    contentDescription = "Filtrar por grupo",
                )
            }
            VayouTvButton(
                onClick = onOpenSearch,
                icon = Icons.Outlined.Search,
                contentDescription = "Buscar canal",
            )
        }
    }
}

@Composable
private fun SkeletonBox() {
    val cs = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChannelCardHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(cs.surfaceVariant.copy(alpha = alpha)),
    )
}

