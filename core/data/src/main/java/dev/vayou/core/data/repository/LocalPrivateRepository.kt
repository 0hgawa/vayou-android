package dev.vayou.core.data.repository

import dev.vayou.core.database.dao.CollectionDao
import dev.vayou.core.database.entities.PrivateVideoEntity
import dev.vayou.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalPrivateRepository @Inject constructor(
    private val collectionDao: CollectionDao,
) : PrivateRepository {

    override fun getVideos(): Flow<List<Video>> = collectionDao.getPrivateVideos().map { entities ->
        entities.map { e ->
            Video(
                id = 0L,
                path = e.videoUri,
                uriString = e.videoUri,
                nameWithExtension = e.name,
                duration = e.duration,
                width = e.width,
                height = e.height,
                size = e.size,
                dateModified = e.dateModified,
            )
        }
    }

    override suspend fun addVideo(filePath: String, video: Video) {
        collectionDao.insertPrivateVideo(
            PrivateVideoEntity(
                videoUri = filePath,
                name = video.nameWithExtension,
                duration = video.duration,
                width = video.width,
                height = video.height,
                size = video.size,
                dateModified = video.dateModified,
            )
        )
    }

    override suspend fun removeVideo(videoUri: String) {
        collectionDao.deletePrivateVideo(videoUri)
    }

    override suspend fun removeVideos(videoUris: List<String>) {
        collectionDao.deletePrivateVideos(videoUris)
    }
}
