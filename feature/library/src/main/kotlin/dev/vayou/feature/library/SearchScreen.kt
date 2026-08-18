package dev.vayou.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.ui.designsystem.MediaListLayoutDefaults
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.ListSectionTitle
import dev.vayou.core.ui.designsystem.components.VayouBackButton
import dev.vayou.core.ui.designsystem.components.VayouEmptyState
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.core.ui.designsystem.components.VayouSearchField
import dev.vayou.core.ui.designsystem.components.VayouSegmentedListItem
import dev.vayou.core.ui.designsystem.components.VayouTextButton
import dev.vayou.core.ui.designsystem.components.VayouTopAppBar
import dev.vayou.core.ui.theme.VayouTheme

/**
 * Finding a film by name.
 *
 * Its own screen rather than a field that appears over the library: searching replaces what is on
 * screen entirely -- the pills, the order, the folders -- and a screen that keeps them while showing
 * none of them is a screen lying about where you are.
 *
 * With nothing typed it shows what was searched for before, which is the shortcut for the handful of
 * things anyone actually looks for twice.
 */
@Composable
internal fun SearchScreen(
    onPlayVideo: (uri: String, title: String) -> Unit,
    onClose: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Straight into the field: nobody opens search to look at an empty box.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BackHandler(onBack = onClose)

    VayouScaffold(
        topBar = {
            VayouTopAppBar(
                title = {
                    VayouSearchField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = stringResource(R.string.search_placeholder),
                        focusRequester = focusRequester,
                        onSearch = {
                            viewModel.rememberQuery()
                            keyboard?.hide()
                        },
                        trailing = {
                            if (query.isNotEmpty()) {
                                VayouIconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(VayouIcons.Close, stringResource(R.string.clear_query))
                                }
                            }
                        },
                    )
                },
                navigationIcon = { VayouBackButton(onClick = onClose) },
            )
        },
    ) {
        when {
            query.isBlank() -> SearchHistory(
                queries = history,
                onRecall = viewModel::recall,
                onForget = viewModel::forget,
                onClearAll = viewModel::clearHistory,
            )

            results.isEmpty -> VayouEmptyState(
                icon = VayouIcons.Search,
                title = stringResource(R.string.nothing_found, query),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MediaListLayoutDefaults.ItemSpacing),
            ) {
                // Folders first and named as such. A folder answers "where is it" and a film answers
                // "which one"; run together they read as one list of things that are not alike.
                if (results.folders.isNotEmpty()) {
                    item { ListSectionTitle(text = stringResource(R.string.folders)) }
                    items(results.folders, key = { it.path }) { folder ->
                        VayouSegmentedListItem(
                            contentPadding = MediaListLayoutDefaults.ListItemPadding,
                            leadingContent = { FolderGraphic() },
                            content = { OneLine(folder.name) },
                            supportingContent = { SupportingLine(folder.path) },
                        )
                    }
                }
                if (results.videos.isNotEmpty()) {
                    item { ListSectionTitle(text = stringResource(R.string.videos)) }
                    items(results.videos, key = { it.uriString }) { video ->
                        VideoRow(
                            video = video,
                            onClick = {
                                // Opening a result is what makes it a search worth remembering.
                                viewModel.rememberQuery()
                                onPlayVideo(video.uriString, video.displayName)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistory(
    queries: List<String>,
    onRecall: (String) -> Unit,
    onForget: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    if (queries.isEmpty()) {
        VayouEmptyState(icon = VayouIcons.Search, title = stringResource(R.string.search_placeholder))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListSectionTitle(text = stringResource(R.string.recent_searches))
                VayouTextButton(onClick = onClearAll, modifier = Modifier.padding(end = VayouTheme.spacing.sm)) {
                    Text(text = stringResource(R.string.clear_history))
                }
            }
        }
        items(queries, key = { it }) { query ->
            VayouSegmentedListItem(
                contentPadding = MediaListLayoutDefaults.ListItemPadding,
                onClick = { onRecall(query) },
                leadingContent = { Icon(VayouIcons.History, contentDescription = null) },
                content = { OneLine(query) },
                trailingContent = {
                    VayouIconButton(onClick = { onForget(query) }) {
                        Icon(VayouIcons.Close, stringResource(R.string.forget_search))
                    }
                },
            )
        }
    }
}
