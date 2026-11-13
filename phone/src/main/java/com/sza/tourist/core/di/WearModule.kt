package com.sza.tourist.core.di

import com.google.gson.Gson
import com.sza.tourist.data.wear.WearableDataLayerRepositoryImpl
import com.sza.tourist.domain.repository.WearableDataLayerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt binding for the phone side of the Wear Data Layer bridge (S0006, S0008).
 *
 * S0008 adds [Gson], which the envelope payloads of the watch pair (Tourist health, weather and the
 * unit-system push) are encoded with.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WearModule {

    @Binds
    @Singleton
    abstract fun bindWearableDataLayerRepository(
        impl: WearableDataLayerRepositoryImpl,
    ): WearableDataLayerRepository

    companion object {

        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()
    }
}
