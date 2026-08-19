package dev.vayou.core.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future

/**
 * The cover for the system's own media controls, read by the loader the rest of the app uses.
 *
 * Media3's own loader opens the address with a `DataSource` and hands the bytes to `BitmapFactory`.
 * That works for a file and fails for what a music library actually stores: `content://` album art
 * is not a file on modern Android but something the provider renders on request, and asked for as
 * bytes it answers `Failed to create image decoder ... unimplemented`. The notification then has no
 * picture, and the platform's media control -- which takes its colours from the picture -- comes up
 * colourless, which is the "transparent" panel.
 *
 * Coil already knows how to ask a provider properly, and it is already in this app for every cover
 * on screen. Handing it this too means one answer to "what does this track look like" instead of
 * two that disagree.
 *
 * Hardware bitmaps are refused on purpose: what comes out of here is put in a notification and
 * crosses a process boundary, and a bitmap that lives only on the GPU cannot be written to a parcel.
 */
@OptIn(UnstableApi::class)
class CoilBitmapLoader(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val scope: CoroutineScope,
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    /** Bytes carried by the file itself, which need no fetching -- only decoding. */
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = scope.future {
        BitmapFactory.decodeByteArray(data, 0, data.size)
            ?: error("Could not decode the artwork carried by the track")
    }

    /**
     * The app's own mark, for a track that has no picture at all.
     *
     * Without it the system's panel has nothing to take its colours from and comes up as a
     * colourless plate -- the "transparent" one -- while every other player on the phone shows
     * something. A mark is not a cover and does not pretend to be: it says which app is playing,
     * which is the honest answer when the file carries no picture.
     *
     * Drawn once and kept: it is the same picture for every track that lacks one, and the panel
     * asks again on each change of track.
     */
    private val mark: Bitmap by lazy {
        // The mark's monochrome drawing, which is the one made to be recoloured: a single path, no
        // colour of its own. The coloured mark cannot serve here -- it is an amber tile with a white
        // triangle painted inside, so a tint paints the triangle too and the whole thing comes out
        // as a block.
        //
        // Looked up by name because it belongs to the design system module, which a service this
        // low does not depend on and should not start depending on for one drawing. Falls back to
        // the launcher icon whole if the name ever changes.
        val id = context.resources.getIdentifier(MonochromeMark, "drawable", context.packageName)
        val glyph = if (id != 0) ResourcesCompat.getDrawable(context.resources, id, context.theme) else null
        createBitmap(MarkSize, MarkSize).also { bitmap ->
            val canvas = Canvas(bitmap)
            if (glyph == null) {
                context.packageManager.getApplicationIcon(context.applicationInfo).apply {
                    setBounds(0, 0, MarkSize, MarkSize)
                    draw(canvas)
                }
                return@also
            }
            canvas.drawColor(PlateColour)
            glyph.setTint(MarkColour)
            val margin = (MarkSize * MarkInset).toInt()
            glyph.setBounds(margin, margin, MarkSize - margin, MarkSize - margin)
            glyph.draw(canvas)
        }
    }

    /**
     * What the track says it looks like, or the mark.
     *
     * Media3 asks this first and only falls back to [loadBitmap] when it returns null, so a track
     * with no artwork of any kind is answered here rather than by a failure further down.
     */
    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val data = metadata.artworkData
        if (data != null) return decodeBitmap(data)
        val uri = metadata.artworkUri ?: return scope.future { mark }
        return scope.future { runCatching { coilBitmapOf(uri) }.getOrDefault(mark) }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = scope.future {
        runCatching { coilBitmapOf(uri) }.getOrDefault(mark)
    }

    private suspend fun coilBitmapOf(uri: Uri): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        return (result as? SuccessResult)?.image?.toBitmap()
            ?: error("No picture at $uri: ${(result as? ErrorResult)?.throwable}")
    }
}

/** Square, and large enough for the lock screen, which draws it biggest. */
private const val MarkSize = 512

/** The drawing the launcher uses for a themed icon: the mark as one path, with no colour of its own. */
private const val MonochromeMark = "vayou_mark_monochrome"

/** A pale plate with the mark a few steps darker on it -- legible at the size a panel draws, and
 *  quiet enough not to be mistaken for a cover. */
private const val PlateColour = 0xFFEDEDED.toInt()

private const val MarkColour = 0xFF6B6B6B.toInt()

/** A sixth of the square on each side. Enough margin that the mark reads as a mark rather than as
 *  a cropped picture, and no more: at a quarter it was a small sign in a large empty plate. */
private const val MarkInset = 0.16f
