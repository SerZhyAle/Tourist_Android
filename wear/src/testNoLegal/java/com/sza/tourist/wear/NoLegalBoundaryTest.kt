package com.sza.tourist.wear

import com.sza.tourist.wear.capability.NoLegalWearRestrictedCapabilities
import com.sza.tourist.wear.domain.catalog.WearAppCatalog
import com.sza.tourist.wear.domain.model.WearAppId
import com.sza.tourist.wear.domain.model.WearDestinationId
import com.sza.tourist.wear.domain.model.WearLaunchAddress
import com.sza.tourist.wear.domain.model.WearLaunchTarget
import com.sza.tourist.wear.domain.usecase.ResolveWearLaunchAddressUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Owner decision 4.3 (S0008): the sideload edition offers the whole kit. The mirror of `StoreBoundaryTest`,
 * in `wear/src/testNoLegal/` for the same reason - the subject is the answer this variant gives.
 */
class NoLegalBoundaryTest {

    private val capabilities = NoLegalWearRestrictedCapabilities()

    @Test
    fun `every capability is offered by the sideload build`() {
        assertTrue("health features", capabilities.offersHealthFeatures)
        assertTrue("screen takeover programs", capabilities.offersScreenTakeoverPrograms)
    }

    @Test
    fun `the apps list is the whole kit`() {
        assertEquals(
            listOf(WearAppId.TOURIST, WearAppId.WATER_FLASHLIGHT, WearAppId.SOS, WearAppId.STOPWATCH),
            WearAppCatalog.apps(capabilities).map { it.id }
        )
    }

    @Test
    fun `an outside caller can reach every destination in the sideload build`() {
        val resolver = ResolveWearLaunchAddressUseCase(capabilities)

        WearDestinationId.entries.forEach { destination ->
            assertEquals(
                WearLaunchAddress.Screen(destination),
                resolver(WearLaunchTarget.Destination(destination))
            )
        }
    }

    /** The manifest half: the dashboard's permissions are declared here and in no other source set. */
    @Test
    fun `the sideload manifest declares what the dashboard reads and the shared one does not`() {
        val root = repositoryRoot()
        val noLegal = File(root, "wear/src/noLegal/AndroidManifest.xml").readText()
        val main = File(root, "wear/src/main/AndroidManifest.xml").readText()

        DASHBOARD_PERMISSIONS.forEach { permission ->
            assertTrue("noLegal must declare $permission", noLegal.contains("\"$permission\""))
            assertFalse("shared manifest must not declare $permission", main.contains(permission))
        }
        assertFalse(
            "full-screen intent serves a feature this product does not carry",
            noLegal.contains("USE_FULL_SCREEN_INTENT")
        )
    }

    private fun repositoryRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Could not locate repository root from ${System.getProperty("user.dir")}")
    }

    private companion object {
        val DASHBOARD_PERMISSIONS = listOf(
            "android.permission.BODY_SENSORS",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
        )
    }
}
