package dev.vayou.core.media

import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What came of trying to take a film in.
 *
 * Three outcomes and not a boolean, because on a modern Android the app cannot delete a file it did
 * not write. It must ask, the asking is a system dialog, and a dialog cannot be awaited from here.
 * So the copy is made and handed back half-finished, with the question still to put.
 */
sealed interface PrivateTake {
    /** Below Android 11, where the app deletes what it likes and the whole thing is done. */
    data class Done(val file: File) : PrivateTake

    /**
     * The copy is made and the original is still there. Put [request] to the viewer, then call
     * [PrivateStorage.settle] with their answer -- yes leaves the copy, no takes it back out.
     */
    data class NeedsPermission(val file: File, val request: IntentSender) : PrivateTake

    /** Nothing was copied and nothing was deleted. */
    data object Failed : PrivateTake
}

/**
 * Moves a film out of everything else's sight, and back again.
 *
 * "Out of sight" is the app's own files directory: no other app may read it, MediaStore does not
 * index it, and no gallery or backup picks it up. That is what makes the folder private, rather than
 * a flag on a row that every other app would ignore.
 *
 * Copy, then delete -- never move. A rename across storage volumes fails, and a delete that ran
 * before the copy was known to be whole would lose the film outright.
 */
@Singleton
class PrivateStorage @Inject constructor(@param:ApplicationContext private val context: Context) {

    suspend fun take(uri: Uri): PrivateTake = withContext(Dispatchers.IO) {
        val destination = runCatching {
            val directory = File(context.filesDir, DirectoryName).also { it.mkdirs() }
            val file = directory.uniqueFile(displayNameOf(uri) ?: FallbackName)
            val source = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            source.use { input -> file.outputStream().use(input::copyTo) }
            file
        }.getOrNull() ?: return@withContext PrivateTake.Failed

        // From Android 11 the app may only delete what it wrote itself. Everything the library shows
        // came from the camera or from a messenger, so this is the ordinary case, not the exception.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender
            }.getOrNull() ?: return@withContext failed(destination)
            return@withContext PrivateTake.NeedsPermission(destination, request)
        }

        val deleted = runCatching { context.contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false)
        if (deleted) PrivateTake.Done(destination) else failed(destination)
    }

    /**
     * Closes a take the viewer was asked about.
     *
     * Refused means the original is still in the library, so the copy is a second version of a film
     * nobody asked to duplicate. It goes.
     */
    suspend fun settle(file: File, isAllowed: Boolean) {
        if (!isAllowed) withContext(Dispatchers.IO) { file.delete() }
    }

    /** Puts it back in Movies and tells the system to index it again. */
    suspend fun release(filePath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val stored = File(filePath)
            if (!stored.exists()) return@runCatching false

            val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            movies.mkdirs()
            val destination = movies.uniqueFile(stored.name)
            stored.inputStream().use { input -> destination.outputStream().use(input::copyTo) }

            // Without this the file is on the card and in no library until the next full scan.
            MediaScannerConnection.scanFile(context, arrayOf(destination.absolutePath), arrayOf(VideoMimeType), null)
            stored.delete()
            true
        }.getOrNull() ?: false
    }

    /** Takes the half-made copy back out, so a failure leaves nothing behind. */
    private fun failed(destination: File): PrivateTake {
        destination.delete()
        return PrivateTake.Failed
    }

    private fun displayNameOf(uri: Uri): String? = context.contentResolver
        .query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
}

/**
 * [name] in this directory, with a number added until nothing is being overwritten.
 *
 * Two films of the same name in two folders are ordinary, and both end up here in one.
 */
private fun File.uniqueFile(name: String): File {
    var candidate = File(this, name)
    if (!candidate.exists()) return candidate

    val base = name.substringBeforeLast(".")
    val extension = name.substringAfterLast(".", "")
    var counter = 1
    while (candidate.exists()) {
        val next = if (extension.isEmpty()) "${base}_$counter" else "${base}_$counter.$extension"
        candidate = File(this, next)
        counter++
    }
    return candidate
}

private const val DirectoryName = "private"

/** For the file whose name the provider will not give up. */
private const val FallbackName = "video.mp4"
