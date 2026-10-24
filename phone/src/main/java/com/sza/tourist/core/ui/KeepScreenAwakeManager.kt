package com.sza.tourist.core.ui

import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Owns the settings dependency that BaseActivity would otherwise hold as a repository field.
 *
 * A UI host must not reference the data layer directly; this manager holds that reference instead and
 * republishes the stream, so BaseActivity and every subclass read settings without naming a repository.
 */
class KeepScreenAwakeManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    val settings: Flow<AppSettings> get() = settingsRepository.getSettings()
}
