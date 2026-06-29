package dev.vayou.feature.videopicker.composables

import android.net.Uri
import dev.vayou.core.model.Video
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Folder
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.extensions.plus
import dev.vayou.feature.videopicker.state.SelectionManager
import dev.vayou.feature.videopicker.state.rememberSelectionManager
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun MediaView(
    rootFolder: Folder,
    preferences: ApplicationPreferences,
    contentPadding: PaddingValues = PaddingValues(),
    selectionManager: SelectionManager = rememberSelectionManager(),
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onFolderClick: (String) -> Unit,
    onVideoClick: (Uri) -> Unit,
    onVideoLoaded: (Uri) -> Unit,
    onFolderPlayClick: ((Folder) -> Unit)? = null,
    onFolderShareClick: ((Folder) -> Unit)? = null,
    onFolderDeleteClick: ((Folder) -> Unit)? = null,
    onVideoPlayClick: ((Video) -> Unit)? = null,
    onVideoRenameClick: ((Video) -> Unit)? = null,
    onVideoInfoClick: ((Video) -> Unit)? = null,
    onVideoShareClick: ((Video) -> Unit)? = null,
    onVideoDeleteClick: ((Video) -> Unit)? = null,
    onVideoAddToPlaylistClick: ((Video) -> Unit)? = null,
    onVideoAddToFavoritesClick: ((Video) -> Unit)? = null,
    onVideoRemoveFromFavoritesClick: ((Video) -> Unit)? = null,
    onVideoMoveToPrivateClick: ((Video) -> Unit)? = null,
    favoriteUris: Set<String> = emptySet(),
    privateUris: Set<String> = emptySet(),
    header: (@Composable () -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current

    val folderMinWidth = 120.dp
    val videoMinWidth = 130.dp
    BoxWithConstraints {
        val contentHorizontalPadding = MediaListLayoutDefaults.outerHorizontal(preferences.mediaLayoutMode)
        val itemSpacing = MediaListLayoutDefaults.itemSpacing(preferences.mediaLayoutMode)
        val maxWidth = this.maxWidth - (contentHorizontalPadding * 2) - itemSpacing
        val maxFolders = (maxWidth / folderMinWidth).toInt()
        val maxVideos = (maxWidth / videoMinWidth).toInt()
        val spans = when (preferences.mediaLayoutMode) {
            MediaLayoutMode.LIST -> 1
            MediaLayoutMode.GRID -> lcm(maxFolders, maxVideos)
        }

        val singleFolderSpan = when (preferences.mediaLayoutMode) {
            MediaLayoutMode.LIST -> 1
            MediaLayoutMode.GRID -> spans / maxFolders
        }
        val singleVideoSpan = when (preferences.mediaLayoutMode) {
            MediaLayoutMode.LIST -> 1
            MediaLayoutMode.GRID -> spans / maxVideos
        }

        val gridPadding = contentPadding + PaddingValues(horizontal = contentHorizontalPadding, vertical = 8.dp)
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            state = lazyGridState,
            columns = GridCells.Fixed(spans),
            contentPadding = gridPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            if (header != null) {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    header()
                }
            }
            itemsIndexed(
                items = rootFolder.folderList,
                key = { _, folder -> folder.path },
                span = { _, _ -> GridItemSpan(singleFolderSpan) },
            ) { _, folder ->
                val selected by remember { derivedStateOf { selectionManager.isFolderSelected(folder) } }
                FolderItem(
                    folder = folder,
                    isRecentlyPlayedFolder = rootFolder.isRecentlyPlayedVideo(folder.recentlyPlayedVideo),
                    preferences = preferences,
                    selected = selected,
                    inSelectionMode = selectionManager.isInSelectionMode,
                    onClick = {
                        if (selectionManager.isInSelectionMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            selectionManager.toggleFolderSelection(folder)
                        } else {
                            onFolderClick(folder.path)
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectionManager.toggleFolderSelection(folder)
                    },
                    onPlayClick = onFolderPlayClick?.let { { it(folder) } },
                    onShareClick = onFolderShareClick?.let { { it(folder) } },
                    onDeleteClick = onFolderDeleteClick?.let { { it(folder) } },
                )
            }

            itemsIndexed(
                items = rootFolder.mediaList,
                key = { _, video -> video.uriString },
                span = { _, _ -> GridItemSpan(singleVideoSpan) },
            ) { _, video ->
                val selected by remember { derivedStateOf { selectionManager.isVideoSelected(video) } }
                VideoItem(
                    video = video,
                    preferences = preferences,
                    isRecentlyPlayedVideo = rootFolder.isRecentlyPlayedVideo(video),
                    selected = selected,
                    inSelectionMode = selectionManager.isInSelectionMode,
                    onClick = {
                        if (selectionManager.isInSelectionMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            selectionManager.toggleVideoSelection(video)
                        } else {
                            onVideoClick(video.uriString.toUri())
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectionManager.toggleVideoSelection(video)
                    },
                    onPlayClick = onVideoPlayClick?.let { { it(video) } },
                    onRenameClick = onVideoRenameClick?.let { { it(video) } },
                    onInfoClick = onVideoInfoClick?.let { { it(video) } },
                    onShareClick = onVideoShareClick?.let { { it(video) } },
                    onDeleteClick = onVideoDeleteClick?.let { { it(video) } },
                    onAddToPlaylistClick = onVideoAddToPlaylistClick?.let { { it(video) } },
                    onAddToFavoritesClick = if (video.uriString !in favoriteUris) onVideoAddToFavoritesClick?.let { { it(video) } } else null,
                    onRemoveFromFavoritesClick = if (video.uriString in favoriteUris) onVideoRemoveFromFavoritesClick?.let { { it(video) } } else null,
                    onMoveToPrivateClick = if (video.uriString !in privateUris) onVideoMoveToPrivateClick?.let { { it(video) } } else null,
                    modifier = Modifier.onFirstVisible { onVideoLoaded(video.uriString.toUri()) },
                )
            }
        }
    }
}

fun lcm(a: Int, b: Int): Int {
    return abs(a * b) / gcd(a, b)
}

fun gcd(a: Int, b: Int): Int {
    return if (b == 0) a else gcd(b, a % b)
}
