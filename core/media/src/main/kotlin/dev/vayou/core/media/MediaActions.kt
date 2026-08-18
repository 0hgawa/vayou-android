package dev.vayou.core.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.ID3v24Tag
import com.mpatric.mp3agic.Mp3File
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What came of asking to change a file the app does not own.
 *
 * Three outcomes and not a boolean, because from Android 11 an app may only write to what it wrote
 * itself. Everything a video library shows came from a camera or a messenger, so permission has to
 * be asked for -- and the asking is a system dialog, which cannot be awaited from down here.
 */
/** What a track says it is: the fields worth correcting by hand, and no more. */
data class MediaTags(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val year: String,
)

sealed interface MediaWrite {
    data object Done : MediaWrite

    /** Put [request] to the viewer, then call back with their answer. */
    data class NeedsPermission(val request: IntentSender) : MediaWrite

    data object Failed : MediaWrite
}

/**
 * The things a viewer does to a file rather than with it: send it somewhere, call it something
 * else, throw it away.
 *
 * Everything here goes through MediaStore rather than through `File`. A path on the shared volume
 * has not been writable since Android 10, and the store is also what has to be told, so that the
 * gallery and this library agree about what exists a second later.
 */
@Singleton
class MediaActions @Inject constructor(@param:ApplicationContext private val context: Context) {

    /**
     * Handed back rather than started here: choosing where a file goes is the system's own sheet,
     * and starting an activity is the screen's business, not a singleton's.
     *
     * [mimeType] decides which apps the sheet offers. Sending a song under the video type hides
     * every music app from it.
     */
    fun shareIntent(uris: List<Uri>, mimeType: String = VideoMimeType): Intent = Intent().apply {
        if (uris.size == 1) {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uris.first())
        } else {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        type = mimeType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    suspend fun delete(uris: List<Uri>): MediaWrite = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@withContext runCatching {
                MediaWrite.NeedsPermission(MediaStore.createDeleteRequest(resolver, uris).intentSender)
            }.getOrDefault(MediaWrite.Failed)
        }
        val deleted = runCatching { uris.all { resolver.delete(it, null, null) > 0 } }.getOrDefault(false)
        if (deleted) MediaWrite.Done else MediaWrite.Failed
    }

    /**
     * Asks for the right to write to [uri]; the rename itself is [applyRename], once the answer is
     * yes. Two calls and not one, because the permission arrives through a dialog and this cannot
     * wait for it.
     */
    suspend fun rename(uri: Uri, to: String): MediaWrite = withContext(Dispatchers.IO) {
        askToWrite(uri) ?: if (applyRename(uri, to)) MediaWrite.Done else MediaWrite.Failed
    }

    /**
     * Corrects what a track says it is. Asks first and writes in [applyTags], as [rename] does.
     *
     * MP3 only: the tag is written into the file, and this reads one format. Callers offer it for
     * the files it can serve rather than letting it fail on the others.
     */
    suspend fun editTags(uri: Uri, tags: MediaTags, cover: Uri?): MediaWrite = withContext(Dispatchers.IO) {
        askToWrite(uri) ?: if (applyTags(uri, tags, cover)) MediaWrite.Done else MediaWrite.Failed
    }

    /**
     * Tags are written through a copy in the cache rather than in place: the tag library needs a
     * file it owns and can seek in, and a failure halfway would otherwise leave the track truncated.
     * Only once the new file is whole is it streamed back over the original.
     */
    suspend fun applyTags(uri: Uri, tags: MediaTags, cover: Uri?): Boolean = withContext(Dispatchers.IO) {
        val work = File(context.cacheDir, TagWorkDir).apply { mkdirs() }
        val source = File(work, "in.mp3")
        val edited = File(work, "out.mp3")
        try {
            resolver.openInputStream(uri)?.use { input ->
                source.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext false

            val mp3 = Mp3File(source)
            mp3.id3v2Tag = (mp3.id3v2Tag as? ID3v2 ?: ID3v24Tag()).apply {
                title = tags.title
                artist = tags.artist
                album = tags.album
                albumArtist = tags.albumArtist
                year = tags.year
                cover?.let { picked ->
                    resolver.openInputStream(picked)?.use { image ->
                        setAlbumImage(image.readBytes(), resolver.getType(picked) ?: DefaultCoverType)
                    }
                }
            }
            mp3.save(edited.absolutePath)

            // "rwt" truncates first, so a shorter file does not leave the old tail behind it.
            resolver.openOutputStream(uri, "rwt")?.use { output ->
                edited.inputStream().use { input -> input.copyTo(output) }
            } ?: return@withContext false

            // The store keeps its own copy of the tags and does not reread the file on its own. The
            // library is drawn from that copy, so without this the screen shows the old name back.
            rescan(uri)
            true
        } catch (error: Exception) {
            Log.w(LogTag, "tags not written", error)
            false
        } finally {
            source.delete()
            edited.delete()
        }
    }

    /**
     * The system's own dialog, or null when this Android needs no asking.
     *
     * From Android 11 an app may only write to what it wrote itself, and a library is made of other
     * apps' files. Null means the caller may simply go ahead.
     */
    private fun askToWrite(uri: Uri): MediaWrite? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            MediaWrite.NeedsPermission(MediaStore.createWriteRequest(resolver, listOf(uri)).intentSender)
        }.getOrDefault(MediaWrite.Failed)
    }

    private fun rescan(uri: Uri) {
        val path = runCatching {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { row ->
                if (row.moveToFirst()) row.getString(0) else null
            }
        }.getOrNull() ?: return
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }

    /** The rename proper. Safe to call once the write is permitted, and only then. */
    suspend fun applyRename(uri: Uri, to: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, to) }
            resolver.update(uri, values, null, null) > 0
        }.getOrDefault(false)
    }

    private val resolver get() = context.contentResolver
}

/** Where the copy being tagged is kept while it is being written. */
private const val TagWorkDir = "tagging"

private const val DefaultCoverType = "image/jpeg"

private const val LogTag = "MediaActions"

const val VideoMimeType = "video/*"

const val AudioMimeType = "audio/*"
