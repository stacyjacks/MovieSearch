package kurmakaeva.anastasia.moviesearch.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Dispatcher for blocking/IO-bound work. Injected (rather than hardcoded) so tests can substitute a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class IoDispatcher
/**
 * Scope that lives as long as the app process. Used for work shared between callers (e.g. coalesced
 * requests), which must not be tied to - and cancelled with - any single caller's coroutine.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
internal object CoroutinesModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** SupervisorJob: one failed child must not cancel the scope (and with it every other request). */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@IoDispatcher ioDispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)
}
