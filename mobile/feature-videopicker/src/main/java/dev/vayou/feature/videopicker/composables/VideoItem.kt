package dev.vayou.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.Video
import dev.vayou.core.ui.components.VayouSegmentedListItem
import dev.vayou.core.ui.components.VayouIconButton
import androidx.compose.ui.res.stringResource
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenu
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenuItem
import dev.vayou.core.ui.theme.VayouPlayerTheme

@Composable
fun VideoItem(
    video: Video,
    isRecentlyPlayedVideo: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onRenameClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
    onAddToFavoritesClick: (() -> Unit)? = null,
    onRemoveFromFavoritesClick: (() -> Unit)? = null,
    onMoveToPrivateClick: (() -> Unit)? = null,
    onRemoveFromPrivateClick: (() -> Unit)? = null,
) {
    when (preferences.mediaLayoutMode) {
        MediaLayoutMode.LIST -> VideoListItem(
            video = video,
            isRecentlyPlayedVideo = isRecentlyPlayedVideo,
            preferences = preferences,
            modifier = modifier,
            selected = selected,
            inSelectionMode = inSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            onPlayClick = onPlayClick,
            onRenameClick = onRenameClick,
            onInfoClick = onInfoClick,
            onShareClick = onShareClick,
            onDeleteClick = onDeleteClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onRemoveFromPlaylistClick = onRemoveFromPlaylistClick,
            onAddToFavoritesClick = onAddToFavoritesClick,
            onRemoveFromFavoritesClick = onRemoveFromFavoritesClick,
            onMoveToPrivateClick = onMoveToPrivateClick,
            onRemoveFromPrivateClick = onRemoveFromPrivateClick,
        )
        MediaLayoutMode.GRID -> VideoGridItem(
            video = video,
            isRecentlyPlayedVideo = isRecentlyPlayedVideo,
            preferences = preferences,
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoListItem(
    video: Video,
    isRecentlyPlayedVideo: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onRenameClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
    onAddToFavoritesClick: (() -> Unit)? = null,
    onRemoveFromFavoritesClick: (() -> Unit)? = null,
    onMoveToPrivateClick: (() -> Unit)? = null,
    onRemoveFromPrivateClick: (() -> Unit)? = null,
) {
    val hasActions = onPlayClick != null || onRenameClick != null || onInfoClick != null || onShareClick != null || onDeleteClick != null || onAddToPlaylistClick != null || onRemoveFromPlaylistClick != null || onAddToFavoritesClick != null || onRemoveFromFavoritesClick != null || onMoveToPrivateClick != null || onRemoveFromPrivateClick != null
    val isRecentAndMarked = isRecentlyPlayedVideo && preferences.markLastPlayedMedia
    VayouSegmentedListItem(
        modifier = modifier,
        selected = selected,
        containerColor = Color.Transparent,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        contentColorOverride = if (isRecentAndMarked) VayouTheme.colors.accent else null,
        supportingColorOverride = if (isRecentAndMarked) VayouTheme.colors.accent else null,
        trailingContent = when {
            inSelectionMode -> selectionTrailingFor(inSelectionMode, selected)
            hasActions -> {
                {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        VayouIconButton(onClick = { expanded = true }, modifier = Modifier.width(36.dp)) {
                            Icon(imageVector = VayouIcons.MoreVert, contentDescription = null)
                        }
                        VayouDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            onPlayClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.play), icon = VayouIcons.Play, onClick = { expanded = false; it() }) }
                            onShareClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.share), icon = VayouIcons.Share, onClick = { expanded = false; it() }) }
                            onAddToPlaylistClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.add_to_playlist), icon = VayouIcons.SaveToPlaylist, onClick = { expanded = false; it() }) }
                            onRemoveFromPlaylistClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.remove_from_playlist), icon = VayouIcons.PlaylistRemove, onClick = { expanded = false; it() }) }
                            onAddToFavoritesClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.add_to_favorites), icon = VayouIcons.StarFilled, onClick = { expanded = false; it() }) }
                            onRemoveFromFavoritesClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.remove_from_favorites), icon = VayouIcons.StarOutlined, onClick = { expanded = false; it() }) }
                            onMoveToPrivateClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.move_to_private), icon = VayouIcons.Lock, onClick = { expanded = false; it() }) }
                            onRemoveFromPrivateClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.remove_from_private), icon = VayouIcons.Lock, onClick = { expanded = false; it() }) }
                            onRenameClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.rename), icon = VayouIcons.Edit, onClick = { expanded = false; it() }) }
                            onInfoClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.info), icon = VayouIcons.Info, onClick = { expanded = false; it() }) }
                            onDeleteClick?.let { VayouDropdownMenuItem(text = stringResource(R.string.delete), icon = VayouIcons.Delete, onClick = { expanded = false; it() }) }
                        }
                    }
                }
            }
            else -> null
        },
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = {
            ThumbnailView(
                video = video,
                modifier = Modifier
                    .width(min(130.dp, LocalConfiguration.current.screenWidthDp.dp * 0.32f)),
            )
        },
        content = {
            Text(
                text = video.displayName,
                maxLines = 2,
                style = VayouTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            val infoText = buildList {
                if (video.height > 0) add("${video.height}p")
                add(video.nameWithExtension.substringAfterLast(".").uppercase())
                add(video.formattedFileSize)
            }.joinToString(" · ")
            if (infoText.isNotEmpty()) {
                Text(
                    text = infoText,
                    style = VayouTheme.typography.bodySmall,
                    color = VayouTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoGridItem(
    video: Video,
    isRecentlyPlayedVideo: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val isRecentAndMarked = isRecentlyPlayedVideo && preferences.markLastPlayedMedia
    VayouSegmentedListItem(
        modifier = modifier.width(IntrinsicSize.Min),
        selected = selected,
        containerColor = Color.Transparent,
        contentPadding = MediaListLayoutDefaults.GridItemPadding,
        contentColorOverride = if (isRecentAndMarked) VayouTheme.colors.accent else null,
        supportingColorOverride = if (isRecentAndMarked) VayouTheme.colors.accent else null,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ThumbnailView(
                    video = video,
                )
                Text(
                    text = video.displayName,
                    maxLines = 1,
                    style = VayouTheme.typography.titleSmall,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (isRecentlyPlayedVideo && preferences.markLastPlayedMedia) VayouTheme.colors.accent else VayouTheme.colors.onSurface,
                )
            }
        },
    )
}

@Composable
private fun ThumbnailView(
    modifier: Modifier = Modifier,
    video: Video,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(VayouTheme.shapes.small)
            .background(VayouTheme.colors.surfaceContainer)
            .aspectRatio(16f / 10f),
    ) {
        Icon(
            imageVector = VayouIcons.Video,
            contentDescription = null,
            tint = VayouTheme.colors.surfaceContainerHighest,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.5f),
        )
        val imageRequest = remember(video.uriString) {
            ImageRequest.Builder(context)
                .data(video.uriString)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        InfoChip(
            text = video.formattedDuration,
            modifier = Modifier
                .padding(5.dp)
                .align(Alignment.BottomEnd),
            backgroundColor = VayouTheme.colors.scrim.copy(alpha = 0.6f),
            contentColor = Color.White,
            shape = VayouTheme.shapes.extraSmall,
        )

        if (video.playedPercentage > 0) {
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VayouTheme.colors.surfaceContainerHigh),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(video.playedPercentage)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(VayouTheme.colors.accent),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun VideoItemRecentlyPlayedPreview() {
    VayouPlayerTheme {
        Surface {
            VideoListItem(
                video = Video.sample,
                preferences = ApplicationPreferences(),
                isRecentlyPlayedVideo = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
fun VideoItemPreview() {
    VayouPlayerTheme {
        Surface {
            VideoListItem(
                video = Video.sample,
                preferences = ApplicationPreferences(),
                isRecentlyPlayedVideo = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
fun VideoGridItemPreview() {
    VayouPlayerTheme {
        VideoGridItem(
            video = Video.sample,
            preferences = ApplicationPreferences(),
            isRecentlyPlayedVideo = true,
        )
    }
}
