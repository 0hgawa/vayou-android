package dev.vayou.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.data.repository.PrivateRepository
import dev.vayou.core.media.services.MediaService
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PrivateViewModel @Inject constructor(
    private val privateRepository: PrivateRepository,
    private val preferencesRepository: PreferencesRepository,
    private val mediaService: MediaService,
) : ViewModel() {

    val preferences: StateFlow<ApplicationPreferences> = preferencesRepository.applicationPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApplicationPreferences())

    val videos: StateFlow<List<Video>> = privateRepository.getVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeVideo(videoUri: String) {
        viewModelScope.launch {
            if (mediaService.restoreFromPrivate(videoUri)) {
                privateRepository.removeVideo(videoUri)
            }
        }
    }

    fun removeVideos(videoUris: List<String>) {
        viewModelScope.launch {
            val restored = videoUris.filter { mediaService.restoreFromPrivate(it) }
            if (restored.isNotEmpty()) privateRepository.removeVideos(restored)
        }
    }

    fun toggleLayoutMode() {
        viewModelScope.launch { preferencesRepository.toggleMediaLayoutMode() }
    }
}
