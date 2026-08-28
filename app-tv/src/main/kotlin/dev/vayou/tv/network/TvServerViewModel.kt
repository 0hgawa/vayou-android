package dev.vayou.tv.network

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.BrowserSort
import dev.vayou.core.smb.BrowserSortBy
import dev.vayou.core.smb.BrowserSortStore
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.FolderFavouritesStore
import dev.vayou.core.smb.SmbClient
import dev.vayou.core.smb.SmbFileItem
import dev.vayou.core.smb.SmbServerStore
import dev.vayou.core.smb.SmbShare
import dev.vayou.core.smb.sortedBy
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Walking a share from the sofa.
 *
 * The same three steps the phone takes -- reach the machine, list what it offers, walk into one --
 * with the credentials read from the store rather than asked for. A television has no keyboard, and
 * a password typed on a D-pad is a minute of work; a share that will not open as a guest and has no
 * saved credentials says so and stops, and the phone is where it gets set up.
 */
@HiltViewModel
class TvServerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val smbClient: SmbClient,
    private val serverStore: SmbServerStore,
    private val folderFavourites: FolderFavouritesStore,
    private val sortStore: BrowserSortStore,
) : ViewModel() {

    val host: String = Uri.decode(savedStateHandle.get<String>(HostArg) ?: error("Opened with no server"))

    /** Where to land, for a pinned folder opened from the home screen. Empty means the share list. */
    private val openAtShare: String = savedStateHandle[ShareArg] ?: ""
    private val openAtPath: String = savedStateHandle[PathArg] ?: ""

    /**
     * The folders pinned on this machine, by share and path.
     *
     * A set of keys rather than the entries themselves: the only question a card asks is whether it
     * is one of them, and asking it of a list would be a scan per card per frame.
     */
    val pinned: StateFlow<Set<String>> = folderFavourites.favourites
        .map { all -> all.filter { it.host == host }.map { keyOf(it.share, it.path) }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(IdleTimeoutMs), emptySet())

    /**
     * Pins a share or a folder inside one, so a path six levels down is one press from the home
     * screen. The same press unpins it, as starring a film does.
     */
    fun togglePinned(share: String, path: String, displayName: String) {
        viewModelScope.launch {
            folderFavourites.toggle(FavoriteFolder(host, share, path, displayName))
        }
    }

    private val _state = MutableStateFlow(TvServerState())
    val state: StateFlow<TvServerState> = _state.asStateFlow()

    /** Cancelled on the next step, so walking quickly does not leave two listings racing. */
    private var walking: Job? = null

    init {
        // Turning the order over resorts what is already on the screen rather than walking to the
        // machine down the hall a second time. The store is the one the phone's browser writes to:
        // a house has one idea of how its files are listed.
        sortStore.sort
            .onEach { sort -> _state.update { it.copy(sort = sort, entries = it.entries.sortedBy(sort)) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val saved = serverStore.credentials(host)
            val reached = if (saved != null) {
                smbClient.connect(host, saved.username, saved.password, saved.domain)
            } else {
                // Tried before anything is asked for: most of what a television is pointed at is a
                // media folder somebody opened to the house.
                smbClient.connectAsGuest(host)
            }
            when {
                reached.isFailure -> _state.update { it.copy(isLoading = false, needsSignIn = true) }
                // Straight to where the viewer pinned, rather than to the list of shares and a walk
                // back down the path they pinned to avoid.
                openAtShare.isNotEmpty() -> walk(openAtShare, openAtPath)
                else -> loadShares()
            }
        }
    }

    /**
     * Sign in and remember it, so this is asked once per machine rather than once per evening.
     *
     * A television asks at all only because the phone has nothing saved for this address: the two
     * are separate packages and neither can read the other's store.
     */
    fun signIn(username: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, needsSignIn = false, hasFailed = false) }
            val reached = smbClient.connect(host, username, password, domain = "")
            if (reached.isSuccess) {
                serverStore.save(host, host, username, password, domain = "")
                loadShares()
            } else {
                _state.update { it.copy(isLoading = false, needsSignIn = true, hasFailed = true) }
            }
        }
    }

    /**
     * What was stepped into or played last, so walking back lands on it.
     *
     * Held here because the screen does not survive what it opens, and neither does a listing
     * survive being walked out of: both are rebuilt from nothing, and a grid rebuilt from nothing
     * puts the focus on whatever is first -- which, six folders in, is never the one just left.
     *
     * A plain field and not state: nothing is drawn from it, and making it observable would redraw
     * a listing of hundreds each time it changed for a value read once, as the listing arrives.
     */
    var lastOpened: String? = null
        private set

    /** Set by the screen for a file, which it opens itself rather than through this model. */
    fun rememberOpened(path: String) {
        lastOpened = path
    }

    fun openShare(share: SmbShare) {
        lastOpened = null
        walk(share.name, "")
    }

    fun openDirectory(item: SmbFileItem) {
        val share = _state.value.share ?: return
        lastOpened = null
        walk(share, item.path)
    }

    /**
     * Back up one level: out of a folder, or out of the share to the list of shares.
     *
     * Returns false at the top, which is the screen's cue to leave rather than to redraw the same
     * thing again.
     */
    fun goUp(): Boolean {
        val current = _state.value
        val share = current.share ?: return false
        if (current.path.isEmpty()) {
            // The share being left, so the list of shares comes back with it under the focus.
            lastOpened = share
            viewModelScope.launch { loadShares() }
            return true
        }
        lastOpened = current.path
        walk(share, current.path.trimEnd('\\').substringBeforeLast('\\', ""))
        return true
    }

    private fun walk(share: String, path: String) {
        walking?.cancel()
        walking = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, hasFailed = false) }
            smbClient.listDirectory(share, path)
                .onSuccess { entries ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            share = share,
                            path = path,
                            shares = emptyList(),
                            // Folders and what can be played, which is films and music both. The
                            // rest is dropped -- a television has nothing to do with a document,
                            // and a listing of them is a listing to scroll past. The client hands
                            // the listing over in whatever order the share gave, because the order
                            // belongs to whoever shows it.
                            entries = entries
                                .filter { entry -> entry.isDirectory || entry.isPlayable }
                                .sortedBy(sortStore.sort.value),
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoading = false, hasFailed = true) } }
        }
    }

    private suspend fun loadShares() {
        smbClient.listShares()
            .onSuccess { shares ->
                _state.update {
                    it.copy(isLoading = false, share = null, path = "", shares = shares, entries = emptyList())
                }
            }
            .onFailure { _state.update { it.copy(isLoading = false, hasFailed = true) } }
    }

    /**
     * Choosing the axis already in use turns the listing around, as it does on the phone: a second
     * press on it means nothing else.
     */
    fun selectSort(by: BrowserSortBy) {
        viewModelScope.launch {
            val current = sortStore.sort.value
            sortStore.setSort(
                if (current.by == by) current.copy(isAscending = !current.isAscending) else current.copy(by = by),
            )
        }
    }

    /** The address of a file on the share, once it is open for reading. */
    suspend fun addressOf(item: SmbFileItem): String? {
        val share = _state.value.share ?: return null
        return smbClient.streamingUris(share, item.path, item.name).getOrNull()?.media?.toString()
    }

    companion object {
        const val HostArg = "host"

        const val ShareArg = "share"

        const val PathArg = "path"
    }
}

data class TvServerState(
    val isLoading: Boolean = true,
    val hasFailed: Boolean = false,
    /** The machine answered but would not have us as we are. */
    val needsSignIn: Boolean = false,
    val sort: BrowserSort = BrowserSort(),
    /** Null while the shares themselves are what is listed. */
    val share: String? = null,
    val path: String = "",
    val shares: List<SmbShare> = emptyList(),
    val entries: List<SmbFileItem> = emptyList(),
)

/** What names a pinned place on one machine: the share, and the path inside it. */
fun keyOf(share: String, path: String) = "$share\\$path"

private const val IdleTimeoutMs = 5_000L
