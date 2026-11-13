package com.sza.tourist.wear.data.preferences.sections

import androidx.datastore.preferences.core.edit
import com.sza.tourist.wear.data.preferences.WearPreferenceKeys
import com.sza.tourist.wear.data.preferences.WearPreferenceSection
import com.sza.tourist.wear.data.preferences.WearSettingsDataStore
import com.sza.tourist.wear.domain.repository.preferences.WearMiniAppPreferences
import com.sza.tourist.wear.domain.stopwatch.WearStopwatchState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trimmed to the stopwatch and the Tourist trip: the calculator and game state of FastMediaSorter's
 * section belong to programs this product does not carry.
 */
@Singleton
class WearMiniAppPreferencesImpl @Inject constructor(
    settings: WearSettingsDataStore
) : WearPreferenceSection(settings), WearMiniAppPreferences {

    // S2825: snapped on the way out rather than on the way in, so a count written by an older build
    // still opens a screen instead of leaving the reader with a region the layout cannot draw.
    override val stopwatchParticipantCount: Flow<Int> = store.data.map { prefs ->
        WearStopwatchState.snapCount(
            prefs[WearPreferenceKeys.STOPWATCH_PARTICIPANT_COUNT] ?: WearStopwatchState.DEFAULT_COUNT
        )
    }

    override suspend fun setStopwatchParticipantCount(count: Int) {
        store.edit { prefs ->
            prefs[WearPreferenceKeys.STOPWATCH_PARTICIPANT_COUNT] = WearStopwatchState.snapCount(count)
        }
    }

    override val stopwatchLastResult: Flow<String?> = store.data.map { prefs ->
        prefs[WearPreferenceKeys.STOPWATCH_LAST_RESULT]
    }

    override suspend fun setStopwatchLastResult(value: String?) {
        writeNullableString(WearPreferenceKeys.STOPWATCH_LAST_RESULT, value)
    }

    // S3555: opaque - the codec rejects an unreadable snapshot, not this layer.
    override val stopwatchSession: Flow<String?> = store.data.map { prefs ->
        prefs[WearPreferenceKeys.STOPWATCH_SESSION]
    }

    override suspend fun setStopwatchSession(value: String?) {
        writeNullableString(WearPreferenceKeys.STOPWATCH_SESSION, value)
    }

    override val touristTrip: Flow<String?> = store.data.map { prefs ->
        prefs[WearPreferenceKeys.TOURIST_TRIP]
    }

    override suspend fun setTouristTrip(value: String?) {
        writeNullableString(WearPreferenceKeys.TOURIST_TRIP, value)
    }
}
