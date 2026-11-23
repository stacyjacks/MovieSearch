package kurmakaeva.anastasia.moviesearch.presentation.search

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object MovieSearchDestination

/** Registers the search screen. Where a selected movie leads is decided by the caller (the app's nav graph). */
fun NavGraphBuilder.movieSearchScreen(onMovieClick: (movieId: Int) -> Unit) {
    composable<MovieSearchDestination> {
        MovieSearchRoute(onMovieClick = onMovieClick)
    }
}
