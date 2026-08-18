package dev.vayou.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.vayou.core.common.extensions.asUtf8Subtitle
import dev.vayou.core.common.extensions.displayNameOf

/** What the file picker should offer. Subtitles arrive typed as often as they arrive as bytes. */
val SubtitleMimeTypes: Array<String> = arrayOf(
    MimeTypes.APPLICATION_SUBRIP,
    MimeTypes.APPLICATION_TTML,
    MimeTypes.TEXT_VTT,
    MimeTypes.TEXT_SSA,
    "application/octet-stream",
    "text/*",
)

/**
 * A file the viewer picked, described the way media3 needs to hear it.
 *
 * The format is taken from the name because that is all there is: a provider hands over
 * `application/octet-stream` for a `.srt` as readily as it hands over the real type, and media3
 * will not sniff a sideloaded subtitle the way it sniffs a container.
 */
@UnstableApi
suspend fun Context.externalSubtitle(
    uri: Uri,
    /** What the caller wants it listed as. Null to ask whoever owns the file. */
    name: String? = null,
    isSelected: Boolean = true,
): MediaItem.SubtitleConfiguration {
    val label = name ?: displayNameOf(uri)
    return MediaItem.SubtitleConfiguration.Builder(asUtf8Subtitle(uri))
        // The original uri and not the converted copy: the copy lives in the cache and its name
        // changes with it, while this is what says "this subtitle is already loaded".
        .setId(uri.toString())
        .setMimeType(subtitleMimeOf(label))
        .setLabel(label)
        // Only what the caller marked. Handing several at once and turning them all on would
        // have the selector pick for itself, which is not the same as being asked.
        .setSelectionFlags(if (isSelected) C.SELECTION_FLAG_DEFAULT else 0)
        .build()
}

/**
 * Puts [subtitle] on the file playing, keeping the position.
 *
 * A media item's subtitles are fixed once it is in the timeline, so the item is replaced. Adding
 * the replacement beside the original and then removing the original, rather than the other way
 * round, is what keeps the decoder from being torn down between the two.
 */
@UnstableApi
fun Player.addSubtitle(subtitle: MediaItem.SubtitleConfiguration) {
    val current = currentMediaItem ?: return
    val existing = current.localConfiguration?.subtitleConfigurations.orEmpty()
    if (existing.any { it.id == subtitle.id }) return

    val index = currentMediaItemIndex
    val position = currentPosition
    addMediaItem(index + 1, current.buildUpon().setSubtitleConfigurations(existing + subtitle).build())
    seekTo(index + 1, position)
    removeMediaItem(index)
}

private fun subtitleMimeOf(name: String?): String = when (name?.substringAfterLast('.')?.lowercase()) {
    "ssa", "ass" -> MimeTypes.TEXT_SSA
    "vtt" -> MimeTypes.TEXT_VTT
    "ttml", "xml", "dfxp" -> MimeTypes.APPLICATION_TTML
    else -> MimeTypes.APPLICATION_SUBRIP
}
