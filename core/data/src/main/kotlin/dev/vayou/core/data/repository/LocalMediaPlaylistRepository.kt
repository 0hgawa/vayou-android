package dev.vayou.core.data.repository

import dev.vayou.core.datastore.datasource.MediaPlaylistsDataSource
import dev.vayou.core.model.MediaLibrary
import dev.vayou.core.model.MediaPlaylists
import dev.vayou.core.model.PrivateVideo
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class LocalMediaPlaylistRepository @Inject constructor(private val dataSource: MediaPlaylistsDataSource) :
    MediaPlaylistRepository {

    override val playlists: Flow<MediaPlaylists> = dataSource.playlists

    override suspend fun create(name: String, library: MediaLibrary): String {
        // Generated here rather than taken from a counter: two lists made on two days must not
        // collide because one was deleted in between.
        val id = UUID.randomUUID().toString()
        dataSource.update { it.add(id, name, library) }
        return id
    }

    override suspend fun rename(id: String, name: String) = dataSource.update { it.rename(id, name) }

    override suspend fun delete(id: String) = dataSource.update { it.remove(id) }

    override suspend fun addItems(id: String, uris: List<String>) = dataSource.update { it.addItems(id, uris) }

    override suspend fun removeItem(id: String, uri: String) = dataSource.update { it.removeItem(id, uri) }

    override suspend fun toggleFavourite(uri: String) = dataSource.update { it.toggleFavourite(uri) }

    override suspend fun recordPlay(uri: String) = dataSource.update { it.recordPlay(uri) }

    override suspend fun forgetItems(uris: Collection<String>) = dataSource.update { it.forgetItems(uris) }

    override suspend fun addPrivate(video: PrivateVideo) = dataSource.update { it.addPrivate(video) }

    override suspend fun removePrivate(filePaths: Collection<String>) =
        dataSource.update { it.removePrivate(filePaths) }
}
