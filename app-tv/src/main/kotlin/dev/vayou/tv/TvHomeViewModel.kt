package dev.vayou.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.model.Video
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.FolderFavouritesStore
import dev.vayou.core.smb.NetworkServerEntry
import dev.vayou.core.smb.PlaylistStore
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.SmbDiscovery
import dev.vayou.core.smb.SmbServerStore
import dev.vayou.core.smb.mergeNetworkServers
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the television opens on.
 *
 * Rows and no browsing: from three metres a viewer is picking up where they left off or
 * going to a place they already saved, and a folder tree is a thing to be spared. Anything not on
 * this screen is one press away behind the row that names it.
 */
@HiltViewModel
class TvHomeViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    private val smbServerStore: SmbServerStore,
    private val playlistStore: PlaylistStore,
    discovery: SmbDiscovery,
    private val folderFavourites: FolderFavouritesStore,
) : ViewModel() {

    /**
     * Keeps a machine somebody typed the address of, before going to it.
     *
     * Kept rather than merely visited, which is where this differs from the phone. An address typed
     * on a D-pad costs a minute, and a machine that discovery cannot see -- on another subnet, or
     * with its announcements off -- would have to be typed again every evening. Saved with no
     * credentials: if the share wants a password the sign-in screen fills them into this same entry.
     */
    fun rememberServer(host: String) {
        viewModelScope.launch {
            smbServerStore.save(host = host, displayName = host, username = "", password = "", domain = "")
        }
    }

    /**
     * Forgets one, with whatever password was kept for it.
     *
     * The other half of being able to add one. A machine typed by hand that turned out to be the
     * wrong address, or one that has gone for good, would otherwise sit on the home screen for ever
     * with no way to take it off -- the phone can, and a television that needs the phone to undo
     * what it just did is a television that should not have offered.
     *
     * A machine merely found on the wire is not forgotten, because it was never remembered: it comes
     * back the moment discovery sees it again.
     */
    fun forgetServer(host: String) {
        viewModelScope.launch { smbServerStore.remove(host) }
    }

    /** A channel list, added where its row is rather than on the screen the row opens. */
    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch { playlistStore.add(name, url) }
    }

    fun removePlaylist(url: String) {
        viewModelScope.launch { playlistStore.remove(url) }
    }

    /** Taken off the home screen. The folder itself is untouched; only the shortcut to it goes. */
    fun unpinFolder(folder: FavoriteFolder) {
        viewModelScope.launch { folderFavourites.remove(folder.host, folder.share, folder.path) }
    }

    val state: StateFlow<TvHomeState> = combine(
        mediaRepository.getVideosFlow(),
        // Saved and found on the wire as one list. A television that has never been set up has
        // nothing saved, and a row that waits for the phone to save something first would never
        // appear at all.
        combine(smbServerStore.savedServers, discovery.discover(), ::mergeNetworkServers),
        playlistStore.playlists,
        folderFavourites.favourites,
    ) { videos, servers, playlists, folders ->
        TvHomeState(
            recent = videos.asSequence()
                .filter { it.lastPlayedAt != null }
                .sortedByDescending { it.lastPlayedAt }
                .take(MaxRow)
                .toList(),
            // The library itself, and not only what has been started. A television opened for the
            // first time has nothing to continue and no server saved yet, and a home screen that
            // answers that with one row of channels is a home screen that looks broken.
            videos = videos.asSequence().sortedByDescending { it.dateModified }.take(MaxRow).toList(),
            servers = servers.take(MaxRow),
            playlists = playlists.take(MaxRow),
            folders = folders.take(MaxRow),
        )
    }.stateIn(
        scope = viewModelScope,
        // Dropped when nothing is watching, and kept for long enough that walking into a channel
        // and pressing back does not rebuild every row.
        started = SharingStarted.WhileSubscribed(IdleTimeoutMs),
        initialValue = TvHomeState(),
    )

    init {
        // The list the app arrives with, so a television that has never been set up still has
        // something to open. Costs nothing on later runs -- the store only seeds an empty one.
        viewModelScope.launch { playlistStore.seedDefaultsIfNeeded() }
    }
}

data class TvHomeState(
    val recent: List<Video> = emptyList(),
    val videos: List<Video> = emptyList(),
    val servers: List<NetworkServerEntry> = emptyList(),
    val playlists: List<SavedPlaylist> = emptyList(),
    val folders: List<FavoriteFolder> = emptyList(),
)

/** A row a viewer can reach the end of with a thumb on a D-pad, and no more. */
private const val MaxRow = 16

private const val IdleTimeoutMs = 5_000L
