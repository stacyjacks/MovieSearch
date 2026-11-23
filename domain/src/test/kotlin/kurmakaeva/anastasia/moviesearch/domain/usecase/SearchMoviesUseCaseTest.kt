package kurmakaeva.anastasia.moviesearch.domain.usecase

import kotlinx.coroutines.test.runTest
import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import kurmakaeva.anastasia.moviesearch.domain.repository.MovieRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMoviesUseCaseTest {
    private class FakeRepository : MovieRepository {
        val queries = mutableListOf<String>()
        override suspend fun searchMovies(query: String, page: Int): Result<MoviePage> {
            queries += query
            return Result.success(MoviePage.EMPTY)
        }
        override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> = error("unused")
    }

    private val repository = FakeRepository()
    private val useCase = SearchMoviesUseCase(repository)

    @Test
    fun blankQuery_returnsEmptyWithoutCallingRepository() = runTest {
        val result = useCase("   ")

        assertEquals(MoviePage.EMPTY, result.getOrThrow())
        assertTrue(repository.queries.isEmpty())
    }

    @Test
    fun query_isTrimmedBeforeSearching() = runTest {
        useCase("  matrix ")

        assertEquals(listOf("matrix"), repository.queries)
    }
}
