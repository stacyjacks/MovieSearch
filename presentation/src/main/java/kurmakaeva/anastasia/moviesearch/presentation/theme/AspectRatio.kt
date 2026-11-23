package kurmakaeva.anastasia.moviesearch.presentation.theme

/** Width-to-height ratios of images, matching the formats TMDB serves them in. */
object AspectRatio {
    /** Movie posters are portrait, 2:3. */
    const val poster: Float = 2f / 3f

    /** Backdrops are widescreen stills, 16:9. */
    const val backdrop: Float = 16f / 9f
}
