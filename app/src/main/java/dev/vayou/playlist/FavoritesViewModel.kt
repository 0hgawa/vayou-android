package dev.vayou.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.FavoritesRepository
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val preferences: StateFlow<ApplicationPreferences> = preferencesRepository.applicationPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApplicationPreferences())

    val videos: StateFlow<List<Video>> = combine(
        favoritesRepository.getUris(),
        mediaRepository.getVideosFlow(),
    ) { favoriteUris, allVideos ->
        val videoMap = allVideos.associateBy { it.uriString }
        favoriteUris.mapNotNull { videoMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeVideo(videoUri: String) {
        viewModelScope.launch { favoritesRepository.removeVideo(videoUri) }
    }

    fun removeVideos(videoUris: List<String>) {
        viewModelScope.launch { favoritesRepository.removeVideos(videoUris) }
    }

    fun toggleLayoutMode() {
        viewModelScope.launch { preferencesRepository.toggleMediaLayoutMode() }
    }
}
