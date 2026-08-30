package dev.vayou.core.common.extensions

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import java.io.File
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val VIDEO_COLLECTION_URI: Uri
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

/**
 * The filesystem path behind a `content://` uri, or null when there is none — a provider is free to
 * serve bytes that were never a file. Each branch below unpacks one authority's document-id format.
 */
fun Context.getPath(uri: Uri): String? {
    if (DocumentsContract.isDocumentUri(this, uri)) {
        when {
            uri.isExternalStorageDocument -> {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId
                    .split(":".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().path + "/" + split[1]
                }
            }

            uri.isDownloadsDocument -> {
                val docId = DocumentsContract.getDocumentId(uri)
                if (docId.isDigitsOnly()) {
                    return try {
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            docId.toLong(),
                        )
                        getDataColumn(contentUri, null, null)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            uri.isMediaDocument -> {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1],
                )
                return contentUri?.let { getDataColumn(it, selection, selectionArgs) }
            }
        }
    } else if (ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true)) {
        if (uri.isLocalPhotoPickerUri) return null
        if (uri.isCloudPhotoPickerUri) return null

        return if (uri.isGooglePhotosUri) {
            uri.lastPathSegment
        } else {
            getDataColumn(uri, null, null)
        }
    } else if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }
    return null
}

private fun Context.getDataColumn(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): String? {
    val column = MediaStore.Images.Media.DATA
    val projection = arrayOf(column)
    try {
        contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        }
    } catch (e: Exception) {
        return null
    }
    return null
}

/**
 * What to call a file that arrived from somewhere else, with no name attached.
 *
 * A provider is the only one who can say: the uri it hands out need not resemble a path, and its
 * last segment is often an opaque id. Falls back to that segment anyway, because a wrong name reads
 * better than a blank space where a name should be.
 */
fun Context.displayNameOf(uri: Uri): String? {
    if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) return File(uri.path.orEmpty()).name

    val name = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            cursor.takeIf { it.moveToFirst() }?.getString(0)
        }
    }.getOrNull()

    return name ?: uri.lastPathSegment
}

suspend fun Context.scanPaths(paths: List<String>): Boolean = suspendCoroutine { continuation ->
    try {
        MediaScannerConnection.scanFile(
            this@scanPaths,
            paths.toTypedArray(),
            arrayOf("video/*"),
        ) { path, uri ->
            Log.d("ScanPath", "scanPaths: path=$path, uri=$uri")
            continuation.resumeWith(Result.success(true))
        }
    } catch (e: Exception) {
        continuation.resumeWith(Result.failure(e))
    }
}

suspend fun Context.scanPath(file: File): Boolean = if (file.isDirectory) {
    file.listFiles()?.all { scanPath(it) } ?: true
} else {
    scanPaths(listOf(file.path))
}

suspend fun Context.scanStorage(storagePath: String? = Environment.getExternalStorageDirectory()?.path): Boolean =
    withContext(Dispatchers.IO) {
        if (storagePath != null) {
            return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                scanPaths(listOf(storagePath))
            } else {
                scanPath(File(storagePath))
            }
        } else {
            false
        }
    }

suspend fun ContentResolver.updateMedia(uri: Uri, contentValues: ContentValues): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        update(
            uri,
            contentValues,
            null,
            null,
        ) > 0
    } catch (e: Exception) {
        Log.w("MediaOps", "updateMedia failed", e)
        false
    }
}

suspend fun ContentResolver.deleteMedia(uri: Uri): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        delete(uri, null, null) > 0
    } catch (e: Exception) {
        Log.w("MediaOps", "deleteMedia failed", e)
        false
    }
}

/**
 * Opens this app's own page in the system settings.
 *
 * The only way back from a permission refused for good. Android stops drawing the dialog once that
 * has happened -- asking again returns refused without showing anything -- so a screen that needs
 * one has nowhere to send anybody except here.
 *
 * NEW_TASK because the caller is as often a service or a composable holding the application context
 * as it is an activity, and settings started from a non-activity context without it throws.
 */
fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun Context.getStorageVolumes() = try {
    getExternalFilesDirs(null)?.mapNotNull {
        File(it.path.substringBefore("/Android")).takeIf { file -> file.exists() }
    } ?: listOf(Environment.getExternalStorageDirectory())
} catch (e: Exception) {
    listOf(Environment.getExternalStorageDirectory())
}

/**
 * The version installed, kept whole in every language.
 *
 * Asked of the system rather than compiled in: the package already knows, and a second copy of the
 * number is a second thing to forget to bump.
 *
 * Wrapped in a left-to-right isolate because a version is one symbol and not a phrase. Arabic reads
 * the other way, and a hyphen between a number and a word is a character with no direction of its own:
 * a build carrying a suffix showed `0.1.0-next` on a television in Cairo as `next-0.1.0`, the two
 * halves laid out in the order the page runs. The isolate says: this run has its own direction, read
 * it as it is written.
 *
 * Kept although today's numbers are digits alone, which the algorithm leaves whole. What a version
 * name may hold is decided in a build file, not here.
 */
fun Context.versionName(): String =
    "\u2066" + packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() + "\u2069"
