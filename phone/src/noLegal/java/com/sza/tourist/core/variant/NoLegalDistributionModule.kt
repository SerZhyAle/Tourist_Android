package com.sza.tourist.core.variant

import com.sza.tourist.core.capability.NoLegalWatchHealthCapability
import com.sza.tourist.core.capability.WatchHealthCapability
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The one DI module of the `noLegal` variant. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NoLegalDistributionModule {

    @Binds
    abstract fun bindDistributionInfo(impl: NoLegalDistributionInfo): DistributionInfo

    @Binds
    abstract fun bindWatchHealthCapability(impl: NoLegalWatchHealthCapability): WatchHealthCapability
}
