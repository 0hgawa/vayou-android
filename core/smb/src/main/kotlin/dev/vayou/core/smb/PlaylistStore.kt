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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.playlistDataStore: DataStore<Preferences> by preferencesDataStore("playlists")

private val SeedVersionKey = intPreferencesKey("seed_version")

private val IptvCountryKey = stringPreferencesKey("iptv_country")

private const val CurrentSeedVersion = 5

private const val LegacyFreeTvUrl = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"

private const val DefaultPlaylistName = "Channels"

@Serializable
data class SavedPlaylist(val name: String, val url: String)

/**
 * The channel lists the viewer added, and the one the app arrives with.
 *
 * A first run gets iptv-org's list for whatever country the phone is set to, because a Streams tab
 * that opens empty is a tab that gets one visit. [seedDefaultsIfNeeded] is versioned rather than
 * run-once so a later build can correct what an earlier one seeded without touching what the viewer
 * added themselves.
 */
@Singleton
class PlaylistStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val store = JsonListStore(context.playlistDataStore, "saved_playlists", SavedPlaylist.serializer())

    val playlists: Flow<List<SavedPlaylist>> = store.flow

    val iptvCountry: Flow<String?> = context.playlistDataStore.data.map { it[IptvCountryKey] }

    suspend fun seedDefaultsIfNeeded() {
        val version = context.playlistDataStore.data.first()[SeedVersionKey] ?: 0
        if (version >= CurrentSeedVersion) return
        if (version < 2) remove(LegacyFreeTvUrl)

        val seeded = store.read().firstOrNull { it.isIptvOrg }
        if (seeded == null) {
            setIptvCountry(Locale.getDefault().country.lowercase().ifBlank { null }, DefaultPlaylistName)
        } else if (seeded.name != DefaultPlaylistName) {
            rename(seeded.url, DefaultPlaylistName)
        }
        context.playlistDataStore.edit { it[SeedVersionKey] = CurrentSeedVersion }
    }

    /** Replaces the iptv-org list with another country's. Null is the global index. */
    suspend fun setIptvCountry(code: String?, displayName: String) {
        val url = IptvCountry(code, displayName).url
        store.update { current -> current.filterNot { it.isIptvOrg } + SavedPlaylist(displayName, url) }
        context.playlistDataStore.edit { prefs ->
            if (code.isNullOrBlank()) prefs.remove(IptvCountryKey) else prefs[IptvCountryKey] = code.lowercase()
        }
    }

    /** One entry per address: adding one already saved replaces what was there. */
    suspend fun add(name: String, url: String) = store.update { current ->
        current.filterNot { it.url == url } +
            SavedPlaylist(name = name.ifBlank { url.substringAfterLast('/') }, url = url)
    }

    suspend fun remove(url: String) = store.update { current -> current.filterNot { it.url == url } }

    suspend fun rename(url: String, newName: String) = store.update { current ->
        current.map { if (it.url == url) it.copy(name = newName) else it }
    }
}

/** Seeded by this app rather than added by the viewer, and so replaceable. */
private val SavedPlaylist.isIptvOrg: Boolean
    get() = url == IptvCountry.GlobalUrl || url.startsWith(IptvCountry.CountryPrefix)
