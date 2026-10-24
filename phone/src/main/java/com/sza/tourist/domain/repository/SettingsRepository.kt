package com.sza.tourist.domain.repository

import com.sza.tourist.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Read access to the product's stored preferences, as far as the ported core needs it.
 *
 * Deliberately narrower than FastMediaSorter's repository of the same name: the write side and every
 * other preference arrive with the phase that ports a class using them. It has no implementation yet,
 * so nothing may inject it into a graph that is built before the phone shell binds one.
 */
interface SettingsRepository {

    fun getSettings(): Flow<AppSettings>
}
