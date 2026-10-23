package com.sza.tourist.core.variant

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
}
