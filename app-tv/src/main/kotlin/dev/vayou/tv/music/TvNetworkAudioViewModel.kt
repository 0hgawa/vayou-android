package dev.vayou.tv.music

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.smb.SmbClient
import dev.vayou.core.smb.smbSharePath
import dev.vayou.core.smb.smbUri
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The track a viewer picked from a share, and the rest of the folder behind it.
 *
 * The folder is the queue, because on a share it is the only thing that stands for an album: there
 * are no tags to group by until every file has been opened and read, and a listener who chose the
 * first track of one wants the second to follow.
 */
@HiltViewModel
class TvNetworkAudioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val smbClient: SmbClient,
) : ViewModel() {

    /**
     * As it arrived, and not decoded again.
     *
     * The address was encoded once to travel as a segment of a route, and navigation decodes it once
     * on the way in, which leaves it exactly as the share module spelled it. Decoding a second time
     * would undo that spelling: a track called `Piano Sonata N#32.mp3` would have its `%23` turned
     * back into a `#`, and a `#` in an address opens the fragment -- everything after it is dropped.
     */
    private val trackUri: String = savedStateHandle[UriArg] ?: error("Opened with no track to play")

    private val _queue = MutableStateFlow<TvAudioQueue?>(null)

    /** Null until the folder has answered; the screen shows nothing rather than a queue of one. */
    val queue: StateFlow<TvAudioQueue?> = _queue.asStateFlow()

    init {
        viewModelScope.launch {
            _queue.value = siblings() ?: TvAudioQueue(listOf(trackUri), startIndex = 0)
        }
    }

    /**
     * Every track in the same folder, in name order, and where the chosen one sits among them.
     *
     * Sorted here rather than by the client, which hands a listing over in whatever order the share
     * gave: a queue's order is the listener's, and by name is the order the folder was written in.
     */
    private suspend fun siblings(): TvAudioQueue? {
        val address = Uri.parse(trackUri)
        val host = address.host ?: return null
        val (share, path) = address.smbSharePath()
        val folder = path.substringBeforeLast('\\', "")

        val tracks = smbClient.listDirectory(share, folder).getOrNull()
            ?.filter { it.isAudio }
            ?.sortedBy { it.name.lowercase() }
            ?: return null

        // Found by where the file is on the share rather than by comparing two addresses. An address
        // is one of several spellings of the same file -- encoded here, decoded there -- and matching
        // on the text of it failed the moment the spelling changed, which left a queue of one and a
        // now-playing screen with no list to open.
        val index = tracks.indexOfFirst { it.path == path }
        return if (index < 0) null else TvAudioQueue(tracks.map { smbUri(host, share, it.path).toString() }, index)
    }

    companion object {
        const val UriArg = "trackUri"
    }
}

/** What to play, and which of it to start on. */
class TvAudioQueue(val tracks: List<String>, val startIndex: Int)
