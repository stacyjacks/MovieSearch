package kurmakaeva.anastasia.moviesearch.domain.model

/** A movie as shown in search results. */
data class Movie(
    val id: Int,
    val title: String,
    val overview: String?,
    val posterUrl: String?,
    val releaseYear: Int?,
    val rating: Double?,
)
