package dev.vayou.core.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import dev.vayou.core.database.entities.PlaylistEntity
import dev.vayou.core.database.entities.PlaylistItemEntity

data class PlaylistWithItems(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlist_id",
    )
    val items: List<PlaylistItemEntity>,
)
