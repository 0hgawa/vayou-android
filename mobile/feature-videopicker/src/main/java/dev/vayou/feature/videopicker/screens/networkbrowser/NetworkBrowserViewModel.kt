package dev.vayou.feature.videopicker.screens.networkbrowser

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.BrowserPreferencesStore
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.FavoritesStore
import dev.vayou.core.smb.FolderFavoritesStore
import dev.vayou.core.smb.IptvCountry
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.smb.PlaylistStore
import dev.vayou.core.smb.parseM3U
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.SavedSmbServer
import dev.vayou.core.smb.SmbClient
import dev.vayou.core.smb.SmbCredentials
import dev.vayou.core.smb.SmbDiscovery
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.smb.SmbServer
import dev.vayou.core.smb.SmbServerStore
import dev.vayou.core.smb.SmbShare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

private const val IPTV_FIXED_NAME = "Canais ao vivo"

@HiltViewModel
class NetworkBrowserViewModel @Inject constructor(
    private val smbClient: SmbClient,
    private val smbDiscovery: SmbDiscovery,
    private val smbServerStore: SmbServerStore,
    private val playlistStore: PlaylistStore,
    private val favoritesStore: FavoritesStore,
    private val folderFavoritesStore: FolderFavoritesStore,
    private val browserPreferencesStore: BrowserPreferencesStore,
) : ViewModel() {

    val browserSort: StateFlow<BrowserSort> = browserPreferencesStore.browserSort

    fun setBrowserSort(sort: BrowserSort) {
        viewModelScope.launch { browserPreferencesStore.setBrowserSort(sort) }
    }

    val favoriteChannels: StateFlow<List<PlaylistChannel>> = favoritesStore.favoritesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val favoriteUrls: StateFlow<Set<String>> = favoritesStore.favoriteUrlsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet(),
    )

    val folderFavorites: StateFlow<List<FavoriteFolder>> = folderFavoritesStore.favoritesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _uiState = MutableStateFlow(NetworkBrowserUiState())
    val uiState: StateFlow<NetworkBrowserUiState> = _uiState.asStateFlow()

    private var discoveryJob: Job? = null

    init {
        viewModelScope.launch {
            smbServerStore.savedServersFlow.collect { servers ->
                _uiState.update { it.copy(savedServers = servers) }
            }
        }
        viewModelScope.launch {
            playlistStore.playlistsFlow.collect { playlists ->
                _uiState.update { it.copy(savedPlaylists = playlists) }
            }
        }
        viewModelScope.launch { playlistStore.seedDefaultsIfNeeded() }
        startDiscovery()
    }

    fun startDiscovery(forceRefresh: Boolean = false) {
        _uiState.update { it.copy(screen = NetworkScreen.ServerList, error = null) }

        if (!forceRefresh && _uiState.value.discoveredServers.isNotEmpty()) return

        discoveryJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }

        discoveryJob = viewModelScope.launch {
            smbDiscovery.discoverServers().collect { servers ->
                _uiState.update { it.copy(discoveredServers = servers, isLoading = false) }
            }
        }
    }

    fun connectToServer(host: String) {
        _uiState.update { it.copy(screen = NetworkScreen.Connecting, isLoading = true, error = null, currentHost = host) }

        viewModelScope.launch {
            if (!smbDiscovery.isReachable(host)) {
                _uiState.update { it.copy(screen = NetworkScreen.ServerList, isLoading = false, error = "Server not found on this network") }
                return@launch
            }
            if (connectWithSavedOrGuest(host)) {
                loadShares(host)
            } else {
                _uiState.update { it.copy(screen = NetworkScreen.Auth, isLoading = false) }
            }
        }
    }

    private suspend fun connectWithSavedOrGuest(host: String): Boolean {
        val creds = smbServerStore.getCredentials(host)
        if (creds != null && creds.username.isNotBlank() &&
            smbClient.connect(host, creds.username, creds.password, creds.domain).isSuccess
        ) return true
        if (smbClient.connectAsGuest(host).isSuccess) {
            smbServerStore.saveServer(host, host, "", "", "")
            return true
        }
        return false
    }

    fun onCredentialsSubmit(username: String, password: String, displayName: String) {
        val host = _uiState.value.currentHost ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = smbClient.connect(host, username, password, "")
            if (result.isSuccess) {
                smbServerStore.saveServer(host, displayName.ifBlank { host }, username, password, "")
                loadShares(host)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Authentication failed",
                    )
                }
            }
        }
    }

    fun connectToShare(shareName: String) {
        val host = _uiState.value.currentHost ?: return
        _uiState.update { it.copy(currentShare = shareName, currentPath = "", pathStack = emptyList()) }
        loadDirectory(host, shareName, "")
    }

    fun navigateInto(item: SmbFileItem) {
        if (!item.isDirectory) return
        val host = _uiState.value.currentHost ?: return
        val share = _uiState.value.currentShare ?: return
        _uiState.update { it.copy(pathStack = it.pathStack + it.currentPath) }
        loadDirectory(host, share, item.path)
    }

    /** Jump to an ancestor via breadcrumb. [segmentIndex] = -1 jumps to share root. */
    fun navigateToSegment(segmentIndex: Int) {
        val host = _uiState.value.currentHost ?: return
        val share = _uiState.value.currentShare ?: return
        val segments = _uiState.value.currentPath.split('\\').filter { it.isNotBlank() }
        if (segmentIndex >= segments.size) return
        val newPath = if (segmentIndex < 0) "" else segments.take(segmentIndex + 1).joinToString("\\")
        val newStack = if (segmentIndex < 0) emptyList() else List(segmentIndex + 1) { i ->
            if (i == 0) "" else segments.take(i).joinToString("\\")
        }
        _uiState.update { it.copy(pathStack = newStack) }
        loadDirectory(host, share, newPath)
    }

    /** Returns true if back was handled, false if already at root */
    fun navigateUp(): Boolean {
        val state = _uiState.value
        return when {
            state.pathStack.isNotEmpty() -> {
                val previousPath = state.pathStack.last()
                _uiState.update { it.copy(pathStack = it.pathStack.dropLast(1)) }
                loadDirectory(state.currentHost!!, state.currentShare!!, previousPath)
                true
            }
            state.screen == NetworkScreen.FileBrowser -> {
                _uiState.update { it.copy(screen = NetworkScreen.ShareList) }
                true
            }
            state.screen == NetworkScreen.PlaylistDetail -> {
                _uiState.update { it.copy(screen = NetworkScreen.ServerList) }
                true
            }
            state.screen == NetworkScreen.IptvFavorites -> {
                _uiState.update { it.copy(screen = NetworkScreen.ServerList) }
                true
            }
            state.screen == NetworkScreen.FolderFavorites -> {
                _uiState.update { it.copy(screen = NetworkScreen.ServerList) }
                true
            }
            state.screen == NetworkScreen.ShareList || state.screen == NetworkScreen.Auth || state.screen == NetworkScreen.Connecting -> {
                viewModelScope.launch(Dispatchers.IO) { smbClient.disconnect() }
                startDiscovery()
                true
            }
            else -> false
        }
    }

    fun removeServer(host: String) {
        viewModelScope.launch { smbServerStore.removeServer(host) }
    }

    fun openEditServer(host: String, displayName: String) {
        viewModelScope.launch {
            val creds = smbServerStore.getCredentials(host) ?: SmbCredentials()
            _uiState.update {
                it.copy(editingServer = EditServerState(host, displayName, creds.username, creds.password, creds.domain))
            }
        }
    }

    fun dismissEditServer() = _uiState.update { it.copy(editingServer = null) }

    fun saveEditServer(displayName: String, username: String, password: String, domain: String) {
        val host = _uiState.value.editingServer?.host ?: return
        viewModelScope.launch {
            smbServerStore.saveServer(host, displayName.ifBlank { host }, username, password, domain)
            _uiState.update { it.copy(editingServer = null) }
        }
    }

    fun openFavorites() {
        _uiState.update { it.copy(screen = NetworkScreen.IptvFavorites, error = null) }
    }

    fun openFolderFavorites() {
        _uiState.update { it.copy(screen = NetworkScreen.FolderFavorites, error = null) }
    }

    fun openFavoriteFolder(favorite: FavoriteFolder) {
        _uiState.update {
            it.copy(
                screen = NetworkScreen.Connecting,
                isLoading = true,
                error = null,
                currentHost = favorite.host,
                currentShare = favorite.share,
                pathStack = emptyList(),
            )
        }
        viewModelScope.launch {
            if (!smbDiscovery.isReachable(favorite.host)) {
                _uiState.update { it.copy(screen = NetworkScreen.FolderFavorites, isLoading = false, error = "Servidor fora da rede") }
                return@launch
            }
            if (connectWithSavedOrGuest(favorite.host)) {
                loadDirectory(favorite.host, favorite.share, favorite.path)
            } else {
                _uiState.update { it.copy(screen = NetworkScreen.FolderFavorites, isLoading = false, error = "Falha ao conectar") }
            }
        }
    }

    fun toggleFolderFavorite(item: SmbFileItem) {
        if (!item.isDirectory) return
        val host = _uiState.value.currentHost ?: return
        val share = _uiState.value.currentShare ?: return
        val displayName = item.name.ifBlank { item.path.substringAfterLast('\\').substringAfterLast('/') }
        viewModelScope.launch {
            folderFavoritesStore.toggle(FavoriteFolder(host, share, item.path, displayName))
        }
    }

    fun toggleShareFavorite(share: SmbShare) {
        val host = _uiState.value.currentHost ?: return
        viewModelScope.launch {
            folderFavoritesStore.toggle(FavoriteFolder(host, share.name, "", share.name))
        }
    }

    fun renameFolderFavorite(favorite: FavoriteFolder, newName: String) {
        viewModelScope.launch { folderFavoritesStore.rename(favorite.host, favorite.share, favorite.path, newName) }
    }

    fun removeFolderFavorite(favorite: FavoriteFolder) {
        viewModelScope.launch { folderFavoritesStore.remove(favorite.host, favorite.share, favorite.path) }
    }

    fun openPlaylist(playlist: SavedPlaylist) {
        loadPlaylist(name = playlist.name, url = playlist.url)
    }

    fun switchIptvCountry(code: String?) {
        val newUrl = if (code.isNullOrBlank()) IptvCountry.GLOBAL_URL
                     else "${IptvCountry.COUNTRY_PREFIX}${code.lowercase()}.m3u"
        if (newUrl == _uiState.value.currentPlaylistUrl) return
        viewModelScope.launch {
            playlistStore.setIptvCountry(code, IPTV_FIXED_NAME)
        }
        loadPlaylist(name = _uiState.value.currentPlaylistName ?: IPTV_FIXED_NAME, url = newUrl)
    }

    private fun loadPlaylist(name: String, url: String) {
        _uiState.update {
            it.copy(
                screen = NetworkScreen.PlaylistDetail,
                isLoading = true,
                error = null,
                currentPlaylistName = name,
                currentPlaylistUrl = url,
                playlistChannels = emptyList(),
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    URL(url).openStream().bufferedReader().use { it.readText() }
                }.let { parseM3U(it) }
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    playlistChannels = result.getOrDefault(emptyList()),
                    error = if (result.isFailure) result.exceptionOrNull()?.message ?: "Failed to load playlist" else null,
                )
            }
        }
    }

    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch { playlistStore.add(name, url) }
    }

    fun removePlaylist(url: String) {
        viewModelScope.launch { playlistStore.remove(url) }
    }

    fun toggleFavorite(channel: PlaylistChannel) {
        viewModelScope.launch { favoritesStore.toggle(channel) }
    }

    fun renamePlaylist(url: String, newName: String) {
        viewModelScope.launch { playlistStore.rename(url, newName) }
    }

    suspend fun startStreaming(item: SmbFileItem): Pair<Uri, List<Uri>> {
        val share = _uiState.value.currentShare ?: return Uri.EMPTY to emptyList()
        return smbClient.startStreaming(share, item.path, item.name).getOrNull() ?: (Uri.EMPTY to emptyList())
    }

    private fun loadShares(host: String) {
        _uiState.update { it.copy(screen = NetworkScreen.ShareList, isLoading = true, shares = emptyList(), currentHost = host) }
        viewModelScope.launch {
            val result = smbClient.listShares()
            _uiState.update {
                it.copy(
                    shares = result.getOrDefault(emptyList()),
                    isLoading = false,
                    error = if (result.isFailure) result.exceptionOrNull()?.message else null,
                )
            }
        }
    }

    private fun loadDirectory(host: String, share: String, path: String) {
        _uiState.update {
            it.copy(
                screen = NetworkScreen.FileBrowser,
                isLoading = true,
                error = null,
                currentPath = path,
                currentHost = host,
                currentShare = share,
            )
        }
        viewModelScope.launch {
            val result = smbClient.listDirectory(share, path)
            result.onSuccess { items ->
                _uiState.update { it.copy(files = items, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to list directory") }
            }
        }
    }

    override fun onCleared() {
        CoroutineScope(Dispatchers.IO).launch { smbClient.disconnect() }
    }

}

data class NetworkBrowserUiState(
    val screen: NetworkScreen = NetworkScreen.ServerList,
    val isLoading: Boolean = false,
    val error: String? = null,
    val discoveredServers: List<SmbServer> = emptyList(),
    val savedServers: List<SavedSmbServer> = emptyList(),
    val savedPlaylists: List<SavedPlaylist> = emptyList(),
    val shares: List<SmbShare> = emptyList(),
    val files: List<SmbFileItem> = emptyList(),
    val currentHost: String? = null,
    val currentShare: String? = null,
    val currentPath: String = "",
    val pathStack: List<String> = emptyList(),
    val editingServer: EditServerState? = null,
    val currentPlaylistName: String? = null,
    val currentPlaylistUrl: String? = null,
    val playlistChannels: List<PlaylistChannel> = emptyList(),
)

data class EditServerState(
    val host: String,
    val displayName: String,
    val username: String,
    val password: String,
    val domain: String,
)

enum class NetworkScreen {
    ServerList,
    Connecting,
    Auth,
    ShareList,
    FileBrowser,
    PlaylistDetail,
    IptvFavorites,
    FolderFavorites,
}
