package kurmakaeva.anastasia.moviesearch.data.remote

import kotlinx.serialization.json.Json
import kurmakaeva.anastasia.moviesearch.data.remote.interceptor.ApiKeyInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Plain builders for the networking stack. Hilt's [di.NetworkModule] calls these with
 * the real config; tests call them directly (e.g. pointing at a MockWebServer).
 */
internal object NetworkFactory {
    private const val TIMEOUT_SECONDS = 30L
    private const val CONTENT_TYPE = "application/json"

    fun createJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun createOkHttpClient(apiKey: String, enableLogging: Boolean): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .apply {
                if (enableLogging) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                            redactQueryParams("api_key")
                        }
                    )
                }
            }
            .build()

    fun createRetrofit(okHttpClient: OkHttpClient, json: Json, baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(CONTENT_TYPE.toMediaType()))
            .build()
}
