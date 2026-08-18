package dev.vayou.core.model

import kotlinx.serialization.Serializable

/**
 * What has been searched for, newest first.
 *
 * Kept because the same handful of films get looked for again and again, and typing "documentário"
 * a second time is a dozen taps the app already knows the answer to.
 */
@Serializable
data class SearchHistory(val queries: List<String> = emptyList()) {

    /**
     * A repeated search moves to the top rather than appearing twice, and the tail falls off the
     * end: a list that only grows is one nobody reaches the bottom of.
     */
    fun remember(query: String): SearchHistory {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return this
        return SearchHistory(
            (
                listOf(trimmed) + queries.filterNot {
                    it.equals(trimmed, ignoreCase = true)
                }
                ).take(Limit),
        )
    }

    fun forget(query: String): SearchHistory = SearchHistory(queries - query)

    fun clear(): SearchHistory = SearchHistory()
}

/** Enough to cover what a viewer is working through this week, and no more. */
private const val Limit = 10
