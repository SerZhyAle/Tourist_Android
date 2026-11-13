package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import com.sza.tourist.wear.domain.model.WearDestinationId
import com.sza.tourist.wear.domain.model.WearLaunchAddress
import com.sza.tourist.wear.domain.model.WearLaunchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private data class Capabilities(
    override val locksSystemShade: Boolean = true,
    override val offersHealthFeatures: Boolean = true,
    override val offersScreenTakeoverPrograms: Boolean = true,
) : WearRestrictedCapabilities

/**
 * S3178: the boundary for an OUTSIDE caller. A destination filtered out of the program list must be
 * refused here too, because a launch intent, the stopwatch notification and the phone's SOS start all
 * arrive with a destination name and none of them passed through a screen this build drew.
 */
class ResolveWearLaunchAddressUseCaseTest {

    private fun resolve(capabilities: Capabilities, destination: WearDestinationId): WearLaunchAddress? =
        ResolveWearLaunchAddressUseCase(capabilities).invoke(WearLaunchTarget.Destination(destination))

    @Test
    fun `the offering build resolves every destination`() {
        WearDestinationId.entries.forEach { destination ->
            assertEquals(
                destination.name,
                WearLaunchAddress.Screen(destination),
                resolve(Capabilities(), destination)
            )
        }
    }

    @Test
    fun `without health features the dashboard does not resolve and the rest does`() {
        val capabilities = Capabilities(offersHealthFeatures = false)

        assertNull(resolve(capabilities, WearDestinationId.TOURIST))
        assertEquals(
            WearLaunchAddress.Screen(WearDestinationId.SOS),
            resolve(capabilities, WearDestinationId.SOS)
        )
        assertEquals(
            WearLaunchAddress.Screen(WearDestinationId.STOPWATCH),
            resolve(capabilities, WearDestinationId.STOPWATCH)
        )
    }

    @Test
    fun `without the screen takeover the flashlight and the distress signal do not resolve`() {
        val capabilities = Capabilities(offersScreenTakeoverPrograms = false)

        assertNull(resolve(capabilities, WearDestinationId.WATER_FLASHLIGHT))
        assertNull(resolve(capabilities, WearDestinationId.SOS))
        assertEquals(
            WearLaunchAddress.Screen(WearDestinationId.TOURIST),
            resolve(capabilities, WearDestinationId.TOURIST)
        )
    }

    @Test
    fun `the home screen and the stopwatch resolve whatever the capabilities say`() {
        val capabilities = Capabilities(offersHealthFeatures = false, offersScreenTakeoverPrograms = false)

        assertEquals(
            WearLaunchAddress.Screen(WearDestinationId.HOME),
            resolve(capabilities, WearDestinationId.HOME)
        )
        assertEquals(
            WearLaunchAddress.Screen(WearDestinationId.STOPWATCH),
            resolve(capabilities, WearDestinationId.STOPWATCH)
        )
    }
}
