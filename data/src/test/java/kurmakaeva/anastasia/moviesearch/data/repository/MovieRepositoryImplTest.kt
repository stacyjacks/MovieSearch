package kurmakaeva.anastasia.moviesearch.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kurmakaeva.anastasia.moviesearch.data.cache.InMemoryCache
import kurmakaeva.anastasia.moviesearch.data.cache.SearchCacheKey
import kurmakaeva.anastasia.moviesearch.data.remote.TmdbApi
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDetailDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieSearchResponseDto
import kurmakaeva.anastasia.moviesearch.domain.error.DataError
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

    private fun TestScope.newRepository(): MovieRepositoryImpl {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return MovieRepositoryImpl(
            api = api,
            searchCache = InMemoryCache<SearchCacheKey, MoviePage>(ttl = 5.minutes, maxEntries = 10, timeSource = time),
            applicationScope = CoroutineScope(SupervisorJob() + dispatcher),
            ioDispatcher = dispatcher,
        )
    }

    // --- Caching ---

    @Test
    fun sameQueryWithinTtl_isServedFromCache() = runTest {
        val repository = newRepository()
        val first = repository.searchMovies("matrix")
        time += 4.minutes
        val second = repository.searchMovies("matrix")

        assertEquals(1, api.searchCalls)
        assertEquals(first, second)
    }

    @Test
    fun sameQueryAfterTtl_hitsApiAgain() = runTest {
        val repository = newRepository()
        repository.searchMovies("matrix")
        time += 5.minutes
        repository.searchMovies("matrix")

        assertEquals(2, api.searchCalls)
    }

    @Test
    fun queriesDifferingOnlyInCaseOrWhitespace_shareCacheEntry() = runTest {
        val repository = newRepository()
        repository.searchMovies("Matrix")
        repository.searchMovies("  matrix ")

        assertEquals(1, api.searchCalls)
    }

    @Test
    fun differentPages_areCachedSeparately() = runTest {
        val repository = newRepository()
        repository.searchMovies("matrix", page = 1)
        repository.searchMovies("matrix", page = 2)

        assertEquals(2, api.searchCalls)
    }

    @Test
    fun failures_areNotCached() = runTest {
        val repository = newRepository()
        api.failNext = true
        assertTrue(repository.searchMovies("matrix").isFailure)

        assertTrue(repository.searchMovies("matrix").isSuccess)
        assertEquals(2, api.searchCalls)
    }

    // --- Concurrency: identical in-flight searches are coalesced ---

    @Test
    fun concurrentIdenticalSearches_makeASingleRequest() = runTest {
        val repository = newRepository()
        api.gate = CompletableDeferred()

        val searches = listOf("matrix", "Matrix", " matrix ").map { q -> async { repository.searchMovies(q) } }
        advanceUntilIdle()
        assertEquals(1, api.searchCalls)

        api.gate!!.complete(Unit)
        val results = searches.awaitAll()

        assertTrue(results.all { it.isSuccess })
        assertEquals(1, results.map { it.getOrThrow() }.distinct().size)
        assertEquals(1, api.searchCalls)
    }

    @Test
    fun concurrentFailure_isReturnedToEveryCaller_andNotCached() = runTest {
        val repository = newRepository()
        api.gate = CompletableDeferred()
        api.failNext = true

        val searches = List(2) { async { repository.searchMovies("matrix") } }
        advanceUntilIdle()
        api.gate!!.complete(Unit)

        assertTrue(searches.awaitAll().all { it.exceptionOrNull() is DataError.Network })
        assertEquals(1, api.searchCalls)
        assertTrue(repository.searchMovies("matrix").isSuccess)
        assertEquals(2, api.searchCalls)
    }

    // --- Cancellation ---

    @Test
    fun oneCallerCancelled_otherCallerStillGetsResult_andItIsCached() = runTest {
        val repository = newRepository()
        api.gate = CompletableDeferred()

        val leaving = async { repository.searchMovies("matrix") }
        val staying = async { repository.searchMovies("matrix") }
        advanceUntilIdle()
        leaving.cancel()
        advanceUntilIdle()
        assertEquals(0, api.cancelledSearches)

        api.gate!!.complete(Unit)
        assertTrue(staying.await().isSuccess)
        assertTrue(leaving.isCancelled)

        repository.searchMovies("matrix")
        assertEquals(1, api.searchCalls)
    }

    @Test
    fun allCallersCancelled_cancelsTheRequest_andNothingIsCached() = runTest {
        val repository = newRepository()
        api.gate = CompletableDeferred()

        val searches = List(2) { async { repository.searchMovies("matrix") } }
        advanceUntilIdle()
        searches.forEach { it.cancel() }
        advanceUntilIdle()

        assertTrue(searches.all { it.isCancelled })
        assertEquals(1, api.cancelledSearches)

        // Not cached as a result or as a failure: the next search hits the API again.
        api.gate = null
        assertTrue(repository.searchMovies("matrix").isSuccess)
        assertEquals(2, api.searchCalls)
    }

    private class FakeTmdbApi : TmdbApi {
        var searchCalls = 0
        var cancelledSearches = 0
        var failNext = false

        /** When set, searches suspend until it completes (simulates a slow network). */
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun searchMovies(query: String, page: Int, includeAdult: Boolean): MovieSearchResponseDto {
            searchCalls++
            try {
                gate?.await()
            } catch (e: CancellationException) {
                cancelledSearches++
                throw e
            }
            if (failNext) {
                failNext = false
                throw IOException("offline")
            }
            return MovieSearchResponseDto(
                page = page,
                results = listOf(MovieDto(id = 603, title = "The Matrix")),
            )
        }

        override suspend fun getMovieDetail(movieId: Int): MovieDetailDto = error("unused")
    }
}
