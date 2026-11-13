package com.sza.tourist.wear.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The one Gson of the watch, for the Data Layer envelopes and payloads. FastMediaSorter provides it from
 * its `WearAppModule`, which this product does not carry.
 */
@Module
@InstallIn(SingletonComponent::class)
object WearJsonModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}
