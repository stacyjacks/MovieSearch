package kurmakaeva.anastasia.moviesearch.domain.model

import java.time.LocalDate

data class MovieDetail(
    val id: Int,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: LocalDate?,
    val runtimeMinutes: Int?,
    val rating: Double?,
    val genres: List<String>,
    val productionCountries: List<String>,
)
