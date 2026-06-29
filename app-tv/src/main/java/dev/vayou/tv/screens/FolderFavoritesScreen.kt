package dev.vayou.tv.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.FolderFavoritesStore
import dev.vayou.tv.ui.ContextAction
import dev.vayou.tv.ui.GridCard
import dev.vayou.tv.ui.ItemContextMenu
import dev.vayou.tv.ui.TvDimensions
import dev.vayou.tv.ui.RenameDialog
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val ScreenPadding = TvDimensions.ScreenPadding
private val GridSpacing = TvDimensions.GridSpacing
private const val GridColumns = 4

@HiltViewModel
class FolderFavoritesViewModel @Inject constructor(
    private val store: FolderFavoritesStore,
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteFolder>> = store.favoritesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun remove(favorite: FavoriteFolder) {
        viewModelScope.launch { store.remove(favorite.host, favorite.share, favorite.path) }
    }

    fun rename(favorite: FavoriteFolder, newName: String) {
        viewModelScope.launch { store.rename(favorite.host, favorite.share, favorite.path, newName) }
    }
}

@Composable
fun FolderFavoritesScreen(
    onOpen: (FavoriteFolder) -> Unit,
    onBack: () -> Unit,
    viewModel: FolderFavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf<FavoriteFolder?>(null) }
    var renaming by remember { mutableStateOf<FavoriteFolder?>(null) }
    BackHandler(onBack = onBack)

    val firstFocus = remember { FocusRequester() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(GridColumns),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 24.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Pastas favoritas",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (favorites.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Nenhuma pasta favorita ainda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(favorites, key = { "${it.host}|${it.share}|${it.path}" }) { fav ->
                val isFirst = favorites.firstOrNull() === fav
                GridCard(
                    title = fav.displayName,
                    icon = Icons.Filled.Folder,
                    accent = true,
                    showFavorite = true,
                    onClick = { onOpen(fav) },
                    onMenu = { menu = fav },
                    modifier = if (isFirst) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
    }
    LaunchedEffect(favorites.size) {
        if (favorites.isNotEmpty()) runCatching { firstFocus.requestFocus() }
    }

    menu?.let { fav ->
        ItemContextMenu(
            title = fav.displayName,
            actions = listOf(
                ContextAction(
                    label = "Remover dos favoritos",
                    icon = Icons.Outlined.Delete,
                    onClick = { viewModel.remove(fav) },
                ),
                ContextAction(
                    label = "Renomear",
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    onClick = { renaming = fav },
                ),
            ),
            onDismiss = { menu = null },
        )
    }

    renaming?.let { fav ->
        RenameDialog(
            title = "Renomear pasta",
            initialValue = fav.displayName,
            onConfirm = {
                viewModel.rename(fav, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
}
