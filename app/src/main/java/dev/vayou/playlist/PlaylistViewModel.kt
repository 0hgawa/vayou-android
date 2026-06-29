package dev.vayou.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.PlaylistRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.Playlist
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val preferences: StateFlow<ApplicationPreferences> = preferencesRepository.applicationPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApplicationPreferences())

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlistRepository.createPlaylist(name) }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { playlistRepository.deletePlaylist(id) }
    }

    fun deletePlaylists(ids: Collection<String>) {
        viewModelScope.launch { ids.forEach { playlistRepository.deletePlaylist(it) } }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch { playlistRepository.renamePlaylist(id, name) }
    }

    fun toggleLayoutMode() {
        viewModelScope.launch { preferencesRepository.toggleMediaLayoutMode() }
    }
}
