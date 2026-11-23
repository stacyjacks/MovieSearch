package kurmakaeva.anastasia.moviesearch.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kurmakaeva.anastasia.moviesearch.data.cache.InMemoryCache
import kurmakaeva.anastasia.moviesearch.data.cache.SearchCacheKey
import kurmakaeva.anastasia.moviesearch.data.remote.TmdbApi
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDetailDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieSearchResponseDto
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

class MovieRepositoryImplTest {

    private val time = TestTimeSource()
    private val api = FakeTmdbApi()
    private val repository = MovieRepositoryImpl(
        api = api,
        searchCache = InMemoryCache<SearchCacheKey, MoviePage>(ttl = 5.minutes, maxEntries = 10, timeSource = time),
    )

    @Test
    fun sameQueryWithinTtl_isServedFromCache() = runTest {
        val first = repository.searchMovies("matrix")
        time += 4.minutes
        val second = repository.searchMovies("matrix")

        assertEquals(1, api.searchCalls)
        assertEquals(first, second)
    }

    @Test
    fun sameQueryAfterTtl_hitsApiAgain() = runTest {
        repository.searchMovies("matrix")
        time += 5.minutes
        repository.searchMovies("matrix")

        assertEquals(2, api.searchCalls)
    }

    @Test
    fun queriesDifferingOnlyInCaseOrWhitespace_shareCacheEntry() = runTest {
        repository.searchMovies("Matrix")
        repository.searchMovies("  matrix ")

        assertEquals(1, api.searchCalls)
    }

    @Test
    fun differentPages_areCachedSeparately() = runTest {
        repository.searchMovies("matrix", page = 1)
        repository.searchMovies("matrix", page = 2)

        assertEquals(2, api.searchCalls)
    }

    @Test
    fun failures_areNotCached() = runTest {
        api.failNext = true
        assertTrue(repository.searchMovies("matrix").exceptionOrNull() is IOException)

        assertTrue(repository.searchMovies("matrix").isSuccess)
        assertEquals(2, api.searchCalls)
    }

    @Test
    fun cancelledSearch_propagatesCancellation_andIsNotCached() = runTest {
        api.hangNext = true
        val search = async { repository.searchMovies("matrix") }
        advanceUntilIdle()

        search.cancel()
        advanceUntilIdle()

        assertTrue(search.isCancelled)
        // Not cached as a result or as a failure: the next search hits the API again.
        assertTrue(repository.searchMovies("matrix").isSuccess)
        assertEquals(2, api.searchCalls)
    }

    private class FakeTmdbApi : TmdbApi {
        var searchCalls = 0
        var failNext = false
        var hangNext = false

        override suspend fun searchMovies(query: String, page: Int, includeAdult: Boolean): MovieSearchResponseDto {
            searchCalls++
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
