package dev.vayou.core.data.repository

import kotlinx.coroutines.flow.Flow

/** What has been searched for before, newest first. */
interface SearchHistoryRepository {

    val queries: Flow<List<String>>

    suspend fun remember(query: String)

    suspend fun forget(query: String)

    suspend fun clear()
}
