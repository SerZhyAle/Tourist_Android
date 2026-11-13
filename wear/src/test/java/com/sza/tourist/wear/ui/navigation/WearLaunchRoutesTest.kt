package com.sza.tourist.wear.ui.navigation

import com.sza.tourist.wear.domain.model.WearAppId
import com.sza.tourist.wear.domain.model.WearDestinationId
import com.sza.tourist.wear.domain.model.WearLaunchAddress
import com.sza.tourist.wear.domain.model.WearLaunchTarget
import com.sza.tourist.wear.domain.model.appIdFor
import com.sza.tourist.wear.domain.model.destinationFor
import com.sza.tourist.wear.domain.model.extras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The address tables of the program list: destination, route and program must agree in both directions. */
class WearLaunchRoutesTest {

    @Test
    fun `every program round trips through its destination`() {
        WearAppId.entries.forEach { program ->
            assertEquals(program, appIdFor(destinationFor(program)))
        }
    }

    @Test
    fun `every program round trips through its route`() {
        WearAppId.entries.forEach { program ->
            val route = WearLaunchRoutes.routeFor(destinationFor(program))

            assertEquals(program, WearLaunchRoutes.appIdForRoute(route))
        }
    }

    @Test
    fun `the program routes are the canonical keys the phone uses`() {
        assertEquals("tourist_info", WearRoutes.TOURIST)
        assertEquals("water_flashlight", WearRoutes.WATER_FLASHLIGHT)
        assertEquals("sos", WearRoutes.SOS)
        assertEquals("stopwatch", WearRoutes.STOPWATCH)
    }

    @Test
    fun `the home destination lands on the start route and belongs to no program`() {
        assertNull(appIdFor(WearDestinationId.HOME))
        assertEquals(WearRoutes.HOME, WearLaunchRoutes.routeFor(WearDestinationId.HOME))
        assertNull(WearLaunchRoutes.appIdForRoute(WearRoutes.HOME))
    }

    @Test
    fun `an address resolves to the route of its destination`() {
        assertEquals(
            WearRoutes.SOS,
            WearLaunchRoutes.routeFor(WearLaunchAddress.Screen(WearDestinationId.SOS))
        )
    }

    @Test
    fun `a destination target writes its name on the wire`() {
        val extras = WearLaunchTarget.Destination(WearDestinationId.STOPWATCH).extras()

        assertEquals(2, extras.size)
        assertEquals(
            listOf("STOPWATCH", "destination"),
            extras.values.map { (it as com.sza.tourist.wear.domain.model.WearLaunchExtra.Text).value }.sorted()
        )
    }
}
