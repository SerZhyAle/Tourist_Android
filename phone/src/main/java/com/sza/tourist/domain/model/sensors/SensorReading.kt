package com.sza.tourist.domain.model.sensors

/**
 * The platform's own confidence in the compass.
 *
 * Mirrors the `SensorManager.SENSOR_STATUS_*` constants without importing them, so the domain layer
 * stays free of `android.hardware` and the compass tile can warn about calibration.
 */
enum class SensorAccuracy {
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH,
    ;

    companion object {

        /** An unknown or no-contact status maps to [UNRELIABLE] - never to a confident value. */
        fun fromPlatformStatus(status: Int): SensorAccuracy = when (status) {
            PLATFORM_LOW -> LOW
            PLATFORM_MEDIUM -> MEDIUM
            PLATFORM_HIGH -> HIGH
            else -> UNRELIABLE
        }

        private const val PLATFORM_LOW = 1
        private const val PLATFORM_MEDIUM = 2
        private const val PLATFORM_HIGH = 3
    }
}

/**
 * Heading and altitude for the compass tile, in display units.
 *
 * [altitudeMeters] is null when no position fix has supplied one, never 0.0: a tile that cannot
 * tell "no fix yet" from "sea level" draws sea level on a mountain.
 */
data class CompassReading(
    val azimuthDegrees: Float,
    val accuracy: SensorAccuracy,
    val altitudeMeters: Double?,
    val takenAtMillis: Long,
)

/**
 * One position sample, feeding the speed tile, the altitude tile and the trip.
 *
 * [distanceDeltaMeters] is the distance since the previous sample, not a running total - the total
 * belongs to the layer that accumulates it, because only that layer knows when it was last reset.
 *
 * The accuracy pair and the bearing default to null so a reading can be built without them; the
 * Tourist trip needs them to judge a fix.
 */
data class MotionReading(
    val speedKmh: Float?,
    val altitudeMeters: Double?,
    val distanceDeltaMeters: Double,
    val takenAtMillis: Long,
    val horizontalAccuracyMeters: Float? = null,
    val verticalAccuracyMeters: Float? = null,
    val bearingDegrees: Float? = null,
) {
    companion object {

        /**
         * Converts the platform's speed to km/h. Done once here rather than per tile so two tiles
         * reading the same fix cannot disagree about rounding.
         */
        fun fromPlatformSpeed(
            rawSpeed: Float?,
            altitudeMeters: Double?,
            distanceDeltaMeters: Double,
            takenAtMillis: Long,
        ): MotionReading = MotionReading(
            speedKmh = rawSpeed?.times(KMH_PER_PLATFORM_UNIT),
            altitudeMeters = altitudeMeters,
            distanceDeltaMeters = distanceDeltaMeters,
            takenAtMillis = takenAtMillis,
        )

        private const val KMH_PER_PLATFORM_UNIT = 3.6f
    }
}

/**
 * The system step counter's own value.
 *
 * [stepsSinceBoot] is reported verbatim: this feature computes no step total of its own, so nothing
 * here subtracts a baseline or rolls over at midnight.
 */
data class StepReading(
    val stepsSinceBoot: Long,
    val takenAtMillis: Long,
)

/** Station pressure as the barometer reports it, before any altitude conversion. */
data class PressureReading(
    val hectopascals: Float,
    val takenAtMillis: Long,
)

data class LightReading(
    val lux: Float,
    val takenAtMillis: Long,
)

/** Field strength as one magnitude; [accuracy] is the platform's own confidence in it. */
data class MagneticFieldReading(
    val microtesla: Float,
    val accuracy: SensorAccuracy,
    val takenAtMillis: Long,
)

data class LevelReading(
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val takenAtMillis: Long,
)
