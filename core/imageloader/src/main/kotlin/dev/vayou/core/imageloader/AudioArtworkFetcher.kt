package dev.vayou.core.imageloader

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.Uri as CoilUri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.pxOrElse
import coil3.toAndroidUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The picture that belongs to a track, asked of whoever actually has it.
 *
 * A track's address is not a picture, so nothing downstream can decode it -- and the address a
 * player used to build instead, `content://media/external/audio/albumart/<id>`, is the legacy one:
 * on this phone the platform answers it with "failed to create image decoder", and the library's
 * album ids do not even map to a real album any more. Every cover then comes up empty, which is
 * what left the system's media panel colourless: it takes its colours from the picture.
 *
 * The provider answers for the *track*: it returns what is inside the file, or the album's picture
 * when the file carries none. Below Android 10 there is no such call, so the tags are read directly.
 *
 * One fetcher and not a special case inside the notification, because the screen, the mini
 * controller, the lock screen and the panel all ask the same question about the same track, and an
 * answer that only one of them knows is how the four came to disagree.
 */
class AudioArtworkFetcher(private val context: Context, private val uri: Uri, private val options: Options) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val bitmap = thumbnail() ?: embedded() ?: return@withContext null
        ImageFetchResult(image = bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    /** What the provider renders, which is the only supported way to ask from Android 10. */
    private fun thumbnail(): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val width = options.size.width.pxOrElse { DefaultSize }
        val height = options.size.height.pxOrElse { DefaultSize }
        return runCatching {
            context.contentResolver.loadThumbnail(uri, Size(width, height), null)
        }.getOrNull()
    }

    /** The tag inside the file, for the versions with no provider call and for what it cannot render. */
    private fun embedded(): Bitmap? = runCatching {
        // Released by hand, not by `use`: this class only became AutoCloseable in Android 10, and
        // on anything older that call does not exist -- which on a phone at the floor this app
        // supports is a crash rather than a missing cover.
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } finally {
            retriever.release()
        }
    }.getOrNull()

    class Factory(private val context: Context) : Fetcher.Factory<CoilUri> {

        override fun create(data: CoilUri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val uri = data.toAndroidUri()
            if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
            if (uri.authority != MediaStore.AUTHORITY) return null
            // Only a track. A picture kept in the store is a picture, and the ordinary path reads
            // it without a round trip through the provider's thumbnailer.
            if (!uri.pathSegments.contains("audio")) return null
            return AudioArtworkFetcher(context, uri, options)
        }
    }
}

/** What the panel and the lock screen draw at, when the caller asks for no size in particular. */
private const val DefaultSize = 512
