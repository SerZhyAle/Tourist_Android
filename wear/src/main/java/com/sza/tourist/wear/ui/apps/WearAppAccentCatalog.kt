package com.sza.tourist.wear.ui.apps

import androidx.annotation.ColorRes
import com.sza.tourist.wear.R
import com.sza.tourist.wear.domain.model.WearAppId

/**
 * The single place on the watch that knows what colour a program is.
 *
 * S2510: the phone half is the phone's program accent catalog. The two modules share no source, so the
 * entire contract between them is the resource NAME - each declares the same `color_program_accent_*`
 * keys and tunes its own values, because the watch screen is dark and may carry a user photo under a scrim,
 * where a tone picked for a light phone surface either glares or disappears.
 *
 * Keyed on [WearAppId] rather than on its `canonicalKey` deliberately: the watch owns which programs
 * it shows.
 *
 * S3434: the watch-only tones (brown, emerald, scarlet) are kept as declared product tones, not drift
 * from a shared palette.
 *
 * Exhaustive with no else branch on purpose: a fifth watch program must fail compilation here rather
 * than silently inherit a default colour.
 */
object WearAppAccentCatalog {

    @ColorRes
    fun accentFor(id: WearAppId): Int = when (id) {
        // S2516: amber, the tone the phone gives the whole flashlight family - the one program of this
        // list that exists on both devices, so it is also the one whose colour must not diverge.
        WearAppId.WATER_FLASHLIGHT -> R.color.color_program_accent_amber
        // S2825: brown, the eleventh name. The phone gives the stopwatch orange because it sits beside
        // the calculator there; here the list is short and a repeat is two adjacent rows looking alike.
        WearAppId.STOPWATCH -> R.color.color_program_accent_brown
        // S3007: emerald green, distinct from the other program accents.
        WearAppId.TOURIST -> R.color.color_program_accent_emerald
        // S3216: scarlet, the fourteenth name. The phone gives this program its plain red; the value
        // diverges by necessity, like the stopwatch's brown above.
        WearAppId.SOS -> R.color.color_program_accent_scarlet
    }
}
