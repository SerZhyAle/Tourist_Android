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

    /** A context whose camera manager answers the lookup as [idList] says and records the torch in [lit]. */
    private class ScriptedCamera(idList: () -> Array<String>) {
        var lit = false
        val characteristics = mockk<CameraCharacteristics> {
            every { get(CameraCharacteristics.FLASH_INFO_AVAILABLE) } returns true
        }
        val manager = mockk<CameraManager> {
            every { cameraIdList } answers { idList() }
            every { getCameraCharacteristics(any()) } returns characteristics
            every { setTorchMode(any(), any()) } answers { lit = secondArg() }
        }
        val context = mockk<Context> {
            every { getSystemService(Context.CAMERA_SERVICE) } returns manager
        }
    }

    @Test
    fun `the flash camera is looked up once and remembered`() {
        var lookups = 0
        val camera = ScriptedCamera { lookups++; arrayOf("0") }
        val torch = TorchController()

        repeat(5) { assertTrue(torch.toggleFlashlight(camera.context)) }

        assertEquals(1, lookups)
    }

    @Test
    fun `a burst of toggles of an even length ends dark even if the camera lookup breaks after the first`() {
        var lookups = 0
        val camera = ScriptedCamera {
            if (lookups++ == 0) arrayOf("0") else throw CameraAccessException(CameraAccessException.CAMERA_ERROR)
        }
        val torch = TorchController()

        repeat(100) { assertTrue(torch.toggleFlashlight(camera.context)) }

        assertFalse(camera.lit)
        assertFalse(torch.isTorchOn)
    }

    @Test
    fun `a lookup that failed is tried again on the next call`() {
        var lookups = 0
        val camera = ScriptedCamera {
            if (lookups++ == 0) throw CameraAccessException(CameraAccessException.CAMERA_ERROR) else arrayOf("0")
        }
        val torch = TorchController()

        assertFalse(torch.setTorch(camera.context, true))
        assertFalse(torch.isTorchOn)

        assertTrue(torch.setTorch(camera.context, true))
        assertTrue(camera.lit)
    }

    @Test
    fun `a remembered camera that has gone is looked up afresh`() {
        var ids = arrayOf("0")
        val camera = ScriptedCamera { ids }
        every { camera.manager.setTorchMode("0", any()) } throws IllegalArgumentException("no camera 0")
        val torch = TorchController()

        assertFalse(torch.setTorch(camera.context, true))

        ids = arrayOf("1")
        every { camera.manager.setTorchMode("1", any()) } answers { camera.lit = secondArg() }
        assertTrue(torch.setTorch(camera.context, true))
        assertTrue(camera.lit)
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
