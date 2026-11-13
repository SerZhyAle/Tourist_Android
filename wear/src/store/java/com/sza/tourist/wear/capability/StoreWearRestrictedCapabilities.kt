package com.sza.tourist.wear.capability

import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import javax.inject.Inject

/**
 * S2486: the answers for the edition Play distributes.
 *
 * `store` is the build that goes through the store, so every capability the review refuses is withheld
 * here - in the release AND in the debug build, because the predicate is about the distribution channel and
 * a debug build of this variant is the same product one step earlier. Keeping a debug carve-out would mean
 * the shipped answer is never the one exercised during development.
 *
 * Owner decision 4.3 of 2026-10-25 (S0008): the Tourist dashboard is withheld, SOS and the water
 * flashlight are offered.
 */
class StoreWearRestrictedCapabilities @Inject constructor() : WearRestrictedCapabilities {

    /**
     * S2812: the store build leaves the system shade alone. Lock task mode is screen pinning for an
     * ordinary app, and a hike kit matches none of the uses Play admits for it.
     */
    override val locksSystemShade: Boolean = false

    /**
     * S2995: the dashboard reads heart rate, steps and location, and Play counts those as health and
     * sensitive data. Sideload-only, which also keeps `BODY_SENSORS`, `health.READ_HEART_RATE`,
     * `ACTIVITY_RECOGNITION` and the location pair out of this edition's manifest.
     */
    override val offersHealthFeatures: Boolean = false

    /**
     * Owner decision 4.3: offered. The touch lock is the product's signature; the Wear OS review items
     * WO-V3 and WO-V13 are a known risk the owner accepted. See [WearRestrictedCapabilities].
     */
    override val offersScreenTakeoverPrograms: Boolean = true
}
