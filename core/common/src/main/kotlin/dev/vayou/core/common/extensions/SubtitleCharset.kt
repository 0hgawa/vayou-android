package dev.vayou.core.common.extensions

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.universalchardet.UniversalDetector

/**
 * The same subtitle, in an encoding the player can read.
 *
 * Media3 reads subtitles as UTF-8 and offers nowhere to say otherwise, while a great many `.srt`
 * files on disk are Latin-1 or Windows-1252 — every one of which turns accented text into rubble.
 * So the encoding is guessed and, when it is not already UTF-8, the file is rewritten into the
 * cache and that copy handed over instead.
 *
 * Returns [uri] unchanged when it is already UTF-8, when the guess fails, or when anything goes
 * wrong reading it. A subtitle with the wrong accents is worth more than no subtitle.
 */
suspend fun Context.asUtf8Subtitle(uri: Uri): Uri = withContext(Dispatchers.IO) {
    runCatching {
        val source = detectCharset(uri) ?: return@runCatching uri
        if (source == StandardCharsets.UTF_8) return@runCatching uri

        val copy = File(subtitleCacheDir(), displayNameOf(uri) ?: uri.hashCode().toString())
        contentResolver.openInputStream(uri)?.use { input ->
            input.reader(source).buffered().use { reader ->
                copy.outputStream().writer(StandardCharsets.UTF_8).buffered().use(reader::copyTo)
            }
        } ?: return@runCatching uri

        Uri.fromFile(copy)
    }.getOrDefault(uri)
}

/**
 * Null when the detector will not commit, which is its answer for short files and for anything that
 * looks like plain ASCII — both of which are already readable as UTF-8.
 */
private fun Context.detectCharset(uri: Uri): Charset? = contentResolver.openInputStream(uri)?.use { input ->
    BufferedInputStream(input).use { buffered ->
        // A subtitle's encoding is settled in its first lines; reading the whole of a
        // feature-length file to learn it again would be the slowest part of opening one.
        val sample = ByteArray(SampleBytes)
        val read = buffered.read(sample, 0, SampleBytes)
        if (read <= 0) return@use null

        UniversalDetector(null).run {
            handleData(sample, 0, read)
            dataEnd()
            detectedCharset?.let(Charset::forName)
        }
    }
}

/**
 * Its own directory, apart from where downloaded subtitles land.
 *
 * They shared one, and a subtitle fetched from the internet was read and rewritten at the same path
 * in the same breath — which truncates it to nothing. The name is the file's, so the label the
 * player shows still reads as the file it came from.
 */
private fun Context.subtitleCacheDir(): File = File(cacheDir, "subtitles-utf8").apply { mkdirs() }

private const val SampleBytes = 100 * 1024
