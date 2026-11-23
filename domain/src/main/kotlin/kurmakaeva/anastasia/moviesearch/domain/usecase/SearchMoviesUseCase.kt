package kurmakaeva.anastasia.moviesearch.domain.usecase

import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import kurmakaeva.anastasia.moviesearch.domain.repository.MovieRepository
import javax.inject.Inject

class SearchMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    /** A blank query returns an empty page without hitting the network. */
    suspend operator fun invoke(query: String, page: Int = 1): Result<MoviePage> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(MoviePage.EMPTY)
        return repository.searchMovies(trimmed, page)
    }
}
