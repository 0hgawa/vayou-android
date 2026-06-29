package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

enum class PlaylistSortBy(val label: String) { NAME("Nome"), CHANNELS("Canais") }
enum class ServerSortBy(val label: String) { NAME("Nome"), HOST("Endereço") }
enum class BrowserSortBy(val label: String) { NAME("Nome"), SIZE("Tamanho") }
data class PlaylistSort(val by: PlaylistSortBy = PlaylistSortBy.NAME, val asc: Boolean = true)
data class ServerSort(val by: ServerSortBy = ServerSortBy.NAME, val asc: Boolean = true)
data class BrowserSort(val by: BrowserSortBy = BrowserSortBy.NAME, val asc: Boolean = true)

private val Context.browserPreferencesDataStore: DataStore<Preferences> by preferencesDataStore("browser_prefs")

private val IPTV_GRID_KEY = booleanPreferencesKey("iptv_grid_mode")
private val IPTV_SORT_ASC_KEY = booleanPreferencesKey("iptv_sort_asc")
private val IPTV_SORT_BY_KEY = intPreferencesKey("iptv_sort_by")
private val SERVERS_GRID_KEY = booleanPreferencesKey("servers_grid_mode")
private val SERVERS_SORT_ASC_KEY = booleanPreferencesKey("servers_sort_asc")
private val SERVERS_SORT_BY_KEY = intPreferencesKey("servers_sort_by")
private val BROWSER_GRID_KEY = booleanPreferencesKey("browser_grid_mode")
private val BROWSER_SORT_ASC_KEY = booleanPreferencesKey("browser_sort_asc")
private val BROWSER_SORT_BY_KEY = intPreferencesKey("browser_sort_by")

@Singleton
class BrowserPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val iptvGridMode: StateFlow<Boolean>
    val serversGridMode: StateFlow<Boolean>
    val iptvSort: StateFlow<PlaylistSort>
    val serversSort: StateFlow<ServerSort>
    val browserGridMode: StateFlow<Boolean>
    val browserSortAsc: StateFlow<Boolean>
    val browserSort: StateFlow<BrowserSort>

    init {
        val snapshot = runBlocking { context.browserPreferencesDataStore.data.first() }
        iptvGridMode = context.browserPreferencesDataStore.data.map { it[IPTV_GRID_KEY] ?: false }
            .stateIn(scope, SharingStarted.Eagerly, snapshot[IPTV_GRID_KEY] ?: false)
        serversGridMode = context.browserPreferencesDataStore.data.map { it[SERVERS_GRID_KEY] ?: false }
            .stateIn(scope, SharingStarted.Eagerly, snapshot[SERVERS_GRID_KEY] ?: false)
        iptvSort = context.browserPreferencesDataStore.data.map { prefs ->
            PlaylistSort(
                by = PlaylistSortBy.entries.getOrNull(prefs[IPTV_SORT_BY_KEY] ?: 0) ?: PlaylistSortBy.NAME,
                asc = prefs[IPTV_SORT_ASC_KEY] ?: true,
            )
        }.stateIn(scope, SharingStarted.Eagerly, PlaylistSort(
            by = PlaylistSortBy.entries.getOrNull(snapshot[IPTV_SORT_BY_KEY] ?: 0) ?: PlaylistSortBy.NAME,
            asc = snapshot[IPTV_SORT_ASC_KEY] ?: true,
        ))
        serversSort = context.browserPreferencesDataStore.data.map { prefs ->
            ServerSort(
                by = ServerSortBy.entries.getOrNull(prefs[SERVERS_SORT_BY_KEY] ?: 0) ?: ServerSortBy.NAME,
                asc = prefs[SERVERS_SORT_ASC_KEY] ?: true,
            )
        }.stateIn(scope, SharingStarted.Eagerly, ServerSort(
            by = ServerSortBy.entries.getOrNull(snapshot[SERVERS_SORT_BY_KEY] ?: 0) ?: ServerSortBy.NAME,
            asc = snapshot[SERVERS_SORT_ASC_KEY] ?: true,
        ))
        browserGridMode = context.browserPreferencesDataStore.data.map { it[BROWSER_GRID_KEY] ?: false }
            .stateIn(scope, SharingStarted.Eagerly, snapshot[BROWSER_GRID_KEY] ?: false)
        browserSortAsc = context.browserPreferencesDataStore.data.map { it[BROWSER_SORT_ASC_KEY] ?: true }
            .stateIn(scope, SharingStarted.Eagerly, snapshot[BROWSER_SORT_ASC_KEY] ?: true)
        browserSort = context.browserPreferencesDataStore.data.map { prefs ->
            BrowserSort(
                by = BrowserSortBy.entries.getOrNull(prefs[BROWSER_SORT_BY_KEY] ?: 0) ?: BrowserSortBy.NAME,
                asc = prefs[BROWSER_SORT_ASC_KEY] ?: true,
            )
        }.stateIn(scope, SharingStarted.Eagerly, BrowserSort(
            by = BrowserSortBy.entries.getOrNull(snapshot[BROWSER_SORT_BY_KEY] ?: 0) ?: BrowserSortBy.NAME,
            asc = snapshot[BROWSER_SORT_ASC_KEY] ?: true,
        ))
    }

    suspend fun setIptvGridMode(value: Boolean) { context.browserPreferencesDataStore.edit { it[IPTV_GRID_KEY] = value } }
    suspend fun setServersGridMode(value: Boolean) { context.browserPreferencesDataStore.edit { it[SERVERS_GRID_KEY] = value } }
    suspend fun setIptvSort(sort: PlaylistSort) {
        context.browserPreferencesDataStore.edit {
            it[IPTV_SORT_BY_KEY] = sort.by.ordinal
            it[IPTV_SORT_ASC_KEY] = sort.asc
        }
    }
    suspend fun setServersSort(sort: ServerSort) {
        context.browserPreferencesDataStore.edit {
            it[SERVERS_SORT_BY_KEY] = sort.by.ordinal
            it[SERVERS_SORT_ASC_KEY] = sort.asc
        }
    }
    suspend fun setBrowserGridMode(value: Boolean) { context.browserPreferencesDataStore.edit { it[BROWSER_GRID_KEY] = value } }
    suspend fun setBrowserSortAsc(value: Boolean) { context.browserPreferencesDataStore.edit { it[BROWSER_SORT_ASC_KEY] = value } }
    suspend fun setBrowserSort(sort: BrowserSort) {
        context.browserPreferencesDataStore.edit {
            it[BROWSER_SORT_BY_KEY] = sort.by.ordinal
            it[BROWSER_SORT_ASC_KEY] = sort.asc
        }
    }
}
