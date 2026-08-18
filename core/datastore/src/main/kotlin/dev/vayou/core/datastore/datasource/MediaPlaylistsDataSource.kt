package dev.vayou.core.datastore.datasource

import androidx.datastore.core.DataStore
import dev.vayou.core.model.MediaPlaylists
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class MediaPlaylistsDataSource @Inject constructor(private val store: DataStore<MediaPlaylists>) {

    val playlists: Flow<MediaPlaylists> = store.data

    /**
     * Swallowed on failure, deliberately: this is called from a row being tapped, and a disk that
     * will not write is not something the tap can do anything about. What is on screen stays as it
     * was, which is the truth.
     */
    suspend fun update(transform: suspend (MediaPlaylists) -> MediaPlaylists) {
        runCatching { store.updateData(transform) }
    }
}
