package com.sza.tourist.data.sensors

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import com.sza.tourist.domain.model.sensors.MotionReading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * The position source was uncovered in FastMediaSorter; ported first, pinned here afterwards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class MotionReadingSourceTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val shadow = shadowOf(manager)

    private fun newSource() = MotionReadingSource(context)

    private fun fix(
        latitude: Double = 50.0,
        longitude: Double = 30.0,
        speed: Float? = null,
        altitude: Double? = null,
        accuracy: Float? = null,
        bearing: Float? = null,
    ) = Location(LocationManager.GPS_PROVIDER).also {
        it.latitude = latitude
        it.longitude = longitude
        speed?.let { value -> it.speed = value }
        altitude?.let { value -> it.altitude = value }
        accuracy?.let { value -> it.accuracy = value }
        bearing?.let { value -> it.bearing = value }
    }

    private fun deliver(location: Location) {
        // The platform drops a fix that is not newer than the last delivered one by the requested
        // interval, so each fix is stamped at the moment it is delivered, two seconds after the last.
        ShadowSystemClock.advanceBy(Duration.ofSeconds(2))
        location.time = System.currentTimeMillis()
        location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        shadow.simulateLocation(location)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `the platform speed arrives in kilometres per hour`() = runTest {
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val received = mutableListOf<MotionReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        deliver(fix(speed = 2.5f, altitude = 812.5, accuracy = 4f, bearing = 90f))

        val reading = received.single()
        assertEquals(9.0f, reading.speedKmh!!, DELTA)
        assertEquals(812.5, reading.altitudeMeters!!, DELTA.toDouble())
        assertEquals(4f, reading.horizontalAccuracyMeters!!, DELTA)
        assertEquals(90f, reading.bearingDegrees!!, DELTA)
    }

    @Test
    fun `a fix with no speed or altitude leaves them null rather than zero`() = runTest {
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val received = mutableListOf<MotionReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        deliver(fix())

        val reading = received.single()
        assertNull(reading.speedKmh)
        assertNull(reading.altitudeMeters)
        assertNull(reading.horizontalAccuracyMeters)
        assertNull(reading.bearingDegrees)
    }

    @Test
    fun `the first fix adds no distance and the next one adds the gap`() = runTest {
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val received = mutableListOf<MotionReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        val first = fix(latitude = 50.0, accuracy = 5f)
        val second = fix(latitude = 50.001, accuracy = 5f)
        deliver(first)
        deliver(second)

        assertEquals(2, received.size)
        assertEquals(0.0, received[0].distanceDeltaMeters, DELTA.toDouble())
        assertEquals(first.distanceTo(second).toDouble(), received[1].distanceDeltaMeters, DELTA.toDouble())
        assertTrue(received[1].distanceDeltaMeters > 100.0)
    }

    @Test
    fun `a fix too coarse to trust adds no distance`() = runTest {
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val received = mutableListOf<MotionReading>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { received += it } }

        deliver(fix(latitude = 50.0, accuracy = 5f))
        deliver(fix(latitude = 50.01, accuracy = 400f))

        assertEquals(0.0, received[1].distanceDeltaMeters, DELTA.toDouble())
    }

    @Test
    fun `updates are removed when the collector leaves`() = runTest {
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { } }
        assertNotNull(shadow.getLocationUpdateListeners(LocationManager.GPS_PROVIDER).firstOrNull())

        job.cancel()

        assertTrue(shadow.getLocationUpdateListeners(LocationManager.GPS_PROVIDER).isEmpty())
    }

    @Test
    fun `a disabled provider is not asked for updates`() = runTest {
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        shadow.setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().readings().collect { } }

        assertTrue(shadow.getLocationUpdateListeners(LocationManager.GPS_PROVIDER).isEmpty())
        assertTrue(shadow.getLocationUpdateListeners(LocationManager.NETWORK_PROVIDER).isEmpty())
    }

    private companion object {
        const val DELTA = 0.01f
    }
}
