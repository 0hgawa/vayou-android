package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

internal val smbJson = Json { ignoreUnknownKeys = true }

private val Context.smbDataStore: DataStore<Preferences> by preferencesDataStore("smb_servers")
private val SERVERS_KEY = stringPreferencesKey("saved_servers")

@Singleton
class SmbServerStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val savedServersFlow: Flow<List<SavedSmbServer>> = context.smbDataStore.data.map { prefs ->
        val raw = prefs[SERVERS_KEY] ?: return@map emptyList()
        runCatching { smbJson.decodeFromString<List<StoredSmbServer>>(raw) }
            .getOrDefault(emptyList())
            .map { it.toSaved() }
    }

    suspend fun getCredentials(host: String): SmbCredentials? =
        loadStored().find { it.host == host }?.toCredentials()

    suspend fun saveServer(host: String, displayName: String, username: String, password: String, domain: String) {
        val servers = loadStored().toMutableList()
        servers.removeAll { it.host == host }
        servers.add(StoredSmbServer(host, displayName, username, password, domain))
        persist(servers)
    }

    suspend fun removeServer(host: String) {
        val servers = loadStored().toMutableList()
        servers.removeAll { it.host == host }
        persist(servers)
    }

    private suspend fun loadStored(): List<StoredSmbServer> {
        val raw = context.smbDataStore.data.first()[SERVERS_KEY] ?: return emptyList()
        return runCatching { smbJson.decodeFromString<List<StoredSmbServer>>(raw) }.getOrDefault(emptyList())
    }

    private suspend fun persist(servers: List<StoredSmbServer>) {
        context.smbDataStore.edit { prefs ->
            prefs[SERVERS_KEY] = smbJson.encodeToString(servers)
        }
    }
}
