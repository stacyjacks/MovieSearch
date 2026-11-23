package kurmakaeva.anastasia.moviesearch.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kurmakaeva.anastasia.moviesearch.data.remote.TmdbApi
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDetailDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieSearchResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MovieRepositoryImplTest {

    private val api = FakeTmdbApi()
    private val repository = MovieRepositoryImpl(api)

    @Test
    fun searchMovies_mapsResponseToDomain() = runTest {
        val page = repository.searchMovies("matrix").getOrThrow()

        assertEquals("The Matrix", page.movies.single().title)
    }

    @Test
    fun failures_areReturnedAsResultFailure() = runTest {
        api.failNext = true

        val result = repository.searchMovies("matrix")

        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun cancelledSearch_propagatesCancellation_insteadOfReturningFailure() = runTest {
        api.hangNext = true
        val search = async { repository.searchMovies("matrix") }
        advanceUntilIdle()

        search.cancel()
        advanceUntilIdle()

        assertTrue(search.isCancelled)
    }

    private class FakeTmdbApi : TmdbApi {
        var failNext = false
        var hangNext = false

        override suspend fun searchMovies(query: String, page: Int, includeAdult: Boolean): MovieSearchResponseDto {
            if (hangNext) {
                hangNext = false
                awaitCancellation()
            }
            if (failNext) {
                failNext = false
                throw IOException("offline")
            }
            return MovieSearchResponseDto(page = page, results = listOf(MovieDto(id = 603, title = "The Matrix")))
        }

        override suspend fun getMovieDetail(movieId: Int): MovieDetailDto = error("unused")
    }
}
