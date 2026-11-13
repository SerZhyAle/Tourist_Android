package com.sza.tourist.domain.model.tourist

import com.sza.tourist.domain.model.sensors.SensorCapability

/**
 * Available telemetry and navigation tiles on the Tourist dashboard.
 *
 * Declaration order is the grid order - the original ten, then the pressure, trip, navigation,
 * environment and weather groups. Names are persisted as the focused tile and kept by R8, so a value
 * is never renamed. [requiredCapability] names the hardware without which the tile is not offered.
 *
 * A [fromWatch] tile reads the paired watch rather than this phone; it is offered by the build's
 * watch-health capability, never by local hardware, and appended last so no persisted name moves.
 *
 * GYROSCOPE is appended last for the same reason; its sensors are registered only while it is focused.
 */
enum class TouristTileType(
    val requiredCapability: SensorCapability? = null,
    val fromWatch: Boolean = false,
) {
    SPEED,
    ALTITUDE,
    COMPASS,
    COORDINATES,
    SATELLITES,
    STEPS(SensorCapability.STEP_COUNTER),
    TRIP_DISTANCE,
    SUN_TIME,
    WEATHER,
    DEW_POINT,
    PRESSURE(SensorCapability.BAROMETER),
    BAROMETRIC_ALTITUDE(SensorCapability.BAROMETER),
    ELEVATION_GAIN_LOSS,
    SLOPE,
    ELAPSED_TIME,
    MOVING_TIME,
    AVERAGE_PACE,
    CADENCE(SensorCapability.STEP_COUNTER),
    GPS_ACCURACY,
    COURSE,
    LEVEL(SensorCapability.LEVEL),
    LIGHT(SensorCapability.LIGHT),
    MAGNETIC_FIELD(SensorCapability.MAGNETOMETER),
    WIND,
    FEELS_LIKE,
    HUMIDITY,
    UV_INDEX,
    PRECIPITATION_PROBABILITY,
    CLOUD_COVER,
    VISIBILITY,
    WATCH_HEART_RATE(fromWatch = true),
    WATCH_BODY_TEMPERATURE(fromWatch = true),
    STOPWATCH,
    COUNTER,
    GYROSCOPE(SensorCapability.ATTITUDE),
}

/**
 * Whether the dashboard offers [this] tile at all. The step counter keeps reading `stepsAvailable`,
 * which also folds in the activity-recognition declaration of the build.
 */
fun TouristTileType.isOffered(state: TouristDashboardState): Boolean = when {
    fromWatch -> state.watch.offered
    requiredCapability == null -> true
    requiredCapability == SensorCapability.STEP_COUNTER -> state.stepsAvailable
    else -> requiredCapability in state.availableCapabilities
}
