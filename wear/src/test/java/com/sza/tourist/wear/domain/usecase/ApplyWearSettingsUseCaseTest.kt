package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.model.UnitSystem
import com.sza.tourist.wear.domain.model.WearSettingsPayload
import com.sza.tourist.wear.domain.repository.WearPreferencesRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Trimmed from FastMediaSorter's test of the same name to the one setting this product applies, the unit
 * system. The source test drives a fake of that product's whole preferences repository through every
 * setting of its settings push; none of those settings exists here.
 */
class ApplyWearSettingsUseCaseTest {

    private val repository: WearPreferencesRepository = mockk(relaxed = true)
    private val useCase = ApplyWearSettingsUseCase(repository)

    @Test
    fun `a payload carrying unitSystem applies it`() = runTest {
        useCase(WearSettingsPayload(unitSystem = "IMPERIAL"))

        coVerify(exactly = 1) { repository.setUnitSystem(UnitSystem.IMPERIAL) }
    }

    @Test
    fun `null unitSystem in payload leaves the watch value untouched`() = runTest {
        // S1781: absence must not change the stored value - the S2462 gate reads it as "not sent".
        useCase(WearSettingsPayload(unitSystem = null))

        coVerify(exactly = 0) { repository.setUnitSystem(any()) }
    }

    @Test
    fun `a unitSystem the decoder did not report present is never applied`() = runTest {
        // Gson fabricates a value for an omitted key, so the value alone cannot say it arrived.
        useCase(WearSettingsPayload(unitSystem = "IMPERIAL"), presentFields = emptySet())

        coVerify(exactly = 0) { repository.setUnitSystem(any()) }
    }

    @Test
    fun `an unknown unitSystem name resolves to the default instead of throwing`() = runTest {
        // Settings arrive over the wire from a phone that may be newer than this watch.
        useCase(WearSettingsPayload(unitSystem = "NAUTICAL"))

        coVerify(exactly = 1) { repository.setUnitSystem(UnitSystem.DEFAULT) }
    }

    @Test
    fun `metric is applied like any other system`() = runTest {
        useCase(WearSettingsPayload(unitSystem = "METRIC"))

        coVerify(exactly = 1) { repository.setUnitSystem(UnitSystem.METRIC) }
    }
}
