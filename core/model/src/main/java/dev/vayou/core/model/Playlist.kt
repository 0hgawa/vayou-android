package dev.vayou.core.model

data class Playlist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val itemCount: Int,
    val thumbnailUris: List<String> = emptyList(),
)
