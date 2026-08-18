package dev.vayou.core.smb

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Lenient on purpose: a document written by an older build must still open on this one. */
internal val SmbJson = Json { ignoreUnknownKeys = true }

/**
 * A list kept as one JSON string inside a [DataStore].
 *
 * Five stores would otherwise repeat the same six lines each: the key, read-and-decode,
 * read-modify-write, and a catch around the parse. The catch is the part worth having in one place
 * -- a document written by an older build with a field this one no longer understands must degrade
 * to an empty list, not throw on the first read after an update.
 *
 * Preferences rather than a typed DataStore because these lists are small, are rewritten whole
 * every time, and each already lives in its own file.
 */
internal class JsonListStore<T>(
    private val dataStore: DataStore<Preferences>,
    keyName: String,
    itemSerializer: KSerializer<T>,
) {
    private val key = stringPreferencesKey(keyName)
    private val serializer = ListSerializer(itemSerializer)

    val flow: Flow<List<T>> = dataStore.data.map { it.decode() }

    suspend fun read(): List<T> = dataStore.data.first().decode()

    /** Reads and writes inside one edit, so two callers cannot overwrite each other's change. */
    suspend fun update(transform: (List<T>) -> List<T>) {
        dataStore.edit { prefs ->
            prefs[key] = SmbJson.encodeToString(serializer, transform(prefs.decode()))
        }
    }

    private fun Preferences.decode(): List<T> {
        val raw = this[key] ?: return emptyList()
        return runCatching { SmbJson.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }
}
