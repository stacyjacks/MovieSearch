package kurmakaeva.anastasia.moviesearch.data.repository

import kotlinx.coroutines.CancellationException
import kurmakaeva.anastasia.moviesearch.data.mapper.toDomain
import kurmakaeva.anastasia.moviesearch.data.remote.TmdbApi
import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import kurmakaeva.anastasia.moviesearch.domain.repository.MovieRepository
import javax.inject.Inject

internal class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
) : MovieRepository {

    override suspend fun searchMovies(query: String, page: Int): Result<MoviePage> =
        safeApiCall { api.searchMovies(query = query, page = page).toDomain() }

    override suspend fun getMovieDetail(movieId: Int): Result<MovieDetail> =
        safeApiCall { api.getMovieDetail(movieId).toDomain() }

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
