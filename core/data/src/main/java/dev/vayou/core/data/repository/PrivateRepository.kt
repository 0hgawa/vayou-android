package dev.vayou.core.data.repository

import dev.vayou.core.model.Video
import kotlinx.coroutines.flow.Flow

interface PrivateRepository {
    fun getVideos(): Flow<List<Video>>
    suspend fun addVideo(filePath: String, video: Video)
    suspend fun removeVideo(videoUri: String)
    suspend fun removeVideos(videoUris: List<String>)
}
