package com.sza.tourist.core.di

import com.sza.tourist.data.settings.SettingsRepositoryImpl
import com.sza.tourist.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the persisted settings store the S0003 core seam has been waiting for: `unit_system` and
 * `prevent_sleep` in the product's own preferences file.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl,
    ): SettingsRepository
}
