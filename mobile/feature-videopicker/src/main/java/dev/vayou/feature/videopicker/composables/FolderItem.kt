package dev.vayou.feature.videopicker.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import dev.vayou.core.common.Utils
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Folder
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouSegmentedListItem
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenu
import dev.vayou.core.ui.designsystem.components.VayouDropdownMenuItem
import dev.vayou.core.ui.theme.VayouPlayerTheme

@Composable
fun FolderItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
) {
    when (preferences.mediaLayoutMode) {
        MediaLayoutMode.LIST -> FolderListItem(
            folder = folder,
            isRecentlyPlayedFolder = isRecentlyPlayedFolder,
            preferences = preferences,
            modifier = modifier,
            selected = selected,
            inSelectionMode = inSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            onPlayClick = onPlayClick,
            onShareClick = onShareClick,
            onDeleteClick = onDeleteClick,
        )
        MediaLayoutMode.GRID -> FolderGridItem(
            folder = folder,
            isRecentlyPlayedFolder = isRecentlyPlayedFolder,
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
private fun FolderListItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onPlayClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
) {
    val hasActions = onPlayClick != null || onShareClick != null || onDeleteClick != null
    val isRecentAndMarked = isRecentlyPlayedFolder && preferences.markLastPlayedMedia
    VayouSegmentedListItem(
        modifier = modifier,
        selected = selected,
        containerColor = Color.Transparent,
        rippleColor = VayouTheme.colors.surfaceContainerHigh,
        contentPadding = MediaListLayoutDefaults.ListItemPadding,
        contentColorOverride = if (isRecentAndMarked) VayouTheme.colors.accent else null,
        supportingColorOverride = if (isRecentAndMarked) VayouTheme.colors.folderColor else null,
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
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.folder_thumb),
                contentDescription = "",
                tint = VayouTheme.colors.folderColor,
                modifier = Modifier
                    .width(min(72.dp, LocalConfiguration.current.screenWidthDp.dp * 0.22f))
                    .aspectRatio(20 / 17f),
            )
        },
        content = {
            Text(
                text = folder.name,
                maxLines = 2,
                style = VayouTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            val videoLabel = stringResource(id = R.string.video.takeIf { folder.mediaList.size == 1 } ?: R.string.videos)
            val folderLabel = stringResource(id = R.string.folder.takeIf { folder.folderList.size == 1 } ?: R.string.folders)
            val infoText = buildList {
                if (folder.mediaList.isNotEmpty()) add("${folder.mediaList.size} $videoLabel")
                if (folder.folderList.isNotEmpty()) add("${folder.folderList.size} $folderLabel")
                add(Utils.formatFileSize(folder.mediaSize))
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
private fun FolderGridItem(
    folder: Folder,
    isRecentlyPlayedFolder: Boolean,
    preferences: ApplicationPreferences,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val isRecentAndMarked = isRecentlyPlayedFolder && preferences.markLastPlayedMedia
    VayouSegmentedListItem(
        modifier = modifier.width(IntrinsicSize.Min),
        selected = selected,
        containerColor = Color.Transparent,
        contentPadding = MediaListLayoutDefaults.GridItemPadding,
        contentColorOverride = if (isRecentAndMarked) VayouTheme.colors.accent else null,
        supportingColorOverride = if (isRecentAndMarked) VayouTheme.colors.folderColor else null,
        onClick = onClick,
        onLongClick = onLongClick,
        content = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.folder_thumb),
                    contentDescription = "",
                    tint = VayouTheme.colors.folderColor,
                    modifier = Modifier
                        .width(min(72.dp, LocalConfiguration.current.screenWidthDp.dp * 0.22f))
                        .aspectRatio(20 / 17f),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = folder.name,
                        maxLines = 1,
                        style = VayouTheme.typography.titleSmall,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    val mediaCount = if (folder.mediaList.isNotEmpty()) {
                        "${folder.mediaList.size} " + stringResource(id = R.string.video.takeIf { folder.mediaList.size == 1 } ?: R.string.videos)
                    } else {
                        null
                    }
                    val folderCount = if (folder.folderList.isNotEmpty()) {
                        "${folder.folderList.size} " + stringResource(id = R.string.folder.takeIf { folder.folderList.size == 1 } ?: R.string.folders)
                    } else {
                        null
                    }
                    Text(
                        text = buildString {
                            mediaCount?.let {
                                append(it)
                                folderCount?.let {
                                    append(", ")
                                    append("\u00A0")
                                }
                            }
                            folderCount?.let {
                                append(it)
                            }
                        },
                        maxLines = 2,
                        style = VayouTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
fun FolderItemRecentlyPlayedPreview() {
    VayouPlayerTheme {
        FolderListItem(
            folder = Folder.sample,
            preferences = ApplicationPreferences(),
            isRecentlyPlayedFolder = true,
        )
    }
}

@PreviewLightDark
@Composable
fun FolderItemPreview() {
    VayouPlayerTheme {
        FolderListItem(
            folder = Folder.sample.copy(folderList = listOf(Folder.sample)),
            preferences = ApplicationPreferences(),
            isRecentlyPlayedFolder = false,
        )
    }
}

@PreviewLightDark
@Composable
fun FolderGridViewPreview() {
    VayouPlayerTheme {
        FolderGridItem(
            folder = Folder.sample.copy(folderList = listOf(Folder.sample)),
            preferences = ApplicationPreferences(),
            isRecentlyPlayedFolder = true,
        )
    }
}
