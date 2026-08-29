package dev.vayou.tv.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.tv.material3.MaterialTheme
import dev.vayou.core.media.MusicSort
import dev.vayou.core.media.Song
import dev.vayou.core.player.ui.musicMediaItem
import dev.vayou.core.player.ui.rememberMusicController
import dev.vayou.core.ui.graphics.rememberArtworkTint
import dev.vayou.tv.R
import dev.vayou.tv.TvCard
import dev.vayou.tv.TvCardGap
import dev.vayou.tv.TvCardStar
import dev.vayou.tv.TvCardWidth
import dev.vayou.tv.TvChoiceList
import dev.vayou.tv.TvMessage
import dev.vayou.tv.TvOrderButton
import dev.vayou.tv.TvScreenInset
import dev.vayou.tv.TvSearchHeader
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

    /** Whether the grid is showing the starred rather than the whole library. */
    var isInFavourites by rememberSaveable { mutableStateOf(false) }

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

    // One way out, and the mark in the header presses it too. The key on the remote walked out of
    // what was opened before leaving the section; the mark, where there was one, went straight home
    // -- so standing in a folder, the two things that mean "back" did different things.
    val goBack = {
        when {
            query != null -> query = null
            isShowingSleeve -> isShowingSleeve = false
            isInFavourites -> isInFavourites = false
            else -> onBack()
        }
    }
    BackHandler(onBack = goBack)

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

    // Opaque, because the screen it is pushed over is still drawn underneath while the two are
    // being swapped: through a transparent one, the cards of the screen behind show as a shadow
    // across this one.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Only for what the listener walked into. At the top the bar above already says "Music".
        val opening = stringResource(R.string.favourites).takeIf { isInFavourites }
        TvSearchHeader(
            title = opening,
            query = query,
            onSearch = { query = it },
            onOpenSearch = { query = "" },
            onBack = goBack,
            // Not over starred: it keeps the order it was built in, and a button that quietly
            // does nothing is worse than no button at all.
            action = if (isInFavourites) {
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

        // Starred is a way of looking at this library rather than another library, so it opens
        // in place: the same grid, the same cards, one fewer of everything else.
        val listed = if (isInFavourites) state.favourites else state.songs
        when {
            state.isLoading -> TvMessage(stringResource(R.string.loading_library))

            found != null -> Grid(
                songs = found,
                nowPlayingId = nowPlayingId,
                heads = false,
                favouriteCount = state.favourites.size,
                onOpenFavourites = { isInFavourites = true },
                onPlay = { index ->
                    controller?.playFrom(found, index)
                    isShowingSleeve = true
                },
                onOpenSleeve = { isShowingSleeve = true },
                onToggleFavourite = viewModel::toggleFavourite,
                emptyMessage = stringResource(R.string.nothing_found).takeIf { found.isEmpty() },
            )
            isInFavourites && listed.isEmpty() -> TvMessage(stringResource(R.string.no_favourites))
            state.songs.isEmpty() -> TvMessage(stringResource(R.string.no_songs))
            else -> Grid(
                songs = listed,
                nowPlayingId = nowPlayingId,
                heads = !isInFavourites,
                favouriteCount = state.favourites.size,
                onOpenFavourites = { isInFavourites = true },
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
    /** True at the top of the library, where the starred sit before the tracks. */
    heads: Boolean,
    favouriteCount: Int,
    onOpenFavourites: () -> Unit,
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
                Sleeve(song.artworkUri)
            }
        }
    }
}

/**
 * A sleeve on a card that is not the shape of one.
 *
 * The card is 16:9 because every other card in the app is, and a viewer should not meet two grids
 * built to different heights. A cover is square. Cropping it to fit cuts away the top and the
 * bottom, which is where a record usually has its name printed; letterboxing it leaves two dead
 * grey panels either side.
 *
 * So the panels take the record's own colour, read off the cover itself, and the sleeve stands
 * clear of the edges in the middle of them. A track with no cover has no colour to give, so the
 * card stays the flat grey it always was with the note in the middle of it -- the framing costs
 * nothing where there is nothing to frame.
 *
 * Only here: [TvCover] is the cover itself and the now-playing screen frames it its own way.
 */
@Composable
private fun Sleeve(model: Any?) {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    // A 24-pixel copy, cached by Coil, and a grid only ever composes the dozen cards it shows.
    val tint = rememberArtworkTint(model = model, fallback = surface)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Brush.verticalGradient(listOf(tint, surface))) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = SleeveInset)
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .background(surface),
            contentAlignment = Alignment.Center,
        ) {
            TvCover(model)
        }
    }
}

/** How far the sleeve stands off the card's own edge, top and bottom. */
private val SleeveInset = 12.dp

/** How each axis is named here. The comparators are shared with the phone; the words are not. */
private val MusicSort.label: Int
    get() = when (this) {
        MusicSort.Title -> R.string.sort_by_title
        MusicSort.Artist -> R.string.sort_by_artist
        MusicSort.Album -> R.string.sort_by_album
        MusicSort.Duration -> R.string.sort_by_length
        MusicSort.DateAdded -> R.string.sort_by_date
    }
