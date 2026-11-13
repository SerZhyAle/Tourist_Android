package com.sza.tourist.wear.data.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

/**
 * S2655: the shared half of every themed settings section - the one store and the write helper.
 *
 * A base class rather than a copy per section so the null-clears-the-key rule below is written once.
 *
 * Trimmed to the kit: FastMediaSorter's `stampedEdit` (the S2093 per-field timestamps of the two-way
 * settings exchange) is not carried, because this product's only synced setting is the unit system and it
 * travels one way, phone to watch.
 */
abstract class WearPreferenceSection(settings: WearSettingsDataStore) {

    protected val store = settings.store

    // A null clears the key rather than writing an empty string, so "never chosen" and "chosen, then
    // cleared" leave the same state.
    protected suspend fun writeNullableString(key: Preferences.Key<String>, value: String?) {
        store.edit { prefs ->
            if (value == null) {
                prefs.remove(key)
            } else {
                prefs[key] = value
            }
        }
    }
}
