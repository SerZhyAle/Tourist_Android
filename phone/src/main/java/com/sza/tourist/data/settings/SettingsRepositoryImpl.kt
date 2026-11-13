package com.sza.tourist.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.model.UnitSystem
import com.sza.tourist.domain.model.sos.SosMode
import com.sza.tourist.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The product's own persisted settings: a small preferences file read once, on first use and off the
 * main thread; afterwards the in-memory state is the truth.
 *
 * Keys follow FastMediaSorter's names (`unit_system`, `prevent_sleep`, `front_flashlight_color`,
 * `flashlight_shortcut_notification_enabled`) so a stored value keeps its meaning; the store itself is
 * this product's - no DataStore, which the sorter reads its settings from.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val mutex = Mutex()
    private val current = MutableStateFlow(AppSettings())

    @Volatile
    private var loaded = false

    override fun getSettings(): Flow<AppSettings> = flow {
        ensureLoaded()
        emitAll(current)
    }

    override suspend fun updateSettings(edit: (AppSettings) -> AppSettings) {
        mutex.withLock {
            if (!loaded) {
                current.value = withContext(Dispatchers.IO) { load() }
                loaded = true
            }
            val updated = edit(current.value)
            withContext(Dispatchers.IO) { persist(updated) }
            current.value = updated
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            current.value = withContext(Dispatchers.IO) { load() }
            loaded = true
        }
    }

    private fun load(): AppSettings = StopwatchSettingsStore.applyTo(
        AppSettings(
            unitSystem = UnitSystem.fromNameOrDefault(prefs.getString(KEY_UNIT_SYSTEM, null)),
            preventSleep = prefs.getBoolean(KEY_PREVENT_SLEEP, true),
            frontFlashlightColor = prefs.getInt(
                KEY_FRONT_FLASHLIGHT_COLOR,
                AppSettings.FRONT_FLASHLIGHT_DEFAULT_COLOR,
            ),
            flashlightShortcutNotificationEnabled =
            prefs.getBoolean(KEY_FLASHLIGHT_SHORTCUT_NOTIFICATION, false),
            sosMode = SosMode.fromNameOrDefault(prefs.getString(KEY_SOS_MODE, null)),
        ),
        StopwatchSettingsStore.read(prefs),
    )

    private fun persist(settings: AppSettings) {
        val editor = prefs.edit()
            .putString(KEY_UNIT_SYSTEM, settings.unitSystem.name)
            .putBoolean(KEY_PREVENT_SLEEP, settings.preventSleep)
            .putInt(KEY_FRONT_FLASHLIGHT_COLOR, settings.frontFlashlightColor)
            .putBoolean(
                KEY_FLASHLIGHT_SHORTCUT_NOTIFICATION,
                settings.flashlightShortcutNotificationEnabled,
            )
            .putString(KEY_SOS_MODE, settings.sosMode.name)
        StopwatchSettingsStore.write(editor, settings)
        editor.apply()
    }

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_UNIT_SYSTEM = "unit_system"
        const val KEY_PREVENT_SLEEP = "prevent_sleep"
        const val KEY_FRONT_FLASHLIGHT_COLOR = "front_flashlight_color"
        const val KEY_FLASHLIGHT_SHORTCUT_NOTIFICATION = "flashlight_shortcut_notification_enabled"
        const val KEY_SOS_MODE = "sos_mode"
    }
}
