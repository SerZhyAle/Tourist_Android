package com.sza.tourist.core.di

import com.sza.tourist.data.repository.MediaStoreMusicTrackRepository
import com.sza.tourist.domain.repository.MusicTrackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the device media store as the audio chooser source. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MusicTrackModule {

    @Binds
    @Singleton
    abstract fun bindMusicTrackRepository(
        impl: MediaStoreMusicTrackRepository,
    ): MusicTrackRepository
}
