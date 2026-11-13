package com.sza.tourist.wear.ui.navigation

/**
 * Every navigation address of the watch app, declared once.
 *
 * A route spelled out at the call site is not checked by anything: the declaration and the jump are two
 * separate string literals, so a typo in either is a silent no-op at runtime - the tap does nothing and no
 * test catches it, because the watch module has no instrumented tests. Naming each route here makes the two
 * ends the same symbol, so a mismatch stops being possible.
 *
 * Trimmed to the kit: the routes of FastMediaSorter's resource, browse, player, settings and mini-program
 * screens are not part of this product, and none of the remaining routes carries an argument.
 */
object WearRoutes {

    /** The start screen: the program list. */
    const val HOME = "home"

    /**
     * Programs of the list. Each value equals its program's `canonicalKey` in `WearAppId`, so a key saved
     * by the phone's program registry resolves straight to a destination.
     */
    const val WATER_FLASHLIGHT = "water_flashlight"

    /** S2825: the watch stopwatch. Ships in both variants with no `WearRestrictedCapabilities` gate. */
    const val STOPWATCH = "stopwatch"

    /**
     * S3007: the watch Tourist telemetry and navigation dashboard; registered only where
     * `offersHealthFeatures` is true.
     */
    const val TOURIST = "tourist_info"

    /**
     * S4185: the Tourist tally counter, opened from its tile on [TOURIST] and registered beside it.
     */
    const val TOURIST_COUNTER = "tourist_counter"

    /**
     * S3216: the distress signal. Carries its `canonicalKey` like the programs above, and that key is
     * the phone's route key too - one program on two devices.
     *
     * S3362: registered behind `offersScreenTakeoverPrograms`, beside the water flashlight.
     */
    const val SOS = "sos"
}
