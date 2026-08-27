package dev.vayou.tv.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import dev.vayou.core.media.MusicSort
import dev.vayou.core.media.Song
import dev.vayou.core.model.SmartPlaylist
import dev.vayou.core.player.ui.musicMediaItem
import dev.vayou.core.player.ui.rememberMusicController
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.tv.R
import dev.vayou.tv.TvCard
import dev.vayou.tv.TvCardGap
import dev.vayou.tv.TvCardMark
import dev.vayou.tv.TvCardStar
import dev.vayou.tv.TvCardWidth
import dev.vayou.tv.TvChoiceList
import dev.vayou.tv.TvMediaList
import dev.vayou.tv.TvMessage
import dev.vayou.tv.TvOrderButton
import dev.vayou.tv.TvScreenInset
import dev.vayou.tv.TvSearchHeader
import dev.vayou.tv.TvTile
import dev.vayou.tv.TvTitleInset

/**
 * The music on this television: a wall of covers, and what is playing.
 *
 * One screen for both, as the library is for its folders. A track chosen from the grid puts the
 * whole grid behind it in the queue, so the rest of the album follows without anyone asking, and
 * back steps out of the sleeve rather than out of the sound.
 */
@Composable
fun TvMusicScreen(onBack: () -> Unit, viewModel: TvMusicViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isShowingSleeve by remember { mutableStateOf(false) }

    /**
     * What has been opened on top of the grid: starred, the list of lists, or one of them by id.
     *
     * One field and reserved names rather than three flags: only one of them can be open, and three
     * booleans would be three ways to say so and one of them wrong.
     */
    var openList by rememberSaveable { mutableStateOf<String?>(null) }

    var isChoosingOrder by remember { mutableStateOf(false) }

    /** What is being looked for, or null while the listener is browsing rather than searching. */
    var query by rememberSaveable { mutableStateOf<String?>(null) }

    // Mirrored off the session rather than held here: the phone, the notification and this screen
    // are all looking at one player, and a second copy of "what is playing" is a second answer.
    var nowPlayingId by remember { mutableStateOf<String?>(null) }
    val controller = rememberMusicController { player ->
        nowPlayingId = player.currentMediaItem?.mediaId
    }
    val nowPlaying = remember(state, nowPlayingId) { state.songs.firstOrNull { it.uriString == nowPlayingId } }
    val openPlaylist = remember(state, openList) { state.playlists.firstOrNull { it.id == openList } }
    val isInFavourites = openList == SmartPlaylist.Favourites

    BackHandler {
        when {
            query != null -> query = null
            isShowingSleeve -> isShowingSleeve = false
            openPlaylist != null -> openList = AllLists
            openList != null -> openList = null
            else -> onBack()
        }
    }

    if (isShowingSleeve && nowPlaying != null && controller != null) {
        // The library knows the title, the artist and the cover before a byte of the file has been
        // read, and knows them when the tags are empty. Handed over rather than looked up again.
        TvNowPlaying(
            controller = controller,
            known = TrackFacts(nowPlaying.title, nowPlaying.artist, nowPlaying.artworkUri),
        )
        return
    }

    // Over the whole library and not over whatever is open, as the shelf of films answers: somebody
    // typing a name is asking where a track is, and answering from the list they happen to be
    // standing in would be answering a question nobody asked. Artist as well as title, because half
    // of what anyone remembers about a song is who sang it.
    val found = remember(state.songs, query) {
        query?.trim()?.takeIf { it.isNotEmpty() }?.let { needle ->
            state.songs.filter {
                it.title.contains(needle, ignoreCase = true) || it.artist.contains(needle, ignoreCase = true)
            }
        }
    }

    if (isChoosingOrder) {
        TvChoiceList(
            title = stringResource(R.string.sort),
            options = MusicSort.entries.map {
                it to stringResource(it.label)
            },
            selected = state.order.by,
            onChoose = viewModel::selectSort,
            onDismiss = { isChoosingOrder = false },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Only for what the listener walked into. At the top the bar above already says "Music".
        val opening = when {
            isInFavourites -> stringResource(R.string.favourites)
            openPlaylist != null -> openPlaylist.name
            openList == AllLists -> stringResource(R.string.playlists)
            else -> null
        }
        TvSearchHeader(
            title = opening,
            query = query,
            onSearch = { query = it },
            onOpenSearch = { query = "" },
            onBack = onBack,
            // Not over starred or a built list: those keep the order they were built in, and a
            // button that quietly does nothing is worse than no button at all.
            action = if (openList != null) {
                null
            } else {
                {
                    TvOrderButton(
                        isAscending = state.order.isAscending,
                        label = stringResource(R.string.sort),
                    ) { isChoosingOrder = true }
                }
            },
        )

        // Starred and the lists are ways of looking at this library rather than other libraries, so
        // they open in place: the same grid, the same cards, one fewer of everything else.
        val listed = when {
            isInFavourites -> state.favourites
            openPlaylist != null -> openPlaylist.items
            else -> state.songs
        }
        when {
            state.isLoading -> TvMessage(stringResource(R.string.loading_library))

            found != null -> Grid(
                songs = found,
                nowPlayingId = nowPlayingId,
                heads = false,
                favouriteCount = state.favourites.size,
                playlistCount = state.playlists.sumOf { it.items.size },
                onOpenFavourites = { openList = SmartPlaylist.Favourites },
                onOpenPlaylists = { openList = AllLists },
                onPlay = { index ->
                    controller?.playFrom(found, index)
                    isShowingSleeve = true
                },
                onOpenSleeve = { isShowingSleeve = true },
                onToggleFavourite = viewModel::toggleFavourite,
                emptyMessage = stringResource(R.string.nothing_found).takeIf { found.isEmpty() },
            )
            openList == AllLists -> Lists(
                playlists = state.playlists,
                onOpen = { openList = it.id },
            )

            isInFavourites && listed.isEmpty() -> TvMessage(stringResource(R.string.no_favourites))
            state.songs.isEmpty() -> TvMessage(stringResource(R.string.no_songs))
            else -> Grid(
                songs = listed,
                nowPlayingId = nowPlayingId,
                heads = openList == null,
                favouriteCount = state.favourites.size,
                playlistCount = state.playlists.sumOf { it.items.size },
                onOpenFavourites = { openList = SmartPlaylist.Favourites },
                onOpenPlaylists = { openList = AllLists },
                onPlay = { index ->
                    controller?.playFrom(listed, index)
                    isShowingSleeve = true
                },
                onOpenSleeve = { isShowingSleeve = true },
                onToggleFavourite = viewModel::toggleFavourite,
            )
        }
    }
}

/** The whole library becomes the queue, starting where the viewer pressed. */
private fun MediaController.playFrom(songs: List<Song>, index: Int) {
    setMediaItems(songs.map(::musicMediaItem), index, 0L)
    prepare()
    play()
}

@Composable
private fun Grid(
    songs: List<Song>,
    nowPlayingId: String?,
    /** True at the top of the library, where the two ways of looking at it sit before the tracks. */
    heads: Boolean,
    favouriteCount: Int,
    playlistCount: Int,
    onOpenFavourites: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onPlay: (Int) -> Unit,
    onOpenSleeve: () -> Unit,
    onToggleFavourite: (Song) -> Unit,
    /** What to say instead of the grid: a search that found nothing, and nothing else. */
    emptyMessage: String? = null,
) {
    if (emptyMessage != null) {
        TvMessage(emptyMessage)
        return
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(heads) { runCatching { first.requestFocus() } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(TvCardWidth),
        contentPadding = PaddingValues(horizontal = TvScreenInset, vertical = TvTitleInset),
        horizontalArrangement = Arrangement.spacedBy(TvCardGap),
        verticalArrangement = Arrangement.spacedBy(TvCardGap),
    ) {
        if (heads) {
            item {
                TvCard(
                    title = stringResource(R.string.favourites),
                    subtitle = pluralStringResource(R.plurals.n_songs, favouriteCount, favouriteCount),
                    onClick = onOpenFavourites,
                    modifier = Modifier.focusRequester(first),
                ) { TvCardStar() }
            }
            item {
                TvCard(
                    title = stringResource(R.string.playlists),
                    subtitle = pluralStringResource(R.plurals.n_songs, playlistCount, playlistCount),
                    onClick = onOpenPlaylists,
                ) { TvCardMark(VayouIcons.MusicPlaylist) }
            }
        }
        itemsIndexed(songs, key = { _, song -> song.uriString }) { index, song ->
            TvCard(
                title = song.title,
                subtitle = song.artist,
                // Pressing the one already playing opens its sleeve rather than starting it again,
                // which would drop a listener back to the top of the track they are in the middle of.
                onClick = { if (song.uriString == nowPlayingId) onOpenSleeve() else onPlay(index) },
                // Held rather than tapped, as the channel list has it: starring is the second thing
                // anyone does to a track, and it does not deserve a button on every card.
                onLongClick = { onToggleFavourite(song) },
                modifier = if (index == 0 && !heads) Modifier.focusRequester(first) else Modifier,
            ) {
                TvCover(song.artworkUri)
            }
        }
    }
}

/** The lists the listener built on the phone. Opening one is all a remote can do with it. */
@Composable
private fun Lists(playlists: List<TvMediaList<Song>>, onOpen: (TvMediaList<Song>) -> Unit) {
    if (playlists.isEmpty()) {
        TvMessage(stringResource(R.string.no_playlists))
        return
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(TvCardWidth),
        contentPadding = PaddingValues(horizontal = TvScreenInset, vertical = TvTitleInset),
        horizontalArrangement = Arrangement.spacedBy(TvCardGap),
        verticalArrangement = Arrangement.spacedBy(TvCardGap),
    ) {
        itemsIndexed(playlists, key = { _, list -> list.id }) { index, list ->
            TvTile(
                title = list.name,
                subtitle = pluralStringResource(R.plurals.n_songs, list.items.size, list.items.size),
                onClick = { onOpen(list) },
                modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
            ) { TvCardMark(VayouIcons.MusicPlaylist) }
        }
    }
}

/** The reserved name for the list of lists, beside the store's own for the derived ones. */
private const val AllLists = "lists"

/** How each axis is named here. The comparators are shared with the phone; the words are not. */
private val MusicSort.label: Int
    get() = when (this) {
        MusicSort.Title -> R.string.sort_by_title
        MusicSort.Artist -> R.string.sort_by_artist
        MusicSort.Album -> R.string.sort_by_album
        MusicSort.Duration -> R.string.sort_by_length
        MusicSort.DateAdded -> R.string.sort_by_date
    }
