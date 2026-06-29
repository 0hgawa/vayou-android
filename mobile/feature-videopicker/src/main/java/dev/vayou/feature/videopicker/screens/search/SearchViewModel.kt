package dev.vayou.feature.videopicker.screens.search

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.data.repository.SearchHistoryRepository
import dev.vayou.core.domain.SearchMediaUseCase
import dev.vayou.core.domain.SearchResults
import dev.vayou.core.media.sync.MediaInfoSynchronizer
import dev.vayou.core.model.ApplicationPreferences
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMediaUseCase: SearchMediaUseCase,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val mediaInfoSynchronizer: MediaInfoSynchronizer,
) : ViewModel() {

    private val uiStateInternal = MutableStateFlow(SearchUiState())
    val uiState = uiStateInternal.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        collectSearchHistory()
        collectPreferences()
        collectSearchResults()
    }

    private fun collectSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.searchHistory.collect { history ->
                uiStateInternal.update { it.copy(searchHistory = history) }
            }
        }
    }

    private fun collectPreferences() {
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { prefs ->
                uiStateInternal.update { it.copy(preferences = prefs) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectSearchResults() {
        viewModelScope.launch {
            searchQuery
                .flatMapLatest { query ->
                    searchMediaUseCase(query)
                }
                .collect { results ->
                    uiStateInternal.update {
                        it.copy(
                            searchResults = results,
                            isSearching = false,
                        )
                    }
                }
        }
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnQueryChange -> onQueryChange(event.query)
            is SearchUiEvent.OnSearch -> onSearch(event.query)
            is SearchUiEvent.OnHistoryItemClick -> onHistoryItemClick(event.query)
            is SearchUiEvent.OnRemoveHistoryItem -> removeHistoryItem(event.query)
            is SearchUiEvent.OnClearHistory -> clearHistory()
            is SearchUiEvent.AddToSync -> addToMediaInfoSynchronizer(event.uri)
        }
    }

    private fun onQueryChange(query: String) {
        uiStateInternal.update { it.copy(query = query, isSearching = query.isNotBlank()) }
        searchQuery.value = query
    }

    private fun onSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searchHistoryRepository.addSearchQuery(query)
        }
    }

    private fun onHistoryItemClick(query: String) {
        uiStateInternal.update { it.copy(query = query, isSearching = true) }
        searchQuery.value = query
        onSearch(query)
    }

    private fun removeHistoryItem(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeSearchQuery(query)
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
        }
    }

    private fun addToMediaInfoSynchronizer(uri: Uri) {
        viewModelScope.launch {
            mediaInfoSynchronizer.sync(uri)
        }
    }
}

@Stable
data class SearchUiState(
    val query: String = "",
    val searchHistory: List<String> = emptyList(),
    val searchResults: SearchResults = SearchResults(),
    val isSearching: Boolean = false,
    val preferences: ApplicationPreferences = ApplicationPreferences(),
)

sealed interface SearchUiEvent {
    data class OnQueryChange(val query: String) : SearchUiEvent
    data class OnSearch(val query: String) : SearchUiEvent
    data class OnHistoryItemClick(val query: String) : SearchUiEvent
    data class OnRemoveHistoryItem(val query: String) : SearchUiEvent
    data object OnClearHistory : SearchUiEvent
    data class AddToSync(val uri: Uri) : SearchUiEvent
}
