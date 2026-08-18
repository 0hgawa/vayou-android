package dev.vayou.core.data.repository

import dev.vayou.core.model.MediaLibrary
import dev.vayou.core.model.MediaPlaylists
import dev.vayou.core.model.PrivateVideo
import kotlinx.coroutines.flow.Flow

/**
 * Lists of local media, plus what is starred and how often it has been played.
 *
 * One store for both libraries: the shape is identical, and an address already says which library
 * it came from.
 */
interface MediaPlaylistRepository {

    val playlists: Flow<MediaPlaylists>

    /** The new list's id, so the caller can open it straight away rather than hunting for it. */
    suspend fun create(name: String, library: MediaLibrary): String

    suspend fun rename(id: String, name: String)

    suspend fun delete(id: String)

    suspend fun addItems(id: String, uris: List<String>)

    suspend fun removeItem(id: String, uri: String)

    suspend fun toggleFavourite(uri: String)

    /** Counted when something starts, which is what "most played" is asked to mean. */
    suspend fun recordPlay(uri: String)

    /** Drops [uris] from lists, favourites and counts, for when the media leaves the device. */
    suspend fun forgetItems(uris: Collection<String>)

    /** Records a film that has been moved into the app's own storage, with what it was. */
    suspend fun addPrivate(video: PrivateVideo)

    /** Forgets films that have been put back where everything else can see them. */
    suspend fun removePrivate(filePaths: Collection<String>)
}
