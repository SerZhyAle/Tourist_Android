package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.repository.WearInteractiveStateRepository
import com.sza.tourist.wear.domain.repository.WearTouristRepository
import com.sza.tourist.wear.domain.tourist.WearTouristState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * S3007: use case observing live telemetry. The focused metric is screen state held by the view model,
 * not an input here.
 *
 * S0020: ambient is not an open screen (owner decision 2026-11-08). While the watch is not interactive nothing
 * is subscribed, so every sensor and location registration is released at once. The flow itself stays open, so
 * the screen keeps its last frame, and a wake subscribes afresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ObserveWearTouristDashboardUseCase @Inject constructor(
    private val touristRepository: WearTouristRepository,
    private val interactiveStateRepository: WearInteractiveStateRepository,
) {

    operator fun invoke(): Flow<WearTouristState> = interactiveStateRepository.observeInteractive()
        .flatMapLatest { interactive ->
            if (interactive) touristRepository.observeTelemetry() else emptyFlow()
        }
}
