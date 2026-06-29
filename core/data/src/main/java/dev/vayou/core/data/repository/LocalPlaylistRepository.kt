package dev.vayou.core.data.repository

import dev.vayou.core.database.dao.PlaylistDao
import dev.vayou.core.database.entities.PlaylistEntity
import dev.vayou.core.database.entities.PlaylistItemEntity
import dev.vayou.core.database.relations.PlaylistWithItems
import dev.vayou.core.model.Playlist
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalPlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAll().map { it.map(PlaylistWithItems::toPlaylist) }

    override fun getPlaylistItems(playlistId: String): Flow<List<String>> =
        playlistDao.getById(playlistId).map { pwi ->
            pwi?.items?.sortedBy { it.position }?.map { it.videoUri } ?: emptyList()
        }

    override suspend fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        playlistDao.upsert(PlaylistEntity(id = id, name = name, createdAt = System.currentTimeMillis()))
        return id
    }

    override suspend fun renamePlaylist(id: String, name: String) {
        playlistDao.rename(id, name)
    }

    override suspend fun deletePlaylist(id: String) {
        playlistDao.delete(id)
    }

    override suspend fun addVideos(playlistId: String, videoUris: List<String>) {
        val startPosition = playlistDao.getItemCount(playlistId)
        playlistDao.upsertItems(
            videoUris.mapIndexed { index, uri ->
                PlaylistItemEntity(
                    playlistId = playlistId,
                    videoUri = uri,
                    position = startPosition + index,
                )
            },
        )
    }

    override suspend fun removeVideo(playlistId: String, videoUri: String) {
        playlistDao.deleteItem(playlistId, videoUri)
    }

    override suspend fun removeVideos(playlistId: String, videoUris: List<String>) {
        playlistDao.deleteItems(playlistId, videoUris)
    }
}

private fun PlaylistWithItems.toPlaylist() = Playlist(
    id = playlist.id,
    name = playlist.name,
    createdAt = playlist.createdAt,
    itemCount = items.size,
    thumbnailUris = items.sortedBy { it.position }.take(4).map { it.videoUri },
)
