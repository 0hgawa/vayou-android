package dev.vayou.core.model

import java.io.Serializable

data class Folder(
    val name: String,
    val path: String,
    val dateModified: Long,
    val parentPath: String? = null,
    val formattedMediaSize: String = "",
    val mediaList: List<Video> = emptyList(),
    val folderList: List<Folder> = emptyList(),
) : Serializable {

    val mediaSize: Long = mediaList.sumOf { it.size } + folderList.sumOf { it.mediaSize }
    val mediaDuration: Long = mediaList.sumOf { it.duration } + folderList.sumOf { it.mediaDuration }

    // Distinct by uri, because the library's root carries both halves of the same set: every video
    // flat in [mediaList], and the same videos again inside the folders of [folderList]. Without
    // this, every consumer sees each file twice — and a list keyed by uri throws on the second one.
    val allMediaList: List<Video> = (mediaList + folderList.flatMap { it.allMediaList })
        .distinctBy { it.uriString }
    val recentlyPlayedVideo: Video? = allMediaList.recentPlayed()
    val firstVideo: Video? = allMediaList.firstOrNull()

    fun isRecentlyPlayedVideo(video: Video?): Boolean {
        if (recentlyPlayedVideo == null) return false
        if (video == null) return false
        return video.path == recentlyPlayedVideo.path
    }

    companion object {
        /** The library itself, as a folder: every video flat in [mediaList] and every folder in
         *  [folderList]. What `GetSortedMediaUseCase` hands the library screen. */
        val root = Folder(name = "Root", path = "/", dateModified = 0)
    }
}
