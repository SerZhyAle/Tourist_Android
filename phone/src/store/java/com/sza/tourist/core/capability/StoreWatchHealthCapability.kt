package com.sza.tourist.core.capability

import javax.inject.Inject

/**
 * The `store` edition's answer: the watch tiles are not offered. The store watch has no Tourist
 * dashboard (owner decision 4.3, S0008) and publishes no health report, and the Play build is declared as
 * having no health features, so the two tiles explain themselves in the grid instead of showing a value.
 */
class StoreWatchHealthCapability @Inject constructor() : WatchHealthCapability {
    override val isOffered: Boolean = false
}
