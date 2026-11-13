package com.sza.tourist.wear.domain.usecase

import android.content.Context
import com.google.gson.Gson
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Owner decision 4.3 (S0008): the Tourist dashboard is sideload-only, so the build that withholds health
 * features must not reach the Data Layer with a heart-rate report either - the store declaration does not
 * depend on the screen staying unreachable.
 */
class PublishTouristHealthUseCaseTest {

    private val context = mockk<Context>()

    private fun capabilities(offersHealth: Boolean) = object : WearRestrictedCapabilities {
        override val locksSystemShade: Boolean = false
        override val offersHealthFeatures: Boolean = offersHealth
        override val offersScreenTakeoverPrograms: Boolean = true
    }

    @Test
    fun `a build that withholds health features sends nothing`() = runTest {
        val useCase = PublishTouristHealthUseCase(context, Gson(), capabilities(offersHealth = false))

        useCase(heartRateBpm = 72, bodyTemperatureCelsius = 36.6f)

        verify { context wasNot Called }
    }
}
