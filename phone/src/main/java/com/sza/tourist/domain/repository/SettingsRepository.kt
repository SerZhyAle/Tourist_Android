package com.sza.tourist.domain.repository

import com.sza.tourist.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Read and edit access to the product's stored preferences, as far as the ported core needs it.
 *
 * Deliberately narrower than FastMediaSorter's repository of the same name: every other preference
 * arrives with the phase that ports a class using it.
 */
interface SettingsRepository {

    fun getSettings(): Flow<AppSettings>

    /**
     * Applies [edit] to the current settings and persists the result. The read-modify-write is one
     * atomic step, so two concurrent writers cannot lose each other's field.
     */
    suspend fun updateSettings(edit: (AppSettings) -> AppSettings)
}
