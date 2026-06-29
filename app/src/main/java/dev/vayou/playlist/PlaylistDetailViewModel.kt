package dev.vayou.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.PlaylistRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Playlist
import dev.vayou.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    val preferences: StateFlow<ApplicationPreferences> = preferencesRepository.applicationPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApplicationPreferences())

    val playlist: StateFlow<Playlist?> = playlistRepository.getPlaylists()
        .map { it.find { p -> p.id == playlistId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val videos: StateFlow<List<Video>> = combine(
        playlistRepository.getPlaylistItems(playlistId),
        mediaRepository.getVideosFlow(),
    ) { orderedUris, allVideos ->
        val videoMap = allVideos.associateBy { it.uriString }
        orderedUris.mapNotNull { videoMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableVideos: StateFlow<List<Video>> = combine(
        playlistRepository.getPlaylistItems(playlistId),
        mediaRepository.getVideosFlow(),
    ) { orderedUris, allVideos ->
        val inPlaylist = orderedUris.toSet()
        allVideos.filter { it.uriString !in inPlaylist }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addVideo(videoUri: String) {
        viewModelScope.launch { playlistRepository.addVideos(playlistId, listOf(videoUri)) }
    }

    fun removeVideo(videoUri: String) {
        viewModelScope.launch { playlistRepository.removeVideo(playlistId, videoUri) }
    }

    fun removeVideos(videoUris: List<String>) {
        viewModelScope.launch { playlistRepository.removeVideos(playlistId, videoUris) }
    }

    fun toggleLayoutMode() {
        viewModelScope.launch { preferencesRepository.toggleMediaLayoutMode() }
    }
}
