package kurmakaeva.anastasia.moviesearch.presentation.detail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class MovieDetailDestination(val movieId: Int)

fun NavController.navigateToMovieDetail(movieId: Int) = navigate(MovieDetailDestination(movieId))

/** Registers the movie detail screen; the movie id is read from the route here, so callers never handle it. */
fun NavGraphBuilder.movieDetailScreen(onBack: () -> Unit) {
    composable<MovieDetailDestination> { backStackEntry ->
        MovieDetailRoute(
            movieId = backStackEntry.toRoute<MovieDetailDestination>().movieId,
            onBack = onBack,
        )
    }
}
