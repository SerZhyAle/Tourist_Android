package com.sza.tourist.ui.sos

import android.content.Context
import com.sza.tourist.core.torch.TorchController
import com.sza.tourist.domain.model.sos.SosMorseCadence
import com.sza.tourist.testing.MainDispatcherRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SosTorchManagerTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)
    private val torchController: TorchController = mockk(relaxed = true)

    @Test
    fun `start drives torch and isLit across Morse cadence spans and stop leaves torch dark`() =
        runTest(dispatcherRule.testDispatcher) {
            val manager = SosTorchManager(torchController)

            manager.start(context)
            runCurrent()

            assertTrue(manager.isRunning)
            assertTrue(manager.isLit.value)
            verify(exactly = 1) { torchController.setTorch(context, true) }

            // Advance past the first dot (150ms) into the intra-character gap (150ms dark).
            advanceTimeBy(SosMorseCadence.cycle[0].durationMs)
            runCurrent()
            assertFalse(manager.isLit.value)
            verify(exactly = 1) { torchController.setTorch(context, false) }

            // Calling start again while running is a no-op.
            manager.start(context)
            runCurrent()
            verify(exactly = 1) { torchController.setTorch(context, true) }

            manager.stop(context)
            assertFalse(manager.isRunning)
            assertFalse(manager.isLit.value)
            verify(exactly = 2) { torchController.setTorch(context, false) }
        }
}
