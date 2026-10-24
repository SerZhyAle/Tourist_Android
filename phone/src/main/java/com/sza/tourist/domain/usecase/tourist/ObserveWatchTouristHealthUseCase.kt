package com.sza.tourist.domain.usecase.tourist

import com.sza.tourist.core.capability.CapabilityAvailability
import com.sza.tourist.domain.model.tourist.WatchHealthStats
import com.sza.tourist.domain.model.tourist.toStats
import com.sza.tourist.domain.repository.WatchHealthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject

/**
 * The watch heart rate and body temperature as the Tourist dashboard shows them.
 *
 * The ticker re-judges freshness while no report arrives, so a reading disappears once the watch stops
 * sending instead of staying on screen. A build without the watch-health capability gets one empty
 * answer and keeps no ticker running. Until S0008 wires the Data Layer, this product's capability
 * answer is "not offered" and the two watch tiles explain themselves in the grid.
 */
class ObserveWatchTouristHealthUseCase @Inject constructor(
    private val repository: WatchHealthRepository,
    private val capabilityAvailability: CapabilityAvailability,
) {

    operator fun invoke(): Flow<WatchHealthStats> {
        val offered = capabilityAvailability.isWatchHealthAvailable()
        if (!offered) return flowOf(WatchHealthStats())
        return combine(repository.latest, clockTicks()) { reading, now -> reading.toStats(offered = true, now) }
            .distinctUntilChanged()
    }

    private fun clockTicks(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(FRESHNESS_CHECK_INTERVAL_MS)
        }
    }

    private companion object {
        const val FRESHNESS_CHECK_INTERVAL_MS = 5_000L
    }
}
