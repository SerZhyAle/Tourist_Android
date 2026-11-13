package com.sza.tourist.wear.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Every key of the `wear_settings` store.
 *
 * S2655: moved out of `WearPreferencesRepositoryImpl` when the settings were split by theme - a key
 * set nested in one section would make the other sections depend on that section rather than on the keys.
 *
 * Trimmed to the kit: the key names are FastMediaSorter's own, so a stored value reads the same on both
 * products; the keys of the media, playback, browse, document and settings-sync sections are not part of
 * this product.
 */
internal object WearPreferenceKeys {

    /** S2731: the measurement system the phone pushed; metric when absent. */
    val WEAR_UNIT_SYSTEM = stringPreferencesKey("wear_unit_system")

    /**
     * S2825: the stopwatch's durable fields - how many participants the screen splits into, and the text
     * of the last finished measurement.
     *
     * S3555 adds the measurement itself. S2825 left it out because its monotonic start instant does not
     * survive a reboot; the stored snapshot carries the wall clock and the boot count beside it, which is
     * what lets `WearStopwatchSnapshot` carry a running start across a reboot instead of promising a
     * continuity the clock cannot keep. It is needed at all because the ongoing-activity indicator outlives
     * the process, and must never reopen a reset stopwatch.
     */
    val STOPWATCH_PARTICIPANT_COUNT = intPreferencesKey("wear_stopwatch_participant_count")
    val STOPWATCH_LAST_RESULT = stringPreferencesKey("wear_stopwatch_last_result")
    val STOPWATCH_SESSION = stringPreferencesKey("wear_stopwatch_session")

    /** S4146: the Tourist trip and its pressure samples, opaque to the store. */
    val TOURIST_TRIP = stringPreferencesKey("wear_tourist_trip")
}
