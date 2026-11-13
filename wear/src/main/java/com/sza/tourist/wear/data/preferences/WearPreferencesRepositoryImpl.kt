package com.sza.tourist.wear.data.preferences

import com.sza.tourist.wear.data.preferences.sections.WearAppearancePreferencesImpl
import com.sza.tourist.wear.data.preferences.sections.WearMiniAppPreferencesImpl
import com.sza.tourist.wear.domain.repository.WearPreferencesRepository
import com.sza.tourist.wear.domain.repository.preferences.WearAppearancePreferences
import com.sza.tourist.wear.domain.repository.preferences.WearMiniAppPreferences
import javax.inject.Inject

/**
 * DataStore-based implementation of [WearPreferencesRepository].
 *
 * S2655: holds no member of its own - each themed section owns its settings and this class only
 * composes them. Every section reads and writes the single `wear_settings` store, handed to them as
 * [WearSettingsDataStore].
 *
 * Trimmed to the two sections the kit's screens read.
 */
class WearPreferencesRepositoryImpl @Inject constructor(
    appearance: WearAppearancePreferencesImpl,
    miniApps: WearMiniAppPreferencesImpl
) : WearPreferencesRepository,
    WearAppearancePreferences by appearance,
    WearMiniAppPreferences by miniApps
