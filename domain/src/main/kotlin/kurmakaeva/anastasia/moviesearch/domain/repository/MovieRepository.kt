package kurmakaeva.anastasia.moviesearch.domain.repository

import kurmakaeva.anastasia.moviesearch.domain.error.DataError
import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage

/**
 * Implemented by the data layer (`:data`). Failures are returned as [Result.failure] holding a
 * [DataError], never thrown. Coroutine cancellation is the exception: it propagates as usual.
 */
interface MovieRepository {
    suspend fun searchMovies(query: String, page: Int = 1): Result<MoviePage>
    suspend fun getMovieDetail(movieId: Int): Result<MovieDetail>
}
