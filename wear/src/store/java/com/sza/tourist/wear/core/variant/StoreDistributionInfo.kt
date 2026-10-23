package com.sza.tourist.wear.core.variant

import javax.inject.Inject

/** The `store` edition's answer to the variant seam. */
class StoreDistributionInfo @Inject constructor() : DistributionInfo {
    override val distribution: Distribution = Distribution.STORE
}
