package kurmakaeva.anastasia.moviesearch.presentation.error

import androidx.annotation.StringRes
import kurmakaeva.anastasia.moviesearch.domain.error.DataError
import kurmakaeva.anastasia.moviesearch.presentation.R

/** What went wrong, as far as the UI is concerned: decides the message and whether Retry is offered. */
enum class ErrorReason(@StringRes val message: Int, val canRetry: Boolean) {
    Network(R.string.error_network, canRetry = true),
    InvalidApiKey(R.string.error_invalid_api_key, canRetry = false),
    Server(R.string.error_server, canRetry = true),
    Unknown(R.string.error_unknown, canRetry = true),
}

internal fun Throwable.toErrorReason(): ErrorReason = when (this) {
    is DataError.Network -> ErrorReason.Network
    is DataError.InvalidApiKey -> ErrorReason.InvalidApiKey
    is DataError.Server -> ErrorReason.Server
    else -> ErrorReason.Unknown
}
