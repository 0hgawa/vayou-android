package dev.vayou.tv.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.FavoritesStore
import dev.vayou.core.smb.IptvCountry
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.smb.PlaylistStore
import dev.vayou.core.smb.parseM3U
import kotlinx.coroutines.Job
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistStore: PlaylistStore,
    private val favoritesStore: FavoritesStore,
) : ViewModel() {

    private val initialUrl: String = checkNotNull(savedStateHandle[UrlArg])

    private val _state = MutableStateFlow(PlaylistDetailState(url = initialUrl))
    val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

    val favoriteUrls: StateFlow<Set<String>> = favoritesStore.favoritesFlow
        .map { list -> list.mapTo(mutableSetOf()) { it.url } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var loadJob: Job? = null

    fun toggleFavorite(channel: PlaylistChannel) {
        viewModelScope.launch { favoritesStore.toggle(channel) }
    }

    fun renameChannel(channel: PlaylistChannel, newName: String) {
        viewModelScope.launch {
            val current = favoritesStore.favoritesFlow.first()
            if (current.any { it.url == channel.url }) {
                favoritesStore.rename(channel.url, newName)
            } else {
                favoritesStore.toggle(channel.copy(name = newName))
            }
        }
    }

    /** Switches the loaded iptv-org playlist to a different country. Keeps the saved tile name fixed. */
    fun switchIptvCountry(countryCode: String?) {
        val newUrl = if (countryCode.isNullOrBlank()) IptvCountry.GLOBAL_URL
                     else "${IptvCountry.COUNTRY_PREFIX}${countryCode.lowercase()}.m3u"
        if (newUrl == _state.value.url) return
        _state.update { it.copy(url = newUrl, channels = emptyList()) }
        viewModelScope.launch {
            playlistStore.setIptvCountry(countryCode, IPTV_FIXED_NAME)
        }
        load()
    }

    init {
        viewModelScope.launch {
            val name = playlistStore.playlistsFlow.first().firstOrNull { it.url == initialUrl }?.name
            _state.update { it.copy(name = name ?: initialUrl) }
            load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val targetUrl = _state.value.url
            _state.update { it.copy(isLoading = true, error = null) }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    URL(targetUrl).openStream().bufferedReader().use { it.readText() }
                }.let(::parseM3U)
            }
            result.fold(
                onSuccess = { channels ->
                    _state.update { it.copy(isLoading = false, channels = channels) }
                    runCatching { playlistStore.updateMetadata(targetUrl, channels.size) }
                },
                onFailure = { ex ->
                    _state.update { it.copy(isLoading = false, error = ex.message ?: "Falha ao carregar lista") }
                },
            )
        }
    }

    companion object {
        const val UrlArg = "url"
        private const val IPTV_FIXED_NAME = "Canais ao vivo"
    }
}

data class PlaylistDetailState(
    val url: String,
    val name: String = "",
    val channels: List<PlaylistChannel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
