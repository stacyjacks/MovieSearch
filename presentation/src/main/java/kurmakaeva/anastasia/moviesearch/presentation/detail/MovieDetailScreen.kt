package kurmakaeva.anastasia.moviesearch.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kurmakaeva.anastasia.moviesearch.domain.model.MovieDetail
import kurmakaeva.anastasia.moviesearch.presentation.R
import kurmakaeva.anastasia.moviesearch.presentation.TITLE_MAX_LINES
import kurmakaeva.anastasia.moviesearch.presentation.component.ErrorState
import kurmakaeva.anastasia.moviesearch.presentation.component.MoviePoster
import kurmakaeva.anastasia.moviesearch.presentation.format.formatReleaseDate
import kurmakaeva.anastasia.moviesearch.presentation.format.formatRuntime
import kurmakaeva.anastasia.moviesearch.presentation.theme.AspectRatio
import kurmakaeva.anastasia.moviesearch.presentation.theme.ComponentSize
import kurmakaeva.anastasia.moviesearch.presentation.theme.MovieSearchTheme
import kurmakaeva.anastasia.moviesearch.presentation.theme.Size
import java.time.LocalDate

/** Stateful entry point: gets its ViewModel from Hilt, keyed to [movieId]. */
@Composable
fun MovieDetailRoute(
    movieId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovieDetailViewModel =
        hiltViewModel<MovieDetailViewModel, MovieDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(movieId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MovieDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    uiState: MovieDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is MovieDetailUiState.Success) {
                        Text(
                            text = uiState.detail.title,
                            maxLines = TITLE_MAX_LINES,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (uiState) {
            MovieDetailUiState.Loading -> Box(contentModifier, Alignment.Center) {
                CircularProgressIndicator()
            }
            is MovieDetailUiState.Error -> ErrorState(uiState.reason, onRetry, contentModifier)
            is MovieDetailUiState.Success -> MovieDetailContent(uiState.detail, contentModifier)
        }
    }
}

@Composable
private fun MovieDetailContent(detail: MovieDetail, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        detail.backdropUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(AspectRatio.backdrop),
            )
        }

        Row(Modifier.padding(Size.large)) {
            MoviePoster(
                url = detail.posterUrl,
                contentDescription = detail.title,
                modifier = Modifier.width(ComponentSize.detailPosterWidth),
            )
            Spacer(Modifier.width(Size.large))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Size.extraSmall),
            ) {
                Text(detail.title, style = MaterialTheme.typography.headlineSmall)
                detail.originalTitle
                    ?.takeIf { it.isNotBlank() && it != detail.title }
                    ?.let {
                        Text(
                            text = stringResource(R.string.detail_original_title, it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                detail.releaseDate?.let {
                    Text(text = formatReleaseDate(it), style = MaterialTheme.typography.bodyMedium)
                }
                detail.runtimeMinutes?.let {
                    Text(text = formatRuntime(it), style = MaterialTheme.typography.bodyMedium)
                }
                detail.rating?.takeIf { it > 0 }?.let {
                    Text(
                        text = stringResource(R.string.movie_rating, it),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (detail.genres.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(horizontal = Size.large),
                horizontalArrangement = Arrangement.spacedBy(Size.small),
                verticalArrangement = Arrangement.spacedBy(Size.small),
            ) {
                detail.genres.forEach { GenreTag(it) }
            }
        }

        detail.overview?.let {
            Section(title = stringResource(R.string.detail_overview)) {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (detail.productionCountries.isNotEmpty()) {
            Section(title = stringResource(R.string.detail_countries)) {
                Text(
                    text = detail.productionCountries.joinToString(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(Size.large))
    }
}

@Composable
private fun GenreTag(name: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Size.medium, vertical = Size.extraSmall),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(start = Size.large, end = Size.large, top = Size.large),
        verticalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun MovieDetailScreenPreview() {
    MovieSearchTheme {
        MovieDetailScreen(
            uiState = MovieDetailUiState.Success(
                MovieDetail(
                    id = 603,
                    title = "The Matrix",
                    originalTitle = "The Matrix",
                    overview = "Set in the 22nd century, The Matrix tells the story of a computer hacker who " +
                        "joins a group of underground insurgents fighting the vast and powerful computers.",
                    posterUrl = null,
                    backdropUrl = null,
                    releaseDate = LocalDate.of(1999, 3, 31),
                    runtimeMinutes = 136,
                    rating = 8.2,
                    genres = listOf("Action", "Science Fiction"),
                    productionCountries = listOf("United States of America"),
                )
            ),
            onBack = {},
            onRetry = {},
        )
    }
}
