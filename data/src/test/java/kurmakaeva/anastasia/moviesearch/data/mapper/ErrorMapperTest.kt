package kurmakaeva.anastasia.moviesearch.data.mapper

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kurmakaeva.anastasia.moviesearch.data.remote.NetworkFactory
import kurmakaeva.anastasia.moviesearch.data.remote.TmdbApi
import kurmakaeva.anastasia.moviesearch.domain.error.DataError
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class ErrorMapperTest {

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun ioFailures_areNetworkErrors() {
        assertTrue(IOException("offline").toDataError() is DataError.Network)
        assertTrue(SocketTimeoutException("timeout").toDataError() is DataError.Network)
    }

    @Test
    fun http401_isInvalidApiKey() {
        assertTrue(httpError(401).toDataError() is DataError.InvalidApiKey)
    }

    @Test
    fun otherHttpErrors_areServerErrors_withTheirStatusCode() {
        val error = httpError(503).toDataError()

        assertTrue(error is DataError.Server)
        assertEquals(503, (error as DataError.Server).code)
    }

    @Test
    fun anythingElse_isUnknown_andDataErrorsPassThrough() {
        assertTrue(SerializationException("bad json").toDataError() is DataError.Unknown)

        val original = DataError.InvalidApiKey()
        assertSame(original, original.toDataError())
    }

    /** End to end through Retrofit: TMDB's real answer to a missing/invalid key is HTTP 401. */
    @Test
    fun tmdb401Response_becomesInvalidApiKey() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse.Builder()
                    .code(401)
                    .body("""{"status_code":7,"status_message":"Invalid API key"}""")
                    .build()
            )
            val client = NetworkFactory.createOkHttpClient(apiKey = "", enableLogging = false)
            val api = NetworkFactory.createRetrofit(client, NetworkFactory.createJson(), server.url("/3/").toString())
                .create(TmdbApi::class.java)

            val error = runCatching { api.searchMovies("matrix") }.exceptionOrNull()!!.toDataError()

            assertTrue(error is DataError.InvalidApiKey)
        }
    }
}
