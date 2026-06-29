package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore("favorites")
private val FAVORITES_KEY = stringPreferencesKey("favorite_channels")

@Singleton
class FavoritesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val favoritesFlow: Flow<List<PlaylistChannel>> = context.favoritesDataStore.data.map { prefs ->
        decode(prefs[FAVORITES_KEY])
    }

    val favoriteUrlsFlow: Flow<Set<String>> = favoritesFlow.map { list ->
        list.mapTo(mutableSetOf(), PlaylistChannel::url)
    }

    suspend fun toggle(channel: PlaylistChannel) {
        context.favoritesDataStore.edit { prefs ->
            val current = decode(prefs[FAVORITES_KEY])
            val updated = if (current.any { it.url == channel.url }) {
                current.filter { it.url != channel.url }
            } else {
                current + channel
            }
            prefs[FAVORITES_KEY] = smbJson.encodeToString(updated)
        }
    }

    suspend fun rename(url: String, newName: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = decode(prefs[FAVORITES_KEY])
            val updated = current.map { if (it.url == url) it.copy(name = newName) else it }
            prefs[FAVORITES_KEY] = smbJson.encodeToString(updated)
        }
    }

    private fun decode(raw: String?): List<PlaylistChannel> {
        raw ?: return emptyList()
        return runCatching { smbJson.decodeFromString<List<PlaylistChannel>>(raw) }.getOrDefault(emptyList())
    }
}
