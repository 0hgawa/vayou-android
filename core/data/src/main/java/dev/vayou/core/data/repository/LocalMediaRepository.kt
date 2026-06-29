package dev.vayou.core.data.repository

import android.net.Uri
import dev.vayou.core.data.mappers.toFolder
import dev.vayou.core.data.mappers.toVideo
import dev.vayou.core.data.mappers.toVideoState
import dev.vayou.core.data.models.VideoState
import dev.vayou.core.database.converter.UriListConverter
import dev.vayou.core.database.dao.DirectoryDao
import dev.vayou.core.database.dao.MediumDao
import dev.vayou.core.database.dao.MediumStateDao
import dev.vayou.core.database.entities.MediumStateEntity
import dev.vayou.core.database.relations.DirectoryWithMedia
import dev.vayou.core.database.relations.MediumWithInfo
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalMediaRepository @Inject constructor(
    private val mediumDao: MediumDao,
    private val mediumStateDao: MediumStateDao,
    private val directoryDao: DirectoryDao,
) : MediaRepository {

    override fun getVideosFlow(): Flow<List<Video>> {
        return mediumDao.getAllWithInfo().map { it.map(MediumWithInfo::toVideo) }
    }

    override fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>> {
        return mediumDao.getAllWithInfoFromDirectory(folderPath).map { it.map(MediumWithInfo::toVideo) }
    }

    override fun getFoldersFlow(): Flow<List<Folder>> {
        return directoryDao.getAllWithMedia().map { it.map(DirectoryWithMedia::toFolder) }
    }

    override suspend fun getVideoByUri(uri: String): Video? {
        return mediumDao.getWithInfo(uri)?.toVideo()
    }

    override suspend fun getVideoState(uri: String): VideoState? {
        return mediumStateDao.get(uri)?.toVideoState()
    }

    private suspend fun updateState(uri: String, transform: MediumStateEntity.() -> MediumStateEntity) {
        val entity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)
        mediumStateDao.upsert(entity.transform())
    }

    override suspend fun updateMediumLastPlayedTime(uri: String, lastPlayedTime: Long) =
        updateState(uri) { copy(lastPlayedTime = lastPlayedTime) }

    override suspend fun updateMediumPosition(uri: String, position: Long) =
        updateState(uri) { copy(playbackPosition = position, lastPlayedTime = System.currentTimeMillis()) }

    override suspend fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float) =
        updateState(uri) { copy(playbackSpeed = playbackSpeed, lastPlayedTime = System.currentTimeMillis()) }

    override suspend fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int) =
        updateState(uri) { copy(audioTrackIndex = audioTrackIndex, lastPlayedTime = System.currentTimeMillis()) }

    override suspend fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int) =
        updateState(uri) { copy(subtitleTrackIndex = subtitleTrackIndex, lastPlayedTime = System.currentTimeMillis()) }

    override suspend fun updateMediumZoom(uri: String, zoom: Float) =
        updateState(uri) { copy(videoScale = zoom, lastPlayedTime = System.currentTimeMillis()) }

    override suspend fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)
        val currentExternalSubs = UriListConverter.fromStringToList(stateEntity.externalSubs)
        if (currentExternalSubs.contains(subtitleUri)) return
        val newExternalSubs = UriListConverter.fromListToString(urlList = currentExternalSubs + subtitleUri)
        mediumStateDao.upsert(stateEntity.copy(externalSubs = newExternalSubs, lastPlayedTime = System.currentTimeMillis()))
    }

    override suspend fun updateSubtitleDelay(uri: String, delay: Long) =
        updateState(uri) { copy(subtitleDelayMilliseconds = delay, lastPlayedTime = System.currentTimeMillis()) }

    override suspend fun updateSubtitleSpeed(uri: String, speed: Float) =
        updateState(uri) { copy(subtitleSpeed = speed, lastPlayedTime = System.currentTimeMillis()) }
}
