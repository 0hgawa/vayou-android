package dev.vayou.core.data.repository

import android.net.Uri
import dev.vayou.core.data.models.RecentPlayback
import dev.vayou.core.data.models.VideoState
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Video
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getVideosFlow(): Flow<List<Video>>
    fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>>
    fun getFoldersFlow(): Flow<List<Folder>>

    /** What was played last, newest first -- network shares included. */
    fun getRecentlyPlayed(limit: Int): Flow<List<RecentPlayback>>

    /** Where the viewer stopped and how long the thing was, written together. */
    suspend fun updateMediumProgress(uri: String, position: Long, duration: Long)

    suspend fun getVideoByUri(uri: String): Video?
    suspend fun getVideoState(uri: String): VideoState?

    suspend fun updateMediumLastPlayedTime(uri: String, lastPlayedTime: Long)
    suspend fun updateMediumPosition(uri: String, position: Long)
    suspend fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float)
    suspend fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int)
    suspend fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int)
    suspend fun updateMediumZoom(uri: String, zoom: Float)
    suspend fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri)
    suspend fun updateSubtitleDelay(uri: String, delay: Long)
    suspend fun updateSubtitleSpeed(uri: String, speed: Float)
}
