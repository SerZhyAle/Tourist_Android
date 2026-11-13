package com.sza.tourist.wear.data.preferences.sections

import androidx.datastore.preferences.core.edit
import com.sza.tourist.wear.data.preferences.WearPreferenceKeys
import com.sza.tourist.wear.data.preferences.WearPreferenceSection
import com.sza.tourist.wear.data.preferences.WearSettingsDataStore
import com.sza.tourist.wear.domain.model.UnitSystem
import com.sza.tourist.wear.domain.repository.preferences.WearAppearancePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trimmed to the unit system: the background, colour scheme, geometry, animations, power-saving, rotation
 * and language settings of FastMediaSorter's section belong to surfaces this product does not carry.
 */
@Singleton
class WearAppearancePreferencesImpl @Inject constructor(
    settings: WearSettingsDataStore
) : WearPreferenceSection(settings), WearAppearancePreferences {

    override val unitSystem: Flow<UnitSystem> = store.data.map { prefs ->
        UnitSystem.fromNameOrDefault(prefs[WearPreferenceKeys.WEAR_UNIT_SYSTEM])
    }

    override suspend fun setUnitSystem(system: UnitSystem) {
        store.edit { prefs ->
            prefs[WearPreferenceKeys.WEAR_UNIT_SYSTEM] = system.name
        }
    }
}
