package dev.vayou.core.media

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A playable track, as [android.provider.MediaStore] describes it.
 *
 * Parcelable so a queue travels to the player as one list, rather than as parallel arrays of
 * addresses, titles and artists that have to be re-zipped by index on arrival.
 */
@Parcelize
data class Song(
    val id: Long,
    val uri: Uri,
    /**
     * File name with its extension. [title] is the tag, which is what the screen shows; this is
     * what a rename operates on.
     */
    val fileName: String,
    /** The directory holding the file, or empty when MediaStore reports no path. */
    val folderPath: String,
    val title: String,
    val artist: String,
    val album: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    /** When MediaStore first saw the file, in epoch seconds -- what "recently added" orders by. */
    val dateAddedSeconds: Long,
    /**
     * Where to ask what this track looks like, which is the track itself.
     *
     * Not a picture but an address to ask about one: the provider answers with the picture inside
     * the file, or the album's when the file carries none, and with nothing at all for a track that
     * has neither -- so whatever draws it falls back rather than trusting this to be an image.
     */
    val artworkUri: Uri?,
) : Parcelable {
    /**
     * [uri] as a string, worked out once.
     *
     * It is the key everything else stores a track under, and re-serialising a `Uri` on every
     * recomposition of every row is work a five-hundred-track list does five hundred times a frame.
     */
    val uriString: String = uri.toString()
}
