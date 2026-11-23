package kurmakaeva.anastasia.moviesearch.presentation.component

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kurmakaeva.anastasia.moviesearch.presentation.theme.AspectRatio

/** TMDB poster in its native 2:3 ratio; shows a neutral block while loading or when there's no poster. */
@Composable
internal fun MoviePoster(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(AspectRatio.poster)
            .clip(MaterialTheme.shapes.small),
    )
}
