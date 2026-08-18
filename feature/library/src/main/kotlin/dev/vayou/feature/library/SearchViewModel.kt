package dev.vayou.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.SearchHistoryRepository
import dev.vayou.core.domain.SearchMediaUseCase
import dev.vayou.core.domain.SearchResults
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    searchMedia: SearchMediaUseCase,
    private val historyRepository: SearchHistoryRepository,
) : ViewModel() {

    private val queryInternal = MutableStateFlow("")
    val query: StateFlow<String> = queryInternal.asStateFlow()

    /**
     * What the library found, a beat behind the typing.
     *
     * Debounced because the whole library is scored on every change, and a fast typist would have
     * the work thrown away on the next letter anyway. `flatMapLatest` cancels the run in flight, so
     * only the last query is ever finished.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val results: StateFlow<SearchResults> = queryInternal
        .debounce(TypingPauseMillis)
        .flatMapLatest(searchMedia::invoke)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), SearchResults())

    val history: StateFlow<List<String>> = historyRepository.queries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), emptyList())

    fun onQueryChange(value: String) {
        queryInternal.value = value
    }

    /**
     * Written down only when a search is finished with, not on every letter.
     *
     * Finished with means: the keyboard's search key, or opening one of the results. Otherwise the
     * history fills with every prefix of every word ever typed.
     */
    fun rememberQuery() {
        val current = queryInternal.value
        if (current.isBlank()) return
        viewModelScope.launch { historyRepository.remember(current) }
    }

    fun recall(query: String) {
        queryInternal.value = query
        rememberQuery()
    }

    fun forget(query: String) {
        viewModelScope.launch { historyRepository.forget(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }
}

/** Long enough to cover the gap between two keystrokes, short enough to feel like live results. */
private const val TypingPauseMillis = 200L

private const val StopTimeoutMillis = 5_000L
