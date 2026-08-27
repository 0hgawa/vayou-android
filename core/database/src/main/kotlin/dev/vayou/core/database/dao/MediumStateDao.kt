package dev.vayou.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.vayou.core.database.entities.MediumStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediumStateDao {

    @Upsert
    suspend fun upsert(mediumState: MediumStateEntity)

    @Upsert
    suspend fun upsertAll(mediaStates: List<MediumStateEntity>)

    @Query("SELECT * FROM media_state WHERE uri = :uri")
    suspend fun get(uri: String): MediumStateEntity?

    @Query("SELECT * FROM media_state WHERE uri = :uri")
    fun getAsFlow(uri: String): Flow<MediumStateEntity?>

    @Query("SELECT * FROM media_state")
    fun getAll(): Flow<List<MediumStateEntity>>

    /**
     * What was watched last, whatever it was played from.
     *
     * The table is keyed by address and takes any of them, so a film opened off a share is written
     * down beside one opened off the device. The library's own list cannot answer this -- it holds
     * what MediaStore knows about, and a share is not something MediaStore knows about.
     */
    @Query(
        "SELECT * FROM media_state WHERE last_played_time IS NOT NULL " +
            "ORDER BY last_played_time DESC LIMIT :limit",
    )
    fun recentlyPlayed(limit: Int): Flow<List<MediumStateEntity>>

    @Query("DELETE FROM media_state WHERE uri in (:uris)")
    suspend fun delete(uris: List<String>)
}
