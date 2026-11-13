package com.sza.tourist.core.capability

import com.sza.tourist.data.tourist.WatchHealthRepositoryImpl
import com.sza.tourist.domain.model.tourist.WatchHealthReading
import com.sza.tourist.domain.model.tourist.WatchHealthStats
import com.sza.tourist.domain.usecase.tourist.ObserveWatchTouristHealthUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The noLegal edition's side of owner decision 4.3 (S0008): the noLegal watch carries the Tourist
 * dashboard and publishes its heart rate and body temperature, so the phone offers the two watch tiles.
 * The mirror of `StoreWatchHealthCapabilityTest`.
 */
class NoLegalWatchHealthCapabilityTest {

    @Test
    fun `the noLegal edition offers the watch health tiles`() {
        assertTrue("watch health tiles", NoLegalWatchHealthCapability().isOffered)
    }

    @Test
    fun `a report reaching a noLegal phone is shown`() = runTest {
        val repository = WatchHealthRepositoryImpl()
        repository.publish(WatchHealthReading(72, 36.5f, System.currentTimeMillis()))

        val stats = ObserveWatchTouristHealthUseCase(repository, NoLegalWatchHealthCapability())().first()

        assertEquals(WatchHealthStats(offered = true, heartRateBpm = 72, bodyTemperatureCelsius = 36.5f), stats)
    }
}
