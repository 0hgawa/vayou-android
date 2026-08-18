package dev.vayou.core.data.repository

import dev.vayou.core.data.models.OpenSubtitleResult
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class RemoteOpenSubtitlesRepository @Inject constructor() : OpenSubtitlesRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun searchByHash(
        movieHash: String,
        movieByteSize: Long,
        languageId: String,
    ): Result<List<OpenSubtitleResult>> = withContext(Dispatchers.IO) {
        performSearch(
            buildString {
                append("moviebytesize-$movieByteSize/moviehash-$movieHash")
                if (languageId.isNotBlank()) append("/sublanguageid-$languageId")
            },
        )
    }

    override suspend fun searchByQuery(query: String, languageId: String): Result<List<OpenSubtitleResult>> =
        withContext(Dispatchers.IO) {
            // The legacy OpenSubtitles endpoint is inconsistent about case, and worse than the
            // old comment here said: "query-Sintel" answers with a 302 and an empty body, where
            // "query-sintel" returns the results. Other titles answer with an empty list instead.
            // Both are worth one more request lowercased; the original casing still wins whenever
            // it works. Retrying only the empty answer, as before, left the redirect as a failure.
            val primary = queryOnce(query, languageId)
            val worthRetrying = primary.isFailure || primary.getOrNull()?.isEmpty() == true
            val lower = query.lowercase()
            if (worthRetrying && lower != query) queryOnce(lower, languageId) else primary
        }

    private fun queryOnce(query: String, languageId: String): Result<List<OpenSubtitleResult>> = performSearch(
        buildString {
            append("query-${URLEncoder.encode(query, "UTF-8").replace("+", "%20")}")
            if (languageId.isNotBlank()) append("/sublanguageid-$languageId")
        },
    )

    override suspend fun downloadSubtitle(result: OpenSubtitleResult, cacheDir: File): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val subtitleDir = File(cacheDir, "subtitles").also { if (!it.exists()) it.mkdirs() }
                val outputFile = File(subtitleDir, result.subFileName)
                val connection = URL(result.subDownloadLink).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT
                // Decided by the first two bytes, not by the .gz in the name. HttpURLConnection asks
                // for gzip and unwraps it itself when the server answers with Content-Encoding, so
                // whether what arrives here is still compressed depends on how the file was served.
                // Wrapping it in a GZIPInputStream either way is how this wrote empty files.
                val body = connection.inputStream.use { it.readBytes() }
                val bytes = if (body.size >= 2 && body[0] == GzipMagicFirst && body[1] == GzipMagicSecond) {
                    GZIPInputStream(body.inputStream()).use { it.readBytes() }
                } else {
                    body
                }
                outputFile.writeBytes(bytes)
                connection.disconnect()
                Result.success(outputFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun performSearch(pathParams: String): Result<List<OpenSubtitleResult>> = try {
        val connection = URL("$BASE_URL/$pathParams").openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        Result.success(json.decodeFromString<List<OpenSubtitleResult>>(responseBody))
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val BASE_URL = "https://rest.opensubtitles.org/search"
        private const val USER_AGENT = "Vayou v1.0"
        private const val TIMEOUT = 15_000

        private const val GzipMagicFirst: Byte = 0x1f
        private const val GzipMagicSecond: Byte = 0x8b.toByte()
    }
}
