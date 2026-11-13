package com.sza.tourist.data.wear

import com.google.gson.Gson
import com.sza.tourist.domain.model.UnitSystem
import com.sza.tourist.domain.model.WearEventEnvelope
import com.sza.tourist.domain.model.WearNode
import com.sza.tourist.domain.repository.WearableDataLayerRepository
import com.sza.tourist.domain.usecase.ObserveUnitSystemUseCase
import com.sza.tourist.domain.usecase.PushWearSettingsUseCase
import com.sza.tourist.service.WearDataLayerPaths
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * S0008: the unit-system push. New in this product - FastMediaSorter pushes from a companion-screen button.
 * Runs the ported [PushWearSettingsUseCase] over a mocked bridge rather than mocking the use case, whose
 * `Result` return does not mock cleanly.
 */
class WearUnitSystemSyncCoordinatorTest {

    private val observeUnitSystem = mockk<ObserveUnitSystemUseCase>()
    private val bridge = mockk<WearableDataLayerRepository>()
    private val pushWearSettings = PushWearSettingsUseCase(bridge, Gson())
    private val sent = mutableListOf<Pair<String, String>>()

    private fun recordSends() {
        coEvery { bridge.putEnvelopeDataItem(any(), any()) } answers {
            val envelope = secondArg<WearEventEnvelope>()
            sent += firstArg<String>() to String(envelope.data, Charsets.UTF_8)
        }
    }

    @Test
    fun `the current system is pushed at start and every change after it`() = runTest {
        every { observeUnitSystem() } returns flowOf(UnitSystem.METRIC, UnitSystem.IMPERIAL)
        coEvery { bridge.getConnectedNodes() } returns listOf(WearNode("node-1", "Watch"))
        recordSends()

        WearUnitSystemSyncCoordinator(observeUnitSystem, pushWearSettings, this).start()
        advanceUntilIdle()

        assertEquals(
            listOf(
                WearDataLayerPaths.SETTINGS_PUSH to """{"unitSystem":"METRIC"}""",
                WearDataLayerPaths.SETTINGS_PUSH to """{"unitSystem":"IMPERIAL"}""",
            ),
            sent,
        )
    }

    @Test
    fun `a second start does not add a second collector`() = runTest {
        every { observeUnitSystem() } returns flowOf(UnitSystem.IMPERIAL)
        coEvery { bridge.getConnectedNodes() } returns listOf(WearNode("node-1", "Watch"))
        recordSends()
        val coordinator = WearUnitSystemSyncCoordinator(observeUnitSystem, pushWearSettings, this)

        coordinator.start()
        coordinator.start()
        advanceUntilIdle()

        assertEquals(1, sent.size)
    }

    @Test
    fun `a push with no watch connected is not an error and the next change still pushes`() = runTest {
        every { observeUnitSystem() } returns flowOf(UnitSystem.METRIC, UnitSystem.IMPERIAL)
        coEvery { bridge.getConnectedNodes() } returnsMany
            listOf(emptyList(), listOf(WearNode("node-1", "Watch")))
        recordSends()

        WearUnitSystemSyncCoordinator(observeUnitSystem, pushWearSettings, this).start()
        advanceUntilIdle()

        assertEquals(listOf(WearDataLayerPaths.SETTINGS_PUSH to """{"unitSystem":"IMPERIAL"}"""), sent)
        coVerify(exactly = 2) { bridge.getConnectedNodes() }
    }
}
