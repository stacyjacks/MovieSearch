package kurmakaeva.anastasia.moviesearch.presentation.detail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import kurmakaeva.anastasia.moviesearch.domain.repository.MovieRepository
import kurmakaeva.anastasia.moviesearch.domain.usecase.GetMovieDetailUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = MovieDetailViewModel(movieId = 603, GetMovieDetailUseCase(repository))

    @Test
    fun loadsDetailForGivenId() = runTest(dispatcher) {
        val viewModel = createViewModel()
        assertEquals(MovieDetailUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(listOf(603), repository.requestedIds)
        assertEquals(MovieDetailUiState.Success(MATRIX), viewModel.uiState.value)
    }

    @Test
    fun failure_showsError_andRetrySucceeds() = runTest(dispatcher) {
        repository.result = Result.failure(IOException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(MovieDetailUiState.Error, viewModel.uiState.value)

        repository.result = Result.success(MATRIX)
        viewModel.onRetry()
        advanceUntilIdle()
        assertEquals(MovieDetailUiState.Success(MATRIX), viewModel.uiState.value)
    }

    private class FakeRepository : MovieRepository {
        val requestedIds = mutableListOf<Int>()
        var result: Result<MovieDetail> = Result.success(MATRIX)

        override suspend fun searchMovies(query: String, page: Int): Result<MoviePage> = error("unused")

        override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> {
            requestedIds += movieId
            return result
        }
    }

    private companion object {
        val MATRIX = MovieDetail(
            id = 603, title = "The Matrix", originalTitle = null, overview = null, posterUrl = null,
            backdropUrl = null, releaseDate = LocalDate.of(1999, 3, 31), runtimeMinutes = 136, rating = 8.2,
            genres = listOf("Action"), productionCountries = emptyList(),
        )
    }
}
