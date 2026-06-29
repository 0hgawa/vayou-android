package dev.vayou.core.media.services

import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import java.io.File

interface MediaService {
    fun initialize(activity: ComponentActivity)
    suspend fun deleteMedia(uris: List<Uri>): Boolean
    suspend fun renameMedia(uri: Uri, to: String): Boolean
    suspend fun shareMedia(uris: List<Uri>)
    suspend fun moveToPrivate(uri: Uri): File?
    suspend fun restoreFromPrivate(filePath: String): Boolean

    companion object {
        fun willSystemAsksForDeleteConfirmation(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        }
    }
}
