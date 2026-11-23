package kurmakaeva.anastasia.moviesearch.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kurmakaeva.anastasia.moviesearch.domain.repository.MovieRepository
import kurmakaeva.anastasia.moviesearch.data.repository.MovieRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository
}
