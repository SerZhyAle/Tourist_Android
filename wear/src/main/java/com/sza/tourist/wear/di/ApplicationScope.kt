package com.sza.tourist.wear.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * S2915: marks the process-lifetime application scope. Mirrors the phone's
 * `com.sza.tourist.core.di.ApplicationScope` - the wear module is standalone and cannot see the phone's
 * qualifier, so it declares its own of the same name.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Provides the [ApplicationScope].
 */
@Module
@InstallIn(SingletonComponent::class)
object WearScopeModule {

    // S2915: the process-lifetime scope Data Layer work launches on. The platform destroys a
    // WearableListenerService shortly after the callback returns, so a scope owned by the service
    // cancels jobs still mid-flight - and a cancelled job reports nothing. This one is never
    // cancelled by anything shorter-lived than the process.
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
