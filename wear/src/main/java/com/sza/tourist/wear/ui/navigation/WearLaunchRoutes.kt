package com.sza.tourist.wear.ui.navigation

import com.sza.tourist.wear.domain.model.WearAppId
import com.sza.tourist.wear.domain.model.WearDestinationId
import com.sza.tourist.wear.domain.model.WearLaunchAddress
import com.sza.tourist.wear.domain.model.appIdFor

/**
 * S2751: the one table turning a domain launch address into a navigation route.
 *
 * Lifted out of the launch resolver, which lives in the domain branch and had to import this module's
 * routes to answer at all. A route is an address inside the navigation graph, so it belongs beside the
 * graph rather than under it (strategic ADR-1).
 */
internal object WearLaunchRoutes {

    /**
     * A program is answered through [appIdFor]; the one destination that is no program is the home
     * screen, which is also where an unnamed target lands.
     */
    fun routeFor(destination: WearDestinationId): String {
        val program = appIdFor(destination)
        return if (program != null) programRouteFor(program) else WearRoutes.HOME
    }

    /**
     * Exhaustive with no else branch on purpose: a new program fails compilation here rather than
     * silently resolving to nothing.
     */
    private fun programRouteFor(program: WearAppId): String = when (program) {
        WearAppId.TOURIST -> WearRoutes.TOURIST
        WearAppId.WATER_FLASHLIGHT -> WearRoutes.WATER_FLASHLIGHT
        WearAppId.SOS -> WearRoutes.SOS
        WearAppId.STOPWATCH -> WearRoutes.STOPWATCH
    }

    /**
     * S3116: the program a navigation route belongs to, or null when the route is not a program's.
     *
     * Derived from [programRouteFor] rather than written out as a second table, for the reason
     * `appIdFor` states about its own inverse: the two would be one edit apart from disagreeing, and
     * here the disagreement would be silent. Every program route is a constant with no argument, so an
     * exact comparison is the whole match (strategic ADR-2).
     */
    fun appIdForRoute(route: String): WearAppId? =
        WearAppId.entries.firstOrNull { program -> programRouteFor(program) == route }

    fun routeFor(address: WearLaunchAddress): String = when (address) {
        is WearLaunchAddress.Screen -> routeFor(address.destination)
    }
}
