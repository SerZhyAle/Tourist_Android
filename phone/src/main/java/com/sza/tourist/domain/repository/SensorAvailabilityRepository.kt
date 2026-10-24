package com.sza.tourist.domain.repository

import com.sza.tourist.domain.model.sensors.SensorCapability

/**
 * Does this device carry the hardware a dashboard tile needs.
 *
 * One answer shared by every tile, so "no sensor - no tile" is a rule of this layer rather than a
 * copy inside each tile.
 *
 * Hardware only, never permissions. A refused permission leaves the tile in place in an honest empty
 * state; an absent sensor removes it from the dashboard entirely. Answering both questions here would
 * collapse those two outcomes into one.
 */
interface SensorAvailabilityRepository {

    fun isAvailable(capability: SensorCapability): Boolean
}
