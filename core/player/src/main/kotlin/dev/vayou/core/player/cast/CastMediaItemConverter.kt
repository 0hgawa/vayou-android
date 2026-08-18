package dev.vayou.core.player.cast

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata as CastMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaTrack

/**
 * Turns what the app plays into what a receiver can fetch.
 *
 * Media3's own converter assumes every address is already reachable from the network, which is true
 * of a stream and false of everything on this phone. This one hands each local address to
 * [server] and sends the URL that comes back.
 */
@OptIn(UnstableApi::class)
internal class CastMediaItemConverter(private val server: CastMediaServer) : MediaItemConverter {

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val uri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY
        val fileName = uri.lastPathSegment
        val mimeType = mediaItem.localConfiguration?.mimeType ?: castMimeTypeFor(uri.path.orEmpty())
        // A channel goes through this phone even when the receiver could reach it itself. Its
        // playlist is a document the receiver has to *read*, and a channel's server names one
        // origin allowed to read it -- its own, never the receiver's. Served from here it carries
        // this phone's permission instead. Only the playlist: the video stays where it is.
        val url = if (mimeType == MimeHls) server.publish(uri, fileName) else uri.reachableUrl(fileName)

        val metadata = CastMetadata(CastMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(
                CastMetadata.KEY_TITLE,
                mediaItem.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: fileName.orEmpty(),
            )
        }

        val info = MediaInfo.Builder(url)
            // A channel has no end to buffer towards. Told it is a finite item, a receiver waits on
            // a duration that never comes and treats the wait as a failed load.
            .setStreamType(if (mimeType in Streaming) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(metadata)
            .apply {
                val subtitles = mediaItem.localConfiguration?.subtitleConfigurations
                if (!subtitles.isNullOrEmpty()) setMediaTracks(subtitles.asCastTracks(fileName))
            }
            .build()

        return MediaQueueItem.Builder(info).build()
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
        val contentId = mediaQueueItem.media?.contentId ?: return MediaItem.EMPTY
        return MediaItem.Builder().setUri(contentId).setMediaId(contentId).build()
    }

    /**
     * Subtitles, renamed on the way out.
     *
     * A receiver refuses a track whose URL has no extension, and it is happiest when the subtitle
     * shares the video's base name -- the sidecar convention it expects on disk. So the name is
     * made up here rather than taken from the file, whatever the file is really called.
     */
    private fun List<MediaItem.SubtitleConfiguration>.asCastTracks(videoFileName: String?): List<MediaTrack> {
        val base = videoFileName?.substringBeforeLast('.') ?: "subtitle"
        return mapIndexed { index, subtitle ->
            val mimeType = subtitle.mimeType ?: castMimeTypeFor(subtitle.uri.path.orEmpty())
            val ordinal = if (size > 1) ".${index + 1}" else ""
            val name = "$base$ordinal${mimeType.subtitleExtension}"
            MediaTrack.Builder(index + 1L, MediaTrack.TYPE_TEXT)
                .setContentId(subtitle.uri.reachableUrl(name))
                .setContentType(mimeType)
                .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                .setName(subtitle.label.orEmpty())
                .setLanguage(subtitle.language.orEmpty())
                .build()
        }
    }

    /** Already on the network, or published on this phone's own server so that it is. */
    private fun Uri.reachableUrl(fileName: String?): String =
        if (scheme in AlreadyReachable) toString() else server.publish(this, fileName)
}

private val AlreadyReachable = setOf("http", "https", "rtsp", "rtmp")

/** What this app only ever plays as a channel, so what it only ever casts as live. */
private val Streaming = setOf(MimeHls, "application/dash+xml")

private val String.subtitleExtension: String
    get() = when (this) {
        "text/vtt" -> ".vtt"
        "text/x-ssa" -> ".ssa"
        "application/ttml+xml" -> ".ttml"
        else -> ".srt"
    }
