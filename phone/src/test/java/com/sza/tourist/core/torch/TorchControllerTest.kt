package com.sza.tourist.core.torch

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCameraCharacteristics

/**
 * The torch was extracted from FastMediaSorter's `DeviceActionHandler`, where it had no test. The
 * extraction is pinned here: the three members every caller uses, on a device with and without a flash.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class TorchControllerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val shadow = shadowOf(cameraManager)

    private fun addCamera(id: String, hasFlash: Boolean) {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics).set(CameraCharacteristics.FLASH_INFO_AVAILABLE, hasFlash)
        shadow.addCamera(id, characteristics)
    }

    @Test
    fun `the torch lights and goes out on request`() {
        addCamera("0", hasFlash = true)
        val torch = TorchController()

        assertTrue(torch.setTorch(context, true))
        assertTrue(shadow.getTorchMode("0"))
        assertTrue(torch.isTorchOn)

        assertTrue(torch.setTorch(context, false))
        assertFalse(shadow.getTorchMode("0"))
        assertFalse(torch.isTorchOn)
    }

    @Test
    fun `toggling flips the state each time`() {
        addCamera("0", hasFlash = true)
        val torch = TorchController()

        assertTrue(torch.toggleFlashlight(context))
        assertTrue(torch.isTorchOn)
        assertTrue(torch.toggleFlashlight(context))
        assertFalse(torch.isTorchOn)
    }

    @Test
    fun `setting the state the torch is already in is safe`() {
        addCamera("0", hasFlash = true)
        val torch = TorchController()
        torch.setTorch(context, true)

        assertTrue(torch.setTorch(context, true))
        assertTrue(torch.isTorchOn)
        assertTrue(torch.setTorch(context, false))
        assertTrue(torch.setTorch(context, false))
        assertFalse(torch.isTorchOn)
    }

    @Test
    fun `the first camera that carries a flash is the one driven`() {
        addCamera("0", hasFlash = false)
        addCamera("1", hasFlash = true)
        val torch = TorchController()

        assertTrue(torch.setTorch(context, true))

        assertTrue(shadow.getTorchMode("1"))
    }

    @Test
    fun `a device without a flash refuses and stays dark`() {
        addCamera("0", hasFlash = false)
        val torch = TorchController()

        assertFalse(torch.setTorch(context, true))
        assertFalse(torch.toggleFlashlight(context))
        assertFalse(torch.isTorchOn)
    }

    @Test
    fun `a device with no camera refuses and stays dark`() {
        val torch = TorchController()

        assertFalse(torch.setTorch(context, true))
        assertFalse(torch.isTorchOn)
    }

    @Test
    fun `a platform that refuses the switch leaves the state where it was`() {
        val characteristics = mockk<CameraCharacteristics> {
            every { get(CameraCharacteristics.FLASH_INFO_AVAILABLE) } returns true
        }
        val refusingManager = mockk<CameraManager> {
            every { cameraIdList } returns arrayOf("0")
            every { getCameraCharacteristics("0") } returns characteristics
            every { setTorchMode(any(), any()) } throws CameraAccessException(CameraAccessException.CAMERA_ERROR)
        }
        val refusingContext = mockk<Context> {
            every { getSystemService(Context.CAMERA_SERVICE) } returns refusingManager
        }
        val torch = TorchController()

        assertFalse(torch.setTorch(refusingContext, true))
        assertEquals(false, torch.isTorchOn)
    }
}
