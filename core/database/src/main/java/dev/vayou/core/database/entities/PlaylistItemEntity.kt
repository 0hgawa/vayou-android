package dev.vayou.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlist_id", "video_uri"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("playlist_id"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["playlist_id"])],
)
data class PlaylistItemEntity(
    @ColumnInfo(name = "playlist_id") val playlistId: String,
    @ColumnInfo(name = "video_uri") val videoUri: String,
    @ColumnInfo(name = "position") val position: Int,
)
