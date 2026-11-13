package com.sza.tourist.wear

import com.sza.tourist.wear.capability.StoreWearRestrictedCapabilities
import com.sza.tourist.wear.domain.catalog.WearAppCatalog
import com.sza.tourist.wear.domain.model.WearAppId
import com.sza.tourist.wear.domain.model.WearDestinationId
import com.sza.tourist.wear.domain.model.WearLaunchAddress
import com.sza.tourist.wear.domain.model.WearLaunchTarget
import com.sza.tourist.wear.domain.usecase.ResolveWearLaunchAddressUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * S3178 / owner decision 4.3 (S0008): the store boundary asserted from inside the variant that ships
 * through Google Play.
 *
 * In `wear/src/testStore/` rather than in the shared test set on purpose: the subject is the answer THIS
 * variant gives, and a shared test could only assert it by constructing the implementation it wants, which
 * proves nothing about which one the build binds. Its mirror is `NoLegalBoundaryTest`.
 *
 * Modelled on FastMediaSorter's `StoreBoundaryTest`; the composition differs because the decision does:
 * the Tourist dashboard is withheld, SOS and the water flashlight are offered.
 */
class StoreBoundaryTest {

    private val capabilities = StoreWearRestrictedCapabilities()

    @Test
    fun `the dashboard is withheld from the store build`() {
        assertFalse("health features", capabilities.offersHealthFeatures)
    }

    @Test
    fun `the screen takeover pair is offered by the store build`() {
        assertTrue("screen takeover programs", capabilities.offersScreenTakeoverPrograms)
    }

    @Test
    fun `the apps list is the flashlight, the distress signal and the stopwatch`() {
        assertEquals(
            listOf(WearAppId.WATER_FLASHLIGHT, WearAppId.SOS, WearAppId.STOPWATCH),
            WearAppCatalog.apps(capabilities).map { it.id }
        )
    }

    @Test
    fun `an outside caller cannot reach the dashboard in the store build`() {
        val resolver = ResolveWearLaunchAddressUseCase(capabilities)

        assertNull(resolver(WearLaunchTarget.Destination(WearDestinationId.TOURIST)))
        assertEquals(
            WearLaunchAddress.Screen(WearDestinationId.SOS),
            resolver(WearLaunchTarget.Destination(WearDestinationId.SOS))
        )
    }

    /**
     * The manifest half of the boundary, read from the files this repository contributes. The merged
     * manifest of a built store variant is read back separately (S0008 acceptance); this guards the
     * sources that produce it.
     */
    @Test
    fun `the store manifest removes every sensitive permission and the shared one declares none`() {
        val root = repositoryRoot()
        val store = File(root, "wear/src/store/AndroidManifest.xml").readText()
        val main = File(root, "wear/src/main/AndroidManifest.xml").readText()

        SENSITIVE.forEach { permission ->
            assertTrue("store must remove $permission", store.removes(permission))
            assertFalse("shared manifest must not declare $permission", main.contains(permission))
        }
    }

    private fun String.removes(permission: String): Boolean =
        Regex("""<uses-permission\s+android:name="${Regex.escape(permission)}"\s+tools:node="remove"\s*/>""")
            .containsMatchIn(this)

    private fun repositoryRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Could not locate repository root from ${System.getProperty("user.dir")}")
    }

    private companion object {
        val SENSITIVE = listOf(
            "android.permission.BODY_SENSORS",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.USE_FULL_SCREEN_INTENT",
        )
    }
}
