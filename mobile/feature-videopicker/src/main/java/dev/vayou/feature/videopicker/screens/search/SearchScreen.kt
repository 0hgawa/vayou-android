package dev.vayou.feature.videopicker.screens.search

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.domain.SearchResults
import dev.vayou.core.domain.asRootFolder
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Video
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.ListSectionTitle
import dev.vayou.core.ui.components.VayouSegmentedListItem
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.extensions.copy
import dev.vayou.core.ui.extensions.plus
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.feature.videopicker.composables.MediaView
import dev.vayou.core.ui.components.VayouIconButton

@Composable
fun SearchRoute(
    viewModel: SearchViewModel = hiltViewModel(),
    onPlayVideo: (uri: Uri) -> Unit,
    onFolderClick: (folderPath: String) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SearchScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onFolderClick = onFolderClick,
        onVideoClick = onPlayVideo,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    uiState: SearchUiState,
    onNavigateUp: () -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onVideoClick: (Uri) -> Unit = {},
    onEvent: (SearchUiEvent) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { onEvent(SearchUiEvent.OnQueryChange(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_videos_and_folders),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = VayouTheme.typography.bodyLarge,
                                color = VayouTheme.colors.onSurface.copy(alpha = 0.5f),
                            )
                        },
                        textStyle = VayouTheme.typography.bodyLarge,
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { onEvent(SearchUiEvent.OnQueryChange("")) }) {
                                    Icon(
                                        imageVector = VayouIcons.Close,
                                        contentDescription = stringResource(R.string.clear_history),
                                    )
                                }
                            } else if (uiState.isSearching) {
                                VayouCircularProgress(
                                    modifier = Modifier.size(VayouTheme.iconSize.md),
                                    strokeWidth = 2.dp,
                                    size = 24.dp,
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                onEvent(SearchUiEvent.OnSearch(uiState.query))
                                keyboardController?.hide()
                            },
                        ),
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                    )
                },
                navigationIcon = {
                    VayouIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = VayouIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = VayouTheme.colors.surface,
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding())
                .padding(start = scaffoldPadding.calculateStartPadding(LocalLayoutDirection.current)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(VayouTheme.colors.background),
            ) {
                val updatedScaffoldPadding = scaffoldPadding.copy(top = 0.dp, start = 0.dp)
                if (uiState.query.isBlank()) {
                    SuggestionsContent(
                        searchHistory = uiState.searchHistory,
                        contentPadding = updatedScaffoldPadding,
                        onHistoryItemClick = { onEvent(SearchUiEvent.OnHistoryItemClick(it)) },
                        onRemoveHistoryItem = { onEvent(SearchUiEvent.OnRemoveHistoryItem(it)) },
                        onClearHistory = { onEvent(SearchUiEvent.OnClearHistory) },
                    )
                } else {
                    SearchResultsContent(
                        searchResults = uiState.searchResults,
                        preferences = uiState.preferences,
                        isSearching = uiState.isSearching,
                        contentPadding = updatedScaffoldPadding,
                        onFolderClick = onFolderClick,
                        onVideoClick = onVideoClick,
                        onVideoLoaded = { onEvent(SearchUiEvent.AddToSync(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionsContent(
    searchHistory: List<String>,
    contentPadding: PaddingValues = PaddingValues(),
    onHistoryItemClick: (String) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp) + contentPadding,
    ) {
        if (searchHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VayouTheme.spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListSectionTitle(
                        text = stringResource(R.string.recent_searches),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(text = stringResource(R.string.clear_history))
                    }
                }
            }

            items(
                items = searchHistory,
                key = { "history_$it" },
            ) { query ->
                SearchHistoryItem(
                    query = query,
                    onClick = { onHistoryItemClick(query) },
                    onRemove = { onRemoveHistoryItem(query) },
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = VayouIcons.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = VayouTheme.colors.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.search_videos_and_folders),
                            style = VayouTheme.typography.bodyLarge,
                            color = VayouTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchHistoryItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    VayouSegmentedListItem(
        modifier = Modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        onClick = onClick,
        leadingContent = {
            Icon(
                imageVector = VayouIcons.History,
                contentDescription = null,
                tint = VayouTheme.colors.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(VayouTheme.iconSize.md),
            ) {
                Icon(
                    imageVector = VayouIcons.Close,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(18.dp),
                    tint = VayouTheme.colors.onSurfaceVariant,
                )
            }
        },
        content = {
            Text(
                text = query,
                style = VayouTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun SearchResultsContent(
    searchResults: SearchResults,
    preferences: ApplicationPreferences,
    isSearching: Boolean,
    contentPadding: PaddingValues = PaddingValues(),
    onFolderClick: (String) -> Unit,
    onVideoClick: (Uri) -> Unit,
    onVideoLoaded: (Uri) -> Unit,
) {
    AnimatedVisibility(
        visible = isSearching,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 100.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            VayouCircularProgress()
        }
    }

    AnimatedVisibility(
        visible = !isSearching,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        if (searchResults.isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 100.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = VayouIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = VayouTheme.colors.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = VayouTheme.typography.bodyLarge,
                        color = VayouTheme.colors.onSurfaceVariant,
                    )
                }
            }
        } else {
            MediaView(
                rootFolder = searchResults.asRootFolder(),
                preferences = preferences,
                onFolderClick = onFolderClick,
                onVideoClick = onVideoClick,
                onVideoLoaded = onVideoLoaded,
                contentPadding = contentPadding,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SearchScreenEmptyPreview() {
    VayouPlayerTheme {
        SearchScreen(
            uiState = SearchUiState(),
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchScreenWithHistoryPreview() {
    VayouPlayerTheme {
        SearchScreen(
            uiState = SearchUiState(
                searchHistory = listOf("avengers", "movie", "trailer"),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchScreenWithResultsPreview() {
    VayouPlayerTheme {
        SearchScreen(
            uiState = SearchUiState(
                query = "movie",
                searchResults = SearchResults(
                    folders = listOf(
                        Folder(
                            name = "Movies",
                            path = "/storage/Movies",
                            dateModified = System.currentTimeMillis(),
                        ),
                    ),
                    videos = listOf(
                        Video.sample.copy(nameWithExtension = "Movie_Clip.mp4", uriString = "content://sample/movie_clip.mp4"),
                        Video.sample.copy(nameWithExtension = "My_Movie.mp4", uriString = "content://sample/my_movie.mp4"),
                    ),
                ),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun SearchScreenNoResultsPreview() {
    VayouPlayerTheme {
        SearchScreen(
            uiState = SearchUiState(
                query = "xyz123",
                searchResults = SearchResults(),
            ),
        )
    }
}
