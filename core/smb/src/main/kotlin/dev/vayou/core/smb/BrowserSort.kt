package dev.vayou.core.smb

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/**
 * What a file listing can be ordered by. Carries no label: how an axis is named belongs to whoever
 * draws it, in that reader's language, and a string sitting here would be one language for everyone.
 *
 * Appended to, never reordered: the chosen axis goes to disk as an ordinal.
 */
enum class BrowserSortBy { Name, Size, Type }

data class BrowserSort(val by: BrowserSortBy = BrowserSortBy.Name, val isAscending: Boolean = true)

private val Context.browserSortDataStore: DataStore<Preferences> by preferencesDataStore("browser_prefs")

private val SortByKey = intPreferencesKey("browser_sort_by")

private val SortAscendingKey = booleanPreferencesKey("browser_sort_asc")

/**
 * How the network browser is ordered, kept across launches.
 *
 * Read once synchronously at construction so the first frame draws in the order it was left in -- a
 * list that appears sorted by name and reorders itself a frame later is worse than one that waits.
 */
@Singleton
class BrowserSortStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val sort: StateFlow<BrowserSort>

    init {
        val onDisk = runBlocking { context.browserSortDataStore.data.first() }
        sort = context.browserSortDataStore.data
            .map { it.toBrowserSort() }
            .stateIn(scope, SharingStarted.Eagerly, onDisk.toBrowserSort())
    }

    suspend fun setSort(sort: BrowserSort) {
        context.browserSortDataStore.edit {
            it[SortByKey] = sort.by.ordinal
            it[SortAscendingKey] = sort.isAscending
        }
    }
}

/**
 * A listing in the order asked for.
 *
 * Folders first, whichever order the files are in and whichever way it runs. Every file manager
 * going back to Windows does this, and it is the reason the client hands the listing over unsorted
 * -- the order belongs to whoever shows it. Outside the axis rather than inside it, or reversing the
 * order would drop the folders to the bottom of a list somebody is navigating with, when what they
 * asked to turn over was the files.
 */
fun List<SmbFileItem>.sortedBy(sort: BrowserSort): List<SmbFileItem> {
    val axis: Comparator<SmbFileItem> = when (sort.by) {
        BrowserSortBy.Name -> compareBy { it.name.lowercase() }
        BrowserSortBy.Size -> compareBy { it.size }
        // Then by name, so files of one kind arrive in an order rather than in whatever order the
        // share listed them.
        BrowserSortBy.Type -> compareBy<SmbFileItem> { it.extension }.thenBy { it.name.lowercase() }
    }
    val order = if (sort.isAscending) axis else axis.reversed()
    return sortedWith(compareBy<SmbFileItem> { !it.isDirectory }.then(order))
}

private fun Preferences.toBrowserSort() = BrowserSort(
    by = BrowserSortBy.entries.getOrNull(this[SortByKey] ?: 0) ?: BrowserSortBy.Name,
    isAscending = this[SortAscendingKey] ?: true,
)
