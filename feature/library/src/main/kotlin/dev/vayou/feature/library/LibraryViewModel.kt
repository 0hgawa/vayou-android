package dev.vayou.feature.library

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.common.Utils
import dev.vayou.core.data.repository.MediaPlaylistRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.domain.GetSortedMediaUseCase
import dev.vayou.core.media.MediaActions
import dev.vayou.core.media.MediaWrite
import dev.vayou.core.media.PrivateStorage
import dev.vayou.core.media.PrivateTake
import dev.vayou.core.model.Folder
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.MediaLibrary
import dev.vayou.core.model.MediaPlaylists
import dev.vayou.core.model.MediaViewMode
import dev.vayou.core.model.PrivateVideo
import dev.vayou.core.model.Sort
import dev.vayou.core.model.Video
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    getSortedMedia: GetSortedMediaUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val playlistRepository: MediaPlaylistRepository,
    private val privateStorage: PrivateStorage,
    private val mediaActions: MediaActions,
) : ViewModel() {

    val uiState = combine(
        getSortedMedia(),
        preferencesRepository.applicationPreferences,
        playlistRepository.playlists,
    ) { root, preferences, playlists ->
        if (root == null) {
            LibraryUiState.Loading
        } else {
            LibraryUiState.Ready(
                root = root,
                viewMode = preferences.mediaViewMode,
                layoutMode = preferences.mediaLayoutMode,
                sort = Sort(by = preferences.sortBy, order = preferences.sortOrder),
                playlists = playlists,
                isShowingRecents = preferences.showRecentVideos,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        // Kept for a moment past the last reader, so a rotation does not re-run the query and
        // flash an empty list on the way back.
        started = SharingStarted.WhileSubscribed(StopTimeoutMillis),
        initialValue = LibraryUiState.Loading,
    )

    /**
     * Choosing the axis already in use turns the list around instead of doing nothing, which is the
     * only reading of a second tap on it that means anything.
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

    fun selectViewMode(mode: MediaViewMode) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { it.copy(mediaViewMode = mode) }
        }
    }

    /** Rows or cards. One button, because there are two ways and it is already showing one of them. */
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

    /**
     * [onCreated] receives the new id, so a list made can be opened straight away. Made and then
     * left behind on a list of lists is a dead end.
     */
    fun createPlaylist(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(playlistRepository.create(name, MediaLibrary.Video)) }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch { playlistRepository.rename(id, name) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { playlistRepository.delete(id) }
    }

    fun addToPlaylist(id: String, uris: List<String>) {
        viewModelScope.launch { playlistRepository.addItems(id, uris) }
    }

    fun removeFromPlaylist(id: String, uri: String) {
        viewModelScope.launch { playlistRepository.removeItem(id, uri) }
    }

    fun toggleFavourite(uri: String) {
        viewModelScope.launch { playlistRepository.toggleFavourite(uri) }
    }

    /**
     * A change to a file that is waiting on the viewer's permission.
     *
     * From Android 11 an app may only write to what it wrote itself, and everything in the library
     * came from a camera or a messenger. The system puts the question; this holds what was being
     * asked about, so the answer can be acted on.
     *
     * One field for all three -- hiding, renaming, deleting -- because only one dialog can be up at
     * a time, and three of these would be three ways for the same screen to be in the same state.
     */
    var pendingWrite: PendingWrite? by mutableStateOf(null)
        private set

    /**
     * One-shot, not state: a message is said once. Buffered, so a write that lands while the shelf
     * is off screen is still reported when it comes back.
     */
    private val _outcomes = Channel<LibraryOutcome>(Channel.BUFFERED)
    val outcomes: Flow<LibraryOutcome> = _outcomes.receiveAsFlow()

    /** Takes a film out of the library and into the app's own storage. */
    fun moveToPrivate(video: Video) {
        viewModelScope.launch {
            when (val taken = privateStorage.take(video.uriString.toUri())) {
                is PrivateTake.Done -> {
                    record(video, taken.file)
                    _outcomes.send(LibraryOutcome.Hidden)
                }
                // Handed to the screen, which is the only thing that can put a system dialog up.
                is PrivateTake.NeedsPermission ->
                    pendingWrite = PendingWrite(taken.request, PendingWrite.Work.Hide(video, taken.file))

                PrivateTake.Failed -> _outcomes.send(LibraryOutcome.HideFailed)
            }
        }
    }

    fun renameVideo(video: Video, to: String) {
        viewModelScope.launch {
            val uri = video.uriString.toUri()
            when (val write = mediaActions.rename(uri, to)) {
                MediaWrite.Done -> Unit
                is MediaWrite.NeedsPermission ->
                    pendingWrite = PendingWrite(write.request, PendingWrite.Work.Rename(uri, to))

                MediaWrite.Failed -> Unit
            }
        }
    }

    /**
     * Films, in one request, however many were picked.
     *
     * One request and not one each, because from Android 11 the system asks the viewer before a
     * file the app did not write is removed, and that question arrives as a dialog. Asked once per
     * film, a folder of forty raises forty dialogs that each overwrite the last, one is answered,
     * and thirty-nine files quietly stay -- which is what emptying a folder used to do. The store
     * takes the whole list and puts a single question for all of it.
     */
    fun deleteVideos(videos: List<Video>) {
        if (videos.isEmpty()) return
        viewModelScope.launch {
            val uris = videos.map { it.uriString }
            when (val write = mediaActions.delete(uris.map(String::toUri))) {
                // Below Android 11 they are already gone, so the lists that named them are pruned now.
                MediaWrite.Done -> playlistRepository.forgetItems(uris)
                is MediaWrite.NeedsPermission ->
                    pendingWrite = PendingWrite(write.request, PendingWrite.Work.Delete(uris))

                MediaWrite.Failed -> _outcomes.send(LibraryOutcome.DeleteFailed)
            }
        }
    }

    /** The intent to hand the system, for the viewer to choose where the films go. */
    fun shareIntent(videos: List<Video>): Intent = mediaActions.shareIntent(videos.map { it.uriString.toUri() })

    /**
     * The viewer's answer to whatever the system just asked about.
     *
     * Refused is not a failure to report: they were asked and they said no, and the library is
     * already showing the world as it still is.
     */
    fun onWriteAnswered(isAllowed: Boolean) {
        val pending = pendingWrite ?: return
        pendingWrite = null
        viewModelScope.launch {
            when (val work = pending.work) {
                is PendingWrite.Work.Hide -> {
                    privateStorage.settle(work.file, isAllowed)
                    if (isAllowed) {
                        record(work.video, work.file)
                        _outcomes.send(LibraryOutcome.Hidden)
                    }
                }
                // The permission was the question; the rename itself still has to be done.
                is PendingWrite.Work.Rename -> if (isAllowed) mediaActions.applyRename(work.uri, work.to)
                // Only now is the file actually gone, so only now do the lists stop naming it.
                is PendingWrite.Work.Delete -> if (isAllowed) playlistRepository.forgetItems(work.uris)
            }
        }
    }

    /**
     * Writes down what the film was, then drops every list and favourite that named it.
     *
     * Written down because once it has left MediaStore nothing can be asked how long it is or what
     * it was called, and the row still has to say. Dropped because those hold public addresses, and
     * this one has stopped resolving.
     */
    private suspend fun record(video: Video, file: File) {
        playlistRepository.addPrivate(
            PrivateVideo(
                filePath = file.absolutePath,
                name = video.nameWithExtension,
                duration = video.duration,
                width = video.width,
                height = video.height,
                size = video.size,
                dateModified = video.dateModified,
            ),
        )
        playlistRepository.forgetItems(listOf(video.uriString))
    }

    /** Puts it back where everything else can see it. Forgotten only once it is actually back:
     *  a failed restore that dropped its record would leave the file reachable from nowhere. */
    fun restoreFromPrivate(filePath: String) {
        viewModelScope.launch {
            if (privateStorage.release(filePath)) playlistRepository.removePrivate(listOf(filePath))
        }
    }
}

/**
 * A private film as a library row.
 *
 * Rebuilt from what was written down rather than read off the file: these have left MediaStore, so
 * there is nothing to read. The path stands in for the address, which is what plays it and what
 * names it in the store.
 */
private fun PrivateVideo.asVideo(): Video = Video(
    id = 0L,
    path = filePath,
    uriString = filePath,
    nameWithExtension = name,
    duration = duration,
    width = width,
    height = height,
    size = size,
    dateModified = dateModified,
    formattedDuration = Utils.formatDurationMillis(duration),
    formattedFileSize = Utils.formatFileSize(size),
)

/** A change to a file, stopped halfway, waiting for the system's dialog to be answered. */
/**
 * Something that happened and left no mark on the shelf.
 *
 * Only those: a film taken out of the library into the locked folder, and the two failures that
 * ended in silence. Starring, renaming and the rest are said by the row they happen on.
 */
enum class LibraryOutcome { Hidden, HideFailed, DeleteFailed }

data class PendingWrite(val request: IntentSender, val work: Work) {

    /** What to do once the viewer has said yes. */
    sealed interface Work {
        /** The copy is already made; all that is left is to record it or take it back out. */
        data class Hide(val video: Video, val file: File) : Work

        data class Rename(val uri: Uri, val to: String) : Work

        /** Every address the one dialog asked about, so all of them are forgotten together. */
        data class Delete(val uris: List<String>) : Work
    }
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Ready(
        val root: Folder,
        val viewMode: MediaViewMode,
        val layoutMode: MediaLayoutMode,
        val sort: Sort,
        val playlists: MediaPlaylists,
        val isShowingRecents: Boolean,
    ) : LibraryUiState {

        /** The library indexed by address, which is how a list of addresses becomes a list of videos. */
        val videosByUri: Map<String, Video> = root.mediaList.associateBy { it.uriString }

        /** What a stored list still points at. Addresses outlive the files they name. */
        fun videosIn(uris: List<String>): List<Video> = uris.mapNotNull(videosByUri::get)

        /** The locked folder, as rows. Carried straight in: these are in no library to look up. */
        val privateVideos: List<Video> = playlists.privateVideos.map(PrivateVideo::asVideo)

        /**
         * What was last watched, newest first, and empty where the viewer has switched it off.
         *
         * Capped: this is a strip to glance along, and a hundred cards in it is a second library
         * laid sideways.
         */
        val recentVideos: List<Video> = if (!isShowingRecents) {
            emptyList()
        } else {
            root.mediaList
                .filter { it.lastPlayedAt != null }
                .sortedByDescending { it.lastPlayedAt?.time }
                .take(RecentLimit)
        }
    }
}

private const val StopTimeoutMillis = 5_000L

/** Twenty, which is a fortnight of watching and about six screens of sideways scroll. */
private const val RecentLimit = 20
