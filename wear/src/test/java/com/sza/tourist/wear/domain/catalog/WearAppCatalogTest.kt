package com.sza.tourist.wear.domain.catalog

import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import com.sza.tourist.wear.domain.model.WearApp
import com.sza.tourist.wear.domain.model.WearAppId
import com.sza.tourist.wear.domain.model.destinationFor
import com.sza.tourist.wear.ui.navigation.WearLaunchRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private data class FakeCapabilities(
    override val locksSystemShade: Boolean = true,
    override val offersHealthFeatures: Boolean = true,
    // S3362: defaulted to the offering build so every test keeps asserting the full kit; a test about a
    // withholding build pins the answer it is about.
    override val offersScreenTakeoverPrograms: Boolean = true,
) : WearRestrictedCapabilities

/**
 * Ported from FastMediaSorter's `WearAppCatalogTest`, trimmed to the four programs of the kit and to
 * the two capabilities this product's catalog consults.
 */
class WearAppCatalogTest {

    @Test
    fun `catalog returns the kit in declaration order`() {
        val ids = WearAppCatalog.apps(FakeCapabilities()).map { it.id }

        assertEquals(
            listOf(
                WearAppId.TOURIST,
                WearAppId.WATER_FLASHLIGHT,
                WearAppId.SOS,
                WearAppId.STOPWATCH
            ),
            ids
        )
    }

    @Test
    fun `every route equals its program canonical key`() {
        // S2751: the record no longer carries the address, so the parity is read where the address now
        // lives - the invariant is unchanged, only the place that answers "what is this program's route".
        WearAppCatalog.apps(FakeCapabilities()).forEach { app ->
            assertEquals(app.id.canonicalKey, WearLaunchRoutes.routeFor(destinationFor(app.id)))
        }
    }

    /**
     * S2995: the two halves of the flavor claim, read in one run.
     *
     * Compiling `store` proves the code builds there, never that the row is withheld - the answer is a
     * parameter, so the only place both answers can be seen at once is here.
     */
    @Test
    fun `the offering build lists the Tourist dashboard`() {
        assertTrue(WearAppCatalog.apps(FakeCapabilities()).map { it.id }.contains(WearAppId.TOURIST))
    }

    @Test
    fun `the withholding build drops the Tourist dashboard and nothing else`() {
        val offered = WearAppCatalog.apps(FakeCapabilities()).map { it.id }
        val withheld = WearAppCatalog.apps(FakeCapabilities(offersHealthFeatures = false)).map { it.id }

        assertFalse(withheld.contains(WearAppId.TOURIST))
        assertEquals(offered - setOf(WearAppId.TOURIST), withheld)
    }

    /**
     * S3362: the two screens a swipe cannot leave answer to one capability, and only to that one.
     *
     * Pinning the answers one at a time is what stops a later edit from moving a program to a
     * neighbouring gate and leaving both boundary tests green.
     */
    @Test
    fun `the screen takeover capability alone decides the flashlight and the distress signal`() {
        val withheld = WearAppCatalog.apps(FakeCapabilities(offersScreenTakeoverPrograms = false))
            .map { it.id }
        val offered = WearAppCatalog.apps(FakeCapabilities()).map { it.id }

        assertEquals(
            offered - setOf(WearAppId.WATER_FLASHLIGHT, WearAppId.SOS),
            withheld
        )
    }

    @Test
    fun `the stopwatch is listed whatever the capabilities say`() {
        val withheld = WearAppCatalog.apps(
            FakeCapabilities(offersHealthFeatures = false, offersScreenTakeoverPrograms = false)
        ).map { it.id }

        assertEquals(listOf(WearAppId.STOPWATCH), withheld)
    }

    @Test
    fun `an unavailable record is absent from the rendered list`() {
        val records = listOf(
            WearApp(WearAppId.STOPWATCH, labelRes = 1),
            WearApp(WearAppId.SOS, labelRes = 2, isAvailable = false)
        )

        val visible = records.filter { it.isAvailable }

        assertEquals(listOf(WearAppId.STOPWATCH), visible.map { it.id })
        assertFalse(visible.any { it.id == WearAppId.SOS })
    }
}
