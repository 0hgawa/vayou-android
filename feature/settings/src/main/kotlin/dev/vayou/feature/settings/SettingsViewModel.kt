package dev.vayou.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.media.sync.MediaSynchronizer
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Folder
import dev.vayou.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val synchronizer: MediaSynchronizer,
    mediaRepository: MediaRepository,
) : ViewModel() {

    val application: StateFlow<ApplicationPreferences> = preferencesRepository.applicationPreferences

    val player: StateFlow<PlayerPreferences> = preferencesRepository.playerPreferences

    /**
     * Every folder the library found, or null until it has looked.
     *
     * Null rather than an empty list, because the two mean opposite things on the screen that shows
     * them: one is a spinner, the other is a phone with no video on it.
     */
    val folders: StateFlow<List<Folder>?> = mediaRepository.getFoldersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SubscriptionTimeout), null)

    fun updateApplication(transform: ApplicationPreferences.() -> ApplicationPreferences) {
        viewModelScope.launch { preferencesRepository.updateApplicationPreferences { it.transform() } }
    }

    fun updatePlayer(transform: PlayerPreferences.() -> PlayerPreferences) {
        viewModelScope.launch { preferencesRepository.updatePlayerPreferences { it.transform() } }
    }

    /** A folder is in the library or out of it, and the row that says which is the same row. */
    fun toggleExcludedFolder(path: String) {
        updateApplication {
            copy(excludeFolders = if (path in excludeFolders) excludeFolders - path else excludeFolders + path)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch { synchronizer.refresh() }
    }

    /**
     * Every setting back to what it shipped as.
     *
     * Both stores, since "settings" to a reader is one thing rather than two files. What is not a
     * setting survives: the folders scanned, where each film was left, the playlists kept.
     */
    fun resetSettings() {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { ApplicationPreferences() }
            preferencesRepository.updatePlayerPreferences { PlayerPreferences() }
        }
    }
}

/** Long enough to survive a rotation, short enough that leaving the screen stops the query. */
private const val SubscriptionTimeout = 5_000L
