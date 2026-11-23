package kurmakaeva.anastasia.moviesearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import kurmakaeva.anastasia.moviesearch.presentation.search.MovieSearchRoute
import kurmakaeva.anastasia.moviesearch.presentation.theme.MovieSearchTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieSearchTheme {
                MovieSearchRoute(onMovieClick = {})
            }
        }
    }
}
