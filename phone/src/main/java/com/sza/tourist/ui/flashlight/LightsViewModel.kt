package com.sza.tourist.ui.flashlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The lights program's settings surface: renders the shade-shortcut switch from the store and writes
 * its flips back. The three launch rows are stateless and never reach this model.
 */
@HiltViewModel
class LightsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AppSettings(),
        )

    fun setFlashlightShortcutEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(flashlightShortcutNotificationEnabled = enabled) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
