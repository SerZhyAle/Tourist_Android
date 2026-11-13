package com.sza.tourist.wear.capability

import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import javax.inject.Inject

/**
 * S2165: the answers for the sideload edition, which no store reviews - every capability is offered.
 *
 * It is a real class rather than a missing store override so that a capability added to the interface
 * later has to be answered here deliberately.
 */
class NoLegalWearRestrictedCapabilities @Inject constructor() : WearRestrictedCapabilities {

    /** S2812: the sideload build holds the shade shut under the water flashlight, as FastMediaSorter's does. */
    override val locksSystemShade: Boolean = true

    /** The Tourist dashboard with its heart-rate and step readings. */
    override val offersHealthFeatures: Boolean = true

    /** The water flashlight and the distress signal, as in the store edition (owner decision 4.3). */
    override val offersScreenTakeoverPrograms: Boolean = true
}
