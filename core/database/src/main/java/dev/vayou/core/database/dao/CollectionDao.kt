package dev.vayou.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.vayou.core.database.entities.FavoriteEntity
import dev.vayou.core.database.entities.PrivateVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT video_uri FROM favorites")
    fun getFavorites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE video_uri = :videoUri")
    suspend fun deleteFavorite(videoUri: String)

    @Query("DELETE FROM favorites WHERE video_uri IN (:videoUris)")
    suspend fun deleteFavorites(videoUris: List<String>)

    @Query("SELECT * FROM private_videos")
    fun getPrivateVideos(): Flow<List<PrivateVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPrivateVideo(entity: PrivateVideoEntity)

    @Query("DELETE FROM private_videos WHERE video_uri = :videoUri")
    suspend fun deletePrivateVideo(videoUri: String)

    @Query("DELETE FROM private_videos WHERE video_uri IN (:videoUris)")
    suspend fun deletePrivateVideos(videoUris: List<String>)
}
