package kurmakaeva.anastasia.moviesearch.presentation.search

import kurmakaeva.anastasia.moviesearch.domain.model.Movie

data class MovieSearchUiState(
    val query: String = "",
    val results: SearchResults = SearchResults.Idle,
)

sealed interface SearchResults {
    /** Nothing searched yet (blank query). */
    data object Idle : SearchResults
    data object Loading : SearchResults
    data class Success(val movies: List<Movie>) : SearchResults
    data object Empty : SearchResults
    data object Error : SearchResults
}
