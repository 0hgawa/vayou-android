package dev.vayou.core.data.repository.fake

import dev.vayou.core.data.repository.PlaylistRepository
import dev.vayou.core.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakePlaylistRepository : PlaylistRepository {

    private val playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    private val itemsFlow = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    override fun getPlaylists(): Flow<List<Playlist>> = playlistsFlow.asStateFlow()

    override fun getPlaylistItems(playlistId: String): Flow<List<String>> =
        itemsFlow.map { it[playlistId].orEmpty() }

    override suspend fun createPlaylist(name: String): String {
        val id = "pl-${playlistsFlow.value.size + 1}"
        playlistsFlow.value = playlistsFlow.value + Playlist(
            id = id,
            name = name,
            createdAt = 0L,
            itemCount = 0,
        )
        itemsFlow.value = itemsFlow.value + (id to emptyList())
        return id
    }

    override suspend fun renamePlaylist(id: String, name: String) {
        playlistsFlow.value = playlistsFlow.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun deletePlaylist(id: String) {
        playlistsFlow.value = playlistsFlow.value.filterNot { it.id == id }
        itemsFlow.value = itemsFlow.value - id
    }

    override suspend fun addVideos(playlistId: String, videoUris: List<String>) {
        val current = itemsFlow.value[playlistId].orEmpty()
        val merged = current + videoUris.filter { it !in current }
        itemsFlow.value = itemsFlow.value + (playlistId to merged)
        updateItemCount(playlistId, merged.size)
    }

    override suspend fun removeVideo(playlistId: String, videoUri: String) {
        removeVideos(playlistId, listOf(videoUri))
    }

    override suspend fun removeVideos(playlistId: String, videoUris: List<String>) {
        val filtered = itemsFlow.value[playlistId].orEmpty().filterNot { it in videoUris }
        itemsFlow.value = itemsFlow.value + (playlistId to filtered)
        updateItemCount(playlistId, filtered.size)
    }

    private fun updateItemCount(playlistId: String, count: Int) {
        playlistsFlow.value = playlistsFlow.value.map {
            if (it.id == playlistId) it.copy(itemCount = count) else it
        }
    }
}
