package kurmakaeva.anastasia.moviesearch.domain.repository

import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage

/** Implemented by the data layer (`:data`). Failures are returned as [Result.failure], never thrown. */
interface MovieRepository {
    suspend fun searchMovies(query: String, page: Int = 1): Result<MoviePage>
    suspend fun getMovieDetail(movieId: Int): Result<MovieDetail>
}
