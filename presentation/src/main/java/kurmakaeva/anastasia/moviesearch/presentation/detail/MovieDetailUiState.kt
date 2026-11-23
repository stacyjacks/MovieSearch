package kurmakaeva.anastasia.moviesearch.presentation.detail

import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.presentation.error.ErrorReason

sealed interface MovieDetailUiState {
    data object Loading : MovieDetailUiState
    data class Success(val detail: MovieDetail) : MovieDetailUiState
    data class Error(val reason: ErrorReason) : MovieDetailUiState
}
