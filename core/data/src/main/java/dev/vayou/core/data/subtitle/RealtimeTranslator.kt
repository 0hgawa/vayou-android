package dev.vayou.core.data.subtitle

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray

object RealtimeTranslator {

    private val cache = ConcurrentHashMap<Pair<String, String>, String>()
    private val json = Json { ignoreUnknownKeys = true }
    private val rateMutex = Mutex()
    private var lastCallMs = 0L

    suspend fun translateBatch(texts: List<String>, targetLanguage: String): List<String?> {
        if (texts.isEmpty()) return emptyList()
        val results = arrayOfNulls<String>(texts.size)
        val pending = mutableListOf<IndexedValue<String>>()

        texts.forEachIndexed { i, text ->
            if (text.isBlank()) return@forEachIndexed
            val cached = cache[text to targetLanguage]
            if (cached != null) results[i] = cached
            else pending += IndexedValue(i, text)
        }

        pending.chunked(MAX_BATCH_SIZE).forEach { chunk ->
            val translated = callBatchExecute(chunk.map { it.value }, targetLanguage) ?: return@forEach
            chunk.forEachIndexed { batchIdx, indexed ->
                translated.getOrNull(batchIdx)?.let { tr ->
                    cache[indexed.value to targetLanguage] = tr
                    results[indexed.index] = tr
                }
            }
        }

        return results.toList()
    }

    private suspend fun callBatchExecute(texts: List<String>, targetLang: String): List<String?>? = withContext(Dispatchers.IO) {
        rateMutex.withLock {
            val wait = MIN_REQUEST_INTERVAL_MS - (System.currentTimeMillis() - lastCallMs)
            if (wait > 0) delay(wait)
            lastCallMs = System.currentTimeMillis()
        }
        try {
            val payload = "f.req=" + URLEncoder.encode(buildFreq(texts, targetLang), "UTF-8")
            val connection = (URL(BATCH_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Encoding", "identity")
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
                doOutput = true
            }
            try {
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode !in 200..299) return@withContext null
                val body = connection.inputStream.use { it.readBytes() }
                parseBatchResponse(body, texts.size)
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildFreq(texts: List<String>, targetLang: String): String {
        val rpcs = buildJsonArray {
            texts.forEachIndexed { i, text ->
                val inner = buildJsonArray {
                    add(
                        buildJsonArray {
                            add(text)
                            add("auto")
                            add(targetLang)
                            add(true)
                        },
                    )
                    add(buildJsonArray { add(JsonNull) })
                }
                add(
                    buildJsonArray {
                        add("MkEWBc")
                        add(inner.toString())
                        add(JsonNull)
                        add(if (i == 0) "generic" else i.toString())
                    },
                )
            }
        }
        return buildJsonArray { add(rpcs) }.toString()
    }

    private fun parseBatchResponse(body: ByteArray, expected: Int): List<String?>? {
        val translations = arrayOfNulls<String>(expected)
        var idx = indexOfByte(body, NEWLINE, 0)
        if (idx < 0) return null
        idx++
        var found = false
        while (idx < body.size) {
            val nl = indexOfByte(body, NEWLINE, idx)
            if (nl < 0) break
            val length = String(body, idx, nl - idx, Charsets.US_ASCII).trim().toIntOrNull() ?: break
            idx = nl + 1
            if (idx + length > body.size) break
            val chunk = String(body, idx, length, Charsets.UTF_8)
            idx += length
            val arr = runCatching { json.parseToJsonElement(chunk).jsonArray }.getOrNull() ?: continue
            for (entry in arr) {
                val e = entry as? JsonArray ?: continue
                if (e.size < 6) continue
                if ((e[0] as? JsonPrimitive)?.contentOrNull != "wrb.fr") continue
                if ((e[1] as? JsonPrimitive)?.contentOrNull != "MkEWBc") continue
                val innerStr = (e[2] as? JsonPrimitive)?.contentOrNull ?: continue
                val tag = (e[5] as? JsonPrimitive)?.contentOrNull ?: continue
                val pos = if (tag == "generic") 0 else tag.toIntOrNull() ?: continue
                if (pos !in 0 until expected) continue
                extractTranslation(innerStr)?.let {
                    translations[pos] = it
                    found = true
                }
            }
        }
        return if (found) translations.toList() else null
    }

    private fun indexOfByte(arr: ByteArray, target: Byte, from: Int): Int {
        for (i in from until arr.size) if (arr[i] == target) return i
        return -1
    }

    private fun extractTranslation(innerStr: String): String? {
        val tree = runCatching { json.parseToJsonElement(innerStr).jsonArray }.getOrNull() ?: return null
        val first = tree.getOrNull(0) as? JsonArray ?: return null
        val text = (first.getOrNull(0) as? JsonPrimitive)?.contentOrNull ?: return null
        return text.ifBlank { null }
    }

    private const val BATCH_URL =
        "https://translate.google.com/_/TranslateWebserverUi/data/batchexecute?rpcids=MkEWBc&rt=c"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val TIMEOUT = 5_000
    private const val MIN_REQUEST_INTERVAL_MS = 100L
    private const val MAX_BATCH_SIZE = 50
    private const val NEWLINE: Byte = 0x0A
}
