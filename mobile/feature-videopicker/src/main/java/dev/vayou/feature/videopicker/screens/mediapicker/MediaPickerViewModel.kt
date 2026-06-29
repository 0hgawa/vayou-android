package dev.vayou.feature.videopicker.screens.mediapicker

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.common.extensions.prettyName
import dev.vayou.core.data.repository.FavoritesRepository
import dev.vayou.core.data.repository.PlaylistRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.data.repository.PrivateRepository
import dev.vayou.core.domain.GetSortedMediaUseCase
import dev.vayou.core.media.services.MediaService
import dev.vayou.core.media.sync.MediaInfoSynchronizer
import dev.vayou.core.media.sync.MediaSynchronizer
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Playlist
import dev.vayou.core.model.Video
import dev.vayou.core.ui.base.DataState
import dev.vayou.feature.videopicker.navigation.FolderArgs
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    getSortedMediaUseCase: GetSortedMediaUseCase,
    savedStateHandle: SavedStateHandle,
    private val mediaService: MediaService,
    private val preferencesRepository: PreferencesRepository,
    private val mediaInfoSynchronizer: MediaInfoSynchronizer,
    private val mediaSynchronizer: MediaSynchronizer,
    private val playlistRepository: PlaylistRepository,
    private val favoritesRepository: FavoritesRepository,
    private val privateRepository: PrivateRepository,
) : ViewModel() {

    private val folderArgs = FolderArgs(savedStateHandle)

    val folderPath = folderArgs.folderId

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteUris: StateFlow<Set<String>> = favoritesRepository.getUris()
        .map { it.toHashSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val privateUris: StateFlow<Set<String>> = privateRepository.getVideos()
        .map { videos -> videos.mapTo(HashSet(videos.size)) { it.uriString } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val uiStateInternal = MutableStateFlow(
        MediaPickerUiState(
            folderName = folderPath?.let { File(folderPath).prettyName },
            preferences = preferencesRepository.applicationPreferences.value,
        ),
    )
    val uiState = uiStateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            getSortedMediaUseCase.invoke(folderPath).collect {
                uiStateInternal.update { currentState ->
                    currentState.copy(
                        mediaDataState = DataState.Success(it),
                    )
                }
            }
        }

        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect {
                uiStateInternal.update { currentState ->
                    currentState.copy(
                        preferences = it,
                    )
                }
            }
        }
    }

    fun onEvent(event: MediaPickerUiEvent) {
        when (event) {
            is MediaPickerUiEvent.DeleteFolders -> deleteFolders(event.folders)
            is MediaPickerUiEvent.DeleteVideos -> deleteVideos(event.videos)
            is MediaPickerUiEvent.ShareVideos -> shareVideos(event.videos)
            is MediaPickerUiEvent.Refresh -> refresh()
            is MediaPickerUiEvent.RenameVideo -> renameVideo(event.uri, event.to)
            is MediaPickerUiEvent.AddToSync -> addToMediaInfoSynchronizer(event.uri)
            is MediaPickerUiEvent.UpdateMenu -> updateMenu(event.preferences)
            is MediaPickerUiEvent.AddVideosToPlaylist -> addVideosToPlaylist(event.playlistId, event.uris)
            is MediaPickerUiEvent.CreatePlaylistAndAddVideos -> createPlaylistAndAddVideos(event.name, event.uris)
            is MediaPickerUiEvent.ToggleFavorite -> toggleFavorite(event.videoUri)
            is MediaPickerUiEvent.MoveToPrivate -> moveToPrivate(event.video)
            is MediaPickerUiEvent.MoveVideosToPrivate -> moveVideosToPrivate(event.uriStrings)
        }
    }

    private fun deleteFolders(folders: List<Folder>) {
        viewModelScope.launch {
            val uris = folders.flatMap { folder ->
                folder.allMediaList.map { video ->
                    video.uriString.toUri()
                }
            }
            mediaService.deleteMedia(uris)
        }
    }

    private fun deleteVideos(uris: List<String>) {
        viewModelScope.launch {
            mediaService.deleteMedia(uris.map { it.toUri() })
        }
    }

    private fun shareVideos(uris: List<String>) {
        viewModelScope.launch {
            mediaService.shareMedia(uris.map { it.toUri() })
        }
    }

    private fun addToMediaInfoSynchronizer(uri: Uri) {
        viewModelScope.launch {
            mediaInfoSynchronizer.sync(uri)
        }
    }

    private fun renameVideo(uri: Uri, to: String) {
        viewModelScope.launch {
            mediaService.renameMedia(uri, to)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            uiStateInternal.update { it.copy(refreshing = true) }
            mediaSynchronizer.refresh()
            uiStateInternal.update { it.copy(refreshing = false) }
        }
    }

    private fun updateMenu(preferences: ApplicationPreferences) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { preferences }
        }
    }

    private fun addVideosToPlaylist(playlistId: String, uris: List<String>) {
        viewModelScope.launch { playlistRepository.addVideos(playlistId, uris) }
    }

    private fun createPlaylistAndAddVideos(name: String, uris: List<String>) {
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(name)
            playlistRepository.addVideos(id, uris)
        }
    }

    private fun toggleFavorite(videoUri: String) {
        viewModelScope.launch {
            if (videoUri in favoriteUris.value) {
                favoritesRepository.removeVideo(videoUri)
            } else {
                favoritesRepository.addVideo(videoUri)
            }
        }
    }

    private fun moveToPrivate(video: Video) {
        viewModelScope.launch {
            val privateFile = mediaService.moveToPrivate(Uri.parse(video.uriString)) ?: return@launch
            privateRepository.addVideo(privateFile.absolutePath, video)
        }
    }

    private fun moveVideosToPrivate(uriStrings: List<String>) {
        val allVideos = (uiStateInternal.value.mediaDataState as? DataState.Success)?.value?.allMediaList ?: return
        val videos = allVideos.filter { it.uriString in uriStrings }
        videos.forEach { moveToPrivate(it) }
    }
}

@Stable
data class MediaPickerUiState(
    val folderName: String?,
    val mediaDataState: DataState<Folder?> = DataState.Loading,
    val refreshing: Boolean = false,
    val preferences: ApplicationPreferences = ApplicationPreferences(),
)

sealed interface MediaPickerUiEvent {
    data class DeleteVideos(val videos: List<String>) : MediaPickerUiEvent
    data class DeleteFolders(val folders: List<Folder>) : MediaPickerUiEvent
    data class ShareVideos(val videos: List<String>) : MediaPickerUiEvent
    data object Refresh : MediaPickerUiEvent
    data class RenameVideo(val uri: Uri, val to: String) : MediaPickerUiEvent
    data class AddToSync(val uri: Uri) : MediaPickerUiEvent
    data class UpdateMenu(val preferences: ApplicationPreferences) : MediaPickerUiEvent
    data class AddVideosToPlaylist(val playlistId: String, val uris: List<String>) : MediaPickerUiEvent
    data class CreatePlaylistAndAddVideos(val name: String, val uris: List<String>) : MediaPickerUiEvent
    data class ToggleFavorite(val videoUri: String) : MediaPickerUiEvent
    data class MoveToPrivate(val video: Video) : MediaPickerUiEvent
    data class MoveVideosToPrivate(val uriStrings: List<String>) : MediaPickerUiEvent
}
