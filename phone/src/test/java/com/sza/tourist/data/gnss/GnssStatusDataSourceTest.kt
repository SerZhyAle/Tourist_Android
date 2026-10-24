package com.sza.tourist.data.gnss

import android.Manifest
import android.app.Application
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import com.sza.tourist.domain.model.gnss.GnssConstellation
import com.sza.tourist.domain.model.gnss.GnssSnapshot
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

/**
 * The satellite and position source was uncovered in FastMediaSorter's tree; ported first, pinned here
 * afterwards. The cut of the sorter's section wrapper is pinned too: an unusable device is an empty flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class GnssStatusDataSourceTest {

    private val application: Application = RuntimeEnvironment.getApplication()
    private val manager = application.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    private val shadow = shadowOf(manager)

    private fun newSource() = GnssStatusDataSource(application)

    private fun grantFineLocation() = shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private class Sat(val constellation: Int, val svid: Int, val cn0: Float, val elevation: Float, val azimuth: Float, val used: Boolean)

    private fun statusOf(vararg satellites: Sat): GnssStatus {
        val builder = GnssStatus.Builder()
        satellites.forEach {
            builder.addSatellite(
                it.constellation, it.svid, it.cn0, it.elevation, it.azimuth,
                true, true, it.used, false, 0f, false, 0f,
            )
        }
        return builder.build()
    }

    @Test
    fun `without the fine location grant the flow is empty and nothing is registered`() = runTest {
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val received = mutableListOf<GnssSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().observe().collect { received += it } }
        idle()

        assertTrue(received.isEmpty())
        assertTrue(shadow.getLocationUpdateListeners(LocationManager.GPS_PROVIDER).isEmpty())
    }

    @Test
    fun `a disabled gps provider gives an empty flow`() = runTest {
        grantFineLocation()
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        val received = mutableListOf<GnssSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().observe().collect { received += it } }
        idle()

        assertTrue(received.isEmpty())
        assertTrue(shadow.getLocationUpdateListeners(LocationManager.GPS_PROVIDER).isEmpty())
    }

    @Test
    fun `a working receiver emits an empty snapshot at once`() = runTest {
        grantFineLocation()
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val received = mutableListOf<GnssSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().observe().collect { received += it } }
        idle()

        val first = received.first()
        assertTrue(first.satellites.isEmpty())
        assertNull(first.coordinate)
    }

    @Test
    fun `a satellite report becomes a snapshot with the constellation and the used flag`() = runTest {
        grantFineLocation()
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val received = mutableListOf<GnssSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().observe().collect { received += it } }
        idle()

        shadow.simulateGnssStatus(
            statusOf(
                Sat(GnssStatus.CONSTELLATION_GPS, 5, 32.5f, 45f, 120f, true),
                Sat(GnssStatus.CONSTELLATION_GALILEO, 9, 0f, 10f, 300f, false),
            ),
        )
        idle()

        val snapshot = received.last()
        assertEquals(2, snapshot.visibleCount)
        assertEquals(1, snapshot.usedInFixCount)
        val gps = snapshot.satellites[0]
        assertEquals(GnssConstellation.GPS, gps.constellation)
        assertEquals(5, gps.satelliteId)
        assertEquals(32.5, gps.cn0DbHz!!, 1e-6)
        assertEquals(45f, gps.elevationDegrees, 0f)
        assertEquals(120f, gps.azimuthDegrees, 0f)
        val galileo = snapshot.satellites[1]
        assertEquals(GnssConstellation.GALILEO, galileo.constellation)
        assertNull("a flat zero is an undecoded signal, not a measurement", galileo.cn0DbHz)
    }

    @Test
    fun `a position update fills the coordinate`() = runTest {
        grantFineLocation()
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val received = mutableListOf<GnssSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().observe().collect { received += it } }
        idle()

        val fix = Location(LocationManager.GPS_PROVIDER).also {
            it.latitude = 50.45
            it.longitude = 30.52
            it.accuracy = 6f
            it.time = 1_792_756_800_000L
            it.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        shadow.simulateLocation(fix)
        idle()

        val coordinate = received.last().coordinate
        assertNotNull(coordinate)
        assertEquals(50.45, coordinate!!.latitudeDegrees, 1e-9)
        assertEquals(30.52, coordinate.longitudeDegrees, 1e-9)
        assertEquals(6f, coordinate.accuracyMeters!!, 0f)
        assertEquals(1_792_756_800_000L, coordinate.fixedAtMillis)
    }

    @Test
    fun `the position request is removed when the collector leaves`() = runTest {
        grantFineLocation()
        shadow.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newSource().observe().collect { } }
        idle()
        assertEquals(1, shadow.getLocationUpdateListeners(LocationManager.GPS_PROVIDER).size)

        job.cancel()

        assertTrue(shadow.getLocationUpdateListeners(LocationManager.GPS_PROVIDER).isEmpty())
    }
}
