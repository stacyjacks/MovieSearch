package kurmakaeva.anastasia.moviesearch.domain.model

/** One page of paginated search results. */
data class MoviePage(
    val movies: List<Movie>,
    val page: Int,
) {
    companion object {
        val EMPTY = MoviePage(movies = emptyList(), page = 1)
    }
}
