package com.sza.tourist.core.capability

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The product's own capability registry, shaped after FastMediaSorter's class of the same name but
 * carrying only what this kit compiles: no OCR, no translation, no cloud.
 *
 * What a distribution withholds is not answered here: a capability that differs between `store` and
 * `noLegal` is an interface in this package with one real implementation per variant source set (owner
 * decision 4.3, S0008) - see [WatchHealthCapability].
 */
@Singleton
class CapabilityAvailability @Inject constructor() {

    /** Whether this build supports audio playback (both `store` and `noLegal` editions do). */
    val supportsAudio: Boolean = true
}
