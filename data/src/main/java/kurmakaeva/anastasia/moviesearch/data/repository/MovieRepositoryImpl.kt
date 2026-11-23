package kurmakaeva.anastasia.moviesearch.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kurmakaeva.anastasia.moviesearch.data.cache.InMemoryCache
import kurmakaeva.anastasia.moviesearch.data.cache.SearchCacheKey
import kurmakaeva.anastasia.moviesearch.data.concurrency.InFlightRequests
import kurmakaeva.anastasia.moviesearch.data.di.ApplicationScope
import kurmakaeva.anastasia.moviesearch.data.di.IoDispatcher
import kurmakaeva.anastasia.moviesearch.data.mapper.toDomain
import kurmakaeva.anastasia.moviesearch.data.remote.TmdbApi
import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import kurmakaeva.anastasia.moviesearch.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * Main-safe: all work (HTTP, JSON mapping, cache writes) runs on the injected IO dispatcher,
 * so callers can use it from the main thread.
 */
internal class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    private val searchCache: InMemoryCache<SearchCacheKey, MoviePage>,
    @ApplicationScope applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MovieRepository {
    private val searchRequests = InFlightRequests<SearchCacheKey, Result<MoviePage>>(applicationScope)

    /**
     * 1. Served from [searchCache] when the same query/page was fetched within the TTL.
     * 2. Otherwise joins an identical search that's already in flight, or starts one; concurrent
     *    identical searches cause a single HTTP request.
     * 3. Cancelling a caller only cancels the HTTP request when no other caller is waiting for it.
     *
     * Only successful results are cached; failures are returned to every waiter and not remembered.
     */
    override suspend fun searchMovies(query: String, page: Int): Result<MoviePage> {
        val key = SearchCacheKey.of(query, page)
        searchCache.get(key)?.let { return Result.success(it) }
        return searchRequests.getOrStart(key) {
            // Re-check: an identical request may have completed (and filled the cache) between the
            // lookup above and starting this one. The cache is written before a request completes,
            // so every request started after that point finds the result here.
            searchCache.get(key)?.let { return@getOrStart Result.success(it) }
            safeApiCall { api.searchMovies(query = key.query, page = key.page).toDomain() }
                .onSuccess { searchCache.put(key, it) }
        }
    }

    override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> = withContext(ioDispatcher) {
        safeApiCall { api.getMovieDetail(movieId).toDomain() }
    }

    /** Like runCatching, but lets coroutine cancellation propagate instead of turning it into a failure. */
    private inline fun <T> safeApiCall(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
