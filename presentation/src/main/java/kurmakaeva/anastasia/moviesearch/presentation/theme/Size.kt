package kurmakaeva.anastasia.moviesearch.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Generic size scale (paddings, spacings) shared across Compose screens. Use these instead of hardcoded `dp` values. */
object Size {
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val extraLarge: Dp = 32.dp
}

/** Fixed dimensions of specific UI components that don't belong on the generic [Size] scale. */
object ComponentSize {
    val listPosterWidth: Dp = 80.dp
    val detailPosterWidth: Dp = 120.dp
}
