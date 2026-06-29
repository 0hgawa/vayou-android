package dev.vayou.core.smb

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
class SmbDataSource(private val client: SmbClient) : BaseDataSource(/* isNetwork = */ true) {

    companion object {
        private const val BUFFER_SIZE = 1024 * 1024  // 1 MB
    }

    private var uri: Uri? = null
    private var fileSize: Long = 0L
    private var position: Long = 0L
    private var bytesRemaining: Long = 0L

    private var file: com.hierynomus.smbj.share.File? = null
    private val buffer = ByteArray(BUFFER_SIZE)
    private var bufferStart = -1L
    private var bufferFilled = 0

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val host = dataSpec.uri.host ?: throw IOException("Invalid SMB URI: ${dataSpec.uri}")
        val (shareName, filePath) = parseUri(dataSpec.uri)

        runCatching { file?.close() }

        val opened = client.openFileForStreaming(host, shareName, filePath)
        file = opened.file
        fileSize = opened.size

        position = dataSpec.position
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length
                         else fileSize - position

        bufferFilled = 0
        bufferStart = -1L

        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(output: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = minOf(length.toLong(), bytesRemaining).toInt()

        // Serve from buffer if position is covered
        serveFromBuffer(position, toRead, output, offset)?.let { n ->
            advance(n)
            return n
        }

        // Buffer miss — fetch next chunk from SMB
        val toFetch = minOf(BUFFER_SIZE.toLong(), fileSize - position).toInt()
        val n = try {
            file!!.read(buffer, position, 0, toFetch)
        } catch (e: Exception) {
            throw IOException("SMB read failed at offset $position", e)
        }
        if (n <= 0) return C.RESULT_END_OF_INPUT

        bufferStart = position
        bufferFilled = n

        val available = minOf(toRead, n)
        buffer.copyInto(output, offset, 0, available)
        advance(available)
        return available
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        runCatching { file?.close() }
        file = null
        uri = null
        bytesRemaining = 0L
        bufferFilled = 0
        bufferStart = -1L
        transferEnded()
    }

    private fun serveFromBuffer(pos: Long, toRead: Int, dst: ByteArray, dstOffset: Int): Int? {
        if (bufferFilled <= 0 || pos < bufferStart || pos >= bufferStart + bufferFilled) return null
        val inBuf = (pos - bufferStart).toInt()
        val available = minOf(toRead, bufferFilled - inBuf)
        buffer.copyInto(dst, dstOffset, inBuf, inBuf + available)
        return available
    }

    private fun advance(bytes: Int) {
        position += bytes
        bytesRemaining -= bytes
        bytesTransferred(bytes)
    }

    private fun parseUri(uri: Uri): Pair<String, String> {
        val segments = uri.pathSegments
        val shareName = segments.firstOrNull() ?: throw IOException("Invalid SMB URI: $uri")
        val filePath = segments.drop(1).joinToString("\\")
        return shareName to filePath
    }
}

@Singleton
class SmbDataSourceFactory @Inject constructor(private val client: SmbClient) : DataSource.Factory {
    @OptIn(UnstableApi::class)
    override fun createDataSource(): SmbDataSource = SmbDataSource(client)
}
