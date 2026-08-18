package dev.vayou.core.model

import java.io.Serializable
import java.util.Date

data class Video(
    val id: Long,
    val path: String,
    val parentPath: String = "",
    val duration: Long,
    val uriString: String,
    val nameWithExtension: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val playbackPosition: Long = 200,
    val dateModified: Long = 0,
    val formattedDuration: String = "",
    val formattedFileSize: String = "",
    val format: String? = null,
    val lastPlayedAt: Date? = null,
    val videoStream: VideoStreamInfo? = null,
    val audioStreams: List<AudioStreamInfo> = emptyList(),
    val subtitleStreams: List<SubtitleStreamInfo> = emptyList(),
) : Serializable {

    val displayName: String = nameWithExtension.substringBeforeLast(".")
    val playedPercentage: Float =
        (playbackPosition.toFloat() / duration.toFloat()).takeIf { playbackPosition >= 0 } ?: 1f
}

fun List<Video>.recentPlayed(): Video? =
    filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt?.time }.firstOrNull()
