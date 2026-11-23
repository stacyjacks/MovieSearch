package kurmakaeva.anastasia.moviesearch.presentation.detail

import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail

sealed interface MovieDetailUiState {
    data object Loading : MovieDetailUiState
    data class Success(val detail: MovieDetail) : MovieDetailUiState
    data object Error : MovieDetailUiState
}
