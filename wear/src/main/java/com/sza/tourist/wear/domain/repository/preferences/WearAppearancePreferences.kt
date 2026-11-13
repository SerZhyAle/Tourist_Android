package com.sza.tourist.wear.domain.repository.preferences

import com.sza.tourist.wear.domain.model.UnitSystem
import kotlinx.coroutines.flow.Flow

/** How the app reads values. Trimmed to the unit system, the one appearance setting the kit stores. */
interface WearAppearancePreferences {

    /**
     * S2731: measurement system inherited from the phone companion. Read by the Tourist dashboard and the
     * clock formatter; written only by the phone's settings push.
     */
    val unitSystem: Flow<UnitSystem>
    suspend fun setUnitSystem(system: UnitSystem)
}
