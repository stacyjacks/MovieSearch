package kurmakaeva.anastasia.moviesearch.data.remote

import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDetailDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB v3 endpoints. The `api_key` query parameter is added to every request
 * by [kurmakaeva.anastasia.moviesearch.data.remote.interceptor.ApiKeyInterceptor].
 */
interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): MovieSearchResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
    ): MovieDetailDto
}
