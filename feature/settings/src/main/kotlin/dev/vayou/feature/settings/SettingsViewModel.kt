package dev.vayou.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.media.MusicLibrary
import dev.vayou.core.media.sync.MediaSynchronizer
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val synchronizer: MediaSynchronizer,
    private val musicLibrary: MusicLibrary,
    mediaRepository: MediaRepository,
) : ViewModel() {

    val application: StateFlow<ApplicationPreferences> = preferencesRepository.applicationPreferences

    val player: StateFlow<PlayerPreferences> = preferencesRepository.playerPreferences

    /**
     * Every folder either library found, or null until they have looked.
     *
     * Both, because a folder is kept out of the library and not out of one half of it -- and until
     * this asked the music as well, a folder holding only audio was one the reader could see in the
     * app and never exclude, since it was not on the list to tick.
     *
     * Null rather than an empty list, because the two mean opposite things on the screen that shows
     * them: one is a spinner, the other is a phone with nothing on it.
     */
    val folders: StateFlow<List<LibraryFolder>?> = combine(
        mediaRepository.getFoldersFlow(),
        // Asked once, when the screen starts listening. The store's own folders do not come and go
        // while a settings page is open, and watching for it would re-run this on every tick of a
        // media scan.
        flow { emit(musicLibrary.folders()) },
    ) { videoFolders, audioFolders ->
        val fromVideo = videoFolders.map { LibraryFolder(name = it.name, path = it.path) }
        val known = videoFolders.mapTo(HashSet()) { it.path }
        val fromAudio = audioFolders.asSequence()
            .filterNot { it in known }
            .map { LibraryFolder(name = it.substringAfterLast('/'), path = it) }
        (fromVideo + fromAudio).sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SubscriptionTimeout), null)

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

    /** Both libraries, because the row says the library and a reader means all of it. */
    fun rescanLibrary() {
        viewModelScope.launch { synchronizer.refresh() }
        musicLibrary.rescan()
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

/** A folder the reader may keep out, named and addressed, whichever library turned it up. */
data class LibraryFolder(val name: String, val path: String)

/** Long enough to survive a rotation, short enough that leaving the screen stops the query. */
private const val SubscriptionTimeout = 5_000L
