package dev.vayou.feature.music

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vayou.core.data.repository.MediaPlaylistRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.media.AudioMimeType
import dev.vayou.core.media.MediaActions
import dev.vayou.core.media.MediaTags
import dev.vayou.core.media.MediaWrite
import dev.vayou.core.media.MusicLibrary
import dev.vayou.core.media.MusicSort
import dev.vayou.core.media.Song
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.MediaLibrary
import dev.vayou.core.model.MediaPlaylists
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A write stopped halfway, waiting for the system's dialog to be answered.
 *
 * One field for both kinds rather than one each: only one dialog can be up at a time, and two
 * fields would be two launchers and a rule about which wins.
 */
data class PendingSongWrite(val request: IntentSender, val work: Work) {
    sealed interface Work {
        /** Nothing is left to do once allowed but forget the lists that named it. */
        data class Delete(val uri: String) : Work

        /** The permission was the question; the tags themselves still have to be written. */
        data class Tags(
            val uri: String,
            val tags: MediaTags,
            val cover: Uri?,
            /** Carried so the cached picture at that address can be dropped once the write lands. */
            val artworkUri: Uri?,
        ) : Work
    }
}

/**
 * Something that happened and left no mark on the screen.
 *
 * Only the ones a listener cannot see for themselves: a write that landed, and the two that failed
 * in silence. Starring, queueing and the rest are said by the screen where they happen.
 */
enum class MusicOutcome { TagsWritten, TagsFailed, DeleteFailed }

/** Where the library stands. Empty is a device with no music; loading is before the first scan. */
sealed interface MusicUiState {
    data object Loading : MusicUiState

    data object Empty : MusicUiState

    data class Success(val songs: List<Song>) : MusicUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class MusicViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val library: MusicLibrary,
    private val playlistRepository: MediaPlaylistRepository,
    private val mediaActions: MediaActions,
    private val imageLoader: ImageLoader,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    /** The lists a track can be added to, and what is starred. Shared with the video library: an
     *  address already says which of the two it came from. */
    val playlists: StateFlow<MediaPlaylists> = playlistRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SubscriptionGraceMs), MediaPlaylists())

    /**
     * A change to a file waiting on the viewer's permission.
     *
     * From Android 11 an app may only delete what it wrote, and nothing in a music library was
     * written by this one. The system asks; this holds what it is asking about.
     */
    var pendingWrite: PendingSongWrite? by mutableStateOf(null)
        private set

    /**
     * True from the moment Save is pressed until the file has actually been written.
     *
     * The write is a copy out, a tag pass and a copy back, and on this Android a system dialog in
     * the middle of it. Without this the form closed on the press and the listener was left looking
     * at the old name wondering whether anything had happened.
     */
    var isWritingTags: Boolean by mutableStateOf(false)
        private set

    /**
     * One-shot, not state: a message is said once. Held in a buffered channel so a write that
     * finishes while the screen is away is still reported when it comes back.
     */
    private val _outcomes = Channel<MusicOutcome>(Channel.BUFFERED)
    val outcomes: Flow<MusicOutcome> = _outcomes.receiveAsFlow()

    fun toggleFavourite(song: Song) {
        viewModelScope.launch { playlistRepository.toggleFavourite(song.uriString) }
    }

    /**
     * List or grid, read from the same preference the video library writes.
     *
     * One setting rather than one per section: the control is the same glyph in the same corner,
     * and a switch that means "here only" is a switch nobody can predict.
     */
    val layoutMode: StateFlow<MediaLayoutMode> = preferencesRepository.applicationPreferences
        .map { it.mediaLayoutMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SubscriptionGraceMs), MediaLayoutMode.LIST)

    fun toggleLayoutMode() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences {
                it.copy(
                    mediaLayoutMode = when (it.mediaLayoutMode) {
                        MediaLayoutMode.LIST -> MediaLayoutMode.GRID
                        MediaLayoutMode.GRID -> MediaLayoutMode.LIST
                    },
                )
            }
        }
    }

    fun createPlaylist(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(playlistRepository.create(name, MediaLibrary.Music)) }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch { playlistRepository.rename(id, name) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { playlistRepository.delete(id) }
    }

    /** Out of the list, not off the phone -- which is why this is not the delete above it. */
    fun removeFromPlaylist(id: String, uri: String) {
        viewModelScope.launch { playlistRepository.removeItem(id, uri) }
    }

    fun addToPlaylist(id: String, songs: List<Song>) {
        viewModelScope.launch { playlistRepository.addItems(id, songs.map { it.uriString }) }
    }

    /** The intent to hand the system, for the listener to choose where the tracks go. */
    fun shareIntent(songs: List<Song>): Intent =
        mediaActions.shareIntent(songs.map { it.uriString.toUri() }, AudioMimeType)

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            when (val write = mediaActions.delete(listOf(song.uriString.toUri()))) {
                // Below Android 11 it is already gone, so the lists that named it are pruned now.
                MediaWrite.Done -> playlistRepository.forgetItems(listOf(song.uriString))
                is MediaWrite.NeedsPermission ->
                    pendingWrite = PendingSongWrite(write.request, PendingSongWrite.Work.Delete(song.uriString))

                MediaWrite.Failed -> _outcomes.send(MusicOutcome.DeleteFailed)
            }
        }
    }

    /**
     * Corrects what the track says it is, in the file itself.
     *
     * The library is drawn from MediaStore's copy of the tags, and that copy is rebuilt from the
     * file: writing anywhere else would show the new name until the next scan and the old one after.
     */
    fun editTags(song: Song, tags: MediaTags, cover: Uri?) {
        isWritingTags = true
        viewModelScope.launch {
            val uri = song.uriString.toUri()
            when (val write = mediaActions.editTags(uri, tags, cover)) {
                MediaWrite.Done -> {
                    finishTags(song.artworkUri)
                    _outcomes.send(MusicOutcome.TagsWritten)
                }
                is MediaWrite.NeedsPermission ->
                    pendingWrite = PendingSongWrite(
                        write.request,
                        PendingSongWrite.Work.Tags(song.uriString, tags, cover, song.artworkUri),
                    )

                MediaWrite.Failed -> {
                    isWritingTags = false
                    _outcomes.send(MusicOutcome.TagsFailed)
                }
            }
        }
    }

    /**
     * Drops the cached cover, so a new one is actually seen.
     *
     * The address of a track's artwork is the album's, and it does not change when the picture
     * behind it does -- so every cache along the way, ours included, keeps handing back the picture
     * it fetched the first time. Only the entries for this one address are dropped: clearing the
     * cache outright would empty a library's worth of covers to answer a question about one track.
     */
    private fun finishTags(artworkUri: Uri?) {
        isWritingTags = false
        val model = artworkUri?.toString() ?: return
        imageLoader.memoryCache?.let { cache -> cache.keys.filter { it.key == model }.forEach(cache::remove) }
        imageLoader.diskCache?.remove(model)
    }

    /**
     * The listener's answer to whatever the system asked about.
     *
     * Refused is not a failure to report: they were asked and they said no, and the library is
     * already showing the world as it still is.
     */
    fun onWriteAnswered(isAllowed: Boolean) {
        val pending = pendingWrite ?: return
        pendingWrite = null
        if (!isAllowed) {
            isWritingTags = false
            return
        }
        viewModelScope.launch {
            when (val work = pending.work) {
                is PendingSongWrite.Work.Delete -> playlistRepository.forgetItems(listOf(work.uri))
                is PendingSongWrite.Work.Tags -> {
                    val written = mediaActions.applyTags(work.uri.toUri(), work.tags, work.cover)
                    finishTags(work.artworkUri)
                    _outcomes.send(if (written) MusicOutcome.TagsWritten else MusicOutcome.TagsFailed)
                }
            }
        }
    }

    private val songs = MutableStateFlow<List<Song>?>(null)

    private val _sort = MutableStateFlow(MusicSort.Title)
    val sort: StateFlow<MusicSort> = _sort.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    val uiState: StateFlow<MusicUiState> = combine(songs, _sort, _isAscending) { list, sort, ascending ->
        when {
            list == null -> MusicUiState.Loading
            list.isEmpty() -> MusicUiState.Empty
            else -> MusicUiState.Success(list.sortedWith(sort.ordering(ascending)))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SubscriptionGraceMs), MusicUiState.Loading)

    /**
     * A media scan reports one change per file, so importing an album fires dozens of these.
     * Collapsing them means one re-query rather than one per track.
     */

    init {
        library.changes
            .debounce(ScanSettleMs)
            // Only once a first scan has landed: before the permission is granted a query returns
            // nothing, and reacting to that would flip the screen to its empty state.
            .onEach { if (songs.value != null) refresh() }
            .launchIn(viewModelScope)
    }

    /** Run once the audio permission is granted. The query itself is off the main thread. */
    fun load() {
        viewModelScope.launch { refresh() }
    }

    /** For a list of groups, which is ordered by name and has no other axis to choose. */
    fun toggleAscending() {
        _isAscending.value = !_isAscending.value
    }

    /** The same axis twice reverses it, which is what the arrow in the header is saying. */
    fun selectSort(sort: MusicSort) {
        if (_sort.value == sort) {
            _isAscending.value = !_isAscending.value
        } else {
            _sort.value = sort
            _isAscending.value = true
        }
    }

    private suspend fun refresh() {
        songs.value = library.all()
    }

    private companion object {
        /** Long enough to swallow a multi-file import, short enough to feel immediate. */
        const val ScanSettleMs = 400L

        const val SubscriptionGraceMs = 5_000L
    }
}
