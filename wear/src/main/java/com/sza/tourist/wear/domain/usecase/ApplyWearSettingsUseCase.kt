package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.model.UnitSystem
import com.sza.tourist.wear.domain.model.WearSettingsPayload
import com.sza.tourist.wear.domain.model.WearSettingsPayloadDecoder
import com.sza.tourist.wear.domain.repository.WearPreferencesRepository
import javax.inject.Inject

/**
 * Applies the settings the paired phone pushed to this watch.
 *
 * Trimmed to the one setting this product exchanges, the unit system. FastMediaSorter's version also
 * applies the media-type, slideshow, view-mode, language, background, colour-scheme, animation,
 * power-saving and playback settings, and merges each against the watch's own edit time so a later edit on
 * either side wins (S2093); none of those settings exists here, and this push is one way, so there is no
 * watch edit to lose to.
 */
class ApplyWearSettingsUseCase @Inject constructor(
    private val preferencesRepository: WearPreferencesRepository
) {

    /**
     * @param presentFields S2462: the contract keys the phone actually carried, from
     *   [WearSettingsPayloadDecoder]. A field outside this set is left alone rather than applied, because
     *   the payload object cannot express its absence - Gson builds the class reflectively, so an omitted
     *   key must never be read off it. Defaults to the whole contract, which is the behaviour of a caller
     *   that holds no decode result.
     */
    suspend operator fun invoke(
        payload: WearSettingsPayload,
        presentFields: Set<String> = WearSettingsPayloadDecoder.CONTRACT_FIELDS
    ) {
        applyUnitSystem(payload, presentFields)
    }

    // S2731: inherited rather than merged - no watch surface edits the unit system, so the watch can
    // never hold a later value than the phone's.
    //
    // S1781: a null incoming value means "the other side did not send this" and never "reset it", so an
    // older phone cannot silently undo the stored choice.
    private suspend fun applyUnitSystem(payload: WearSettingsPayload, presentFields: Set<String>) {
        val raw = payload.unitSystem?.takeIf { UNIT_SYSTEM_FIELD in presentFields } ?: return
        preferencesRepository.setUnitSystem(UnitSystem.fromNameOrDefault(raw))
    }

    private companion object {
        const val UNIT_SYSTEM_FIELD = "unitSystem"
    }
}
