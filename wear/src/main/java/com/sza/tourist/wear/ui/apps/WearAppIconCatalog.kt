package com.sza.tourist.wear.ui.apps

import androidx.annotation.DrawableRes
import com.sza.tourist.wear.R
import com.sza.tourist.wear.domain.model.WearAppId

/**
 * The single place on the watch that knows which glyph a program wears.
 *
 * S2511 lifted this table out of `AppsScreen.kt`, where it was private to the composable file. Sits beside
 * [WearAppAccentCatalog], which answers the same shape of question about colour.
 *
 * Icons live here rather than on the program record so the domain layer carries no drawing concern.
 *
 * S2474: every program on the watch reuses the exact same icon as its counterpart in the phone app,
 * ensuring visual recognition across both form factors.
 *
 * Exhaustive with no else branch on purpose: a fifth watch program must fail compilation here rather than
 * silently inherit some default glyph.
 */
object WearAppIconCatalog {

    @DrawableRes
    fun iconFor(id: WearAppId): Int = when (id) {
        WearAppId.WATER_FLASHLIGHT -> R.drawable.ic_water_flashlight
        // S2825: the phone's own stopwatch glyph, copied name-for-name rather than redrawn.
        WearAppId.STOPWATCH -> R.drawable.ic_stopwatch
        // S3007: the phone's own tourist glyph, copied name-for-name.
        WearAppId.TOURIST -> R.drawable.ic_tourist
        // S3216: the phone's own SOS glyph, copied name-for-name - one program on two devices.
        WearAppId.SOS -> R.drawable.ic_sos
    }
}
