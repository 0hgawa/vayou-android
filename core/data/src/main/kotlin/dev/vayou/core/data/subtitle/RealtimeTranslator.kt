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

/**
 * Translates subtitle text via Google Translate's unofficial endpoints.
 *
 * Primary path is the `batchexecute` RPC (up to [MAX_BATCH_SIZE] strings per
 * request), which keeps request volume — and therefore the chance of being
 * rate-limited — low. Because that endpoint is unofficial it can still answer
 * 429/403 or time out, so two safety nets are layered on top:
 *  - the batch call retries with backoff on rate-limit ([RETRY_DELAYS]);
 *  - anything the batch still couldn't translate goes to the endpoint the
 *    Translate browser extension uses, which Google throttles far less, in
 *    chunks of [MAX_FALLBACK_CHARS] rather than one request per line.
 *
 * Both endpoints are unofficial and neither is promised to anyone, which is
 * why there are two: they are rate-limited separately, so the one that is
 * still answering carries the other.
 */
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
            if (cached != null) {
                results[i] = cached
            } else {
                pending += IndexedValue(i, text)
            }
        }

        pending.chunked(MAX_BATCH_SIZE).forEach { chunk ->
            val batch = callBatchWithRetry(chunk.map { it.value }, targetLanguage)
            val stillPending = mutableListOf<IndexedValue<String>>()
            chunk.forEachIndexed { batchIdx, indexed ->
                val tr = batch?.getOrNull(batchIdx)
                if (tr != null) {
                    cache[indexed.value to targetLanguage] = tr
                    results[indexed.index] = tr
                } else {
                    stillPending += indexed
                }
            }
            // Whatever the batch endpoint missed -- a whole batch that failed, or gaps in one that
            // otherwise worked -- goes to the reserve in as few requests as it fits into.
            if (stillPending.isNotEmpty()) {
                val recovered = translateFallback(stillPending.map { it.value }, targetLanguage)
                stillPending.forEachIndexed { offset, indexed ->
                    recovered.getOrNull(offset)?.let { tr ->
                        cache[indexed.value to targetLanguage] = tr
                        results[indexed.index] = tr
                    }
                }
            }
        }

        return results.toList()
    }

    // ---- Primary endpoint: batchexecute, retried with backoff on rate-limit ----

    private suspend fun callBatchWithRetry(texts: List<String>, targetLang: String): List<String?>? {
        for (delayMs in RETRY_DELAYS) {
            if (delayMs > 0) delay(delayMs)
            when (val result = callBatchOnce(texts, targetLang)) {
                is BatchResult.Ok -> return result.translations
                BatchResult.RateLimited -> continue
                BatchResult.Failed -> return null
            }
        }
        return null
    }

    private suspend fun callBatchOnce(texts: List<String>, targetLang: String): BatchResult =
        withContext(Dispatchers.IO) {
            throttle()
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
                    val code = connection.responseCode
                    if (code == HTTP_TOO_MANY_REQUESTS || code == HttpURLConnection.HTTP_FORBIDDEN) {
                        return@withContext BatchResult.RateLimited
                    }
                    if (code !in 200..299) return@withContext BatchResult.Failed
                    val body = connection.inputStream.use { it.readBytes() }
                    parseBatchResponse(body, texts.size)?.let { BatchResult.Ok(it) } ?: BatchResult.Failed
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                BatchResult.Failed
            }
        }

    private sealed interface BatchResult {
        data class Ok(val translations: List<String?>) : BatchResult
        data object RateLimited : BatchResult
        data object Failed : BatchResult
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

    // ---- Fallback endpoint: the one the Translate extension calls, throttled far less ----

    /**
     * Translates whatever the batch endpoint could not, in as few requests as it can.
     *
     * The reserve used to be the public `gtx` endpoint, and it was the wrong one twice over.
     * Google rate-limits it harder than anything else it answers on: measured from one network
     * within a minute of each other, `gtx` replied 429 while this one replied 200. Since `gtx` was
     * the only reserve there was, a limited address meant no translation at all rather than a
     * slower one.
     *
     * Several lines per request, joined by a blank line, because this endpoint hands the blank
     * lines back untouched -- which the desktop build had already found and this one had not. A
     * reserve that asked once per line spent a hundred requests where ten would do, and that is
     * how an address gets limited in the first place.
     */
    private suspend fun translateFallback(texts: List<String>, targetLang: String): List<String?> {
        val results = arrayOfNulls<String>(texts.size)
        var index = 0
        while (index < texts.size) {
            val start = index
            var budget = 0
            val chunk = mutableListOf<String>()
            // At least one, however long it is: a single line over the budget still has to be sent,
            // and a chunk that refused it would loop for ever.
            while (index < texts.size && (chunk.isEmpty() || budget + texts[index].length <= MAX_FALLBACK_CHARS)) {
                budget += texts[index].length + SEPARATOR.length
                chunk += texts[index].withoutBlankLines()
                index++
            }
            translateJoined(chunk, targetLang)?.forEachIndexed { offset, translated ->
                results[start + offset] = translated
            }
        }
        return results.toList()
    }

    /**
     * One request for a whole chunk, split back on the separator that joined it.
     *
     * Null unless the pieces come back in the same number they went out. The split is the whole of
     * the agreement between the two ends, and a response that broke it would shift every line onto
     * its neighbour's timing -- which is worse than showing the original words.
     */
    private suspend fun translateJoined(texts: List<String>, targetLang: String): List<String?>? {
        for (delayMs in RETRY_DELAYS) {
            if (delayMs > 0) delay(delayMs)
            when (val result = fallbackOnce(texts.joinToString(SEPARATOR), targetLang)) {
                is FallbackResult.Ok -> {
                    val parts = result.text.split(SEPARATOR)
                    return if (parts.size == texts.size) parts.map { it.trim().ifBlank { null } } else null
                }
                FallbackResult.RateLimited -> continue
                FallbackResult.Failed -> return null
            }
        }
        return null
    }

    /**
     * A blank line inside a line of dialogue would be read as the end of it.
     *
     * The separator is the only thing telling the far end where one caption stops, so a caption
     * carrying one of its own would come back as two and push everything after it along by one.
     */
    private fun String.withoutBlankLines(): String = BLANK_LINES.replace(this, "\n")

    private suspend fun fallbackOnce(text: String, targetLang: String): FallbackResult = withContext(Dispatchers.IO) {
        throttle()
        try {
            val query = URLEncoder.encode(text, "UTF-8")
            val url = "$FALLBACK_URL?client=dict-chrome-ex&sl=auto&tl=$targetLang&q=$query"
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                connectTimeout = TIMEOUT
                readTimeout = TIMEOUT
            }
            try {
                val code = connection.responseCode
                if (code == HTTP_TOO_MANY_REQUESTS || code == HttpURLConnection.HTTP_FORBIDDEN) {
                    return@withContext FallbackResult.RateLimited
                }
                if (code !in 200..299) return@withContext FallbackResult.Failed
                val body = connection.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)
                parseFallback(body)?.let { FallbackResult.Ok(it) } ?: FallbackResult.Failed
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            FallbackResult.Failed
        }
    }

    private sealed interface FallbackResult {
        data class Ok(val text: String) : FallbackResult
        data object RateLimited : FallbackResult
        data object Failed : FallbackResult
    }

    /**
     * The answer is `[["translated text","detected source"]]`, so the translation is `[0][0]`.
     *
     * Only that one field. The element beside it is the language the endpoint decided the text was
     * in, and reading the pair as a whole appends "en" to the end of every caption.
     */
    private fun parseFallback(body: String): String? {
        val arr = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull() ?: return null
        val first = arr.getOrNull(0) as? JsonArray ?: return null
        val text = (first.getOrNull(0) as? JsonPrimitive)?.contentOrNull ?: return null
        return text.ifBlank { null }
    }

    // ---- Shared rate limiter (both endpoints share the same budget) ----

    private suspend fun throttle() {
        rateMutex.withLock {
            val wait = MIN_REQUEST_INTERVAL_MS - (System.currentTimeMillis() - lastCallMs)
            if (wait > 0) delay(wait)
            lastCallMs = System.currentTimeMillis()
        }
    }

    private const val BATCH_URL =
        "https://translate.google.com/_/TranslateWebserverUi/data/batchexecute?rpcids=MkEWBc&rt=c"
    private const val FALLBACK_URL = "https://clients5.google.com/translate_a/t"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val TIMEOUT = 5_000
    private const val MIN_REQUEST_INTERVAL_MS = 100L
    private const val MAX_BATCH_SIZE = 50

    /** What tells the reserve where one caption ends, and what its answer is split back on. */
    private const val SEPARATOR = "\n\n"

    /** As much text as the reserve is asked for at once, which is what the desktop build settled on. */
    private const val MAX_FALLBACK_CHARS = 4_500

    private val BLANK_LINES = Regex("\n{2,}")
    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val NEWLINE: Byte = 0x0A
    private val RETRY_DELAYS = longArrayOf(0L, 800L, 1600L)
}
