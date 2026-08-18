package dev.vayou.core.player.cast

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Every address on this device, so a receiver reaches us on whichever one the Wi-Fi is. */
private const val AllInterfaces = "0.0.0.0"

/** Zero: the system picks a free port, so two runs never collide. */
private const val AnyPort = 0

private const val MediaPath = "/media/"

private const val RangeHeader = "range"

private const val StreamBufferSize = 128 * 1024

/** Named here rather than spelled out at each of its three uses. */
internal const val MimeHls = "application/x-mpegURL"

private const val FetchTimeoutMs = 10_000

/** Enough for a shortener that lands on a load balancer that lands on a region. */
private const val MaxRedirects = 5

private val Redirects = setOf(301, 302, 303, 307, 308)

/**
 * A browser's, because a channel's server is answering one.
 *
 * The default on Android names Dalvik, and a public channel list has entries behind a CDN that
 * turns that away -- a channel that played a moment ago through the player's own client would
 * start failing the day it began coming through here.
 */
private const val BrowserUserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

/** The two schemes that mean the address is somewhere else, and so something to fetch. */
private val Remote = setOf("http", "https")

/**
 * What a proxied playlist is called on the way out.
 *
 * The name is never read back. It is on the end of the URL because a receiver picks a reader by
 * looking at the extension, and one of ours would otherwise arrive with none.
 */
private const val PlaylistName = "playlist.m3u8"

/** `URI="…"`, the way a playlist names another playlist inside a tag. */
private val UriAttribute = Regex("""URI="([^"]*)"""")

/** Whether this address is itself a playlist, and so something the receiver must be able to read. */
private val String.isPlaylist: Boolean
    get() = substringBefore('?').substringAfterLast('.', "").equals("m3u8", ignoreCase = true)

/**
 * What kind of file this is, by its extension.
 *
 * By name rather than by asking the store: half of what gets cast comes from a share or a download
 * and has no store entry, and a receiver handed a wildcard type guesses wrong often enough to
 * matter.
 *
 * Careful with the wildcard in a comment: a block comment in Kotlin nests, so writing it out opens
 * one that never closes and silently swallows the rest of the file.
 */
fun castMimeTypeFor(path: String): String = MimeTypes[path.substringAfterLast('.', "").lowercase()] ?: "video/*"

/**
 * The address this phone answers on, for the receiver to come back to.
 *
 * The first non-loopback IPv4 on any interface. Loopback resolves on the phone and nowhere else,
 * which is the shape of bug that looks like the receiver being broken.
 */
private val localAddress: String
    get() = runCatching {
        NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull() ?: "127.0.0.1"

/** `bytes=start-end`, either end allowed to be missing, clamped to what the file has. */
private fun String.asByteRange(total: Long): Pair<Long, Long> {
    val parts = removePrefix("bytes=").trim().split('-', limit = 2)
    val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
    val end = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: (total - 1)
    return start to end.coerceAtMost(total - 1)
}

/**
 * [length] bytes from where [handle] is now, and not one more.
 *
 * A `RandomAccessFile` would happily read past the end of a range and hand the receiver more than
 * it asked for, which it answers by dropping the connection.
 */
private class BoundedFileStream(private val handle: RandomAccessFile, private val length: Long) : InputStream() {

    private var read = 0L

    override fun read(): Int {
        if (read >= length) return -1
        return handle.read().also { if (it >= 0) read++ }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (read >= length) return -1
        val wanted = len.toLong().coerceAtMost(length - read).toInt()
        return handle.read(b, off, wanted).also { if (it > 0) read += it }
    }

    override fun available(): Int = (length - read).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    override fun close() = handle.close()
}

/**
 * A web server on the phone, so a receiver can read a file that is on the phone.
 *
 * This is the part casting a local file needs that no SDK provides. A receiver is another machine
 * on the network: it is handed a URL and fetches it itself, and `content://media/external/...`
 * means nothing outside this process. So every local address is published here as an `http://` one,
 * bound to the Wi-Fi address, for as long as the session lasts.
 *
 * Range requests are answered, because seeking is the whole reason a receiver asks for one, and
 * without `Accept-Ranges` it will not let anyone scrub at all.
 */
class CastMediaServer(private val contentResolver: ContentResolver) : NanoHTTPD(AllInterfaces, AnyPort) {

    /**
     * Published addresses, by the number they were given.
     *
     * A number rather than the address itself: a `content://` uri inside a URL has to survive two
     * rounds of escaping, and one of the two devices doing it wrong is a file that will not play
     * for no visible reason.
     */
    private val published = ConcurrentHashMap<String, Uri>()

    /**
     * The same table read the other way, so an address published twice keeps its number.
     *
     * A channel's playlist is fetched again every few seconds for as long as it is on. Numbering
     * each fetch afresh would add a row a fetch -- an evening on one channel is thousands of rows
     * for one address.
     */
    private val numbers = ConcurrentHashMap<Uri, String>()

    private val nextId = AtomicInteger(0)

    /** Resolved once per address: the query behind it hits a content provider. */
    private val pathCache = ConcurrentHashMap<Uri, String>()

    fun ensureStarted() {
        if (!isAlive) start()
    }

    /**
     * A URL the receiver can fetch [uri] from.
     *
     * [fileName] rides on the end and is never read back. It is there for the receiver, which picks
     * a decoder by looking at the extension in the URL, and refuses a subtitle track outright when
     * the URL has none.
     */
    fun publish(uri: Uri, fileName: String? = null): String {
        val id = numbers.getOrPut(uri) { nextId.getAndIncrement().toString().also { published[it] = uri } }
        val suffix = fileName?.takeIf { it.isNotBlank() }?.let { "/" + it.replace(" ", "%20") }.orEmpty()
        return "http://$localAddress:$listeningPort$MediaPath$id$suffix"
    }

    override fun serve(session: IHTTPSession): Response {
        val path = session.uri?.takeIf { it.startsWith(MediaPath) } ?: return notFound()
        val uri = published[path.removePrefix(MediaPath).substringBefore('/')] ?: return notFound()
        if (uri.scheme in Remote) return servePlaylist(uri)
        val filePath = filePathFor(uri)
        return if (filePath == null) serveThroughResolver(session, uri) else serveFile(session, filePath)
    }

    /**
     * A channel's playlist, fetched here and handed on with this phone's permission on it.
     *
     * The one thing casting a channel needs that no SDK provides, and the mirror of what the rest
     * of this class does for a local file. A receiver is a web page, and a web page may only read
     * what the server it asked from says it may -- while a channel's playlist typically names one
     * origin, its own, which the receiver is not. The player on this phone is a native client and
     * no such rule applies to it, which is why the same address plays here and hangs there.
     *
     * Only the playlists come through. They are a few kilobytes of text; the video they point at
     * stays absolute and the receiver fetches it straight from the network it lives on, which is
     * the whole reason this costs nothing. Segment servers say `*` -- it is the playlist in front
     * of them that does not.
     */
    private fun servePlaylist(uri: Uri): Response {
        val (base, body) = fetchText(uri) ?: return notFound()
        return newFixedLengthResponse(Response.Status.OK, MimeHls, body.withReachableAddresses(base)).forReceiver()
    }

    /** The text at [uri] and the address it turned out to be at, which relative lines resolve against. */
    private fun fetchText(uri: Uri): Pair<URL, String>? = runCatching {
        var url = URL(uri.toString())
        repeat(MaxRedirects) {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = FetchTimeoutMs
                readTimeout = FetchTimeoutMs
                setRequestProperty("User-Agent", BrowserUserAgent)
                // Followed by hand: the built-in one stops at a change of protocol, and a public
                // channel list is full of shorteners that cross it. Following it here is also how
                // the address the playlist is really at becomes known.
                instanceFollowRedirects = false
            }
            val location = connection.takeIf { it.responseCode in Redirects }?.getHeaderField("Location")
            if (location == null) {
                return@runCatching url to connection.inputStream.bufferedReader().use { it.readText() }
            }
            connection.disconnect()
            url = URL(url, location)
        }
        null
    }.getOrNull()

    /**
     * Every address in a playlist made absolute, and every address that is itself a playlist
     * pointed back here.
     *
     * Absolute because the receiver now reads this from a different host to the one that wrote it,
     * and a relative line would resolve against this phone. Back here because a playlist is read
     * rather than played, and reading is the thing the receiver is not allowed to do.
     */
    private fun String.withReachableAddresses(base: URL): String = buildString(length) {
        this@withReachableAddresses.lineSequence().forEach { line ->
            append(
                when {
                    line.isBlank() -> line
                    !line.startsWith('#') -> reachable(line, base)
                    else -> UriAttribute.find(line)?.let { attribute ->
                        line.replaceRange(attribute.range, """URI="${reachable(attribute.groupValues[1], base)}"""")
                    } ?: line
                },
            )
            append('\n')
        }
    }

    private fun reachable(address: String, base: URL): String {
        val absolute = runCatching { URL(base, address).toString() }.getOrDefault(address)
        return if (absolute.isPlaylist) publish(Uri.parse(absolute), PlaylistName) else absolute
    }

    /**
     * The real path behind [uri], where there is one.
     *
     * Worth finding: a `RandomAccessFile` seeks straight to a byte offset, while a stream from the
     * resolver has to be read and thrown away up to it. On a two-hour film, a scrub near the end is
     * the difference between instant and half a minute.
     */
    private fun filePathFor(uri: Uri): String? {
        pathCache[uri]?.let { return it }
        val resolved = when (uri.scheme) {
            "file" -> uri.path
            "content" -> uri.dataColumn()
            else -> null
        }
        return resolved?.also { pathCache[uri] = it }
    }

    private fun Uri.dataColumn(): String? = runCatching {
        contentResolver.query(this, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { File(it).canRead() } else null
        }
    }.getOrNull()

    private fun serveFile(session: IHTTPSession, filePath: String): Response {
        val file = File(filePath).takeIf { it.exists() } ?: return notFound()
        val mimeType = castMimeTypeFor(filePath)
        val total = file.length()
        val handle = RandomAccessFile(file, "r")
        val range = session.headers[RangeHeader]

        if (range == null || total <= 0) {
            return streaming(Response.Status.OK, mimeType, BoundedFileStream(handle, total), total)
        }

        val (start, end) = range.asByteRange(total)
        handle.seek(start)
        val length = end - start + 1
        return streaming(Response.Status.PARTIAL_CONTENT, mimeType, BoundedFileStream(handle, length), length)
            .withRange(start, end, total)
    }

    private fun serveThroughResolver(session: IHTTPSession, uri: Uri): Response {
        val mimeType = castMimeTypeFor(uri.path ?: uri.toString())
        val total = contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: return notFound()
        val range = session.headers[RangeHeader]

        if (range == null || total <= 0) {
            val stream = contentResolver.openInputStream(uri) ?: return notFound()
            return streaming(Response.Status.OK, mimeType, stream.buffered(StreamBufferSize), total)
        }

        val (start, end) = range.asByteRange(total)
        val descriptor = contentResolver.openFileDescriptor(uri, "r") ?: return notFound()
        val stream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
        // No seek: a descriptor stream only goes forward, so the offset is read and discarded.
        if (start > 0) stream.skip(start)

        val length = end - start + 1
        return streaming(Response.Status.PARTIAL_CONTENT, mimeType, stream.buffered(StreamBufferSize), length)
            .withRange(start, end, total)
    }

    private fun streaming(status: Response.Status, mimeType: String, data: InputStream, length: Long): Response =
        newFixedLengthResponse(status, mimeType, data, length).forReceiver().apply {
            // Without Accept-Ranges a receiver will not let anyone scrub, whatever the bar shows.
            addHeader("Accept-Ranges", "bytes")
        }

    private fun Response.forReceiver(): Response = apply {
        addHeader("Connection", "keep-alive")
        // A channel's playlist is a different document every few seconds. A cached one is a still.
        addHeader("Cache-Control", "no-cache")
        addHeader("Access-Control-Allow-Origin", "*")
    }

    private fun Response.withRange(start: Long, end: Long, total: Long): Response =
        apply { addHeader("Content-Range", "bytes $start-$end/$total") }

    private fun notFound(): Response = newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
}

private val MimeTypes = mapOf(
    "mp4" to "video/mp4",
    "m4v" to "video/mp4",
    "mkv" to "video/x-matroska",
    "webm" to "video/webm",
    "avi" to "video/x-msvideo",
    "mov" to "video/quicktime",
    "qt" to "video/quicktime",
    "wmv" to "video/x-ms-wmv",
    "flv" to "video/x-flv",
    "3gp" to "video/3gpp",
    "ts" to "video/MP2T",
    // The two a channel arrives as. Without them every IPTV address fell through to "video/*",
    // which is not a type any receiver accepts: it refused the item and stepped to the next one in
    // the queue, walking the channel list a failure at a time.
    "m3u8" to MimeHls,
    "mpd" to "application/dash+xml",
    "mts" to "video/MP2T",
    "m2ts" to "video/MP2T",
    "mpg" to "video/mpeg",
    "mpeg" to "video/mpeg",
    "ogv" to "video/ogg",
    "vob" to "video/x-ms-vob",
    "mp3" to "audio/mpeg",
    "flac" to "audio/flac",
    "m4a" to "audio/mp4",
    "aac" to "audio/aac",
    "ogg" to "audio/ogg",
    "opus" to "audio/opus",
    "wav" to "audio/wav",
    "vtt" to "text/vtt",
    "srt" to "application/x-subrip",
    "ssa" to "text/x-ssa",
    "ass" to "text/x-ssa",
    "ttml" to "application/ttml+xml",
    "dfxp" to "application/ttml+xml",
)
