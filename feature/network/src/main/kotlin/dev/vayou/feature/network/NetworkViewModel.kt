package dev.vayou.feature.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.BrowserSortStore
import dev.vayou.core.smb.ChannelFavouritesStore
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.FolderFavouritesStore
import dev.vayou.core.smb.IptvCountry
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.smb.PlaylistStore
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.SavedSmbServer
import dev.vayou.core.smb.SmbClient
import dev.vayou.core.smb.SmbCredentials
import dev.vayou.core.smb.SmbDiscovery
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.smb.SmbServer
import dev.vayou.core.smb.SmbServerStore
import dev.vayou.core.smb.SmbShare
import dev.vayou.core.smb.StreamingUris
import dev.vayou.core.smb.parseM3U
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the app arrives with, and what the country picker keeps calling the list it swaps. */
private const val LivePlaylistName = "Channels"

/**
 * Everything that is not on this phone: the shares on the network, and the channel lists.
 *
 * One ViewModel for both, because both are the same act -- reaching something over a wire -- and
 * because a single connection is what the SMB client holds. Split in two, the tab nobody is looking
 * at would still own a session.
 */
@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val smbClient: SmbClient,
    private val discovery: SmbDiscovery,
    private val serverStore: SmbServerStore,
    private val playlistStore: PlaylistStore,
    private val channelFavourites: ChannelFavouritesStore,
    private val folderFavourites: FolderFavouritesStore,
    private val browserSortStore: BrowserSortStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    val browserSort: StateFlow<BrowserSort> = browserSortStore.sort

    val favouriteChannels: StateFlow<List<PlaylistChannel>> = channelFavourites.favourites.held(emptyList())

    val favouriteChannelUrls: StateFlow<Set<String>> = channelFavourites.favouriteUrls.held(emptySet())

    val favouriteFolders: StateFlow<List<FavoriteFolder>> = folderFavourites.favourites.held(emptyList())

    private var discoveryJob: Job? = null

    init {
        viewModelScope.launch {
            serverStore.savedServers.collect { servers -> _uiState.update { it.copy(savedServers = servers) } }
        }
        viewModelScope.launch {
            playlistStore.playlists.collect { saved -> _uiState.update { it.copy(savedPlaylists = saved) } }
        }
        viewModelScope.launch { playlistStore.seedDefaultsIfNeeded() }
        scan()
    }

    // ---- servers -------------------------------------------------------------------------------

    fun scan(force: Boolean = false) {
        _uiState.update { it.copy(screen = NetworkScreen.ServerList, error = null) }
        if (!force && _uiState.value.discoveredServers.isNotEmpty()) return

        discoveryJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        discoveryJob = viewModelScope.launch {
            discovery.discover().collect { servers ->
                _uiState.update { it.copy(discoveredServers = servers, isLoading = false) }
            }
        }
    }

    fun connectTo(host: String) {
        _uiState.update {
            it.copy(screen = NetworkScreen.Connecting, isLoading = true, error = null, host = host)
        }
        viewModelScope.launch {
            if (!discovery.isReachable(host)) {
                fail(NetworkScreen.ServerList, NetworkError.NotOnThisNetwork)
                return@launch
            }
            // Credentials first, then guest. A share open to everyone should not put a password box
            // in front of someone who does not have one.
            if (connectWithSavedOrGuest(host)) {
                loadShares(host)
            } else {
                _uiState.update { it.copy(screen = NetworkScreen.Auth, isLoading = false) }
            }
        }
    }

    private suspend fun connectWithSavedOrGuest(host: String): Boolean {
        val saved = serverStore.credentials(host)
        if (saved != null &&
            saved.username.isNotBlank() &&
            smbClient.connect(host, saved.username, saved.password, saved.domain).isSuccess
        ) {
            return true
        }
        if (smbClient.connectAsGuest(host).isSuccess) {
            // Remembered with no credentials, so the address survives the next scan finding nothing.
            serverStore.save(host, host, username = "", password = "", domain = "")
            return true
        }
        return false
    }

    fun submitCredentials(username: String, password: String, displayName: String) {
        val host = _uiState.value.host ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            if (smbClient.connect(host, username, password).isSuccess) {
                serverStore.save(host, displayName.ifBlank { host }, username, password, domain = "")
                loadShares(host)
            } else {
                _uiState.update { it.copy(isLoading = false, error = NetworkError.WrongCredentials) }
            }
        }
    }

    fun openShare(share: SmbShare) {
        val host = _uiState.value.host ?: return
        _uiState.update { it.copy(share = share.name, path = "", pathStack = emptyList()) }
        loadDirectory(host, share.name, path = "")
    }

    fun openDirectory(item: SmbFileItem) {
        if (!item.isDirectory) return
        val state = _uiState.value
        val host = state.host ?: return
        val share = state.share ?: return
        _uiState.update { it.copy(pathStack = it.pathStack + it.path) }
        loadDirectory(host, share, item.path)
    }

    /** Jumps to an ancestor from the breadcrumb. -1 is the root of the share. */
    fun openAncestor(segmentIndex: Int) {
        val state = _uiState.value
        val host = state.host ?: return
        val share = state.share ?: return
        val segments = state.path.pathSegments
        if (segmentIndex >= segments.size) return

        val path = if (segmentIndex < 0) "" else segments.take(segmentIndex + 1).joinToString("\\")
        // The stack has to be rebuilt, not trimmed: jumping five levels up leaves five entries that
        // no longer describe the way back.
        val stack = if (segmentIndex < 0) {
            emptyList()
        } else {
            List(segmentIndex + 1) { level -> segments.take(level).joinToString("\\") }
        }
        _uiState.update { it.copy(pathStack = stack) }
        loadDirectory(host, share, path)
    }

    /** True when there was somewhere to go; false means this screen is the root and the app is not. */
    fun navigateUp(): Boolean {
        val state = _uiState.value
        return when {
            // Checked before the stack is touched: popping it and then finding nowhere to go
            // would leave the trail one step shorter than the folder it describes.
            state.pathStack.isNotEmpty() && state.host != null && state.share != null -> {
                val previous = state.pathStack.last()
                _uiState.update { it.copy(pathStack = it.pathStack.dropLast(1)) }
                loadDirectory(state.host, state.share, previous)
                true
            }

            state.screen == NetworkScreen.FileBrowser -> {
                _uiState.update { it.copy(screen = NetworkScreen.ShareList) }
                true
            }

            state.screen in ScreensAboveTheServerList -> {
                _uiState.update { it.copy(screen = NetworkScreen.ServerList) }
                true
            }

            state.screen in ScreensHoldingAConnection -> {
                viewModelScope.launch(Dispatchers.IO) { smbClient.disconnect() }
                scan()
                true
            }

            else -> false
        }
    }

    fun forgetServer(host: String) {
        viewModelScope.launch { serverStore.remove(host) }
    }

    fun editServer(host: String, displayName: String) {
        viewModelScope.launch {
            val credentials = serverStore.credentials(host) ?: SmbCredentials()
            _uiState.update {
                it.copy(
                    editingServer = EditingServer(
                        host = host,
                        displayName = displayName,
                        username = credentials.username,
                        password = credentials.password,
                        domain = credentials.domain,
                    ),
                )
            }
        }
    }

    fun dismissEditServer() = _uiState.update { it.copy(editingServer = null) }

    fun saveEditedServer(displayName: String, username: String, password: String, domain: String) {
        val host = _uiState.value.editingServer?.host ?: return
        viewModelScope.launch {
            serverStore.save(host, displayName.ifBlank { host }, username, password, domain)
            _uiState.update { it.copy(editingServer = null) }
        }
    }

    // ---- favourites ----------------------------------------------------------------------------

    fun openChannelFavourites() = _uiState.update {
        it.copy(screen = NetworkScreen.ChannelFavourites, error = null)
    }

    fun openFolderFavourites() = _uiState.update {
        it.copy(screen = NetworkScreen.FolderFavourites, error = null)
    }

    fun openFavouriteFolder(favourite: FavoriteFolder) {
        _uiState.update {
            it.copy(
                screen = NetworkScreen.Connecting,
                isLoading = true,
                error = null,
                host = favourite.host,
                share = favourite.share,
                pathStack = emptyList(),
            )
        }
        viewModelScope.launch {
            if (!discovery.isReachable(favourite.host)) {
                fail(NetworkScreen.FolderFavourites, NetworkError.NotOnThisNetwork)
                return@launch
            }
            if (connectWithSavedOrGuest(favourite.host)) {
                loadDirectory(favourite.host, favourite.share, favourite.path)
            } else {
                fail(NetworkScreen.FolderFavourites, NetworkError.WrongCredentials)
            }
        }
    }

    fun toggleFolderFavourite(item: SmbFileItem) {
        if (!item.isDirectory) return
        val state = _uiState.value
        val host = state.host ?: return
        val share = state.share ?: return
        viewModelScope.launch {
            folderFavourites.toggle(FavoriteFolder(host, share, item.path, item.name))
        }
    }

    fun toggleShareFavourite(share: SmbShare) {
        val host = _uiState.value.host ?: return
        viewModelScope.launch {
            folderFavourites.toggle(FavoriteFolder(host, share.name, path = "", displayName = share.name))
        }
    }

    fun renameFolderFavourite(favourite: FavoriteFolder, newName: String) {
        viewModelScope.launch {
            folderFavourites.rename(favourite.host, favourite.share, favourite.path, newName)
        }
    }

    fun removeFolderFavourite(favourite: FavoriteFolder) {
        viewModelScope.launch { folderFavourites.remove(favourite.host, favourite.share, favourite.path) }
    }

    fun setChannelsFavourite(channels: List<PlaylistChannel>, isFavourite: Boolean) {
        viewModelScope.launch { channelFavourites.setFavourite(channels, isFavourite) }
    }

    fun toggleChannelFavourite(channel: PlaylistChannel) {
        viewModelScope.launch { channelFavourites.toggle(channel) }
    }

    // ---- playlists -----------------------------------------------------------------------------

    fun openPlaylist(playlist: SavedPlaylist) = loadPlaylist(playlist.name, playlist.url)

    fun switchCountry(code: String?) {
        val url = IptvCountry(code).url
        if (url == _uiState.value.playlistUrl) return
        viewModelScope.launch { playlistStore.setIptvCountry(code, LivePlaylistName) }
        loadPlaylist(_uiState.value.playlistName ?: LivePlaylistName, url)
    }

    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch { playlistStore.add(name, url) }
    }

    fun removePlaylist(url: String) {
        viewModelScope.launch { playlistStore.remove(url) }
    }

    fun renamePlaylist(url: String, newName: String) {
        viewModelScope.launch { playlistStore.rename(url, newName) }
    }

    private fun loadPlaylist(name: String, url: String) {
        _uiState.update {
            it.copy(
                screen = NetworkScreen.Playlist,
                isLoading = true,
                error = null,
                playlistName = name,
                playlistUrl = url,
                channels = emptyList(),
            )
        }
        viewModelScope.launch {
            val channels = runCatching {
                withContext(Dispatchers.IO) { URL(url).openStream().bufferedReader().use { it.readText() } }
            }.map(::parseM3U)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    channels = channels.getOrDefault(emptyList()),
                    error = if (channels.isFailure) NetworkError.PlaylistUnreachable else null,
                )
            }
        }
    }

    // ---- playback ------------------------------------------------------------------------------

    fun setBrowserSort(sort: BrowserSort) {
        viewModelScope.launch { browserSortStore.setSort(sort) }
    }

    /** Null when the share went away between the listing and the tap. */
    suspend fun streamingUris(item: SmbFileItem): StreamingUris? {
        val share = _uiState.value.share ?: return null
        return smbClient.streamingUris(share, item.path, item.name).getOrNull()
    }

    // ---- loading -------------------------------------------------------------------------------

    private fun loadShares(host: String) {
        _uiState.update {
            it.copy(screen = NetworkScreen.ShareList, isLoading = true, shares = emptyList(), host = host)
        }
        viewModelScope.launch {
            val shares = smbClient.listShares()
            _uiState.update {
                it.copy(
                    shares = shares.getOrDefault(emptyList()),
                    isLoading = false,
                    error = if (shares.isFailure) NetworkError.CannotList else null,
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
                host = host,
                share = share,
                path = path,
            )
        }
        viewModelScope.launch {
            val files = smbClient.listDirectory(share, path)
            _uiState.update {
                it.copy(
                    files = files.getOrDefault(emptyList()),
                    isLoading = false,
                    error = if (files.isFailure) NetworkError.CannotList else null,
                )
            }
        }
    }

    private fun fail(screen: NetworkScreen, error: NetworkError) = _uiState.update {
        it.copy(screen = screen, isLoading = false, error = error)
    }

    override fun onCleared() {
        // Not viewModelScope: it is cancelled by the time this runs, and the session has to be told
        // to close on a scope that outlives the screen.
        CoroutineScope(Dispatchers.IO).launch { smbClient.disconnect() }
    }

    private fun <T> Flow<T>.held(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(SubscriptionGraceMs), initial)
}

/** Long enough to survive a rotation, short enough that a backgrounded screen stops collecting. */
private const val SubscriptionGraceMs = 5_000L

data class NetworkUiState(
    val screen: NetworkScreen = NetworkScreen.ServerList,
    val isLoading: Boolean = false,
    val error: NetworkError? = null,
    val discoveredServers: List<SmbServer> = emptyList(),
    val savedServers: List<SavedSmbServer> = emptyList(),
    val savedPlaylists: List<SavedPlaylist> = emptyList(),
    val shares: List<SmbShare> = emptyList(),
    val files: List<SmbFileItem> = emptyList(),
    val host: String? = null,
    val share: String? = null,
    val path: String = "",
    /** Where each step back goes. Grows on the way down and is rebuilt on a jump. */
    val pathStack: List<String> = emptyList(),
    val editingServer: EditingServer? = null,
    val playlistName: String? = null,
    val playlistUrl: String? = null,
    val channels: List<PlaylistChannel> = emptyList(),
)

data class EditingServer(
    val host: String,
    val displayName: String,
    val username: String,
    val password: String,
    val domain: String,
)

/**
 * What went wrong, as a value rather than a sentence.
 *
 * The message a server throws is in whatever language the library was written in -- usually
 * English, sometimes a status code. Naming the four things that actually go wrong lets the screen
 * say them in the reader's own.
 */
enum class NetworkError { NotOnThisNetwork, WrongCredentials, CannotList, PlaylistUnreachable }

enum class NetworkScreen {
    ServerList,
    Connecting,
    Auth,
    ShareList,
    FileBrowser,
    Playlist,
    ChannelFavourites,
    FolderFavourites,
}

/** Reached from the server list, and returning to it. */
private val ScreensAboveTheServerList = setOf(
    NetworkScreen.Playlist,
    NetworkScreen.ChannelFavourites,
    NetworkScreen.FolderFavourites,
)

/** Holding a session that has to be closed on the way out. */
private val ScreensHoldingAConnection = setOf(
    NetworkScreen.ShareList,
    NetworkScreen.Auth,
    NetworkScreen.Connecting,
)

/** A Windows path as its parts, with no empties from a leading or doubled separator. */
internal val String.pathSegments: List<String>
    get() = split('\\').filter { it.isNotBlank() }
