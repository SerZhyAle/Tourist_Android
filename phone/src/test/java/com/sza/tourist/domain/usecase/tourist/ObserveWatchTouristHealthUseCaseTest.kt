package com.sza.tourist.domain.usecase.tourist

import com.sza.tourist.core.capability.WatchHealthCapability
import com.sza.tourist.data.tourist.WatchHealthRepositoryImpl
import com.sza.tourist.domain.model.tourist.WatchHealthReading
import com.sza.tourist.domain.model.tourist.WatchHealthStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * S0008: the tiles follow the build's watch-health answer. The per-variant answers themselves are pinned in
 * the `testStore` and `testNoLegal` source sets; this test fixes what the use case does with either one.
 */
class ObserveWatchTouristHealthUseCaseTest {

    private val repository = WatchHealthRepositoryImpl()

    private fun capability(offered: Boolean) = object : WatchHealthCapability {
        override val isOffered: Boolean = offered
    }

    @Test
    fun `a build that does not offer the tiles answers once with an empty, not offered state`() = runTest {
        repository.publish(WatchHealthReading(72, 36.5f, System.currentTimeMillis()))

        val stats = ObserveWatchTouristHealthUseCase(repository, capability(offered = false))().first()

        assertEquals(WatchHealthStats(offered = false), stats)
    }

    @Test
    fun `a build that offers the tiles shows a fresh reading`() = runTest {
        repository.publish(WatchHealthReading(72, 36.5f, System.currentTimeMillis()))

        val stats = ObserveWatchTouristHealthUseCase(repository, capability(offered = true))().first()

        assertEquals(WatchHealthStats(offered = true, heartRateBpm = 72, bodyTemperatureCelsius = 36.5f), stats)
    }

    @Test
    fun `a build that offers the tiles shows them empty until the watch has reported`() = runTest {
        val stats = ObserveWatchTouristHealthUseCase(repository, capability(offered = true))().first()

        assertEquals(WatchHealthStats(offered = true), stats)
    }
}
