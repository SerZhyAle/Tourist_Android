package com.sza.tourist.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.model.UnitSystem
import com.sza.tourist.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.getSettings().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setUnitSystem(value: UnitSystem) = edit { it.copy(unitSystem = value) }
    fun setPreventSleep(value: Boolean) = edit { it.copy(preventSleep = value) }

    private fun edit(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }
}
