package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.smbServerDataStore: DataStore<Preferences> by preferencesDataStore("smb_servers")

/** The servers the viewer saved, with the credentials needed to reach them again. */
@Singleton
class SmbServerStore @Inject constructor(@ApplicationContext context: Context) {

    private val store = JsonListStore(context.smbServerDataStore, "saved_servers", StoredSmbServer.serializer())

    val savedServers: Flow<List<SavedSmbServer>> = store.flow.map { servers -> servers.map { it.toSaved() } }

    suspend fun credentials(host: String): SmbCredentials? = store.read().find { it.host == host }?.toCredentials()

    /** One entry per host: saving an address already known replaces what was there. */
    suspend fun save(host: String, displayName: String, username: String, password: String, domain: String) =
        store.update { current ->
            current.filterNot { it.host == host } +
                StoredSmbServer(host, displayName, username, password, domain)
        }

    suspend fun remove(host: String) = store.update { current -> current.filterNot { it.host == host } }
}
