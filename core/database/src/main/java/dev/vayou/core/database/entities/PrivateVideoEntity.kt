package dev.vayou.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "private_videos")
data class PrivateVideoEntity(
    @PrimaryKey @ColumnInfo(name = "video_uri") val videoUri: String,
    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "duration") val duration: Long = 0L,
    @ColumnInfo(name = "width") val width: Int = 0,
    @ColumnInfo(name = "height") val height: Int = 0,
    @ColumnInfo(name = "size") val size: Long = 0L,
    @ColumnInfo(name = "date_modified") val dateModified: Long = 0L,
)
