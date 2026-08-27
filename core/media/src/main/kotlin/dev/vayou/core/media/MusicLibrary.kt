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
import dev.vayou.core.data.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
    private val preferencesRepository: PreferencesRepository,
) {

    /** A look asked for by hand, which is what the rescan in the settings is. */
    private val rescans = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * A tick whenever the store's audio changes.
     *
     * Here and not in a view model: the observer is one registration for the whole app, and a
     * second screen that also watches used to mean a second observer and a second scan of the
     * library for every change. Shared while anyone is listening and let go when nobody is.
     */
    val changes: Flow<Unit> = merge(
        callbackFlow {
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
        },
        rescans,
        // Keeping a folder out of the library changes what the library holds as surely as deleting
        // it would, and the reader who just excluded one is looking at the list while they do it.
        // Dropped once, because a subscriber is handed the setting as it stands and that is not a
        // change.
        preferencesRepository.applicationPreferences
            .map { it.excludeFolders }
            .distinctUntilChanged()
            .drop(1)
            .map { },
    ).shareIn(scope, SharingStarted.WhileSubscribed(), replay = 0)

    /** Look again, for a reader who thinks something was missed. */
    fun rescan() {
        rescans.tryEmit(Unit)
    }

    /**
     * Every track on the device, unordered -- callers sort by whatever they are showing -- less
     * whatever sits in a folder the reader has kept out.
     *
     * Excluded here rather than by each screen, so the list, the folders, the queue handed to the
     * player and the television all agree about what the library holds. The video side does this in
     * its own use case, which is why a folder excluded there went on showing its music.
     */
    suspend fun all(): List<Song> = query(
        selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        excluding = preferencesRepository.applicationPreferences.value.excludeFolders.toSet(),
    )

    /**
     * Every folder holding audio, whether it is in the library or kept out of it.
     *
     * Unfiltered on purpose: this answers the screen that does the excluding, and a folder that
     * vanished from that list the moment it was excluded could never be let back in.
     *
     * Reads the one column rather than going through [all]: the paths are all it needs, and
     * building a track per row to throw it away is a library of objects for a list of folders.
     */
    suspend fun folders(): Set<String> = withContext(dispatcher) {
        context.contentResolver.query(
            Collection,
            arrayOf(MediaStore.Audio.Media.DATA),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null,
        )?.use { cursor ->
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val paths = LinkedHashSet<String>()
            while (cursor.moveToNext()) {
                val folder = cursor.getString(pathColumn).orEmpty().substringBeforeLast('/', "")
                if (folder.isNotEmpty()) paths += folder
            }
            paths
        }.orEmpty()
    }

    /** The one track at [uriString], or null once the store no longer has it. */
    suspend fun byUri(uriString: String): Song? {
        val id = uriString.toUri().lastPathSegment?.toLongOrNull() ?: return null
        return query(
            selection = "${MediaStore.Audio.Media._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
        ).firstOrNull()
    }

    private suspend fun query(
        selection: String,
        selectionArgs: Array<String>? = null,
        excluding: Set<String> = emptySet(),
    ): List<Song> = withContext(dispatcher) {
        context.contentResolver.query(
            Collection,
            Projection,
            selection,
            selectionArgs,
            // No sort order: the chosen one is applied in memory, and asking the store to sort as
            // well is the same work done twice, the first time by the wrong key.
            null,
        )?.use { cursor -> cursor.readSongs(excluding) }.orEmpty()
    }

    private fun Cursor.readSongs(excluding: Set<String>): List<Song> {
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
        val dateAddedColumn = getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

        // Sized up front, so a large library does not grow its list by doubling it a dozen times.
        val songs = ArrayList<Song>(count)
        while (moveToNext()) {
            val folderPath = getString(pathColumn).orEmpty().substringBeforeLast('/', "")
            // Tested before the track is built rather than after: a folder is usually kept out
            // because of how much is in it, and each of those would be an object made to be dropped.
            if (folderPath in excluding) continue
            val id = getLong(idColumn)
            val trackUri = ContentUris.withAppendedId(Collection, id)
            songs += Song(
                id = id,
                uri = trackUri,
                fileName = getString(fileNameColumn).orEmpty(),
                folderPath = folderPath,
                title = getString(titleColumn).orEmpty(),
                artist = getString(artistColumn).orEmpty(),
                album = getString(albumColumn).orEmpty(),
                mimeType = getString(mimeTypeColumn).orEmpty(),
                sizeBytes = getLong(sizeColumn),
                durationMs = getLong(durationColumn),
                dateAddedSeconds = getLong(dateAddedColumn),
                // The track's own address, not the album's.
                //
                // Two reasons, and the second is a bug the first hid. The album address is the
                // legacy one a player built by hand, and on modern Android the provider answers it
                // with "failed to create image decoder" -- so covers came up empty. And a picture
                // written into one file is that file's, not its album's: editing a track's cover
                // changed nothing on screen, because the screen was asking the album what it looked
                // like. Asked about the track, the provider answers with what is inside it and
                // falls back to the album's when the file carries none.
                artworkUri = trackUri,
            )
        }
        return songs
    }

    private companion object {
        val Collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

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
