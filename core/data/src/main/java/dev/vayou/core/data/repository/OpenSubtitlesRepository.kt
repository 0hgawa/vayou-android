package dev.vayou.core.data.repository

import dev.vayou.core.data.models.OpenSubtitleResult
import java.io.File

interface OpenSubtitlesRepository {
    suspend fun searchByHash(movieHash: String, movieByteSize: Long, languageId: String = ""): Result<List<OpenSubtitleResult>>
    suspend fun searchByQuery(query: String, languageId: String = ""): Result<List<OpenSubtitleResult>>
    suspend fun downloadSubtitle(result: OpenSubtitleResult, cacheDir: File): Result<File>
}
