package kurmakaeva.anastasia.moviesearch.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kurmakaeva.anastasia.moviesearch.domain.model.Movie
import kurmakaeva.anastasia.moviesearch.presentation.R
import kurmakaeva.anastasia.moviesearch.presentation.SUMMARY_MAX_LINES
import kurmakaeva.anastasia.moviesearch.presentation.component.MoviePoster
import kurmakaeva.anastasia.moviesearch.presentation.theme.ComponentSize
import kurmakaeva.anastasia.moviesearch.presentation.theme.MovieSearchTheme
import kurmakaeva.anastasia.moviesearch.presentation.theme.Size

/** Stateful entry point: gets its ViewModel from Hilt. */
@Composable
fun MovieSearchRoute(
    onMovieClick: (movieId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovieSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MovieSearchScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onRetry = viewModel::onRetry,
        onMovieClick = onMovieClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSearchScreen(
    uiState: MovieSearchUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
    onMovieClick: (movieId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.search_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.search_clear),
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Size.large, vertical = Size.small),
            )
            when (val results = uiState.results) {
                SearchResults.Idle -> {
                    CenteredMessage(stringResource(R.string.search_idle))
                }
                SearchResults.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                SearchResults.Empty -> {
                    CenteredMessage(
                        stringResource(R.string.search_empty,
                            uiState.query.trim())
                    )
                }
                SearchResults.Error -> {
                    CenteredMessage(stringResource(R.string.search_error)) {
                        Spacer(Modifier.height(Size.medium))
                        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
                is SearchResults.Success -> {
                    MovieList(results.movies, onMovieClick)
                }
            }
        }
    }
}

@Composable
private fun MovieList(movies: List<Movie>, onMovieClick: (movieId: Int) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(Size.large),
        verticalArrangement = Arrangement.spacedBy(Size.medium),
    ) {
        items(
            items = movies,
            key = { it.id }
        ) { movie ->
            MovieItem(movie, onClick = { onMovieClick(movie.id) })
        }
    }
}

@Composable
private fun MovieItem(movie: Movie, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Size.medium)) {
            MoviePoster(
                url = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.width(ComponentSize.listPosterWidth),
            )
            Spacer(Modifier.width(Size.medium))
            Column(Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(movie.title, movie.releaseYear?.let {
                        "($it)"
                    }).joinToString(" "),
                    style = MaterialTheme.typography.titleMedium,
                )
                movie.rating?.takeIf { it > 0 }?.let {
                    Text(
                        text = stringResource(R.string.movie_rating, it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                movie.overview?.let {
                    Spacer(Modifier.height(Size.extraSmall))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = SUMMARY_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String, extra: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Size.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        extra()
    }
}

@Preview(showBackground = true)
@Composable
private fun MovieSearchScreenPreview() {
    MovieSearchTheme {
        MovieSearchScreen(
            uiState = MovieSearchUiState(
                query = "matrix",
                results = SearchResults.Success(
                    listOf(
                        Movie(
                            id = 603,
                            title = "The Matrix",
                            overview = "A hacker learns the truth about reality.",
                            posterUrl = null,
                            releaseYear = 1999,
                            rating = 8.2
                        ),
                        Movie(
                            id = 604,
                            title = "The Matrix Reloaded",
                            overview = null,
                            posterUrl = null,
                            releaseYear = 2003,
                            rating = 7.1
                        ),
                    )
                ),
            ),
            onQueryChange = {},
            onClearQuery = {},
            onRetry = {},
            onMovieClick = {},
        )
    }
}
