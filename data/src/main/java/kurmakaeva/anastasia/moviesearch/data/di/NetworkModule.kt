package kurmakaeva.anastasia.moviesearch.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kurmakaeva.anastasia.moviesearch.data.BuildConfig
import kurmakaeva.anastasia.moviesearch.data.remote.NetworkFactory
import kurmakaeva.anastasia.moviesearch.data.remote.TmdbApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = NetworkFactory.createJson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = NetworkFactory.createOkHttpClient(
        apiKey = BuildConfig.TMDB_API_KEY,
        enableLogging = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        NetworkFactory.createRetrofit(okHttpClient, json, BuildConfig.TMDB_BASE_URL)

    @Provides
    @Singleton
    fun provideTmdbApi(retrofit: Retrofit): TmdbApi = retrofit.create(TmdbApi::class.java)
}
