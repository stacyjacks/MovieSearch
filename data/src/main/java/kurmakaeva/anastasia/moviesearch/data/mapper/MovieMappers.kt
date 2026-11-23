package kurmakaeva.anastasia.moviesearch.data.mapper

import kurmakaeva.anastasia.moviesearch.domain.model.Movie
import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import kurmakaeva.anastasia.moviesearch.data.remote.TmdbImageUrl
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDetailDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieSearchResponseDto
import java.time.LocalDate
import java.time.format.DateTimeParseException

internal fun MovieSearchResponseDto.toDomain() = MoviePage(
    movies = results.map { it.toDomain() },
    page = page,
)

internal fun MovieDto.toDomain() = Movie(
    id = id,
    title = title,
    overview = overview?.takeIf { it.isNotBlank() },
    posterUrl = TmdbImageUrl.build(posterPath, TmdbImageUrl.Size.W342),
    releaseYear = releaseDate?.take(4)?.toIntOrNull(),
    rating = voteAverage,
)

internal fun MovieDetailDto.toDomain() = MovieDetail(
    id = id,
    title = title,
    originalTitle = originalTitle,
    overview = overview?.takeIf { it.isNotBlank() },
    posterUrl = TmdbImageUrl.build(posterPath, TmdbImageUrl.Size.W780),
    backdropUrl = TmdbImageUrl.build(backdropPath, TmdbImageUrl.Size.W780),
    releaseDate = releaseDate.toLocalDateOrNull(),
    runtimeMinutes = runtime?.takeIf { it > 0 },
    rating = voteAverage,
    genres = genres.map { it.name },
    productionCountries = productionCountries.map { it.name },
)

/** TMDB sends `yyyy-MM-dd`, but also `""` for unknown dates. */
private fun String?.toLocalDateOrNull(): LocalDate? =
    if (isNullOrBlank()) null else try {
        LocalDate.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
