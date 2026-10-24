package com.sza.tourist.core.di

import com.sza.tourist.data.weather.OpenMeteoWeatherProvider
import com.sza.tourist.data.weather.WeatherRepositoryImpl
import com.sza.tourist.domain.repository.WeatherRepository
import com.sza.tourist.domain.weather.WeatherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Bindings for the Open-Meteo weather feed the dashboard's weather tiles read. The client is the
 * product's one shared OkHttpClient, at FastMediaSorter's timeouts, so a later network consumer
 * reuses the same seam.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WeatherModule {

    @Binds
    @Singleton
    abstract fun bindWeatherProvider(
        impl: OpenMeteoWeatherProvider,
    ): WeatherProvider

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        impl: WeatherRepositoryImpl,
    ): WeatherRepository

    companion object {

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        private const val NETWORK_TIMEOUT_SECONDS = 20L
    }
}
