package dev.vayou.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.vayou.core.database.entities.PlaylistEntity
import dev.vayou.core.database.entities.PlaylistItemEntity
import dev.vayou.core.database.relations.PlaylistWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Upsert
    suspend fun upsert(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertItems(items: List<PlaylistItemEntity>)

    @Transaction
    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    fun getAll(): Flow<List<PlaylistWithItems>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getById(playlistId: String): Flow<PlaylistWithItems?>

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlist_id = :playlistId")
    suspend fun getItemCount(playlistId: String): Int

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId AND video_uri = :videoUri")
    suspend fun deleteItem(playlistId: String, videoUri: String)

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId AND video_uri IN (:videoUris)")
    suspend fun deleteItems(playlistId: String, videoUris: List<String>)
}
