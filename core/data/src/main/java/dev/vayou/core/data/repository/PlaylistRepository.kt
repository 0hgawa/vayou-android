package dev.vayou.core.data.repository

import dev.vayou.core.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    fun getPlaylistItems(playlistId: String): Flow<List<String>>
    suspend fun createPlaylist(name: String): String
    suspend fun renamePlaylist(id: String, name: String)
    suspend fun deletePlaylist(id: String)
    suspend fun addVideos(playlistId: String, videoUris: List<String>)
    suspend fun removeVideo(playlistId: String, videoUri: String)
    suspend fun removeVideos(playlistId: String, videoUris: List<String>)
}
