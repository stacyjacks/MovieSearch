package kurmakaeva.anastasia.moviesearch.data.mapper

import kurmakaeva.anastasia.moviesearch.data.remote.model.GenreDto
import kurmakaeva.anastasia.moviesearch.data.remote.model.MovieDetailDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MovieMappersTest {
    private val dto = MovieDetailDto(
        id = 603,
        title = "The Matrix",
        posterPath = "/poster.jpg",
        releaseDate = "1999-03-31",
        runtime = 136,
        genres = listOf(GenreDto(28, "Action")),
    )

    @Test
    fun detail_mapsDatesImagesAndGenres() {
        val detail = dto.toDomain()

        assertEquals(LocalDate.of(1999, 3, 31), detail.releaseDate)
        assertEquals("https://image.tmdb.org/t/p/w780/poster.jpg", detail.posterUrl)
        assertNull(detail.backdropUrl)
        assertEquals(listOf("Action"), detail.genres)
    }

    @Test
    fun detail_blankOrInvalidDate_andZeroRuntime_becomeNull() {
        assertNull(dto.copy(releaseDate = "").toDomain().releaseDate)
        assertNull(dto.copy(releaseDate = "soon").toDomain().releaseDate)
        assertNull(dto.copy(runtime = 0).toDomain().runtimeMinutes)
    }
}
