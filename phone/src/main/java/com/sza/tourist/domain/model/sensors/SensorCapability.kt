package com.sza.tourist.domain.model.sensors

/**
 * The hardware question the Tourist dashboard asks before it offers a tile, so a tile whose sensor
 * is absent is not shown rather than shown empty.
 *
 * Hardware only. Whether the matching runtime permission was granted is a deliberately separate
 * axis: folding the two together would drop a tile from the dashboard when the user refuses a
 * permission, instead of leaving the tile in place in an empty state.
 */
enum class SensorCapability {
    COMPASS,
    LOCATION,
    STEP_COUNTER,
    BAROMETER,
    LIGHT,
    MAGNETOMETER,
    LEVEL,

    /** Device attitude from the game or geomagnetic rotation vector. */
    ATTITUDE,
}
