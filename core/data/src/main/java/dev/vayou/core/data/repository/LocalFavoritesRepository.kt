package dev.vayou.core.data.repository

import dev.vayou.core.database.dao.CollectionDao
import dev.vayou.core.database.entities.FavoriteEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class LocalFavoritesRepository @Inject constructor(
    private val collectionDao: CollectionDao,
) : FavoritesRepository {

    override fun getUris(): Flow<List<String>> = collectionDao.getFavorites()

    override suspend fun addVideo(videoUri: String) {
        collectionDao.insertFavorite(FavoriteEntity(videoUri))
    }

    override suspend fun removeVideo(videoUri: String) {
        collectionDao.deleteFavorite(videoUri)
    }

    override suspend fun removeVideos(videoUris: List<String>) {
        collectionDao.deleteFavorites(videoUris)
    }
}
