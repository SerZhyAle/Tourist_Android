package com.sza.tourist.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Payload for pushing the phone's settings that the watch mirrors, phone to watch.
 * Fields mirror the setters in WearPreferencesRepository on the watch side.
 *
 * S1631: keys pinned - the watch reads this contract by its real field names.
 *
 * Trimmed to the one field this product pushes. FastMediaSorter's payload carries the media-type switches,
 * view modes, language, background, colour scheme, power-saving and playback settings of its Wear companion
 * screen, plus the timestamps and capability maps of the two-way exchange (S2093); none of those surfaces
 * exist here, and the unit system is the whole of the settings sync (handoff section 5).
 */
data class WearSettingsPayload(
    // S2731: name of the phone's UnitSystem. Nullable for the S1781 reason - a phone that predates it
    // omits the key, and only a nullable field lets the watch keep its own stored value instead of
    // reading the absence as a system choice the owner never made.
    @SerializedName("unitSystem") val unitSystem: String? = null,
)
