package kurmakaeva.anastasia.moviesearch.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import kurmakaeva.anastasia.moviesearch.presentation.detail.movieDetailScreen
import kurmakaeva.anastasia.moviesearch.presentation.detail.navigateToMovieDetail
import kurmakaeva.anastasia.moviesearch.presentation.search.MovieSearchDestination
import kurmakaeva.anastasia.moviesearch.presentation.search.movieSearchScreen

/** The app's navigation graph: which screens exist and how they connect. Screens themselves don't know about each other. */
@Composable
fun MovieSearchNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = MovieSearchDestination,
        modifier = modifier,
    ) {
        movieSearchScreen(onMovieClick = navController::navigateToMovieDetail)
        movieDetailScreen(onBack = navController::navigateUp)
    }
}
