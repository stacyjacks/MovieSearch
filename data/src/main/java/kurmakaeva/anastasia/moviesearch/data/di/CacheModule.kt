package kurmakaeva.anastasia.moviesearch.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kurmakaeva.anastasia.moviesearch.domain.model.MoviePage
import kurmakaeva.anastasia.moviesearch.data.cache.InMemoryCache
import kurmakaeva.anastasia.moviesearch.data.cache.SearchCacheKey
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Module
@InstallIn(SingletonComponent::class)
internal object CacheModule {
    /** Requirement: identical searches within 5 minutes are served from memory. */
    private val SEARCH_CACHE_TTL = 5.minutes
    private const val SEARCH_CACHE_MAX_ENTRIES = 50

    @Provides
    @Singleton
    fun provideSearchCache(): InMemoryCache<SearchCacheKey, MoviePage> =
        InMemoryCache(ttl = SEARCH_CACHE_TTL, maxEntries = SEARCH_CACHE_MAX_ENTRIES)
}
