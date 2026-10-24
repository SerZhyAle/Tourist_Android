package com.sza.tourist.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.sza.tourist.domain.model.sensors.CompassReading
import com.sza.tourist.domain.model.sensors.SensorAccuracy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import kotlin.math.cos
import kotlin.math.sin

/**
 * The heading source was uncovered in FastMediaSorter; ported first, pinned here afterwards. Every
 * expectation is the platform's own geometry, never a number read back from the implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class OrientationReadingSourceTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val shadow = shadowOf(manager)

    private fun newSource() = OrientationReadingSource(context)

    @Test
    fun `a device with no heading sensor gets an empty flow`() = runTest {
        val received = mutableListOf<CompassReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        assertTrue(received.isEmpty())
        assertTrue(shadow.listeners.isEmpty())
    }

    @Test
    fun `the fused rotation vector yields a bearing in zero to 360 degrees`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR))
        val received = mutableListOf<CompassReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        shadow.sendSensorEventToListeners(rotationAboutUp(0.0))
        assertEquals(0f, received.single().azimuthDegrees, DELTA)
        assertNull(received.single().altitudeMeters)
    }

    @Test
    fun `a quarter turn counter-clockwise from above is a bearing of 270`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR))
        val received = mutableListOf<CompassReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        shadow.sendSensorEventToListeners(rotationAboutUp(Math.PI / 2))

        assertEquals(270f, received.single().azimuthDegrees, DELTA)
    }

    @Test
    fun `samples closer than a second are dropped and a later one passes`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR))
        val received = mutableListOf<CompassReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        shadow.sendSensorEventToListeners(rotationAboutUp(0.0))
        shadow.sendSensorEventToListeners(rotationAboutUp(0.0))
        assertEquals(1, received.size)

        // The throttle reads the wall clock, which Robolectric does not drive, so the wait is real.
        Thread.sleep(THROTTLE_WAIT_MS)
        shadow.sendSensorEventToListeners(rotationAboutUp(0.0))
        assertEquals(2, received.size)
    }

    @Test
    fun `the platform accuracy rides along with each reading`() = runTest {
        val sensor = ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR)
        shadow.addSensor(sensor)
        val received = mutableListOf<CompassReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        shadow.listeners.single().onAccuracyChanged(sensor, SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
        shadow.sendSensorEventToListeners(rotationAboutUp(0.0))

        assertEquals(SensorAccuracy.HIGH, received.single().accuracy)
    }

    @Test
    fun `the listener is unregistered when the collector leaves`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR))
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { } }
        assertEquals(1, shadow.listeners.size)

        job.cancel()

        assertTrue(shadow.listeners.isEmpty())
    }

    @Test
    fun `without the fused sensor the accelerometer and magnetometer pair delivers a heading`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_MAGNETIC_FIELD))
        val received = mutableListOf<CompassReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        // One sensor alone is not enough to orient by.
        shadow.sendSensorEventToListeners(vectorEvent(Sensor.TYPE_ACCELEROMETER, 0f, 0f, GRAVITY))
        assertTrue(received.isEmpty())

        // Flat on a table with the top edge towards magnetic north: the northern-hemisphere field
        // points north and down.
        shadow.sendSensorEventToListeners(vectorEvent(Sensor.TYPE_MAGNETIC_FIELD, 0f, NORTH_FIELD, DOWN_FIELD))

        assertEquals(0f, received.single().azimuthDegrees, DELTA)
    }

    @Test
    fun `the pair stays silent in free fall`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_MAGNETIC_FIELD))
        val received = mutableListOf<CompassReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        shadow.sendSensorEventToListeners(vectorEvent(Sensor.TYPE_ACCELEROMETER, 0f, 0f, 0f))
        shadow.sendSensorEventToListeners(vectorEvent(Sensor.TYPE_MAGNETIC_FIELD, 0f, NORTH_FIELD, DOWN_FIELD))

        assertTrue(received.isEmpty())
    }

    @Test
    fun `only the magnetometer's accuracy is reported for the pair`() = runTest {
        val accelerometer = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = ShadowSensor.newInstance(Sensor.TYPE_MAGNETIC_FIELD)
        shadow.addSensor(accelerometer)
        shadow.addSensor(magnetometer)
        val received = mutableListOf<CompassReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        val listener = shadow.listeners.first()
        listener.onAccuracyChanged(magnetometer, SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
        listener.onAccuracyChanged(accelerometer, SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
        shadow.sendSensorEventToListeners(vectorEvent(Sensor.TYPE_ACCELEROMETER, 0f, 0f, GRAVITY))
        shadow.sendSensorEventToListeners(vectorEvent(Sensor.TYPE_MAGNETIC_FIELD, 0f, NORTH_FIELD, DOWN_FIELD))

        assertEquals(SensorAccuracy.MEDIUM, received.single().accuracy)
    }

    /** A rotation vector for a turn of [radians] about the up axis, counter-clockwise seen from above. */
    private fun rotationAboutUp(radians: Double) = ShadowSensorManager.createSensorEvent(4, Sensor.TYPE_ROTATION_VECTOR).also {
        it.values[0] = 0f
        it.values[1] = 0f
        it.values[2] = sin(radians / 2).toFloat()
        it.values[3] = cos(radians / 2).toFloat()
    }

    private fun vectorEvent(type: Int, x: Float, y: Float, z: Float) =
        ShadowSensorManager.createSensorEvent(3, type).also {
            it.values[0] = x
            it.values[1] = y
            it.values[2] = z
        }

    private companion object {
        const val DELTA = 0.01f
        const val THROTTLE_WAIT_MS = 1_100L
        const val GRAVITY = 9.81f
        const val NORTH_FIELD = 22f
        const val DOWN_FIELD = -40f
    }
}
