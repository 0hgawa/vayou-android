package dev.vayou.feature.player.cast

import android.content.ContentResolver
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private const val STREAM_BUFFER_SIZE = 128 * 1024
private const val TAG = "CastMediaServer"

class CastMediaServer(
    private val contentResolver: ContentResolver,
    private val cacheDir: File? = null,
) : NanoHTTPD("0.0.0.0", 0) {

    private val mediaMap = ConcurrentHashMap<String, Uri>()
    private val streamIdCounter = AtomicInteger(0)
    private val pathCache = ConcurrentHashMap<Uri, String?>()
    private val remuxCache = ConcurrentHashMap<String, String>()
    private val remuxFailed = ConcurrentHashMap.newKeySet<String>()
    private val remuxInProgress = ConcurrentHashMap.newKeySet<String>()

    fun ensureStarted() {
        if (!isAlive) start()
    }

    fun getStreamUrl(uri: Uri, filename: String? = null): String {
        val id = streamIdCounter.getAndIncrement().toString()
        mediaMap[id] = uri
        val suffix = filename
            ?.takeIf { it.isNotBlank() }
            ?.let { "/" + it.replace(" ", "%20") }
            .orEmpty()
        return "http://$localIpAddress:$listeningPort/media/$id$suffix"
    }

    override fun serve(session: IHTTPSession): Response {
        val path = session.uri
        if (path == null || !path.startsWith("/media/")) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }

        val id = path.removePrefix("/media/").substringBefore('/')
        val uri = mediaMap[id]
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")

        val filePath = resolveFilePath(uri)
        return if (filePath != null) {
            serveFromFile(session, filePath)
        } else {
            serveFromContentResolver(session, uri)
        }
    }

    private fun resolveFilePath(uri: Uri): String? {
        pathCache[uri]?.let { return it }
        val resolved = resolveFilePathUncached(uri)
        pathCache[uri] = resolved
        return resolved
    }

    private fun resolveFilePathUncached(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        if (uri.scheme == "content") {
            try {
                contentResolver.query(
                    uri,
                    arrayOf(MediaStore.MediaColumns.DATA),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val path = cursor.getString(0)
                        if (path != null && File(path).canRead()) return path
                    }
                }
            } catch (_: Exception) {
                // Fallback to ContentResolver streaming
            }
        }
        return null
    }

    private fun serveFromFile(session: IHTTPSession, filePath: String): Response {
        val servePath = if (filePath.lowercase().endsWith(".mkv")) {
            remuxToMp4(filePath) ?: filePath
        } else {
            filePath
        }

        val file = File(servePath)
        if (!file.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }

        val mimeType = mimeTypeForPath(servePath)
        val totalSize = file.length()
        val rangeHeader = session.headers["range"]
        val raf = RandomAccessFile(file, "r")

        if (rangeHeader != null && totalSize > 0) {
            val (start, end) = parseRange(rangeHeader, totalSize)
            val length = end - start + 1
            raf.seek(start)
            val stream = RandomAccessInputStream(raf, length)
            return newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, stream, length).apply {
                addHeader("Content-Range", "bytes $start-$end/$totalSize")
                addStreamingHeaders()
            }
        }

        val stream = RandomAccessInputStream(raf, totalSize)
        return newFixedLengthResponse(Response.Status.OK, mimeType, stream, totalSize).apply {
            addStreamingHeaders()
        }
    }

    private fun serveFromContentResolver(session: IHTTPSession, uri: Uri): Response {
        val mimeType = mimeTypeForUri(uri)
        val pfd = contentResolver.openFileDescriptor(uri, "r")
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Cannot open")
        val totalSize = pfd.statSize
        pfd.close()

        val rangeHeader = session.headers["range"]
        if (rangeHeader != null && totalSize > 0) {
            val (start, end) = parseRange(rangeHeader, totalSize)
            val length = end - start + 1

            val pfd2 = contentResolver.openFileDescriptor(uri, "r")
                ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot read")
            val rawStream = ParcelFileDescriptor.AutoCloseInputStream(pfd2)
            if (start > 0) rawStream.skip(start)
            val stream = BufferedInputStream(rawStream, STREAM_BUFFER_SIZE)

            return newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, stream, length).apply {
                addHeader("Content-Range", "bytes $start-$end/$totalSize")
                addStreamingHeaders()
            }
        }

        val stream = BufferedInputStream(
            contentResolver.openInputStream(uri)
                ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot read"),
            STREAM_BUFFER_SIZE,
        )

        return newFixedLengthResponse(Response.Status.OK, mimeType, stream, totalSize).apply {
            addStreamingHeaders()
        }
    }

    private fun remuxToMp4(mkvPath: String): String? {
        if (mkvPath in remuxFailed) return null

        remuxCache[mkvPath]?.let { cached ->
            if (File(cached).exists()) return cached
        }

        if (!remuxInProgress.add(mkvPath)) return null

        val dir = cacheDir ?: run { remuxInProgress.remove(mkvPath); return null }
        val castCacheDir = File(dir, "cast_remux").apply { mkdirs() }
        val outputFile = File(castCacheDir, File(mkvPath).nameWithoutExtension + ".mp4")

        if (outputFile.exists()) {
            remuxCache[mkvPath] = outputFile.absolutePath
            remuxInProgress.remove(mkvPath)
            return outputFile.absolutePath
        }

        Log.d(TAG, "Remuxing MKV→MP4: $mkvPath")
        val startTime = System.currentTimeMillis()

        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(mkvPath)

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    trackMap[i] = muxer.addTrack(format)
                    extractor.selectTrack(i)
                }
            }

            muxer.start()
            val buffer = ByteBuffer.allocate(STREAM_BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val outTrack = trackMap[extractor.sampleTrackIndex]
                if (outTrack == null) {
                    extractor.advance()
                    continue
                }

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(outTrack, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Remux done in ${elapsed}ms: ${outputFile.length() / 1024}KB")

            remuxCache[mkvPath] = outputFile.absolutePath
            remuxInProgress.remove(mkvPath)
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Remux failed: ${e.message}")
            outputFile.delete()
            remuxFailed.add(mkvPath)
            remuxInProgress.remove(mkvPath)
            return null
        }
    }

    private fun parseRange(rangeHeader: String, totalSize: Long): Pair<Long, Long> {
        val range = rangeHeader.replace("bytes=", "").trim()
        val parts = range.split("-", limit = 2)
        val start = parts[0].toLongOrNull() ?: 0
        val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
            (parts[1].toLongOrNull() ?: (totalSize - 1)).coerceAtMost(totalSize - 1)
        } else {
            totalSize - 1
        }
        return start to end
    }

    private fun Response.addStreamingHeaders() {
        addHeader("Accept-Ranges", "bytes")
        addHeader("Connection", "keep-alive")
        addHeader("Cache-Control", "no-cache")
        addHeader("Access-Control-Allow-Origin", "*")
    }

    companion object {

        fun mimeTypeForUri(uri: Uri): String = mimeTypeForPath(uri.path ?: uri.toString())

        private fun mimeTypeForPath(path: String): String {
            val lpath = path.lowercase()
            return when {
                lpath.endsWith(".mp4") || lpath.endsWith(".m4v") -> "video/mp4"
                lpath.endsWith(".mkv") -> "video/x-matroska"
                lpath.endsWith(".webm") -> "video/webm"
                lpath.endsWith(".avi") -> "video/x-msvideo"
                lpath.endsWith(".mov") || lpath.endsWith(".qt") -> "video/quicktime"
                lpath.endsWith(".wmv") -> "video/x-ms-wmv"
                lpath.endsWith(".flv") -> "video/x-flv"
                lpath.endsWith(".3gp") -> "video/3gpp"
                lpath.endsWith(".ts") || lpath.endsWith(".mts") || lpath.endsWith(".m2ts") -> "video/MP2T"
                lpath.endsWith(".mpg") || lpath.endsWith(".mpeg") -> "video/mpeg"
                lpath.endsWith(".ogv") || lpath.endsWith(".ogg") -> "video/ogg"
                lpath.endsWith(".vob") -> "video/x-ms-vob"
                lpath.endsWith(".vtt") -> "text/vtt"
                lpath.endsWith(".srt") -> "application/x-subrip"
                lpath.endsWith(".ssa") || lpath.endsWith(".ass") -> "text/x-ssa"
                lpath.endsWith(".ttml") || lpath.endsWith(".dfxp") -> "application/ttml+xml"
                else -> "video/*"
            }
        }

        val localIpAddress: String
            get() {
                try {
                    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
                    for (intf in interfaces) {
                        for (addr in intf.inetAddresses) {
                            if (!addr.isLoopbackAddress && addr is Inet4Address) {
                                return addr.hostAddress ?: continue
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fallback
                }
                return "127.0.0.1"
            }
    }
}

private class RandomAccessInputStream(
    private val raf: RandomAccessFile,
    private val remaining: Long,
) : InputStream() {

    private var bytesRead = 0L

    override fun read(): Int {
        if (bytesRead >= remaining) return -1
        val b = raf.read()
        if (b >= 0) bytesRead++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (bytesRead >= remaining) return -1
        val toRead = len.toLong().coerceAtMost(remaining - bytesRead).toInt()
        val read = raf.read(b, off, toRead)
        if (read > 0) bytesRead += read
        return read
    }

    override fun close() {
        raf.close()
    }

    override fun available(): Int = (remaining - bytesRead).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
