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
        performSearch(buildString {
            append("moviebytesize-$movieByteSize/moviehash-$movieHash")
            if (languageId.isNotBlank()) append("/sublanguageid-$languageId")
        })
    }

    override suspend fun searchByQuery(
        query: String,
        languageId: String,
    ): Result<List<OpenSubtitleResult>> = withContext(Dispatchers.IO) {
        // The legacy OpenSubtitles REST endpoint matches inconsistently
        // across case — the same title returns dozens of hits in one
        // casing and zero in another. Re-run lowercased on a miss before
        // giving up. Original casing wins when it works; the fallback
        // only costs an extra request when there is nothing to lose.
        val primary = queryOnce(query, languageId)
        val noHits = primary.getOrNull()?.isEmpty() == true
        val lower = query.lowercase()
        if (noHits && lower != query) queryOnce(lower, languageId) else primary
    }

    private fun queryOnce(query: String, languageId: String): Result<List<OpenSubtitleResult>> =
        performSearch(buildString {
            append("query-${URLEncoder.encode(query, "UTF-8").replace("+", "%20")}")
            if (languageId.isNotBlank()) append("/sublanguageid-$languageId")
        })

    override suspend fun downloadSubtitle(
        result: OpenSubtitleResult,
        cacheDir: File,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val subtitleDir = File(cacheDir, "subtitles").also { if (!it.exists()) it.mkdirs() }
            val outputFile = File(subtitleDir, result.subFileName)
            val connection = URL(result.subDownloadLink).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = TIMEOUT
            connection.readTimeout = TIMEOUT
            connection.inputStream.use { raw ->
                GZIPInputStream(raw).use { gzip ->
                    outputFile.outputStream().use { out -> gzip.copyTo(out) }
                }
            }
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
    }
}
