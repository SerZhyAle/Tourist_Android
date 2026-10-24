package com.sza.tourist.core.di

import com.sza.tourist.data.tourist.TouristCounterRepositoryImpl
import com.sza.tourist.data.tourist.TouristTripRepositoryImpl
import com.sza.tourist.data.tourist.WatchHealthRepositoryImpl
import com.sza.tourist.domain.repository.TouristCounterRepository
import com.sza.tourist.domain.repository.TouristTripRepository
import com.sza.tourist.domain.repository.WatchHealthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings for the Tourist dashboard's persisted surfaces: the trip, the tally counter and the watch
 * health holder (a stand-in until S0008 ports the Data Layer bridge that fills it).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TouristModule {

    @Binds
    @Singleton
    abstract fun bindTouristTripRepository(
        impl: TouristTripRepositoryImpl,
    ): TouristTripRepository

    @Binds
    @Singleton
    abstract fun bindTouristCounterRepository(
        impl: TouristCounterRepositoryImpl,
    ): TouristCounterRepository

    @Binds
    @Singleton
    abstract fun bindWatchHealthRepository(
        impl: WatchHealthRepositoryImpl,
    ): WatchHealthRepository
}
