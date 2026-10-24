package com.sza.tourist.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.model.UnitSystem
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
 * The product's own persisted settings, the implementation the core seam
 * [SettingsRepository] has needed since S0003: a small preferences file read once, on first use and
 * off the main thread; afterwards the in-memory state is the truth.
 *
 * Keys follow FastMediaSorter's names (`unit_system`, `prevent_sleep`) so a stored value keeps its
 * meaning; the store itself is this product's - no DataStore, which the sorter reads its settings from,
 * and no write side yet: the phase that gains a settings screen adds it.
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

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            current.value = withContext(Dispatchers.IO) { load() }
            loaded = true
        }
    }

    private fun load(): AppSettings = AppSettings(
        unitSystem = UnitSystem.fromNameOrDefault(prefs.getString(KEY_UNIT_SYSTEM, null)),
        preventSleep = prefs.getBoolean(KEY_PREVENT_SLEEP, true),
    )

    private companion object {
        const val PREFS_NAME = "settings"
        const val KEY_UNIT_SYSTEM = "unit_system"
        const val KEY_PREVENT_SLEEP = "prevent_sleep"
    }
}
