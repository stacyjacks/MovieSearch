package kurmakaeva.anastasia.moviesearch.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailDto(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genres") val genres: List<GenreDto> = emptyList(),
    @SerialName("production_countries") val productionCountries: List<ProductionCountryDto> = emptyList(),
)

@Serializable
data class GenreDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
)

@Serializable
data class ProductionCountryDto(
    @SerialName("iso_3166_1") val isoCode: String,
    @SerialName("name") val name: String,
)
