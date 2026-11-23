package kurmakaeva.anastasia.moviesearch.data.mapper

import kurmakaeva.anastasia.moviesearch.domain.error.DataError
import retrofit2.HttpException
import java.io.IOException

private const val HTTP_UNAUTHORIZED = 401

/** Translates networking/parsing exceptions into the domain's [DataError]. */
internal fun Throwable.toDataError(): DataError = when (this) {
    is DataError -> this
    // TMDB answers 401 both for a missing and for an invalid API key.
    is HttpException -> when (code()) {
        HTTP_UNAUTHORIZED -> DataError.InvalidApiKey(this)
        else -> DataError.Server(code(), this)
    }
    is IOException -> DataError.Network(this) // no connection, timeout, DNS failure, connection reset...
    else -> DataError.Unknown(this) // e.g. kotlinx.serialization failing to parse the response
}
