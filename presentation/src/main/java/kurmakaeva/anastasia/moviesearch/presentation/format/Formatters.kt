package kurmakaeva.anastasia.moviesearch.presentation.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kurmakaeva.anastasia.moviesearch.presentation.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Formats a date in the user's locale, e.g. "Mar 31, 1999" or "31 Mar 1999". */
@Composable
internal fun formatReleaseDate(date: LocalDate): String {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    return date.format(formatter)
}

/** Formats a runtime in minutes as "2 h 16 min", or just "45 min" when under an hour. */
@Composable
internal fun formatRuntime(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) {
        stringResource(R.string.runtime_hours_minutes, hours, rest)
    } else {
        stringResource(R.string.runtime_minutes, rest)
    }
}
