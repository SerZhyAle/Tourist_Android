package com.sza.tourist.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.sza.tourist.domain.model.sensors.LevelReading
import com.sza.tourist.domain.model.sensors.LightReading
import com.sza.tourist.domain.model.sensors.MagneticFieldReading
import com.sza.tourist.domain.model.sensors.PressureReading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
 * The barometer, light, magnetic field and tilt feeds of the dashboard.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class EnvironmentSensorReadingSourceTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val shadow = shadowOf(manager)

    private fun newSource() = EnvironmentSensorReadingSource(context)

    private fun event(type: Int, vararg values: Float) =
        ShadowSensorManager.createSensorEvent(values.size, type).also { values.copyInto(it.values) }

    @Test
    fun `pressure is reported in hectopascals as the barometer gives it`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_PRESSURE))
        val received = mutableListOf<PressureReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().pressure().collect { received += it } }

        shadow.sendSensorEventToListeners(event(Sensor.TYPE_PRESSURE, 1013.25f))

        assertEquals(1013.25f, received.single().hectopascals, 0f)
    }

    @Test
    fun `light is reported in lux`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_LIGHT))
        val received = mutableListOf<LightReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().light().collect { received += it } }

        shadow.sendSensorEventToListeners(event(Sensor.TYPE_LIGHT, 320f))

        assertEquals(320f, received.single().lux, 0f)
    }

    @Test
    fun `the magnetic field is one magnitude over the three axes`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_MAGNETIC_FIELD))
        val received = mutableListOf<MagneticFieldReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().magneticField().collect { received += it } }

        shadow.sendSensorEventToListeners(event(Sensor.TYPE_MAGNETIC_FIELD, 3f, 4f, 12f))

        assertEquals(13f, received.single().microtesla, DELTA)
    }

    @Test
    fun `a device lying flat reads no pitch and no roll`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        val received = mutableListOf<LevelReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().level().collect { received += it } }

        shadow.sendSensorEventToListeners(event(Sensor.TYPE_ACCELEROMETER, 0f, 0f, GRAVITY))

        assertEquals(0f, received.single().pitchDegrees, DELTA)
        assertEquals(0f, received.single().rollDegrees, DELTA)
    }

    @Test
    fun `a top edge raised thirty degrees reads a pitch of thirty`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        val received = mutableListOf<LevelReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().level().collect { received += it } }

        shadow.sendSensorEventToListeners(event(Sensor.TYPE_ACCELEROMETER, 0f, GRAVITY * sin(THIRTY), GRAVITY * cos(THIRTY)))

        assertEquals(30f, received.single().pitchDegrees, DELTA)
        assertEquals(0f, received.single().rollDegrees, DELTA)
    }

    @Test
    fun `a right edge raised thirty degrees reads a roll of thirty`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        val received = mutableListOf<LevelReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().level().collect { received += it } }

        shadow.sendSensorEventToListeners(event(Sensor.TYPE_ACCELEROMETER, -GRAVITY * sin(THIRTY), 0f, GRAVITY * cos(THIRTY)))

        assertEquals(0f, received.single().pitchDegrees, DELTA)
        assertEquals(30f, received.single().rollDegrees, DELTA)
    }

    @Test
    fun `the gravity sensor is preferred over the raw accelerometer`() = runTest {
        val gravity = ShadowSensor.newInstance(Sensor.TYPE_GRAVITY)
        shadow.addSensor(gravity)
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().level().collect { } }

        assertTrue(shadow.hasListener(shadow.listeners.single(), gravity))
    }

    @Test
    fun `a missing sensor gives an empty flow and no listener`() = runTest {
        val received = mutableListOf<PressureReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().pressure().collect { received += it } }

        assertTrue(received.isEmpty())
        assertTrue(shadow.listeners.isEmpty())
    }

    @Test
    fun `the listener is unregistered when the collector leaves`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_PRESSURE))
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().pressure().collect { } }
        assertEquals(1, shadow.listeners.size)

        job.cancel()

        assertTrue(shadow.listeners.isEmpty())
    }

    private companion object {
        const val DELTA = 0.01f
        const val GRAVITY = 9.81f
        val THIRTY = Math.toRadians(30.0).toFloat()
    }
}
