package kurmakaeva.anastasia.moviesearch.data.remote

import kurmakaeva.anastasia.moviesearch.data.BuildConfig

/** Builds full image URLs from the relative paths TMDB returns (e.g. `/abc.jpg`). */
object TmdbImageUrl {
    enum class Size(val value: String) {
        W342("w342"),
        W780("w780"),
    }

    fun build(path: String?, size: Size = Size.W780): String? =
        path?.takeIf { it.isNotBlank() }?.let { "${BuildConfig.TMDB_IMAGE_BASE_URL}${size.value}$it" }
}
