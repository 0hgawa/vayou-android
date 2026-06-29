package dev.vayou.core.data.repository

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getUris(): Flow<List<String>>
    suspend fun addVideo(videoUri: String)
    suspend fun removeVideo(videoUri: String)
    suspend fun removeVideos(videoUris: List<String>)
}
