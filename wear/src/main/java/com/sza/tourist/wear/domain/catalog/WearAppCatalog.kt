package com.sza.tourist.wear.domain.catalog

import com.sza.tourist.wear.R
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import com.sza.tourist.wear.domain.model.WearApp
import com.sza.tourist.wear.domain.model.WearAppId

/**
 * The program list renders what this catalog returns; it never decides for itself which programs exist.
 *
 * Order is fixed here rather than at the call site so a fifth program stays a one-record change and
 * cannot silently reorder the screen. The unavailable filter runs here for the same reason: a program
 * that later hides itself does so without the screen learning a second concept.
 *
 * S2457 / S2995: [capabilities] provides BUILD-TIME answers from `WearRestrictedCapabilities`, whose
 * implementation is chosen by the variant source set and cannot change while the app runs.
 *
 * S2751: moved here from the Apps screen's own package. What it returns is domain records, so its place
 * above the layer that consumes them was an accident of the screen having been its first caller.
 *
 * Trimmed to the kit; the order is the phone's program home (Dashboard, Lights, SOS, Stopwatch).
 */
object WearAppCatalog {

    fun apps(capabilities: WearRestrictedCapabilities): List<WearApp> {
        return listOf(
            // S3042: health features (Tourist step and heart-rate telemetry) withheld from the store
            // variant - owner decision 4.3.
            WearApp(
                id = WearAppId.TOURIST,
                labelRes = R.string.wear_tourist_app,
                isAvailable = capabilities.offersHealthFeatures
            ),
            // S3362: the screen consumes every pointer event so a wet wrist cannot dismiss it, which
            // is the gesture WO-V3 asks for on almost every screen. Owner decision 4.3 offers it in
            // both variants; the gate stays so a refused review is one line to turn back.
            WearApp(
                id = WearAppId.WATER_FLASHLIGHT,
                labelRes = R.string.wear_water_flashlight_app,
                isAvailable = capabilities.offersScreenTakeoverPrograms
            ),
            // S3216: the same screen takeover as the water flashlight, plus a full-screen strobe and
            // the alarm stream raised to its maximum, so both are answered by one capability.
            WearApp(
                id = WearAppId.SOS,
                labelRes = R.string.wear_app_sos,
                isAvailable = capabilities.offersScreenTakeoverPrograms
            ),
            // S2825: listed in both variants - the stopwatch needs no permission and no hardware, so
            // there is nothing here for a store review to withhold.
            WearApp(
                id = WearAppId.STOPWATCH,
                labelRes = R.string.wear_app_stopwatch
            )
        ).filter { it.isAvailable }
    }
}
