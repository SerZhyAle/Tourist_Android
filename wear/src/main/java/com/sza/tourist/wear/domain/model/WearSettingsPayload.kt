package com.sza.tourist.wear.domain.model

/**
 * Payload for receiving the settings the paired phone pushes to this watch.
 *
 * Trimmed to the one field this product pushes. FastMediaSorter's payload carries the media-type,
 * slideshow, view-mode, language, background, colour-scheme, animation, power-saving and playback
 * settings of its Wear companion screen plus the field timestamps of its two-way exchange; none of
 * those surfaces is part of this product, and the push is one way (phone to watch).
 *
 * The key name is the wire contract: the phone's copy of this class serialises the same name, and the
 * two modules share no source. Gson builds the class reflectively, so a key the phone omitted arrives
 * as null.
 */
data class WearSettingsPayload(
    // S2731: name of the phone's UnitSystem. Nullable for the S1781 reason - a phone that predates it
    // omits the key, and only a nullable field lets the watch keep its own stored value instead of
    // reading the absence as a system choice the owner never made.
    val unitSystem: String? = null
)
