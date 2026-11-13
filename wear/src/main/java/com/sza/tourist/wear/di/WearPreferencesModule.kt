package com.sza.tourist.wear.di

import com.sza.tourist.wear.data.preferences.WearPreferencesRepositoryImpl
import com.sza.tourist.wear.domain.repository.WearPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * S2655: the settings binding, in its own module.
 *
 * The themed sections and the composite that delegates to them all carry `@Inject` constructors, so Hilt
 * needs only the interface-to-implementation binding and `@Binds` states it without a body.
 *
 * Trimmed: the clock-style, face-slot and phone-battery stores of FastMediaSorter's module belong to
 * surfaces this product does not carry.
 */
@Module
@InstallIn(SingletonComponent::class)
interface WearPreferencesModule {

    @Binds
    @Singleton
    fun bindWearPreferencesRepository(impl: WearPreferencesRepositoryImpl): WearPreferencesRepository
}
