package dev.vayou.tv.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.BrowserPreferencesStore
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.FolderFavoritesStore
import dev.vayou.core.smb.SmbClient
import dev.vayou.core.smb.SmbCredentials
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.smb.SmbServerStore
import dev.vayou.core.smb.SmbShare
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SmbBrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val smbClient: SmbClient,
    private val serverStore: SmbServerStore,
    private val folderFavoritesStore: FolderFavoritesStore,
    private val browserPreferencesStore: BrowserPreferencesStore,
) : ViewModel() {

    val browserSort: StateFlow<BrowserSort> = browserPreferencesStore.browserSort

    fun setBrowserSort(sort: BrowserSort) {
        viewModelScope.launch { browserPreferencesStore.setBrowserSort(sort) }
    }

    val host: String = checkNotNull(savedStateHandle[HostArg])
    private val initialShare: String? = savedStateHandle.get<String?>(ShareArg)?.takeIf { it.isNotBlank() }
    private val initialPath: String = savedStateHandle.get<String?>(PathArg).orEmpty()

    private val _state = MutableStateFlow(SmbBrowserState(host = host, status = SmbStatus.Connecting))
    val state: StateFlow<SmbBrowserState> = _state.asStateFlow()
    private var loadJob: Job? = null

    val favoritePaths: StateFlow<Set<String>> = folderFavoritesStore.favoritesFlow
        .map { list -> list.filter { it.host == host }.mapTo(mutableSetOf()) { "${it.share}|${it.path}" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleFolderFavorite(item: SmbFileItem) {
        if (!item.isDirectory) return
        val share = _state.value.shareName ?: return
        viewModelScope.launch {
            folderFavoritesStore.toggle(
                FavoriteFolder(host = host, share = share, path = item.path, displayName = item.name),
            )
        }
    }

    fun renameFolderFavorite(item: SmbFileItem, newName: String) {
        if (!item.isDirectory) return
        val share = _state.value.shareName ?: return
        viewModelScope.launch {
            folderFavoritesStore.rename(host, share, item.path, newName)
        }
    }

    fun toggleShareFavorite(share: SmbShare) {
        viewModelScope.launch {
            folderFavoritesStore.toggle(
                FavoriteFolder(host = host, share = share.name, path = "", displayName = share.name),
            )
        }
    }

    fun renameShareFavorite(share: SmbShare, newName: String) {
        viewModelScope.launch {
            folderFavoritesStore.rename(host, share.name, "", newName)
        }
    }

    init {
        viewModelScope.launch {
            val saved = serverStore.getCredentials(host)
            if (saved != null) {
                connectAndLoadShares(saved)
            } else {
                _state.update { it.copy(status = SmbStatus.NeedsAuth) }
            }
        }
    }

    fun submitCredentials(displayName: String, username: String, password: String, domain: String) {
        viewModelScope.launch {
            _state.update { it.copy(status = SmbStatus.Connecting, error = null) }
            val creds = SmbCredentials(username = username, password = password, domain = domain)
            val ok = connectAndLoadShares(creds)
            if (ok) {
                serverStore.saveServer(host, displayName.ifBlank { host }, username, password, domain)
            }
        }
    }

    fun openShare(share: SmbShare) {
        loadDirectory(share.name, "")
    }

    fun openDirectory(item: SmbFileItem) {
        val current = _state.value
        val share = current.shareName ?: return
        loadDirectory(share, item.path)
    }

    fun navigateToSegment(segmentIndex: Int) {
        val share = _state.value.shareName ?: return
        val segments = _state.value.path.split('\\').filter { it.isNotBlank() }
        if (segmentIndex >= segments.size) return
        val newPath = if (segmentIndex < 0) "" else segments.take(segmentIndex + 1).joinToString("\\")
        if (newPath == _state.value.path) return
        loadDirectory(share, newPath)
    }

    fun navigateUp() {
        val current = _state.value
        when {
            current.path.isEmpty() && current.shareName != null -> {
                loadJob?.cancel()
                loadJob = viewModelScope.launch {
                    _state.update { it.copy(isLoading = true) }
                    loadShares()
                    _state.update { it.copy(isLoading = false) }
                }
            }
            current.path.isNotEmpty() && current.shareName != null -> {
                val parent = current.path.trimEnd('\\', '/').substringBeforeLast('/', "")
                loadDirectory(current.shareName, parent)
            }
        }
    }

    private fun loadDirectory(shareName: String, path: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = smbClient.listDirectory(shareName, path)
            result.onSuccess { entries ->
                _state.update {
                    it.copy(
                        status = SmbStatus.Browsing,
                        shareName = shareName,
                        path = path,
                        entries = entries.filter { item -> item.isDirectory || item.isVideo },
                        shares = emptyList(),
                        error = null,
                        isLoading = false,
                    )
                }
            }.onFailure { ex ->
                _state.update { it.copy(status = SmbStatus.Browsing, error = ex.message, isLoading = false) }
            }
        }
    }

    private suspend fun connectAndLoadShares(creds: SmbCredentials): Boolean {
        val connect = smbClient.connect(host, creds.username, creds.password, creds.domain)
        return connect.fold(
            onSuccess = {
                if (initialShare != null) {
                    loadDirectory(initialShare, initialPath)
                } else {
                    loadShares()
                }
                true
            },
            onFailure = { ex ->
                _state.update {
                    it.copy(status = SmbStatus.NeedsAuth, error = ex.message ?: "Falha ao conectar")
                }
                false
            },
        )
    }

    private suspend fun loadShares() {
        val result = smbClient.listShares()
        result.onSuccess { shares ->
            _state.update {
                it.copy(
                    status = SmbStatus.Browsing,
                    shareName = null,
                    path = "",
                    shares = shares,
                    entries = emptyList(),
                    error = null,
                )
            }
        }.onFailure { ex ->
            _state.update {
                it.copy(status = SmbStatus.NeedsAuth, error = ex.message ?: "Falha ao listar shares")
            }
        }
    }

    companion object {
        const val HostArg = "host"
        const val ShareArg = "share"
        const val PathArg = "path"
    }
}

enum class SmbStatus { Connecting, NeedsAuth, Browsing }

data class SmbBrowserState(
    val host: String,
    val status: SmbStatus,
    val shareName: String? = null,
    val path: String = "",
    val shares: List<SmbShare> = emptyList(),
    val entries: List<SmbFileItem> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false,
)
