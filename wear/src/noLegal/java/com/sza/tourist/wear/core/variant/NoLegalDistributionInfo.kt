package com.sza.tourist.wear.core.variant

import javax.inject.Inject

/** The `noLegal` sideload edition's answer to the variant seam. */
class NoLegalDistributionInfo @Inject constructor() : DistributionInfo {
    override val distribution: Distribution = Distribution.NO_LEGAL
}
