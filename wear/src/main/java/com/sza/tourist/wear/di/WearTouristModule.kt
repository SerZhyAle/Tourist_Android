package com.sza.tourist.wear.di

import com.sza.tourist.wear.data.tourist.AndroidWearInteractiveStateRepository
import com.sza.tourist.wear.data.tourist.AndroidWearTouristRepository
import com.sza.tourist.wear.data.tourist.DataLayerWearTouristWeatherRepository
import com.sza.tourist.wear.data.tourist.PreferencesWearTouristCounterRepository
import com.sza.tourist.wear.domain.repository.WearInteractiveStateRepository
import com.sza.tourist.wear.domain.repository.WearTouristCounterRepository
import com.sza.tourist.wear.domain.repository.WearTouristRepository
import com.sza.tourist.wear.domain.repository.WearTouristWeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * S3007: Hilt binding module for [WearTouristRepository]; S4152 adds [WearTouristWeatherRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WearTouristModule {

    @Binds
    @Singleton
    abstract fun bindWearTouristRepository(
        impl: AndroidWearTouristRepository,
    ): WearTouristRepository

    @Binds
    @Singleton
    abstract fun bindWearTouristWeatherRepository(
        impl: DataLayerWearTouristWeatherRepository,
    ): WearTouristWeatherRepository

    @Binds
    @Singleton
    abstract fun bindWearTouristCounterRepository(
        impl: PreferencesWearTouristCounterRepository,
    ): WearTouristCounterRepository

    @Binds
    @Singleton
    abstract fun bindWearInteractiveStateRepository(
        impl: AndroidWearInteractiveStateRepository,
    ): WearInteractiveStateRepository
}
