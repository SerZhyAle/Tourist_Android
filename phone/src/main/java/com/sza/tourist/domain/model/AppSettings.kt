package com.sza.tourist.domain.model

import com.sza.tourist.domain.model.sos.SosMode

/**
 * The product's stored preferences, as far as the ported core reads them.
 *
 * This is the seam, not a copy of FastMediaSorter's sorter-wide settings record: it holds only what a
 * ported class reads today and grows with each phase that ports a reader of another preference.
 * Persisted by [com.sza.tourist.data.settings.SettingsRepositoryImpl] in the `settings` preferences
 * file: [unitSystem], [preventSleep] since the phone shell, [frontFlashlightColor] and
 * [flashlightShortcutNotificationEnabled] since the lights phase, and [sosMode] since the SOS phase.
 * The source's program switches (`front_flashlight_enabled`, `water_flashlight_enabled`, `enable_sos`)
 * are dropped: the programs are always offered on the start screen here, so the switch has nothing to
 * gate.
 */
data class AppSettings(
    // The measurement system every unit-bearing surface reads. Metric by default.
    val unitSystem: UnitSystem = UnitSystem.DEFAULT,
    val preventSleep: Boolean = true,
    // ARGB of the front flashlight's glow. White is the start state; the picker writes any other
    // choice here and it survives the run.
    val frontFlashlightColor: Int = FRONT_FLASHLIGHT_DEFAULT_COLOR,
    // The camera flashlight has no switch of its own - it is offered wherever the device has a flash -
    // so its shade shortcut needs one here, and a permanent notification nobody asked for is a defect
    // rather than a service. Off until the user flips the row on the lights screen.
    val flashlightShortcutNotificationEnabled: Boolean = false,
    // The mode the distress signal last ran in, which is also the mode the next one starts in.
    val sosMode: SosMode = SosMode.ALL,
    // Stopwatch options persisted across sessions.
    val stopwatchParticipantCount: Int = STOPWATCH_DEFAULT_PARTICIPANTS,
    val stopwatchMusicEnabled: Boolean = false,
    val stopwatchMusicUri: String = "",
    val stopwatchVolumeKeysControl: Boolean = true,
) {
    companion object {
        /** Opaque white - the flashlight starts as a plain white lamp until a colour is picked. */
        const val FRONT_FLASHLIGHT_DEFAULT_COLOR: Int = 0xFFFFFFFF.toInt()
        const val STOPWATCH_DEFAULT_PARTICIPANTS: Int = 1
        val STOPWATCH_PARTICIPANT_OPTIONS: List<Int> = listOf(1, 2, 4)
    }
}
