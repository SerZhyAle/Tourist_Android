package com.sza.tourist.domain.usecase

import com.sza.tourist.domain.model.UnitSystem
import com.sza.tourist.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Reads the measurement system out of the settings, exposed to a screen through a use case instead
 * of injecting the repository into its ViewModel. [distinctUntilChanged] keeps an unrelated
 * settings write from re-emitting here.
 */
class ObserveUnitSystemUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(): Flow<UnitSystem> = settingsRepository.getSettings()
        .map { it.unitSystem }
        .distinctUntilChanged()
}
