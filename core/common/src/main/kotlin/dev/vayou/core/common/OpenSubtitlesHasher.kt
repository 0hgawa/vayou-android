package dev.vayou.core.common

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OpenSubtitlesHasher {

    private const val CHUNK_SIZE = 65536L

    suspend fun computeHash(filePath: String): Pair<String, Long>? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || file.length() < CHUNK_SIZE) return@withContext null

            val fileSize = file.length()
            var hash = fileSize

            RandomAccessFile(file, "r").use { raf ->
                hash += checksumChunk(raf, 0)
                hash += checksumChunk(raf, fileSize - CHUNK_SIZE)
            }

            String.format("%016x", hash) to fileSize
        } catch (_: Exception) {
            null
        }
    }

    private fun checksumChunk(raf: RandomAccessFile, offset: Long): Long {
        raf.seek(offset)
        val buffer = ByteArray(CHUNK_SIZE.toInt())
        raf.readFully(buffer)
        val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        var sum = 0L
        while (byteBuffer.hasRemaining()) {
            sum += byteBuffer.long
        }
        return sum
    }
}
