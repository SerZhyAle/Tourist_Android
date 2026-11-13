package com.sza.tourist.core.di

import com.sza.tourist.core.util.ElapsedClock
import com.sza.tourist.core.util.SystemElapsedClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the monotonic time source measured intervals read. */
@Module
@InstallIn(SingletonComponent::class)
object ElapsedClockModule {

    @Provides
    @Singleton
    fun provideElapsedClock(): ElapsedClock = SystemElapsedClock
}
