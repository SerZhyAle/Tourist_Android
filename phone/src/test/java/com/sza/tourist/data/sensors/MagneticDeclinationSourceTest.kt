package com.sza.tourist.data.sensors

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The declination comes from the platform's world magnetic model, so the model's exact figure is the
 * platform's to change; what is pinned here is the sign and the order of magnitude, which are not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class MagneticDeclinationSourceTest {

    private val source = MagneticDeclinationSource()

    @Test
    fun `magnetic north lies east of true north in Kyiv`() {
        val declination = source.declinationDegrees(KYIV_LATITUDE, KYIV_LONGITUDE, KYIV_ALTITUDE, AUTUMN_2026)
        assertTrue("declination $declination", declination in 4f..12f)
    }

    @Test
    fun `magnetic north is within a few degrees of true north in London`() {
        val declination = source.declinationDegrees(LONDON_LATITUDE, LONDON_LONGITUDE, null, AUTUMN_2026)
        assertTrue("declination $declination", declination in -4f..4f)
    }

    private companion object {
        const val KYIV_LATITUDE = 50.45
        const val KYIV_LONGITUDE = 30.52
        const val KYIV_ALTITUDE = 180.0
        const val LONDON_LATITUDE = 51.51
        const val LONDON_LONGITUDE = -0.13

        /** 2026-10-23 12:00 UTC. */
        const val AUTUMN_2026 = 1_792_756_800_000L
    }
}
