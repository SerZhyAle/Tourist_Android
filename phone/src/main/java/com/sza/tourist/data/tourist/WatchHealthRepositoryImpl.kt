package com.sza.tourist.data.tourist

import com.sza.tourist.domain.model.tourist.WatchHealthReading
import com.sza.tourist.domain.repository.WatchHealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The phone-side holder of the last watch health report, until S0008 ports the Data Layer bridge that
 * fills it. The dashboard reads the interface, never this class, so the bridge lands behind the same
 * seam.
 */
@Singleton
class WatchHealthRepositoryImpl @Inject constructor() : WatchHealthRepository {

    private val state = MutableStateFlow<WatchHealthReading?>(null)

    override val latest: StateFlow<WatchHealthReading?> = state.asStateFlow()

    override fun publish(reading: WatchHealthReading) {
        state.value = reading
    }
}
