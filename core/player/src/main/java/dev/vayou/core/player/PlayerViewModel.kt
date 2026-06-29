package dev.vayou.core.player

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.common.OpenSubtitlesHasher
import dev.vayou.core.common.extensions.getPath
import dev.vayou.core.data.models.OpenSubtitleResult
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.OpenSubtitlesRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.domain.GetSortedPlaylistUseCase
import dev.vayou.core.model.LoopMode
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.Video
import dev.vayou.core.model.VideoContentScale
import dev.vayou.core.player.model.SubtitleStyleState
import dev.vayou.core.player.state.SubtitleOptionsEvent
import dev.vayou.core.player.state.VideoZoomEvent
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val application: Application,
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val openSubtitlesRepository: OpenSubtitlesRepository,
    private val getSortedPlaylistUseCase: GetSortedPlaylistUseCase,
) : ViewModel() {

    var playWhenReady: Boolean = true

    private val _onlineSubtitleState = MutableStateFlow(OnlineSubtitleSearchState())
    val onlineSubtitleState = _onlineSubtitleState.asStateFlow()

    private val internalUiState = MutableStateFlow(
        PlayerUiState(
            playerPreferences = preferencesRepository.playerPreferences.value,
        ),
    )
    val uiState = internalUiState.asStateFlow()

    private var currentVideoUri: String = ""

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { prefs ->
                internalUiState.update { it.copy(playerPreferences = prefs) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { prefs ->
                internalUiState.update { it.copy(useDynamicColors = prefs.useDynamicColors) }
            }
        }
    }

    suspend fun getPlaylistFromUri(uri: Uri): List<Video> {
        return getSortedPlaylistUseCase.invoke(uri)
    }

    fun updateVideoZoom(uri: String, zoom: Float) {
        viewModelScope.launch {
            mediaRepository.updateMediumZoom(uri, zoom)
        }
    }

    fun updatePlayerBrightness(value: Float) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(playerBrightness = value) }
        }
    }

    fun updateVideoContentScale(contentScale: VideoContentScale) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(playerVideoZoom = contentScale) }
        }
    }

    fun setLoopMode(loopMode: LoopMode) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences { it.copy(loopMode = loopMode) }
        }
    }

    fun onVideoZoomEvent(event: VideoZoomEvent) {
        when (event) {
            is VideoZoomEvent.ContentScaleChanged -> {
                updateVideoContentScale(event.contentScale)
            }
            is VideoZoomEvent.ZoomChanged -> {
                updateVideoZoom(event.mediaItem.mediaId, event.zoom)
            }
        }
    }

    fun onSubtitleOptionEvent(event: SubtitleOptionsEvent) {
        when (event) {
            is SubtitleOptionsEvent.DelayChanged -> {
                viewModelScope.launch { mediaRepository.updateSubtitleDelay(event.mediaItem.mediaId, event.delay) }
            }
            is SubtitleOptionsEvent.SpeedChanged -> {
                viewModelScope.launch { mediaRepository.updateSubtitleSpeed(event.mediaItem.mediaId, event.speed) }
            }
        }
    }

    fun updateSubtitleStyle(state: SubtitleStyleState) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(
                    subtitleTextSize = state.textSize,
                    subtitleFont = state.font,
                    subtitleTextColor = state.textColor,
                    subtitleShadow = state.shadow,
                    subtitleTextBold = state.textBold,
                    subtitleBackground = state.background,
                    subtitleOutlineEnabled = state.outlineEnabled,
                    subtitleOutlineColor = state.outlineColor,
                    subtitleVerticalPosition = state.verticalPosition,
                )
            }
        }
    }

    fun initOnlineSubtitleSearch(videoUri: String, fileName: String) {
        currentVideoUri = videoUri
        _onlineSubtitleState.value = OnlineSubtitleSearchState(query = fileName.substringBeforeLast("."))
    }

    fun updateOnlineSubtitleQuery(query: String) {
        _onlineSubtitleState.update { it.copy(query = query) }
    }

    fun updateOnlineSubtitleLanguage(languageId: String) {
        _onlineSubtitleState.update { it.copy(languageId = languageId) }
    }

    fun searchSubtitlesOnline() {
        val state = _onlineSubtitleState.value
        viewModelScope.launch {
            _onlineSubtitleState.update {
                it.copy(isLoading = true, error = null, results = emptyList(), hasSearched = true)
            }
            val uri = Uri.parse(currentVideoUri)
            val filePath = application.getPath(uri)
            val lang = state.languageId

            val hashResult = filePath?.let { OpenSubtitlesHasher.computeHash(it) }
            val hashSearchResult = if (hashResult != null) {
                openSubtitlesRepository.searchByHash(hashResult.first, hashResult.second, lang)
            } else Result.success(emptyList())
            val querySearchResult = if (state.query.isNotBlank()) {
                openSubtitlesRepository.searchByQuery(state.query, lang)
            } else Result.success(emptyList())

            val hashResults = hashSearchResult.getOrDefault(emptyList())
            val queryResults = querySearchResult.getOrDefault(emptyList())
            val seen = mutableSetOf<String>()
            val merged = (hashResults + queryResults).filter { seen.add(it.subDownloadLink) }

            val error = when {
                merged.isNotEmpty() -> null
                hashSearchResult.isFailure -> hashSearchResult.exceptionOrNull()?.message ?: "search_error"
                querySearchResult.isFailure -> querySearchResult.exceptionOrNull()?.message ?: "search_error"
                else -> "no_results"
            }
            _onlineSubtitleState.update {
                it.copy(
                    isLoading = false,
                    results = merged.take(50),
                    error = error,
                )
            }
        }
    }

    fun downloadOnlineSubtitle(result: OpenSubtitleResult, index: Int, onSuccess: (Uri) -> Unit) {
        viewModelScope.launch {
            _onlineSubtitleState.update { it.copy(downloadingIndex = index) }
            openSubtitlesRepository.downloadSubtitle(result, application.cacheDir)
                .onSuccess { file ->
                    _onlineSubtitleState.update { it.copy(downloadingIndex = null) }
                    onSuccess(Uri.fromFile(file))
                }
                .onFailure {
                    _onlineSubtitleState.update { state -> state.copy(downloadingIndex = null, error = "download_failed") }
                }
        }
    }

    fun clearOnlineSubtitleState() {
        _onlineSubtitleState.value = OnlineSubtitleSearchState()
    }

    fun updatePlayerPreferences(update: (PlayerPreferences) -> PlayerPreferences) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences(update)
        }
    }
}

@Stable
data class PlayerUiState(
    val playerPreferences: PlayerPreferences? = null,
    val useDynamicColors: Boolean = false,
)

@Stable
data class OnlineSubtitleSearchState(
    val query: String = "",
    val languageId: String = "",
    val results: List<OpenSubtitleResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val downloadingIndex: Int? = null,
    val hasSearched: Boolean = false,
)
