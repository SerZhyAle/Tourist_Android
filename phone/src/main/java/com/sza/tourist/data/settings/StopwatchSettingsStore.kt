package com.sza.tourist.data.settings

import android.content.SharedPreferences
import com.sza.tourist.domain.model.AppSettings

/**
 * Owns persistence of the stopwatch tool's own options.
 *
 * Adapted from FastMediaSorter's DataStore sub-store to the `SharedPreferences` backing
 * [SettingsRepositoryImpl] while keeping the exact key names and defaults.
 */
object StopwatchSettingsStore {

    private const val KEY_PARTICIPANT_COUNT = "stopwatch_participant_count"
    private const val KEY_MUSIC_ENABLED = "stopwatch_music_enabled"
    private const val KEY_MUSIC_URI = "stopwatch_music_uri"
    private const val KEY_VOLUME_KEYS_CONTROL = "stopwatch_volume_keys_control"

    /** Stopwatch fields read from preferences, ready for [AppSettings]. */
    data class Values(
        val participantCount: Int,
        val musicEnabled: Boolean,
        val musicUri: String,
        val volumeKeysControl: Boolean,
    )

    fun read(preferences: SharedPreferences): Values = Values(
        participantCount = preferences.getInt(
            KEY_PARTICIPANT_COUNT,
            AppSettings.STOPWATCH_DEFAULT_PARTICIPANTS,
        ),
        musicEnabled = preferences.getBoolean(KEY_MUSIC_ENABLED, false),
        musicUri = preferences.getString(KEY_MUSIC_URI, null) ?: "",
        // The one default here that is not the quiet option: the volume keys drive the measurement,
        // and the switch exists to give them back, not to hand them over.
        volumeKeysControl = preferences.getBoolean(KEY_VOLUME_KEYS_CONTROL, true),
    )

    /** Applies the group onto [settings] instead of having the repository restate every field name. */
    fun applyTo(settings: AppSettings, values: Values): AppSettings = settings.copy(
        stopwatchParticipantCount = values.participantCount,
        stopwatchMusicEnabled = values.musicEnabled,
        stopwatchMusicUri = values.musicUri,
        stopwatchVolumeKeysControl = values.volumeKeysControl,
    )

    fun write(editor: SharedPreferences.Editor, settings: AppSettings) {
        editor
            .putInt(KEY_PARTICIPANT_COUNT, settings.stopwatchParticipantCount)
            .putBoolean(KEY_MUSIC_ENABLED, settings.stopwatchMusicEnabled)
            .putString(KEY_MUSIC_URI, settings.stopwatchMusicUri)
            .putBoolean(KEY_VOLUME_KEYS_CONTROL, settings.stopwatchVolumeKeysControl)
    }
}
