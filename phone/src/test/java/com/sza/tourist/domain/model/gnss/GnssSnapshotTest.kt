package com.sza.tourist.domain.model.gnss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GnssSnapshotTest {

    private fun satellite(id: Int, used: Boolean, cn0: Double?) = GnssSatellite(
        constellation = GnssConstellation.GPS,
        satelliteId = id,
        cn0DbHz = cn0,
        usedInFix = used,
        elevationDegrees = 40f,
        azimuthDegrees = 120f,
    )

    private fun snapshot(vararg satellites: GnssSatellite) =
        GnssSnapshot(satellites = satellites.toList(), coordinate = null, takenAtMillis = 0L)

    @Test
    fun `visible and used counts come from the satellite list`() {
        val snapshot = snapshot(satellite(1, used = true, cn0 = 30.0), satellite(2, used = false, cn0 = 20.0), satellite(3, used = true, cn0 = null))

        assertEquals(3, snapshot.visibleCount)
        assertEquals(2, snapshot.usedInFixCount)
    }

    @Test
    fun `the mean signal leaves out satellites that reported none`() {
        val snapshot = snapshot(satellite(1, used = true, cn0 = 30.0), satellite(2, used = true, cn0 = 20.0), satellite(3, used = false, cn0 = null))

        assertEquals(25.0, snapshot.meanCn0DbHz!!, 1e-9)
    }

    @Test
    fun `the mean signal is null while no satellite has reported one`() {
        assertNull(snapshot(satellite(1, used = false, cn0 = null)).meanCn0DbHz)
        assertNull(snapshot().meanCn0DbHz)
    }

    @Test
    fun `an empty snapshot has no satellites and no fix`() {
        val empty = GnssSnapshot.empty(takenAtMillis = 42L)

        assertTrue(empty.satellites.isEmpty())
        assertNull(empty.coordinate)
        assertEquals(42L, empty.takenAtMillis)
        assertEquals(0, empty.visibleCount)
        assertEquals(0, empty.usedInFixCount)
    }
}
