package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playlistDataStore: DataStore<Preferences> by preferencesDataStore("playlists")
private val PLAYLISTS_KEY = stringPreferencesKey("saved_playlists")
private val SEED_VERSION_KEY = intPreferencesKey("seed_version")
private val IPTV_COUNTRY_KEY = stringPreferencesKey("iptv_country")
private const val CURRENT_SEED_VERSION = 4
private const val LEGACY_FREE_TV_URL = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"
private const val IPTV_ORG_COUNTRY_URL_PREFIX = "https://iptv-org.github.io/iptv/countries/"
private const val IPTV_ORG_GLOBAL_URL = "https://iptv-org.github.io/iptv/index.m3u"
private const val IPTV_DEFAULT_NAME = "Canais ao vivo"

@Serializable
data class SavedPlaylist(
    val name: String,
    val url: String,
    val channelCount: Int = 0,
)

@Singleton
class PlaylistStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val playlistsFlow: Flow<List<SavedPlaylist>> = context.playlistDataStore.data.map { prefs ->
        decode(prefs[PLAYLISTS_KEY])
    }

    val iptvCountryFlow: Flow<String?> = context.playlistDataStore.data.map { it[IPTV_COUNTRY_KEY] }

    suspend fun seedDefaultsIfNeeded() {
        val prefs = context.playlistDataStore.data.first()
        val version = prefs[SEED_VERSION_KEY] ?: 0
        if (version >= CURRENT_SEED_VERSION) return
        if (version < 2) remove(LEGACY_FREE_TV_URL)
        val current = load()
        val existing = current.firstOrNull {
            it.url == IPTV_ORG_GLOBAL_URL || it.url.startsWith(IPTV_ORG_COUNTRY_URL_PREFIX)
        }
        if (existing == null) {
            val country = Locale.getDefault().country.lowercase().ifBlank { null }
            setIptvCountry(country, IPTV_DEFAULT_NAME)
        } else if (existing.name != IPTV_DEFAULT_NAME) {
            rename(existing.url, IPTV_DEFAULT_NAME)
        }
        context.playlistDataStore.edit { it[SEED_VERSION_KEY] = CURRENT_SEED_VERSION }
    }

    /** Replaces the iptv-org country playlist with the given country (null = global index). */
    suspend fun setIptvCountry(code: String?, displayName: String) {
        val url = if (code.isNullOrBlank()) IPTV_ORG_GLOBAL_URL
                  else "$IPTV_ORG_COUNTRY_URL_PREFIX${code.lowercase()}.m3u"
        val current = load().filterNot {
            it.url == IPTV_ORG_GLOBAL_URL || it.url.startsWith(IPTV_ORG_COUNTRY_URL_PREFIX)
        }
        persist(current + SavedPlaylist(name = displayName, url = url))
        context.playlistDataStore.edit { prefs ->
            if (code.isNullOrBlank()) prefs.remove(IPTV_COUNTRY_KEY)
            else prefs[IPTV_COUNTRY_KEY] = code.lowercase()
        }
    }

    suspend fun add(name: String, url: String) {
        val list = load().toMutableList()
        list.removeAll { it.url == url }
        list.add(SavedPlaylist(name = name.ifBlank { url.substringAfterLast('/') }, url = url))
        persist(list)
    }

    suspend fun remove(url: String) {
        persist(load().filter { it.url != url })
    }

    suspend fun rename(url: String, newName: String) {
        persist(load().map { if (it.url == url) it.copy(name = newName) else it })
    }

    suspend fun updateMetadata(url: String, channelCount: Int) {
        persist(load().map {
            if (it.url == url) it.copy(channelCount = channelCount) else it
        })
    }

    private suspend fun load(): List<SavedPlaylist> =
        decode(context.playlistDataStore.data.first()[PLAYLISTS_KEY])

    private fun decode(raw: String?): List<SavedPlaylist> {
        raw ?: return emptyList()
        return runCatching { smbJson.decodeFromString<List<SavedPlaylist>>(raw) }.getOrDefault(emptyList())
    }

    private suspend fun persist(list: List<SavedPlaylist>) {
        context.playlistDataStore.edit { it[PLAYLISTS_KEY] = smbJson.encodeToString(list) }
    }
}
