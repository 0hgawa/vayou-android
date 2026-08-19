package dev.vayou.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vayou.core.common.Dispatcher
import dev.vayou.core.common.VayouDispatchers
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * One line of a track's words, timed if anything said when it is sung.
 *
 * Null time is a plain lyric: the whole text is shown at once and nothing follows the playhead.
 */
data class LyricLine(val atMs: Long?, val text: String)

/** What a track has to say for itself, in the order it says it. */
data class Lyrics(val lines: List<LyricLine>) {
    /** True when every line knows its moment, which is what lets the sheet follow the playing. */
    val isTimed: Boolean = lines.isNotEmpty() && lines.all { it.atMs != null }
}

/**
 * The words of a track, from the two places a local library actually keeps them.
 *
 * An `.lrc` beside the file first, because that is where a curated collection puts timed words and
 * because it costs one small text file to find out. Then the tag inside the file: `SYLT` if it is
 * there, `USLT` if it is not.
 *
 * Nothing is fetched from the internet. There is no free and lawful source of lyrics -- the ones
 * that exist want a commercial agreement, and scraping a site is the kind of thing that takes an
 * app off the store -- and a player whose point is working without a network should not grow a
 * feature that needs one.
 *
 * The tag is read from the front of the file rather than by opening the whole thing, which is what
 * the tag editor does when it has to rewrite one: a chapter of an audiobook is a hundred megabytes,
 * and copying it to a cache to read a few kilobytes of text is a wait for nothing.
 */
@Singleton
class LyricsReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(VayouDispatchers.IO) private val dispatcher: CoroutineDispatcher,
) {

    suspend fun lyricsFor(song: Song): Lyrics? = withContext(dispatcher) {
        sidecarOf(song) ?: embeddedIn(song)
    }

    /** `Track.lrc` next to `Track.mp3`, which is the convention every player reads. */
    private fun sidecarOf(song: Song): Lyrics? {
        if (song.folderPath.isBlank()) return null
        val file = File(song.folderPath, song.fileName.substringBeforeLast('.') + ".lrc")
        val text = runCatching { file.takeIf { it.canRead() }?.readText() }.getOrNull() ?: return null
        return parseLrc(text).takeIf { it.lines.isNotEmpty() }
    }

    private fun embeddedIn(song: Song): Lyrics? {
        val frames = runCatching {
            context.contentResolver.openInputStream(song.uri)?.use(::readId3Frames)
        }.getOrNull() ?: return null
        val timed = frames[SyltFrame]?.let(::parseSylt)
        if (timed != null && timed.lines.isNotEmpty()) return timed
        val plain = frames[UsltFrame]?.let(::parseUslt)
        return plain?.takeIf { it.lines.isNotEmpty() }
    }
}

/**
 * The frames of an ID3v2 tag, read from the front of the stream and no further.
 *
 * The tag says how long it is in its tenth byte onwards, so the whole of it can be taken in one
 * read and the rest of the file left alone. Only the two frames this asks about are kept.
 */
private fun readId3Frames(stream: InputStream): Map<String, ByteArray> {
    val header = ByteArray(HeaderSize)
    if (stream.read(header) != HeaderSize) return emptyMap()
    if (String(header, 0, 3, Charsets.ISO_8859_1) != "ID3") return emptyMap()
    val major = header[3].toInt()
    if (major !in 3..4) return emptyMap()
    val size = syncSafe(header, 6)
    if (size <= 0 || size > MaxTagBytes) return emptyMap()

    val body = ByteArray(size)
    if (stream.read(body) != size) return emptyMap()

    val frames = mutableMapOf<String, ByteArray>()
    var at = 0
    while (at + FrameHeaderSize <= size) {
        val id = String(body, at, 4, Charsets.ISO_8859_1)
        // Padding: the tag is longer than its frames and the rest is zeroes.
        if (id.isBlank() || id[0].code == 0) break
        val length = if (major == 4) syncSafe(body, at + 4) else plainSize(body, at + 4)
        if (length <= 0 || at + FrameHeaderSize + length > size) break
        if (id == UsltFrame || id == SyltFrame) {
            frames[id] = body.copyOfRange(at + FrameHeaderSize, at + FrameHeaderSize + length)
        }
        at += FrameHeaderSize + length
    }
    return frames
}

/** Seven bits to the byte, which is how a tag says its length without ever looking like audio. */
private fun syncSafe(bytes: ByteArray, at: Int): Int = (bytes[at].toInt() and 0x7F shl 21) or
    (bytes[at + 1].toInt() and 0x7F shl 14) or
    (bytes[at + 2].toInt() and 0x7F shl 7) or
    (bytes[at + 3].toInt() and 0x7F)

private fun plainSize(bytes: ByteArray, at: Int): Int = (bytes[at].toInt() and 0xFF shl 24) or
    (bytes[at + 1].toInt() and 0xFF shl 16) or
    (bytes[at + 2].toInt() and 0xFF shl 8) or
    (bytes[at + 3].toInt() and 0xFF)

/** Unsynchronised words: one block of text, after the encoding, the language and a description. */
private fun parseUslt(frame: ByteArray): Lyrics {
    if (frame.size <= UsltPrefix) return Lyrics(emptyList())
    val charset = charsetOf(frame[0])
    val afterDescription = skipTerminated(frame, UsltPrefix, charset)
    val text = String(frame, afterDescription, frame.size - afterDescription, charset)
    return parseLrc(text).takeIf { it.lines.any { line -> line.atMs != null } }
        ?: Lyrics(text.lines().map { LyricLine(null, it.trim()) }.dropWhile { it.text.isBlank() })
}

/**
 * Synchronised words: the same prefix, then pairs of text and the millisecond it lands on.
 *
 * Only the millisecond format is read. The other one counts MPEG frames, which means nothing
 * without decoding the file, and no tagger in use writes it.
 */
private fun parseSylt(frame: ByteArray): Lyrics {
    if (frame.size <= SyltPrefix) return Lyrics(emptyList())
    val charset = charsetOf(frame[0])
    if (frame[4].toInt() != TimestampMilliseconds) return Lyrics(emptyList())
    var at = skipTerminated(frame, SyltPrefix, charset)
    val lines = mutableListOf<LyricLine>()
    while (at + 4 < frame.size) {
        val end = terminatorAt(frame, at, charset)
        val text = String(frame, at, end - at, charset).trim()
        at = end + charset.terminatorSize
        if (at + 4 > frame.size) break
        val atMs = plainSize(frame, at).toLong()
        at += 4
        if (text.isNotBlank()) lines += LyricLine(atMs, text)
    }
    return Lyrics(lines)
}

/**
 * `[mm:ss.cc] words`, the format an `.lrc` is written in.
 *
 * A line may carry several stamps when the same words repeat, and the file may open with tags of
 * its own -- `[ar:]`, `[ti:]` -- which are stamps that are not times and are dropped as such.
 */
internal fun parseLrc(text: String): Lyrics {
    val lines = mutableListOf<LyricLine>()
    text.lineSequence().forEach { raw ->
        // What the file says about itself -- who wrote it, which track it belongs to -- looks like a
        // stamp and is not one. Sung as a verse, it would be the first thing the sheet showed.
        if (MetadataPattern.containsMatchIn(raw)) return@forEach
        val stamps = StampPattern.findAll(raw).toList()
        val words = raw.substring(stamps.lastOrNull()?.range?.last?.plus(1) ?: 0).trim()
        if (stamps.isEmpty()) {
            if (words.isNotBlank()) lines += LyricLine(null, words)
            return@forEach
        }
        stamps.forEach { stamp ->
            val (minutes, seconds, fraction) = stamp.destructured
            val hundredths = fraction.padEnd(3, '0').take(3).toLong()
            val atMs = minutes.toLong() * 60_000 + (seconds.toDouble() * 1000).toLong() + hundredths
            lines += LyricLine(atMs, words)
        }
    }
    // A file that times anything times everything that matters: what is left over is a stray line
    // of notes or a blank, and keeping it would say the words are untimed and stop the sheet from
    // following them.
    val timed = lines.filter { it.atMs != null }
    return Lyrics(if (timed.isEmpty()) lines else timed.sortedBy { it.atMs })
}

private fun charsetOf(encoding: Byte) = when (encoding.toInt()) {
    0 -> Terminated(Charsets.ISO_8859_1, 1)
    1 -> Terminated(Charsets.UTF_16, 2)
    2 -> Terminated(Charsets.UTF_16BE, 2)
    else -> Terminated(Charsets.UTF_8, 1)
}

/** A text and how many zero bytes end it, which is one or two depending on the encoding. */
private class Terminated(val charset: java.nio.charset.Charset, val terminatorSize: Int)

private fun String(bytes: ByteArray, at: Int, length: Int, charset: Terminated) =
    String(bytes, at, length.coerceAtLeast(0), charset.charset)

private fun skipTerminated(frame: ByteArray, from: Int, charset: Terminated): Int =
    terminatorAt(frame, from, charset) + charset.terminatorSize

private fun terminatorAt(frame: ByteArray, from: Int, charset: Terminated): Int {
    var at = from
    while (at + charset.terminatorSize <= frame.size) {
        val ended = (0 until charset.terminatorSize).all { frame[at + it].toInt() == 0 }
        if (ended) return at
        at += charset.terminatorSize
    }
    return frame.size
}

private const val HeaderSize = 10

private const val FrameHeaderSize = 10

/** A megabyte of tag is already ten times the largest anyone writes; beyond it something is wrong. */
private const val MaxTagBytes = 1 shl 20

private const val UsltFrame = "USLT"

private const val SyltFrame = "SYLT"

/** Encoding, three letters of language, and a description that ends in zero. */
private const val UsltPrefix = 4

/** The same, plus the time format and what the words are -- lyrics, a transcript, a chord chart. */
private const val SyltPrefix = 6

private const val TimestampMilliseconds = 2

/** `[ar:...]`, `[ti:...]`, `[offset:...]` -- letters before the colon, never a time. */
private val MetadataPattern = Regex("""^\s*\[[a-zA-Z#]+:""")

private val StampPattern = Regex("""\[(\d{1,3}):(\d{2}(?:\.\d{1,3})?)(?:\.(\d{1,3}))?]""")
