package kurmakaeva.anastasia.moviesearch.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kurmakaeva.anastasia.moviesearch.domain.usecase.SearchMoviesUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MovieSearchViewModel @Inject constructor(
    private val searchMovies: SearchMoviesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MovieSearchUiState())
    val uiState: StateFlow<MovieSearchUiState> = _uiState.asStateFlow()

    /**
     * The single in-flight search. Starting a new search (typing, clearing, retrying) cancels it,
     * which also cancels the underlying HTTP call. A cancelled search never updates [uiState]:
     * cancellation surfaces as a CancellationException at the suspended call, so the code that
     * publishes results is never reached, and all state updates happen on the main thread.
     */
    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        search(query, debounce = true)
    }

    fun onClearQuery() = onQueryChange("")

    fun onRetry() {
        search(_uiState.value.query, debounce = false)
    }

    private fun search(query: String, debounce: Boolean) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = SearchResults.Idle) }
            return
        }
        searchJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS.milliseconds)
            _uiState.update { it.copy(results = SearchResults.Loading) }
            val results = searchMovies(query).fold(
                onSuccess = { page ->
                    if (page.movies.isEmpty()) SearchResults.Empty
                    else SearchResults.Success(page.movies)
                },
                onFailure = { SearchResults.Error },
            )
            _uiState.update { it.copy(results = results) }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
