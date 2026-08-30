package dev.vayou.feature.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.BrowserSortBy
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.smb.sortedBy
import dev.vayou.core.ui.asFileSize
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouEmptyState
import dev.vayou.core.ui.designsystem.components.VayouListHeader
import dev.vayou.core.ui.designsystem.components.VayouWaiting
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Inside a share.
 *
 * The order is a header rather than an icon in the bar, the same control the local library uses, so
 * the current order is readable without opening anything.
 */
@Composable
internal fun FileBrowser(
    share: String,
    path: String,
    isLoading: Boolean,
    error: NetworkError?,
    files: List<SmbFileItem>,
    searchQuery: String,
    sort: BrowserSort,
    favouritedPaths: Set<String>,
    onOpenAncestor: (Int) -> Unit,
    onOpenSort: () -> Unit,
    onOpenDirectory: (SmbFileItem) -> Unit,
    onToggleFolderFavourite: (SmbFileItem) -> Unit,
    onPlayVideo: (SmbFileItem) -> Unit,
    onPlayAudio: (SmbFileItem) -> Unit,
    onPlayFromStart: (SmbFileItem) -> Unit,
    onShowDetails: (SmbFileItem) -> Unit,
) {
    when {
        isLoading -> {
            VayouWaiting()
            return
        }
        error != null -> {
            ErrorState(error)
            return
        }
        files.isEmpty() -> {
            VayouEmptyState(VayouIcons.FolderOff, stringResource(R.string.no_files_found))
            return
        }
    }

    val shown = remember(files, searchQuery, sort) { files.matching(searchQuery).sortedBy(sort) }
    if (shown.isEmpty()) {
        VayouEmptyState(VayouIcons.Search, stringResource(R.string.no_results_found))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
    ) {
        if (share.isNotEmpty()) {
            item { Breadcrumb(share = share, path = path, onSegmentClick = onOpenAncestor) }
        }
        item {
            VayouListHeader(
                label = stringResource(sort.by.label),
                isAscending = sort.isAscending,
                onClick = onOpenSort,
            )
        }
        items(shown, key = { it.path }) { file ->
            NetworkRow(
                icon = {
                    if (file.isDirectory) {
                        NetworkFolderGraphic()
                    } else {
                        NetworkTile(
                            icon = file.kindIcon(),
                            // A file, so neutral; what cannot be opened is quieter still, rather
                            // than looking playable and doing nothing when tapped.
                            tint = VayouTheme.colors.onSurfaceVariant
                                .copy(alpha = if (file.isPlayable) 1f else UnplayableAlpha),
                        )
                    }
                },
                title = file.name,
                subtitle = if (!file.isDirectory && file.size > 0) file.size.asFileSize() else null,
                // A share holds both kinds and each has a player of its own: a track opened in the
                // video player is a black rectangle with a seek bar under it.
                onClick = {
                    when {
                        file.isDirectory -> onOpenDirectory(file)
                        file.isVideo -> onPlayVideo(file)
                        file.isAudio -> onPlayAudio(file)
                    }
                },
                trailingContent = when {
                    file.isDirectory -> {
                        {
                            FolderFavouriteMenu(
                                name = file.name,
                                isFavourite = file.path in favouritedPaths,
                                onToggle = { onToggleFolderFavourite(file) },
                            )
                        }
                    }

                    file.isVideo -> {
                        {
                            VideoActionsMenu(
                                name = file.name,
                                subtitle = if (file.size > 0) file.size.asFileSize() else null,
                                onPlayFromStart = { onPlayFromStart(file) },
                                onShowDetails = { onShowDetails(file) },
                            )
                        }
                    }

                    else -> null
                },
            )
        }
    }
}

/**
 * The trail back up.
 *
 * It scrolls to its end whenever the path changes, because the segment that matters on arrival is
 * the one just entered, and a long path pushes it off the right edge.
 */
@Composable
private fun Breadcrumb(share: String, path: String, onSegmentClick: (Int) -> Unit) {
    val segments = remember(path) { path.pathSegments }
    val scrollState = rememberScrollState()
    LaunchedEffect(path) { scrollState.animateScrollTo(scrollState.maxValue) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = VayouTheme.spacing.lg, vertical = VayouTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Segment(text = share, isCurrent = segments.isEmpty()) { onSegmentClick(ShareRoot) }
        segments.forEachIndexed { index, segment ->
            Text(
                text = "›",
                style = VayouTheme.typography.labelMedium,
                color = VayouTheme.colors.onSurfaceVariant.copy(alpha = SeparatorAlpha),
                modifier = Modifier.padding(horizontal = SeparatorGap),
            )
            Segment(text = segment, isCurrent = index == segments.lastIndex) { onSegmentClick(index) }
        }
    }
}

@Composable
private fun Segment(text: String, isCurrent: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = VayouTheme.typography.labelMedium,
        // The segment you are in gets the colour. The ones behind it are still links, so they stay
        // quiet -- a trail where every step but the current one is highlighted reads as "you are not
        // here", which is the opposite of what a breadcrumb is for.
        color = if (isCurrent) VayouTheme.colors.onSurface else VayouTheme.colors.onSurfaceVariant,
        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = Modifier
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(SegmentPadding),
    )
}

private fun List<SmbFileItem>.matching(query: String): List<SmbFileItem> =
    if (query.isBlank()) this else filter { it.name.contains(query, ignoreCase = true) }

internal val BrowserSortBy.label: Int
    get() = when (this) {
        BrowserSortBy.Name -> R.string.sort_by_name
        BrowserSortBy.Size -> R.string.sort_by_size
        BrowserSortBy.Type -> R.string.sort_by_type
    }

/** What [dev.vayou.feature.network.NetworkViewModel.openAncestor] reads as "the share itself". */
private const val ShareRoot = -1

private const val UnplayableAlpha = 0.45f

private const val SeparatorAlpha = 0.5f

private val SeparatorGap = 2.dp

private val SegmentPadding = 4.dp
