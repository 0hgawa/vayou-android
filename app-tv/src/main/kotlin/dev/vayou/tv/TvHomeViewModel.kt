package dev.vayou.tv

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.model.Video
import dev.vayou.core.smb.ChannelFavouritesStore
import dev.vayou.core.smb.FavoriteFolder
import dev.vayou.core.smb.FolderFavouritesStore
import dev.vayou.core.smb.NetworkServerEntry
import dev.vayou.core.smb.PlaylistStore
import dev.vayou.core.smb.SavedPlaylist
import dev.vayou.core.smb.SmbDiscovery
import dev.vayou.core.smb.SmbFileItem
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
    private val channelFavourites: ChannelFavouritesStore,
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

    /**
     * The card the viewer opened last, so coming back lands on it.
     *
     * Held here because the screen does not survive what it opens: navigating away takes its
     * composition apart, and a home rebuilt from nothing puts the focus at the head of the topmost
     * row -- which, after playing something, is a different card from the one just left.
     *
     * A plain field and not state: nothing on the screen is drawn from it, and making it observable
     * would recompose every row on each move of the focus for a value read once, on the way in.
     */
    internal var lastOpened: Pair<HomeRow, Any>? = null
        private set

    internal fun rememberOpened(row: HomeRow, key: Any) {
        lastOpened = row to key
    }

    val state: StateFlow<TvHomeState> = combine(
        mediaRepository.getVideosFlow(),
        // Read from the table that writes down every play, rather than from the library: a film
        // watched off a share is not in MediaStore and never could be, so the library's own list
        // cannot answer "what did I watch last" for the half of the viewing that happens over the
        // network. Eleven rows with an index on the address -- it costs nothing to ask.
        mediaRepository.getRecentlyPlayed(MaxRow),
        // Saved and found on the wire as one list. A television that has never been set up has
        // nothing saved, and a row that waits for the phone to save something first would never
        // appear at all.
        combine(smbServerStore.savedServers, discovery.discover(), ::mergeNetworkServers),
        combine(playlistStore.playlists, channelFavourites.favouriteUrls, ::Pair),
        folderFavourites.favourites,
    ) { videos, played, servers, (playlists, starredChannels), folders ->
        val byUri = videos.associateBy { it.uriString }
        TvHomeState(
            // Films, and only films, in the order they were watched. A local one is drawn from
            // the library entry, which knows its name, its length and where to find a frame of it;
            // one off a share is drawn from its address alone, which is all there is until the file
            // is opened again. Nothing here reaches for the network: a server that is switched off
            // would hold the home screen waiting on a timeout for each card it owns.
            //
            // Live channels have nowhere to resume to -- the player refuses to write a position for
            // one -- and they arrive named after the last part of their address, which for a stream
            // is always "index". Everything this app plays over http is a channel.
            //
            // Music is left out because this is a row of things to watch, and a row of films with
            // the odd track among them reads as a mistake rather than as a convenience. Tracks are
            // reached through the music library, which is arranged for them.
            //
            // Both tests run before anything is built, so nothing is made to be thrown away.
            recent = played.asSequence()
                .filterNot { it.uri.startsWith("http") || isTrack(it.uri) }
                .map { entry ->
                    byUri[entry.uri]?.let { TvRecent.Local(it) } ?: TvRecent.Remote(entry.uri, entry.watched)
                }
                .toList(),
            // The library itself, and not only what has been started. A television opened for the
            // first time has nothing to continue and no server saved yet, and a home screen that
            // answers that with one row of channels is a home screen that looks broken.
            videos = videos.asSequence().sortedByDescending { it.dateModified }.take(MaxRow).toList(),
            servers = servers.take(MaxRow),
            playlists = playlists.take(MaxRow),
            favouriteChannels = starredChannels.size,
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

/**
 * Something watched before, either from this device or from a machine on the network.
 *
 * Two shapes because there are two amounts known. A film in the library has been read: it has a
 * name, a length, and a frame to show. One on a share has only the address it was played from --
 * finding out more means opening it again, over the network, which is the one thing this row must
 * not do while it is only being drawn.
 */
sealed interface TvRecent {

    val id: String

    /** How far in the viewer got, or null where the length was never written down. */
    val watched: Float?

    data class Local(val video: Video) : TvRecent {
        override val id: String get() = video.uriString

        override val watched: Float? get() = video.playedPercentage.takeIf { it > 0f }
    }

    data class Remote(val uri: String, override val watched: Float?) : TvRecent {
        override val id: String get() = uri

        // Worked out once, when the row is built, and not on every read: the card asks for this
        // each time it is drawn, and a getter would decode the address on every recomposition of a
        // row that scrolls.
        val displayName: String = Uri.decode(uri.substringAfterLast('/')).substringBeforeLast('.')
    }
}

data class TvHomeState(
    val recent: List<TvRecent> = emptyList(),
    val videos: List<Video> = emptyList(),
    val servers: List<NetworkServerEntry> = emptyList(),
    val playlists: List<SavedPlaylist> = emptyList(),
    /** How many channels are starred, across every list -- nought hides the card that opens them. */
    val favouriteChannels: Int = 0,
    val folders: List<FavoriteFolder> = emptyList(),
)

/** A row a viewer can reach the end of with a thumb on a D-pad, and no more. */
private const val MaxRow = 16

private const val IdleTimeoutMs = 5_000L

/**
 * Whether an address names music rather than a film.
 *
 * Asked of the share's own model rather than by keeping a list of extensions here: the browser two
 * screens away answers the same question off the same sets, and a second copy of them would be a
 * second list to remember the day a format is added.
 */
private fun isTrack(uri: String): Boolean =
    SmbFileItem(name = Uri.decode(uri.substringAfterLast('/')), path = "", isDirectory = false).isAudio
