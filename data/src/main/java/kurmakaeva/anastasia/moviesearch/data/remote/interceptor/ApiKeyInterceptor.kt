package kurmakaeva.anastasia.moviesearch.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/** Appends the TMDB `api_key` query parameter to every outgoing request. */
internal class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.newBuilder()
            .addQueryParameter(API_KEY_PARAM, apiKey)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }

    private companion object {
        const val API_KEY_PARAM = "api_key"
    }
}
