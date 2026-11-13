package com.sza.tourist.wear.core.variant

import com.sza.tourist.wear.capability.NoLegalWearRestrictedCapabilities
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
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
    abstract fun bindRestrictedCapabilities(impl: NoLegalWearRestrictedCapabilities): WearRestrictedCapabilities
}
