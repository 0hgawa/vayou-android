package dev.vayou.core.media.services

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vayou.core.common.extensions.deleteMedia
import dev.vayou.core.common.extensions.getPath
import dev.vayou.core.common.extensions.updateMedia
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class LocalMediaService @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaService {

    private lateinit var activity: Activity
    private val contentResolver = context.contentResolver
    private var resultOkCallback: () -> Unit = {}
    private var resultCancelledCallback: () -> Unit = {}
    private var mediaRequestLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    override fun initialize(activity: ComponentActivity) {
        this.activity = activity
        mediaRequestLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> resultOkCallback()
                Activity.RESULT_CANCELED -> resultCancelledCallback()
            }
        }
    }

    override suspend fun deleteMedia(uris: List<Uri>): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteMediaR(uris)
        } else {
            deleteMediaBelowR(uris)
        }
    }

    override suspend fun renameMedia(uri: Uri, to: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            renameMediaR(uri, to)
        } else {
            renameMediaBelowR(uri, to)
        }
    }

    override suspend fun shareMedia(uris: List<Uri>) {
        val intent = Intent.createChooser(
            Intent().apply {
                type = "video/*"
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            },
            null,
        )
        activity.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchWriteRequest(
        uris: List<Uri>,
        onResultCanceled: () -> Unit = {},
        onResultOk: () -> Unit = {},
    ) {
        resultOkCallback = onResultOk
        resultCancelledCallback = onResultCanceled
        MediaStore.createWriteRequest(contentResolver, uris).also { intent ->
            mediaRequestLauncher?.launch(IntentSenderRequest.Builder(intent).build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchDeleteRequest(
        uris: List<Uri>,
        onResultCanceled: () -> Unit = {},
        onResultOk: () -> Unit = {},
    ) {
        resultOkCallback = onResultOk
        resultCancelledCallback = onResultCanceled
        MediaStore.createDeleteRequest(contentResolver, uris).also { intent ->
            mediaRequestLauncher?.launch(IntentSenderRequest.Builder(intent).build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun deleteMediaR(uris: List<Uri>): Boolean = suspendCancellableCoroutine { continuation ->
        launchDeleteRequest(
            uris = uris,
            onResultOk = { continuation.resume(true) },
            onResultCanceled = { continuation.resume(false) },
        )
    }

    private suspend fun deleteMediaBelowR(uris: List<Uri>): Boolean {
        return uris.map { uri ->
            contentResolver.deleteMedia(uri)
        }.all { it }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun renameMediaR(uri: Uri, to: String): Boolean = suspendCancellableCoroutine { continuation ->
        val scope = CoroutineScope(Dispatchers.Default)
        launchWriteRequest(
            uris = listOf(uri),
            onResultOk = {
                scope.launch {
                    val result = contentResolver.updateMedia(
                        uri = uri,
                        contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, to)
                        },
                    )
                    continuation.resume(result)
                }
            },
            onResultCanceled = { continuation.resume(false) },
        )
        continuation.invokeOnCancellation { scope.cancel() }
    }

    private suspend fun renameMediaBelowR(uri: Uri, to: String): Boolean {
        return runCatching {
            val oldFile = context.getPath(uri)?.let { File(it) } ?: throw Error()
            val newFile = File(oldFile.parentFile, to)
            oldFile.renameTo(newFile).also { success ->
                if (success) {
                    contentResolver.updateMedia(
                        uri = uri,
                        contentValues = ContentValues().apply {
                            put(MediaStore.Files.FileColumns.DISPLAY_NAME, to)
                            put(MediaStore.Files.FileColumns.TITLE, to)
                            put(MediaStore.Files.FileColumns.DATA, newFile.path)
                        },
                    )
                }
            }
        }.getOrNull() ?: false
    }

    override suspend fun moveToPrivate(uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val privateDir = File(context.filesDir, "private").also { it.mkdirs() }
            val fileName = contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
                ?: "video_${System.currentTimeMillis()}.mp4"

            var destFile = File(privateDir, fileName)
            var counter = 1
            while (destFile.exists()) {
                val base = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                destFile = File(privateDir, if (ext.isEmpty()) "${base}_$counter" else "${base}_$counter.$ext")
                counter++
            }

            val inputStream = contentResolver.openInputStream(uri) ?: return@runCatching null
            inputStream.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            val deleted = deleteMedia(listOf(uri))
            if (!deleted) {
                destFile.delete()
                return@runCatching null
            }

            destFile
        }.getOrNull()
    }

    override suspend fun restoreFromPrivate(filePath: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val privateFile = File(filePath)
            if (!privateFile.exists()) return@runCatching false

            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            moviesDir.mkdirs()
            val fileName = privateFile.name
            var destFile = File(moviesDir, fileName)
            var counter = 1
            while (destFile.exists()) {
                val base = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                destFile = File(moviesDir, if (ext.isEmpty()) "${base}_$counter" else "${base}_$counter.$ext")
                counter++
            }

            privateFile.inputStream().use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("video/*"), null)
            privateFile.delete()
            true
        }.getOrNull() ?: false
    }
}
