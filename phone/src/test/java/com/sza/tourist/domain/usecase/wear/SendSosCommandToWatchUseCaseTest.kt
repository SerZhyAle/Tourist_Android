package com.sza.tourist.domain.usecase.wear

import com.sza.tourist.domain.model.WearNode
import com.sza.tourist.domain.model.sos.SosMode
import com.sza.tourist.domain.repository.WearableDataLayerRepository
import com.sza.tourist.service.WearDataLayerPaths
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SendSosCommandToWatchUseCaseTest {

    private val dataLayerRepository: WearableDataLayerRepository = mockk(relaxed = true)
    private val useCase = SendSosCommandToWatchUseCase(dataLayerRepository)

    @Test
    fun `start sends SOS_START_FROM_PHONE with mode name to every connected node`() = runTest {
        val nodes = listOf(
            WearNode(id = "node-1", displayName = "Watch 1"),
            WearNode(id = "node-2", displayName = "Watch 2"),
        )
        coEvery { dataLayerRepository.getConnectedNodes() } returns nodes

        useCase.start(SosMode.SOUND_ONLY)

        val expectedPayload = SosMode.SOUND_ONLY.name.encodeToByteArray()
        coVerify(exactly = 1) {
            dataLayerRepository.sendMessage("node-1", WearDataLayerPaths.SOS_START_FROM_PHONE, expectedPayload)
        }
        coVerify(exactly = 1) {
            dataLayerRepository.sendMessage("node-2", WearDataLayerPaths.SOS_START_FROM_PHONE, expectedPayload)
        }
    }

    @Test
    fun `stop sends SOS_STOP_FROM_PHONE with empty payload to every connected node`() = runTest {
        val nodes = listOf(WearNode(id = "node-1", displayName = "Watch 1"))
        coEvery { dataLayerRepository.getConnectedNodes() } returns nodes

        useCase.stop()

        coVerify(exactly = 1) {
            dataLayerRepository.sendMessage("node-1", WearDataLayerPaths.SOS_STOP_FROM_PHONE, ByteArray(0))
        }
    }

    @Test
    fun `a failing node does not prevent sending to subsequent nodes`() = runTest {
        val nodes = listOf(
            WearNode(id = "node-1", displayName = "Watch 1"),
            WearNode(id = "node-2", displayName = "Watch 2"),
        )
        coEvery { dataLayerRepository.getConnectedNodes() } returns nodes
        coEvery { dataLayerRepository.sendMessage("node-1", any(), any()) } throws IOException("radio lost")

        useCase.start(SosMode.ALL)

        coVerify(exactly = 1) {
            dataLayerRepository.sendMessage("node-2", WearDataLayerPaths.SOS_START_FROM_PHONE, SosMode.ALL.name.encodeToByteArray())
        }
    }

    @Test
    fun `CancellationException from sendMessage is rethrown`() = runTest {
        val nodes = listOf(WearNode(id = "node-1", displayName = "Watch 1"))
        coEvery { dataLayerRepository.getConnectedNodes() } returns nodes
        coEvery { dataLayerRepository.sendMessage("node-1", any(), any()) } throws CancellationException("cancelled")

        try {
            useCase.stop()
            fail("Expected CancellationException to be rethrown")
        } catch (_: CancellationException) {
            // Expected.
        }
    }
}
