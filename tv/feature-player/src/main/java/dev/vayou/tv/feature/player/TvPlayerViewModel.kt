package dev.vayou.tv.feature.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.domain.GetSortedPlaylistUseCase
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.Video
import dev.vayou.core.smb.RecentPlaysStore
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class TvPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val getSortedPlaylistUseCase: GetSortedPlaylistUseCase,
    private val recentPlaysStore: RecentPlaysStore,
) : ViewModel() {

    val videoUri: String = Uri.decode(
        savedStateHandle.get<String>(VideoUriArg)
            ?: error("Missing $VideoUriArg argument"),
    )

    data class PlayerReady(val startPosition: Long, val playlist: List<Video>)

    private val _ready = MutableStateFlow<PlayerReady?>(null)
    val ready: StateFlow<PlayerReady?> = _ready.asStateFlow()

    val playerPreferences: StateFlow<PlayerPreferences> = preferencesRepository.playerPreferences

    init {
        viewModelScope.launch {
            val positionDeferred = async { mediaRepository.getVideoByUri(videoUri)?.playbackPosition ?: 0L }
            val videosDeferred = async { getSortedPlaylistUseCase(Uri.parse(videoUri)) }
            _ready.value = PlayerReady(startPosition = positionDeferred.await(), playlist = videosDeferred.await())
        }
    }

    fun saveProgress(position: Long, duration: Long) {
        val finalPosition = when {
            duration <= 0L -> return
            position < 0L -> 0L
            position >= duration - CompletionThresholdMs -> 0L
            else -> position
        }
        viewModelScope.launch {
            withContext(NonCancellable) {
                mediaRepository.updateMediumPosition(videoUri, finalPosition)
                mediaRepository.updateMediumLastPlayedTime(videoUri, System.currentTimeMillis())
                if (!videoUri.startsWith("content://")) {
                    val name = Uri.parse(videoUri).lastPathSegment
                        ?.let(Uri::decode)
                        ?.takeIf { it.isNotBlank() }
                        ?: videoUri
                    recentPlaysStore.record(videoUri, name, duration, finalPosition)
                }
            }
        }
    }

    fun updatePreferences(update: (PlayerPreferences) -> PlayerPreferences) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences(update)
        }
    }

    companion object {
        const val VideoUriArg = "videoUri"
        private const val CompletionThresholdMs = 5_000L
    }
}
