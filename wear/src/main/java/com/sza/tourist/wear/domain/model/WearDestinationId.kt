package com.sza.tourist.wear.domain.model

/**
 * S2511: the whole set of destinations an outside caller may name.
 *
 * The app's only exported Activity receives these names. Naming a navigation route directly would hand every
 * app installed on the watch the right to open an arbitrary internal screen, so the wire carries a value of
 * this perch and the route is resolved on the inside (strategic ADR-3). A name absent from here reads as "no
 * target was named", which lands on the ordinary launch rather than somewhere the caller chose.
 *
 * Trimmed to the kit: the programs in the order the program list draws them, then the home screen.
 */
enum class WearDestinationId {
    /** S3007: the watch Tourist dashboard, addressable as a shortcut. */
    TOURIST,

    WATER_FLASHLIGHT,

    /** S3216: the distress signal, addressable as a shortcut - the fastest entrance an emergency has. */
    SOS,

    /** S2825: the watch stopwatch, addressable as a shortcut like every program above. */
    STOPWATCH,

    /**
     * S2511: the app's own home screen, the program list.
     *
     * Last because it is no program: naming it here rather than launching with no target at all keeps every
     * address answering the same contract, so a tap that lands on the home screen is a stated destination
     * rather than the fallback an unreadable intent also produces.
     */
    HOME
}

/**
 * The destination a program is reached at.
 *
 * Exhaustive with no else branch on purpose: each new watch program must be given an address here rather
 * than silently dropping out of the shortcut grid.
 */
fun destinationFor(id: WearAppId): WearDestinationId = when (id) {
    WearAppId.TOURIST -> WearDestinationId.TOURIST
    WearAppId.WATER_FLASHLIGHT -> WearDestinationId.WATER_FLASHLIGHT
    WearAppId.SOS -> WearDestinationId.SOS
    WearAppId.STOPWATCH -> WearDestinationId.STOPWATCH
}

/**
 * The program a destination stands for, or null when the destination is the home screen (S3109).
 *
 * Derived from [destinationFor] rather than written out as a second table: the two would be one
 * edit apart from disagreeing, and a table that can disagree with its own inverse is the shape a
 * tile and its screen come to show different things through.
 */
fun appIdFor(destination: WearDestinationId): WearAppId? =
    WearAppId.entries.firstOrNull { app -> destinationFor(app) == destination }
