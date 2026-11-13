package com.sza.tourist.wear.domain.model

import androidx.annotation.StringRes

/**
 * Programs of the watch, in the order the phone's program home lists the kit (Dashboard, Lights, SOS,
 * Stopwatch).
 *
 * [canonicalKey] is the phone's route key for the same program, not a name invented here. The phone's
 * program registry addresses a program by that key, so keeping it identical is what lets the two lists be
 * read as one kit without renaming a key already saved on a device.
 *
 * Trimmed from FastMediaSorter's list to the kit: the other programs of that list (calculator, network
 * monitor, game, recorder, system info, motion monitor, body sensor, blood pressure, broadcast, clipboard)
 * are not part of this product.
 */
enum class WearAppId(val canonicalKey: String) {

    /**
     * S3007: the watch's Tourist telemetry and navigation dashboard, sharing the phone's `tourist_info` route key.
     */
    TOURIST("tourist_info"),

    /**
     * S2516: the display used as a light behind a lock no touch opens. Shares its `canonicalKey` with
     * the phone's program of the same name, which has a torch as well - the watch has no flash unit,
     * so only the lit screen half exists here.
     */
    WATER_FLASHLIGHT("water_flashlight"),

    /**
     * S3216: siren and white-screen strobe on the Morse SOS cadence, behind the water flashlight's
     * touch lock. Shares its `canonicalKey` with the phone's program of the same name - starting it on
     * one device starts it on the other, so the two must be addressable as one program.
     */
    SOS("sos"),

    /**
     * S2825: the watch's stopwatch, sharing the phone's `stopwatch` route key. The phone half (S1411)
     * is addressed by that key in launcher cells and panel layouts, so the watch spells it identically
     * rather than inventing a second name for one program.
     */
    STOPWATCH("stopwatch")
}

/**
 * One row of the program list.
 *
 * [isAvailable] exists from the first entry although none of the first three ever clears it: a program
 * that later needs a switch or a hardware check sets this field instead of forcing the list to grow a
 * second concept.
 */
data class WearApp(
    val id: WearAppId,
    @StringRes val labelRes: Int,
    val isAvailable: Boolean = true
)
