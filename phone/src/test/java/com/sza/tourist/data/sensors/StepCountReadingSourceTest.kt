package com.sza.tourist.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.sza.tourist.domain.model.sensors.SensorCapability
import com.sza.tourist.domain.model.sensors.StepReading
import com.sza.tourist.domain.repository.SensorAvailabilityRepository
import io.mockk.every
import io.mockk.mockk
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

/**
 * The step counter source was uncovered in FastMediaSorter; ported first, pinned here afterwards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class StepCountReadingSourceTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val shadow = shadowOf(manager)

    private fun availability(stepCounter: Boolean) = mockk<SensorAvailabilityRepository> {
        every { isAvailable(SensorCapability.STEP_COUNTER) } returns stepCounter
    }

    private fun stepEvent(steps: Float) =
        ShadowSensorManager.createSensorEvent(1, Sensor.TYPE_STEP_COUNTER).also { it.values[0] = steps }

    @Test
    fun `the counter is reported verbatim`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_STEP_COUNTER))
        val received = mutableListOf<StepReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            StepCountReadingSource(context, availability(stepCounter = true)).readings().collect { received += it }
        }

        shadow.sendSensorEventToListeners(stepEvent(48_213f))

        assertEquals(48_213L, received.single().stepsSinceBoot)
    }

    @Test
    fun `a capability the repository denies gives an empty flow even with the sensor present`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_STEP_COUNTER))
        val received = mutableListOf<StepReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            StepCountReadingSource(context, availability(stepCounter = false)).readings().collect { received += it }
        }

        assertTrue(shadow.listeners.isEmpty())
        assertTrue(received.isEmpty())
    }

    @Test
    fun `a device without the sensor gives an empty flow`() = runTest {
        val received = mutableListOf<StepReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            StepCountReadingSource(context, availability(stepCounter = true)).readings().collect { received += it }
        }

        assertTrue(shadow.listeners.isEmpty())
        assertTrue(received.isEmpty())
    }

    @Test
    fun `the listener is unregistered when the collector leaves`() = runTest {
        shadow.addSensor(ShadowSensor.newInstance(Sensor.TYPE_STEP_COUNTER))
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            StepCountReadingSource(context, availability(stepCounter = true)).readings().collect { }
        }
        assertEquals(1, shadow.listeners.size)

        job.cancel()

        assertTrue(shadow.listeners.isEmpty())
    }
}
