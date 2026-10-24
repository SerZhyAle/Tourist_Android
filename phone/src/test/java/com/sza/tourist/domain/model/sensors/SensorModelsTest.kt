package com.sza.tourist.domain.model.sensors

import com.sza.tourist.domain.model.tourist.GpsAccuracyGrade
import com.sza.tourist.domain.model.tourist.TripMotionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure parts of the ported reading models and the trip policy. Their thresholds are shared with the
 * watch, so a change to any of them is a change on both devices and has to fail here first.
 */
class SensorModelsTest {

    @Test
    fun `platform accuracy statuses map one to one`() {
        assertEquals(SensorAccuracy.LOW, SensorAccuracy.fromPlatformStatus(1))
        assertEquals(SensorAccuracy.MEDIUM, SensorAccuracy.fromPlatformStatus(2))
        assertEquals(SensorAccuracy.HIGH, SensorAccuracy.fromPlatformStatus(3))
    }

    @Test
    fun `an unknown or no-contact status is never a confident value`() {
        assertEquals(SensorAccuracy.UNRELIABLE, SensorAccuracy.fromPlatformStatus(0))
        assertEquals(SensorAccuracy.UNRELIABLE, SensorAccuracy.fromPlatformStatus(-1))
        assertEquals(SensorAccuracy.UNRELIABLE, SensorAccuracy.fromPlatformStatus(99))
    }

    @Test
    fun `metres per second become kilometres per hour`() {
        val reading = MotionReading.fromPlatformSpeed(
            rawSpeed = 10f,
            altitudeMeters = 100.0,
            distanceDeltaMeters = 5.0,
            takenAtMillis = 7L,
        )

        assertEquals(36f, reading.speedKmh!!, 1e-4f)
        assertEquals(100.0, reading.altitudeMeters!!, 0.0)
        assertEquals(5.0, reading.distanceDeltaMeters, 0.0)
        assertEquals(7L, reading.takenAtMillis)
    }

    @Test
    fun `an unreported speed stays null and the accuracy pair defaults to null`() {
        val reading = MotionReading.fromPlatformSpeed(null, null, 0.0, 0L)

        assertNull(reading.speedKmh)
        assertNull(reading.horizontalAccuracyMeters)
        assertNull(reading.verticalAccuracyMeters)
        assertNull(reading.bearingDegrees)
    }

    @Test
    fun `moving needs a speed and a trusted accuracy`() {
        assertTrue(TripMotionPolicy.isMoving(speedKmh = 5f, horizontalAccuracyM = 8f))
        assertTrue(TripMotionPolicy.isMoving(speedKmh = 1.8f, horizontalAccuracyM = 50f))
    }

    @Test
    fun `too slow or too coarse or unmeasured is not moving`() {
        assertFalse(TripMotionPolicy.isMoving(speedKmh = 1.7f, horizontalAccuracyM = 8f))
        assertFalse(TripMotionPolicy.isMoving(speedKmh = 5f, horizontalAccuracyM = 50.1f))
        assertFalse(TripMotionPolicy.isMoving(speedKmh = null, horizontalAccuracyM = 8f))
        assertFalse(TripMotionPolicy.isMoving(speedKmh = 5f, horizontalAccuracyM = null))
    }

    @Test
    fun `the gps accuracy word follows the thresholds`() {
        assertEquals(GpsAccuracyGrade.GOOD, TripMotionPolicy.gpsAccuracyGrade(10f))
        assertEquals(GpsAccuracyGrade.MEDIUM, TripMotionPolicy.gpsAccuracyGrade(10.1f))
        assertEquals(GpsAccuracyGrade.MEDIUM, TripMotionPolicy.gpsAccuracyGrade(30f))
        assertEquals(GpsAccuracyGrade.POOR, TripMotionPolicy.gpsAccuracyGrade(30.1f))
        assertNull(TripMotionPolicy.gpsAccuracyGrade(null))
    }
}
