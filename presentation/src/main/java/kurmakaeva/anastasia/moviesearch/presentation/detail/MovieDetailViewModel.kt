package kurmakaeva.anastasia.moviesearch.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kurmakaeva.anastasia.moviesearch.domain.usecase.GetMovieDetailUseCase

/** The movie id comes from the navigation route and is passed in via Hilt assisted injection. */
@HiltViewModel(assistedFactory = MovieDetailViewModel.Factory::class)
class MovieDetailViewModel @AssistedInject constructor(
    @Assisted private val movieId: Int,
    private val getMovieDetail: GetMovieDetailUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(movieId: Int): MovieDetailViewModel
    }

    private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = MovieDetailUiState.Loading
            _uiState.value = getMovieDetail(movieId).fold(
                onSuccess = { MovieDetailUiState.Success(it) },
                onFailure = { MovieDetailUiState.Error },
            )
        }
    }
}
