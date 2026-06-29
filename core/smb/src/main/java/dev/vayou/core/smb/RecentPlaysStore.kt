package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private val Context.recentPlaysDataStore: DataStore<Preferences> by preferencesDataStore("recent_plays")
private val RECENTS_KEY = stringPreferencesKey("entries")
private const val MaxEntries = 30

@Serializable
data class RecentPlay(
    val uri: String,
    val displayName: String,
    val lastPlayedAt: Long,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
)

@Singleton
class RecentPlaysStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val recentsFlow: Flow<List<RecentPlay>> = context.recentPlaysDataStore.data.map { prefs ->
        decode(prefs[RECENTS_KEY])
    }

    suspend fun record(uri: String, displayName: String, durationMs: Long, positionMs: Long) {
        val list = load().toMutableList()
        list.removeAll { it.uri == uri }
        list.add(0, RecentPlay(uri, displayName, System.currentTimeMillis(), durationMs, positionMs))
        persist(list.take(MaxEntries))
    }

    suspend fun remove(uri: String) {
        persist(load().filter { it.uri != uri })
    }

    private suspend fun load(): List<RecentPlay> =
        decode(context.recentPlaysDataStore.data.first()[RECENTS_KEY])

    private fun decode(raw: String?): List<RecentPlay> {
        raw ?: return emptyList()
        return runCatching { smbJson.decodeFromString<List<RecentPlay>>(raw) }.getOrDefault(emptyList())
    }

    private suspend fun persist(list: List<RecentPlay>) {
        context.recentPlaysDataStore.edit { it[RECENTS_KEY] = smbJson.encodeToString(list) }
    }
}
