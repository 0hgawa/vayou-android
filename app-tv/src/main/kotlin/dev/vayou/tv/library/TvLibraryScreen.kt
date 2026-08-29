package dev.vayou.tv.library

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import dev.vayou.core.model.SmartPlaylist
import dev.vayou.core.model.Sort
import dev.vayou.core.model.Video
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.tv.R
import dev.vayou.tv.TvCard
import dev.vayou.tv.TvCardFolder
import dev.vayou.tv.TvCardGap
import dev.vayou.tv.TvCardMark
import dev.vayou.tv.TvCardStar
import dev.vayou.tv.TvCardWidth
import dev.vayou.tv.TvChoiceList
import dev.vayou.tv.TvDetails
import dev.vayou.tv.TvMessage
import dev.vayou.tv.TvOptionItem
import dev.vayou.tv.TvOptions
import dev.vayou.tv.TvOrderButton
import dev.vayou.tv.TvScreenInset
import dev.vayou.tv.TvSearchHeader
import dev.vayou.tv.TvTile
import dev.vayou.tv.TvTitleInset

/**
 * The films on this television, by folder, flat, or in the lists the viewer built.
 *
 * One screen for all three and for whatever opening one of them shows, rather than a route per
 * level: every level is a grid of cards, and pushing a route for each step would put an entry in the
 * back stack for every folder walked through on the way to one film.
 */
@Composable
fun TvLibraryScreen(
    onPlayVideo: (Video, isFromStart: Boolean) -> Unit,
    onBack: () -> Unit,
    /** True when the viewer pressed the magnifier on the home screen rather than the shelf itself. */
    viewModel: TvLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var openFolderPath by rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * What has been opened on top of the grid: starred, the folders, the lists, or one list by id.
     *
     * One field and reserved names rather than a flag each, the way the store already names its
     * derived lists: only one of them can be open, and four booleans would be four ways to say so
     * and three of them wrong.
     */
    var openList by rememberSaveable { mutableStateOf<String?>(null) }

    /** What is being looked for, or null while the viewer is browsing rather than searching. */
    var query by rememberSaveable { mutableStateOf<String?>(null) }

    /** The film whose options are up, or null while the grid is just a grid. */
    var acting by remember { mutableStateOf<Video?>(null) }

    var isChoosingOrder by remember { mutableStateOf(false) }

    /** The film whose details are up. Read only: nothing here is a thing to press. */
    var showing by remember { mutableStateOf<Video?>(null) }

    // The system's own dialog, which on this Android is what asks before a file goes. Only a screen
    // can raise one, and only the viewer can answer it.
    val confirmDelete = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onDeleteAnswered(it.resultCode == Activity.RESULT_OK)
    }
    viewModel.pendingDelete?.let { asked ->
        LaunchedEffect(asked) { confirmDelete.launch(IntentSenderRequest.Builder(asked.request).build()) }
    }

    val openFolder = remember(state, openFolderPath) {
        openFolderPath?.let { path -> state.folders.firstOrNull { it.path == path } }
    }
    val isInFavourites = openList == SmartPlaylist.Favourites

    /** The films of whatever was opened, or null while a grid of somethings is what is listed. */
    val opened: List<Video>? = when {
        openFolder != null -> openFolder.mediaList
        isInFavourites -> state.favourites
        else -> null
    }

    // One way out, and the mark in the header presses it too. The key on the remote walked out of
    // what was opened before leaving the section; the mark, where there was one, went straight home
    // -- so standing in a folder, the two things that mean "back" did different things.
    val goBack = {
        when {
            query != null -> query = null
            openFolderPath != null -> openFolderPath = null
            openList != null -> openList = null
            else -> onBack()
        }
    }
    BackHandler(onBack = goBack)

    // Over the whole library and not over whatever is open. A viewer who types a name is asking
    // where a film is, and answering only from the folder they happen to be standing in would be
    // answering a question nobody asked.
    val found = remember(state.videos, query) {
        query?.trim()?.takeIf { it.isNotEmpty() }?.let { needle ->
            state.videos.filter { it.displayName.contains(needle, ignoreCase = true) }
        }
    }

    if (isChoosingOrder) {
        TvChoiceList(
            title = stringResource(R.string.sort),
            options = SortAxes.map {
                it.by to stringResource(it.label)
            },
            selected = state.sort.by,
            onChoose = viewModel::selectSort,
            onDismiss = { isChoosingOrder = false },
        )
    }

    showing?.let { video ->
        TvDetails(
            title = video.displayName,
            lines = listOfNotNull(
                stringResource(R.string.info_file) to video.nameWithExtension,
                video.parentPath.takeIf { it.isNotBlank() }?.let { stringResource(R.string.info_location) to it },
                stringResource(R.string.info_size) to video.formattedFileSize,
                stringResource(R.string.info_duration) to video.formattedDuration,
                stringResource(R.string.info_resolution) to "${video.width} × ${video.height}",
                video.format?.let { stringResource(R.string.info_format) to it },
            ),
            onDismiss = { showing = null },
        )
    }

    acting?.let { video ->
        val isStarred = state.favourites.any { it.uriString == video.uriString }
        TvOptions(
            face = {
                AsyncImage(
                    model = video.uriString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            title = video.displayName,
            subtitle = video.formattedDuration,
            onDismiss = { acting = null },
            options = listOfNotNull(
                // Only for a film that was left half-watched. On any other it is what pressing the
                // card already does, and a menu that offers the thing you just did is a menu that
                // makes a viewer wonder what the difference was.
                TvOptionItem(VayouIcons.Replay, stringResource(R.string.play_from_start)) {
                    onPlayVideo(video, true)
                }.takeIf { video.playbackPosition > 0L },
                TvOptionItem(
                    // The state it is in, not the state it will be in. The card itself marks a
                    // favourite with a filled star, so a menu that showed a hollow one over the very
                    // card wearing a filled one was two answers to one question.
                    icon = if (isStarred) VayouIcons.StarFilled else VayouIcons.StarOutlined,
                    label = stringResource(if (isStarred) R.string.remove_favourite else R.string.add_favourite),
                ) { viewModel.toggleFavourite(video) },
                TvOptionItem(VayouIcons.Info, stringResource(R.string.details)) { showing = video },
                // Last, and it is the only one here that cannot be undone. Android puts its own
                // dialog in the way, which is the confirmation -- a second one of ours before it
                // would be two questions for one press.
                TvOptionItem(VayouIcons.Delete, stringResource(R.string.delete)) { viewModel.deleteVideo(video) },
            ),
        )
    }

    // Opaque, because the screen it is pushed over is still drawn underneath while the two are
    // being swapped: through a transparent one, the cards of the screen behind show as a shadow
    // across this one.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Only for what the viewer walked into. At the top the bar above already says "Videos", and
        // a heading repeating it is a line of screen spent saying nothing.
        val opening = when {
            openFolder != null -> openFolder.name
            isInFavourites -> stringResource(R.string.favourites)
            openList == AllVideos -> stringResource(R.string.all_videos)
            else -> null
        }
        TvSearchHeader(
            title = opening,
            query = query,
            onSearch = { query = it },
            onOpenSearch = { query = "" },
            onBack = goBack,
            // Only over the listings that obey it, as on the phone. Starred and the built lists keep
            // the order they were built in, and a button that quietly does nothing is worse than no
            // button: the viewer presses it twice and concludes the television is broken.
            action = if (openList == null || openList == AllVideos) {
                {
                    TvOrderButton(
                        isAscending = state.sort.order == Sort.Order.ASCENDING,
                        label = stringResource(R.string.sort),
                    ) { isChoosingOrder = true }
                }
            } else {
                null
            },
        )

        when {
            state.isLoading -> TvMessage(stringResource(R.string.loading_library))

            found != null -> Cards(stringResource(R.string.nothing_found).takeIf { found.isEmpty() }) { first ->
                videos(found, { onPlayVideo(it, false) }, { acting = it }, first)
            }

            opened != null -> Cards(
                stringResource(if (isInFavourites) R.string.no_favourites else R.string.no_videos)
                    .takeIf { opened.isEmpty() },
            ) { first ->
                videos(opened, { onPlayVideo(it, false) }, { acting = it }, first)
            }

            openList == AllVideos ->
                Cards(stringResource(R.string.no_videos).takeIf { state.videos.isEmpty() }) { first ->
                    videos(state.videos, { onPlayVideo(it, false) }, { acting = it }, first)
                }

            // Folders, and not every film at once. The phone opens its library the same way, and a
            // remote is the reason it matters more here: a thumb scrolls two hundred cards in one
            // movement, and a D-pad presses two hundred times. The whole list is still one card
            // away for anyone who would rather have it.
            else -> Cards(stringResource(R.string.no_videos).takeIf { state.videos.isEmpty() }) { first ->
                // Both at the head of the grid, in with the folders rather than each in a section
                // of its own: they are ways of looking at this library, not other libraries, and a
                // television has room on its rail for what is visited daily.
                item {
                    TvTile(
                        title = stringResource(R.string.favourites),
                        subtitle = countOf(state.favourites.size),
                        onClick = { openList = SmartPlaylist.Favourites },
                        modifier = Modifier.focusRequester(first),
                    ) { TvCardStar() }
                }
                item {
                    TvTile(
                        title = stringResource(R.string.all_videos),
                        subtitle = countOf(state.videos.size),
                        onClick = { openList = AllVideos },
                    ) { TvCardMark(VayouIcons.VideoLibrary) }
                }
                itemsIndexed(state.folders, key = { _, folder -> folder.path }) { _, folder ->
                    TvTile(
                        title = folder.name,
                        subtitle = countOf(folder.mediaList.size),
                        onClick = { openFolderPath = folder.path },
                    ) { TvCardFolder() }
                }
            }
        }
    }
}

/** The axes, in the order they are worth having from a sofa: what it is called, then when it came. */
private class SortAxis(val by: Sort.By, val label: Int)

private val SortAxes = listOf(
    SortAxis(Sort.By.TITLE, R.string.sort_by_title),
    SortAxis(Sort.By.DATE, R.string.sort_by_date),
    SortAxis(Sort.By.LENGTH, R.string.sort_by_length),
    SortAxis(Sort.By.SIZE, R.string.sort_by_size),
    SortAxis(Sort.By.PATH, R.string.sort_by_path),
)

/** The films of a folder, a list, or the whole library: the same card either way. */
private fun LazyGridScope.videos(
    videos: List<Video>,
    onPlay: (Video) -> Unit,
    onOptions: (Video) -> Unit,
    first: FocusRequester?,
) {
    itemsIndexed(videos, key = { _, video -> video.uriString }) { index, video ->
        TvCard(
            title = video.displayName,
            subtitle = video.formattedDuration,
            onClick = { onPlay(video) },
            // Held rather than tapped, as the channel list has it: what can be done to a film is
            // not worth a button on every card, and a remote has only the one button.
            onLongClick = { onOptions(video) },
            modifier = if (index == 0 && first != null) Modifier.focusRequester(first) else Modifier,
        ) {
            AsyncImage(
                model = video.uriString,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * A grid of cards, with the first of them handed the focus as soon as there is one.
 *
 * A television screen with nothing focused is a screen the remote cannot use: every press goes
 * nowhere and it reads as frozen. [emptyMessage] is null for a grid that cannot be empty.
 */
@Composable
private fun Cards(emptyMessage: String?, content: LazyGridScope.(FocusRequester) -> Unit) {
    if (emptyMessage != null) {
        TvMessage(emptyMessage)
        return
    }
    val first = remember { FocusRequester() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(TvCardWidth),
        contentPadding = PaddingValues(horizontal = TvScreenInset, vertical = TvTitleInset),
        horizontalArrangement = Arrangement.spacedBy(TvCardGap),
        verticalArrangement = Arrangement.spacedBy(TvCardGap),
    ) {
        content(first)
    }
    // Once per grid: this composes only where there is something to focus, and each branch of the
    // screen is its own call site and so its own effect.
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

/** The name of the grid of every film at once, beside the store's own for the starred. A string
 *  because that is what [openList] holds. */
private const val AllVideos = "videos"

@Composable
private fun countOf(size: Int): String = pluralStringResource(R.plurals.n_videos, size, size)
