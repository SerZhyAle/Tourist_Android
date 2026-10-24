package com.sza.tourist.core.capability

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The product's own capability registry, shaped after FastMediaSorter's class of the same name but
 * carrying only what this kit compiles: no OCR, no translation, no cloud - and, until S0008 wires the
 * Data Layer, no watch health either. The store/noLegal composition of watch health is decision 4.3's
 * record in S0008.
 */
@Singleton
class CapabilityAvailability @Inject constructor() {

    /** Whether the Tourist dashboard may show the watch's heart rate and body temperature. */
    fun isWatchHealthAvailable(): Boolean = WATCH_HEALTH_COMPILED_IN

    private companion object {
        const val WATCH_HEALTH_COMPILED_IN = false
    }
}
