package dev.vayou.tv.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.FavoritesStore
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.tv.ui.ChannelCard
import dev.vayou.tv.ui.ContextAction
import dev.vayou.tv.ui.ItemContextMenu
import dev.vayou.tv.ui.TvDimensions
import dev.vayou.tv.ui.RenameDialog
import dev.vayou.tv.ui.VayouSearchField
import dev.vayou.tv.ui.VayouTvButton
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val ScreenPadding = TvDimensions.ScreenPadding
private val GridSpacing = TvDimensions.GridSpacing
private const val GridColumns = 3

@HiltViewModel
class IptvFavoritesViewModel @Inject constructor(
    private val favoritesStore: FavoritesStore,
) : ViewModel() {
    val favorites: StateFlow<List<PlaylistChannel>> = favoritesStore.favoritesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun toggle(channel: PlaylistChannel) {
        viewModelScope.launch { favoritesStore.toggle(channel) }
    }

    fun rename(url: String, newName: String) {
        viewModelScope.launch { favoritesStore.rename(url, newName) }
    }
}

@Composable
fun IptvFavoritesScreen(
    onPlay: (PlaylistChannel) -> Unit,
    onBack: () -> Unit,
    viewModel: IptvFavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var menuChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    var renameChannel by remember { mutableStateOf<PlaylistChannel?>(null) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }

    BackHandler {
        if (searchOpen) {
            searchOpen = false
            query = ""
        } else {
            onBack()
        }
    }

    val filtered = remember(favorites, query) {
        if (query.isBlank()) favorites
        else favorites.filter { it.name.contains(query, ignoreCase = true) }
    }

    val firstFocus = remember { FocusRequester() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(GridColumns),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
            FavoritesHeader(
                searchOpen = searchOpen,
                query = query,
                onQueryChange = { query = it },
                onOpenSearch = { searchOpen = true },
                onSearchSubmit = { runCatching { firstFocus.requestFocus() } },
            )
        }
        when {
            favorites.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Nenhum canal favorito ainda.",
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
                    isFavorite = true,
                    onClick = { onPlay(channel) },
                    onLongPress = { menuChannel = channel },
                    modifier = if (isFirst) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
    }
    LaunchedEffect(filtered.size, searchOpen) {
        if (filtered.isNotEmpty() && !searchOpen) runCatching { firstFocus.requestFocus() }
    }

    menuChannel?.let { channel ->
        ItemContextMenu(
            title = channel.name,
            actions = listOf(
                ContextAction(
                    label = "Remover dos favoritos",
                    icon = Icons.Outlined.Delete,
                    onClick = { viewModel.toggle(channel) },
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
                viewModel.rename(channel.url, it)
                renameChannel = null
            },
            onDismiss = { renameChannel = null },
        )
    }
}

@Composable
private fun FavoritesHeader(
    searchOpen: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onSearchSubmit: () -> Unit,
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
                placeholder = "Buscar favorito",
                autoFocus = true,
                onSearch = onSearchSubmit,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = "Favoritos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            VayouTvButton(
                onClick = onOpenSearch,
                icon = Icons.Outlined.Search,
                contentDescription = "Buscar favorito",
            )
        }
    }
}
