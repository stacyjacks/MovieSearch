package kurmakaeva.anastasia.moviesearch.domain.error

/**
 * Why loading data failed. [kurmakaeva.anastasia.moviesearch.domain.repository.MovieRepository]
 * returns one of these as the exception of a failed [Result], so callers can react to the kind
 * of failure without knowing about HTTP or networking libraries.
 */
sealed class DataError(message: String, cause: Throwable?) : Exception(message, cause) {

    /** No connection, timeout, DNS failure: retrying later may help. */
    class Network(cause: Throwable) : DataError("Network error", cause)

    /** The API rejected the request's API key (missing or invalid): retrying won't help. */
    class InvalidApiKey(cause: Throwable? = null) : DataError("Missing or invalid API key", cause)

    /** The server answered with an error status (e.g. 5xx). */
    class Server(val code: Int, cause: Throwable? = null) : DataError("Server error $code", cause)

    /** Anything else, e.g. a response that couldn't be parsed. */
    class Unknown(cause: Throwable) : DataError("Unexpected error", cause)
}
