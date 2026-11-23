package kurmakaeva.anastasia.moviesearch.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.Call
import okhttp3.EventListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TmdbApiTest {
    private val server = MockWebServer()
    private lateinit var api: TmdbApi

    @Before
    fun setUp() {
        server.start()
        val client = NetworkFactory.createOkHttpClient(apiKey = "test-key", enableLogging = false)
        val retrofit = NetworkFactory.createRetrofit(client, NetworkFactory.createJson(), server.url("/3/").toString())
        api = retrofit.create(TmdbApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun searchMovies_addsApiKeyAndParsesResponse() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body(
                    """
                    {"page":1,"total_pages":1,"total_results":1,"unknown_field":true,
                     "results":[{"id":603,"title":"The Matrix","poster_path":null,
                                 "release_date":"1999-03-31","original_language":"en"}]}
                    """.trimIndent()
                )
                .build()
        )

        val response = api.searchMovies("matrix")

        val request = server.takeRequest()
        assertEquals("/3/search/movie", request.url.encodedPath)
        assertEquals("matrix", request.url.queryParameter("query"))
        assertEquals("test-key", request.url.queryParameter("api_key"))
        assertEquals(1, response.results.size)
        assertEquals("The Matrix", response.results.first().title)
        assertEquals(null, response.results.first().posterPath)
    }

    @Test
    fun getMovieDetail_buildsPathAndParsesNestedObjects() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body(
                    """
                    {"id":603,"title":"The Matrix","runtime":136,
                     "genres":[{"id":28,"name":"Action"}],
                     "production_countries":[{"iso_3166_1":"US","name":"United States of America"}]}
                    """.trimIndent()
                )
                .build()
        )

        val detail = api.getMovieDetail(603)

        assertEquals("/3/movie/603", server.takeRequest().url.encodedPath)
        assertEquals(136, detail.runtime)
        assertEquals("Action", detail.genres.single().name)
        assertEquals("US", detail.productionCountries.single().isoCode)
    }

    @Test
    fun cancellingCoroutine_cancelsHttpCall() {
        val callCancelled = CountDownLatch(1)
        val client = NetworkFactory.createOkHttpClient(apiKey = "test-key", enableLogging = false)
            .newBuilder()
            .eventListener(object : EventListener() {
                override fun canceled(call: Call) = callCancelled.countDown()
            })
            .build()
        val api = NetworkFactory.createRetrofit(client, NetworkFactory.createJson(), server.url("/3/").toString())
            .create(TmdbApi::class.java)
        server.enqueue(MockResponse.Builder().body("{}").headersDelay(10, TimeUnit.SECONDS).build())

        runBlocking {
            val search = launch(Dispatchers.IO) { api.searchMovies("matrix") }
            server.takeRequest() // the request has reached the server and is waiting for a response
            search.cancelAndJoin()
        }

        assertTrue("OkHttp call was not cancelled", callCancelled.await(5, TimeUnit.SECONDS))
    }
}
