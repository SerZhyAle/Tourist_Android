package com.sza.tourist.domain.repository

import com.sza.tourist.domain.model.tourist.WatchHealthReading
import kotlinx.coroutines.flow.StateFlow

/**
 * The last Tourist health report received from the watch. Filled by the companion bridge, which arrives
 * with the Data Layer port (S0008); read by the Tourist dashboard, which does not depend on that bridge.
 */
interface WatchHealthRepository {

    val latest: StateFlow<WatchHealthReading?>

    fun publish(reading: WatchHealthReading)
}
