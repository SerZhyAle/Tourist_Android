package com.sza.tourist.ui.flashlight.helpers

import android.content.Context
import com.sza.tourist.core.torch.TorchController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/** The trampoline's whole job: one delegated call, nothing else. */
class FlashlightToggleManagerTest {

    @Test
    fun `toggle asks the torch controller and hands back its answer`() {
        val context = mockk<Context>()
        val torch = mockk<TorchController> { every { toggleFlashlight(context) } returns true }
        val manager = FlashlightToggleManager(context, torch)

        org.junit.Assert.assertTrue(manager.toggle())

        verify(exactly = 1) { torch.toggleFlashlight(context) }
    }

    @Test
    fun `a device that refuses the toggle is reported as refused`() {
        val context = mockk<Context>()
        val torch = mockk<TorchController> { every { toggleFlashlight(context) } returns false }

        org.junit.Assert.assertFalse(FlashlightToggleManager(context, torch).toggle())
    }
}
