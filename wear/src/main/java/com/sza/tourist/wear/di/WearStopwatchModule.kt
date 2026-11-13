package com.sza.tourist.wear.di

import com.sza.tourist.wear.data.repository.WearStopwatchSessionRepositoryImpl
import com.sza.tourist.wear.domain.repository.WearStopwatchSessionRepository
import com.sza.tourist.wear.domain.stopwatch.WearStopwatchOngoingIndicator
import com.sza.tourist.wear.ui.apps.stopwatch.WearStopwatchOngoingNotificationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * S3555: the stopwatch's process-wide bindings; the singleton scope is what lets the measurement outlive
 * every screen. FastMediaSorter keeps it out of its `WearAppModule`, which this product does not carry.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WearStopwatchModule {

    @Binds
    @Singleton
    abstract fun bindStopwatchSession(impl: WearStopwatchSessionRepositoryImpl): WearStopwatchSessionRepository

    @Binds
    @Singleton
    abstract fun bindStopwatchOngoingIndicator(
        impl: WearStopwatchOngoingNotificationManager
    ): WearStopwatchOngoingIndicator
}
