package kurmakaeva.anastasia.moviesearch.presentation.search

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kurmakaeva.anastasia.moviesearch.domain.error.DataError
import kurmakaeva.anastasia.moviesearch.domain.model.Movie
import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import kurmakaeva.anastasia.moviesearch.domain.repository.MovieRepository
import kurmakaeva.anastasia.moviesearch.domain.usecase.SearchMoviesUseCase
import kurmakaeva.anastasia.moviesearch.presentation.error.ErrorReason
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MovieSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private lateinit var viewModel: MovieSearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = MovieSearchViewModel(SearchMoviesUseCase(repository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun typing_debouncesThenShowsResults() = runTest(dispatcher) {
        viewModel.onQueryChange("mat")
        viewModel.onQueryChange("matrix")
        advanceTimeBy(MovieSearchViewModel.SEARCH_DEBOUNCE_MS + 1)
        advanceUntilIdle()

        assertEquals(listOf("matrix"), repository.queries)
        assertEquals(SearchResults.Success(listOf(MATRIX)), viewModel.uiState.value.results)
    }

    @Test
    fun noResults_showsEmpty() = runTest(dispatcher) {
        repository.result = Result.success(MoviePage.EMPTY)
        viewModel.onQueryChange("zzzz")
        advanceUntilIdle()

        assertEquals(SearchResults.Empty, viewModel.uiState.value.results)
    }

    @Test
    fun failure_showsError_andRetrySucceeds() = runTest(dispatcher) {
        repository.result = Result.failure(DataError.Network(IOException("offline")))
        viewModel.onQueryChange("matrix")
        advanceUntilIdle()
        assertEquals(SearchResults.Error(ErrorReason.Network), viewModel.uiState.value.results)

        repository.result = Result.success(PAGE)
        viewModel.onRetry()
        advanceUntilIdle()
        assertEquals(SearchResults.Success(listOf(MATRIX)), viewModel.uiState.value.results)
    }

    @Test
    fun invalidApiKey_showsItsOwnError_withoutRetry() = runTest(dispatcher) {
        repository.result = Result.failure(DataError.InvalidApiKey())
        viewModel.onQueryChange("matrix")
        advanceUntilIdle()

        val error = viewModel.uiState.value.results as SearchResults.Error
        assertEquals(ErrorReason.InvalidApiKey, error.reason)
        assertFalse(error.reason.canRetry)
    }

    @Test
    fun clearingQuery_returnsToIdle() = runTest(dispatcher) {
        viewModel.onQueryChange("matrix")
        viewModel.onQueryChange("")
        advanceUntilIdle()

        assertEquals(SearchResults.Idle, viewModel.uiState.value.results)
        assertEquals(emptyList<String>(), repository.queries)
    }

    @Test
    fun newQueryWhileRequestInFlight_cancelsItAndIgnoresItsResult() = runTest(dispatcher) {
        val slowRepository = SuspendingRepository()
        val viewModel = MovieSearchViewModel(SearchMoviesUseCase(slowRepository))

        viewModel.onQueryChange("matrix")
        advanceUntilIdle()
        assertEquals(SearchResults.Loading, viewModel.uiState.value.results)

        viewModel.onQueryChange("inception")
        advanceUntilIdle()
        assertEquals(listOf("matrix"), slowRepository.cancelled)

        // A late response for the stale query must not be shown.
        slowRepository.respond("matrix", PAGE)
        slowRepository.respond("inception", INCEPTION_PAGE)
        advanceUntilIdle()

        assertEquals(SearchResults.Success(listOf(INCEPTION)), viewModel.uiState.value.results)
        assertEquals("inception", viewModel.uiState.value.query)
    }

    @Test
    fun clearWhileRequestInFlight_cancelsItAndReturnsToIdle() = runTest(dispatcher) {
        val slowRepository = SuspendingRepository()
        val viewModel = MovieSearchViewModel(SearchMoviesUseCase(slowRepository))

        viewModel.onQueryChange("matrix")
        advanceUntilIdle()
        viewModel.onClearQuery()
        slowRepository.respond("matrix", PAGE)
        advanceUntilIdle()

        assertEquals(listOf("matrix"), slowRepository.cancelled)
        assertEquals(MovieSearchUiState(query = "", results = SearchResults.Idle), viewModel.uiState.value)
    }

    /** Suspends each search until [respond] is called, and records searches that were cancelled while waiting. */
    private class SuspendingRepository : MovieRepository {
        private val pending = mutableMapOf<String, CompletableDeferred<Result<MoviePage>>>()
        val cancelled = mutableListOf<String>()

        fun respond(query: String, page: MoviePage) {
            pending[query]?.complete(Result.success(page))
        }

        override suspend fun searchMovies(query: String, page: Int): Result<MoviePage> {
            val response = CompletableDeferred<Result<MoviePage>>().also { pending[query] = it }
            try {
                return response.await()
            } catch (e: CancellationException) {
                cancelled += query
                throw e
            }
        }

        override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> = error("unused")
    }

    private class FakeRepository : MovieRepository {
        val queries = mutableListOf<String>()
        var result: Result<MoviePage> = Result.success(PAGE)

        override suspend fun searchMovies(query: String, page: Int): Result<MoviePage> {
            queries += query
            return result
        }

        override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> = error("unused")
    }

    private companion object {
        val MATRIX = Movie(603, "The Matrix", null, null, 1999, 8.2)
        val PAGE = MoviePage(listOf(MATRIX), page = 1)
        val INCEPTION = Movie(27205, "Inception", null, null, 2010, 8.4)
        val INCEPTION_PAGE = MoviePage(listOf(INCEPTION), page = 1)
    }
}
