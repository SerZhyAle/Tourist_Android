package com.sza.tourist.core.di

import com.sza.tourist.data.sensors.DeviceSensorAvailabilityRepositoryImpl
import com.sza.tourist.domain.repository.SensorAvailabilityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings for the sensor readings layer.
 *
 * Only the availability repository needs a binding - the reading sources are `@Singleton` classes with
 * an `@Inject constructor`, which Hilt resolves without a `@Provides`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModule {

    @Binds
    @Singleton
    abstract fun bindSensorAvailabilityRepository(
        impl: DeviceSensorAvailabilityRepositoryImpl,
    ): SensorAvailabilityRepository
}
