package dev.vayou.tv.library

import android.content.IntentSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaPlaylistRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.domain.GetSortedMediaUseCase
import dev.vayou.core.media.MediaActions
import dev.vayou.core.media.MediaWrite
import dev.vayou.core.model.Folder
import dev.vayou.core.model.MediaLibrary
import dev.vayou.core.model.Sort
import dev.vayou.core.model.Video
import dev.vayou.tv.TvMediaList
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything on this television, in the order the viewer asked for.
 *
 * The order is the one preference both apps already share rather than a second one kept here. Set
 * from the sofa, the phone opens the same way round, which is the only behaviour that needs no
 * explaining -- and it leaves the television able to undo an order set on the phone, which a
 * shorter list of its own would not have been.
 */
@HiltViewModel
class TvLibraryViewModel @Inject constructor(
    getSortedMedia: GetSortedMediaUseCase,
    private val playlistRepository: MediaPlaylistRepository,
    private val preferencesRepository: PreferencesRepository,
    private val mediaActions: MediaActions,
) : ViewModel() {

    /**
     * The film the system is being asked about, or null when it is being asked nothing.
     *
     * From Android 11 an app may only delete what it wrote itself, and everything a library shows
     * came from a camera or a messenger -- so the asking is the system's own dialog, which only a
     * screen can raise. Held here rather than there because the answer outlives the card that was
     * held down: the grid rebuilds itself the moment the file goes.
     */
    var pendingDelete: PendingDelete? by mutableStateOf(null)
        private set

    val state: StateFlow<TvLibraryState> = combine(
        getSortedMedia(),
        playlistRepository.playlists,
        preferencesRepository.applicationPreferences,
    ) { root, playlists, preferences ->
        val sort = Sort(by = preferences.sortBy, order = preferences.sortOrder)
        if (root == null) {
            TvLibraryState(sort = sort)
        } else {
            // An address is what a list stores, and it outlives the file it names. Resolved once
            // here rather than per card, so a list of a hundred is one pass and not a hundred scans.
            val byUri = root.mediaList.associateBy(Video::uriString)
            TvLibraryState(
                isLoading = false,
                sort = sort,
                folders = root.folderList,
                videos = root.mediaList,
                favourites = playlists.favouriteUris.mapNotNull(byUri::get),
                playlists = playlists.of(MediaLibrary.Video).map { list ->
                    TvMediaList(list.id, list.name, list.itemUris.mapNotNull(byUri::get))
                },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        // Kept for a moment past the last reader, so switching to the home screen and back does not
        // re-run the query and flash an empty grid on the way in.
        started = SharingStarted.WhileSubscribed(IdleTimeoutMs),
        initialValue = TvLibraryState(),
    )

    /** Starred is one set shared with the music library: an address says which it came from. */
    fun toggleFavourite(video: Video) {
        viewModelScope.launch { playlistRepository.toggleFavourite(video.uriString) }
    }

    /**
     * Throws the file away, once the system has agreed.
     *
     * Below Android 11 it is already gone by the time this returns, so the lists that named it stop
     * naming it now; above it, only once the viewer has said yes.
     */
    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            when (val write = mediaActions.delete(listOf(video.uriString.toUri()))) {
                MediaWrite.Done -> playlistRepository.forgetItems(listOf(video.uriString))
                is MediaWrite.NeedsPermission -> pendingDelete = PendingDelete(write.request, video.uriString)
                MediaWrite.Failed -> Unit
            }
        }
    }

    /**
     * Refused is not a failure to report: they were asked and they said no, and the shelf is
     * already showing the world as it still is.
     */
    fun onDeleteAnswered(isAllowed: Boolean) {
        val asked = pendingDelete ?: return
        pendingDelete = null
        if (!isAllowed) return
        viewModelScope.launch { playlistRepository.forgetItems(listOf(asked.uri)) }
    }

    /**
     * Choosing the axis already in use turns the list around instead of doing nothing, which is the
     * only reading of a second press on it that means anything -- and it is how the phone reads it.
     */
    fun selectSort(by: Sort.By) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { current ->
                if (current.sortBy != by) {
                    current.copy(sortBy = by)
                } else {
                    current.copy(
                        sortOrder = when (current.sortOrder) {
                            Sort.Order.ASCENDING -> Sort.Order.DESCENDING
                            Sort.Order.DESCENDING -> Sort.Order.ASCENDING
                        },
                    )
                }
            }
        }
    }
}

/** A deletion waiting on the system's own dialog, and what it is about. */
class PendingDelete(val request: IntentSender, val uri: String)

data class TvLibraryState(
    val isLoading: Boolean = true,
    val sort: Sort = Sort(Sort.By.TITLE, Sort.Order.ASCENDING),
    val folders: List<Folder> = emptyList(),
    val videos: List<Video> = emptyList(),
    val favourites: List<Video> = emptyList(),
    val playlists: List<TvMediaList<Video>> = emptyList(),
)

private const val IdleTimeoutMs = 5_000L
