package dev.vayou.core.media

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vayou.core.common.Dispatcher
import dev.vayou.core.common.VayouDispatchers
import dev.vayou.core.common.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

/**
 * The audio on this device, read from [MediaStore].
 *
 * Its own class because two screens need it and neither owns it: the library lists everything, and
 * the player has to describe the one track it is playing. The player is a separate activity, so
 * without this it would either carry the whole queue in its intent -- which it cannot, reopened
 * from the mini controller -- or repeat this projection and its column reads.
 */
@Singleton
class MusicLibrary @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(VayouDispatchers.IO) private val dispatcher: CoroutineDispatcher,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    /**
     * A tick whenever the store's audio changes.
     *
     * Here and not in a view model: the observer is one registration for the whole app, and a
     * second screen that also watches used to mean a second observer and a second scan of the
     * library for every change. Shared while anyone is listening and let go when nobody is.
     */
    val changes: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 0)

    /** Every track on the device, unordered -- callers sort by whatever they are showing. */
    suspend fun all(): List<Song> = query(selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0")

    /** The one track at [uriString], or null once the store no longer has it. */
    suspend fun byUri(uriString: String): Song? {
        val id = uriString.toUri().lastPathSegment?.toLongOrNull() ?: return null
        return query(
            selection = "${MediaStore.Audio.Media._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
        ).firstOrNull()
    }

    private suspend fun query(selection: String, selectionArgs: Array<String>? = null): List<Song> =
        withContext(dispatcher) {
            context.contentResolver.query(
                Collection,
                Projection,
                selection,
                selectionArgs,
                // No sort order: the chosen one is applied in memory, and asking the store to sort as
                // well is the same work done twice, the first time by the wrong key.
                null,
            )?.use { cursor -> cursor.readSongs() }.orEmpty()
        }

    private fun Cursor.readSongs(): List<Song> {
        // Resolved once, not once per row: eleven columns across a five-hundred-track library would
        // otherwise be five and a half thousand string lookups.
        val idColumn = getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val fileNameColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val pathColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val titleColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val mimeTypeColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val sizeColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val durationColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val dateAddedColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

        // Sized up front, so a large library does not grow its list by doubling it a dozen times.
        val songs = ArrayList<Song>(count)
        while (moveToNext()) {
            val id = getLong(idColumn)
            val albumId = getLong(albumIdColumn)
            songs += Song(
                id = id,
                uri = ContentUris.withAppendedId(Collection, id),
                fileName = getString(fileNameColumn).orEmpty(),
                folderPath = getString(pathColumn).orEmpty().substringBeforeLast('/', ""),
                title = getString(titleColumn).orEmpty(),
                artist = getString(artistColumn).orEmpty(),
                album = getString(albumColumn).orEmpty(),
                mimeType = getString(mimeTypeColumn).orEmpty(),
                sizeBytes = getLong(sizeColumn),
                durationMs = getLong(durationColumn),
                dateAddedSeconds = getLong(dateAddedColumn),
                artworkUri = albumId.takeIf { it > 0 }?.let { ContentUris.withAppendedId(AlbumArt, it) },
            )
        }
        return songs
    }

    private companion object {
        val Collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        /** Undocumented and unchanged since Android 1, and the only route to a cover before API 29. */
        val AlbumArt = "content://media/external/audio/albumart".toUri()

        val Projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
        )
    }
}
