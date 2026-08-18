package dev.vayou.core.datastore.datasource

import androidx.datastore.core.DataStore
import dev.vayou.core.model.SearchHistory
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchHistoryDataSource @Inject constructor(private val store: DataStore<SearchHistory>) {

    val queries: Flow<List<String>> = store.data.map { it.queries }

    /** Swallowed on failure: a search that cannot be written down is still a search that ran. */
    suspend fun update(transform: (SearchHistory) -> SearchHistory) {
        runCatching { store.updateData { transform(it) } }
    }
}
