package com.sza.tourist.core.capability

import com.sza.tourist.data.tourist.WatchHealthRepositoryImpl
import com.sza.tourist.domain.model.tourist.WatchHealthReading
import com.sza.tourist.domain.model.tourist.WatchHealthStats
import com.sza.tourist.domain.usecase.tourist.ObserveWatchTouristHealthUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The store edition's side of owner decision 4.3 (S0008): the store watch has no Tourist dashboard and
 * sends no health report, so the phone does not offer the two watch tiles - whatever a stray report says.
 * The mirror of `NoLegalWatchHealthCapabilityTest`.
 */
class StoreWatchHealthCapabilityTest {

    @Test
    fun `the store edition does not offer the watch health tiles`() {
        assertFalse("watch health tiles", StoreWatchHealthCapability().isOffered)
    }

    @Test
    fun `a report reaching a store phone is never shown`() = runTest {
        val repository = WatchHealthRepositoryImpl()
        repository.publish(WatchHealthReading(72, 36.5f, System.currentTimeMillis()))

        val stats = ObserveWatchTouristHealthUseCase(repository, StoreWatchHealthCapability())().first()

        assertEquals(WatchHealthStats(offered = false), stats)
    }
}
