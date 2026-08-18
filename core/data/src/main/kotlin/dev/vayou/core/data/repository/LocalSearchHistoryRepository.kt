package dev.vayou.core.data.repository

import dev.vayou.core.datastore.datasource.SearchHistoryDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class LocalSearchHistoryRepository @Inject constructor(private val dataSource: SearchHistoryDataSource) :
    SearchHistoryRepository {

    override val queries: Flow<List<String>> = dataSource.queries

    override suspend fun remember(query: String) = dataSource.update { it.remember(query) }

    override suspend fun forget(query: String) = dataSource.update { it.forget(query) }

    override suspend fun clear() = dataSource.update { it.clear() }
}
