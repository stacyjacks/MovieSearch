package kurmakaeva.anastasia.moviesearch.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kurmakaeva.anastasia.moviesearch.presentation.R
import kurmakaeva.anastasia.moviesearch.presentation.error.ErrorReason
import kurmakaeva.anastasia.moviesearch.presentation.theme.Size

/** Full-screen error message; offers Retry only when retrying can help. */
@Composable
internal fun ErrorState(reason: ErrorReason, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Size.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(reason.message),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (reason.canRetry) {
            Spacer(Modifier.height(Size.medium))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}
