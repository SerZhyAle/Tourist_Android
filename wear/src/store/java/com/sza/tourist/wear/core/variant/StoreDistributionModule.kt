package com.sza.tourist.wear.core.variant

import com.sza.tourist.wear.capability.StoreWearRestrictedCapabilities
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The one DI module of the `store` variant. */
@Module
@InstallIn(SingletonComponent::class)
abstract class StoreDistributionModule {

    @Binds
    abstract fun bindDistributionInfo(impl: StoreDistributionInfo): DistributionInfo

    @Binds
    abstract fun bindRestrictedCapabilities(impl: StoreWearRestrictedCapabilities): WearRestrictedCapabilities
}
