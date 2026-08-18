package dev.vayou.tv.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaPlaylistRepository
import dev.vayou.core.media.MusicLibrary
import dev.vayou.core.media.MusicSort
import dev.vayou.core.media.Song
import dev.vayou.core.model.MediaLibrary
import dev.vayou.tv.TvMediaList
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Every track on this television, in the order the listener asked for.
 *
 * The phone's own library query, unchanged: it is one read of MediaStore and the two apps have no
 * reason to disagree about what is on the device. What is not here is the phone's four tabs --
 * albums, artists, folders -- because a remote reaches a track by scrolling to it, and three more
 * ways in are three more presses before the first note.
 */
@HiltViewModel
class TvMusicViewModel @Inject constructor(
    private val library: MusicLibrary,
    private val playlistRepository: MediaPlaylistRepository,
) : ViewModel() {

    /** Null until MediaStore has answered, which is what tells an empty library from a slow one. */
    private val scanned = MutableStateFlow<List<Song>?>(null)

    /**
     * Kept here and not on disc, as the phone's music library keeps it: the axis is a way of
     * looking at a list while you are in it, and a television that remembered an evening spent
     * sorted by length would open the wrong way round a week later.
     */
    private val order = MutableStateFlow(TvMusicOrder())

    /**
     * The library in the chosen order, carried with the order that produced it.
     *
     * Sorted here rather than below, so starring a track -- which is a write the store reports back
     * -- reorders nothing. The order rides along because it is drawn beside the list, and reading it
     * separately below would be reading it a beat before the list it describes.
     */
    private val ordered = combine(scanned, order) { songs, order ->
        order to songs?.sortedWith(order.by.ordering(order.isAscending))
    }

    val state: StateFlow<TvMusicState> = combine(
        ordered,
        playlistRepository.playlists,
    ) { (order, songs), playlists ->
        if (songs == null) {
            TvMusicState()
        } else {
            // Starred is stored as addresses, shared with the film library: an address already says
            // which of the two it came from, so the pass here is over this library alone.
            val starred = playlists.favouriteUris.toHashSet()
            val byUri = songs.associateBy(Song::uriString)
            TvMusicState(
                isLoading = false,
                order = order,
                songs = songs,
                favourites = songs.filter { it.uriString in starred },
                playlists = playlists.of(MediaLibrary.Music).map { list ->
                    TvMediaList(list.id, list.name, list.itemUris.mapNotNull(byUri::get))
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(IdleTimeoutMs), TvMusicState())

    init {
        // Unsorted: the order is put on above, where changing it costs a resort and not a second
        // read of MediaStore.
        viewModelScope.launch { scanned.value = library.all() }
    }

    fun toggleFavourite(song: Song) {
        viewModelScope.launch { playlistRepository.toggleFavourite(song.uriString) }
    }

    /** The same axis twice reverses it, which is what the arrow beside it is saying. */
    fun selectSort(by: MusicSort) {
        order.update {
            if (it.by == by) it.copy(isAscending = !it.isAscending) else TvMusicOrder(by)
        }
    }
}

/** Which axis the library is on, and which way it runs. */
data class TvMusicOrder(val by: MusicSort = MusicSort.Title, val isAscending: Boolean = true)

data class TvMusicState(
    val isLoading: Boolean = true,
    val order: TvMusicOrder = TvMusicOrder(),
    val songs: List<Song> = emptyList(),
    val favourites: List<Song> = emptyList(),
    val playlists: List<TvMediaList<Song>> = emptyList(),
)

private const val IdleTimeoutMs = 5_000L
