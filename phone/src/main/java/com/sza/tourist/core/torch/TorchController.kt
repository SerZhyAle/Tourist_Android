package com.sza.tourist.core.torch

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the physical camera flash - the torch - with no camera preview, via [CameraManager.setTorchMode].
 * Singleton so the best-effort torch state survives across callers.
 *
 * The one torch seam: the SOS blinker, the water flashlight, the toggle and the notification-shade
 * shortcut all go through it rather than reaching for `CameraManager` themselves.
 */
@Singleton
class TorchController @Inject constructor() {

    // Best-effort torch state. The camera framework has no torch-state getter, and a TorchCallback would
    // be an unbalanced process-lifetime listener, so a toggle tracks state locally. An external torch
    // change can desync this by one tap, which self-corrects on the next toggle.
    @Volatile
    private var torchEnabled = false

    /**
     * The shade shortcut renders "lit" or "dark" and this is the only place that knows which.
     * Read-only on purpose - the state is still written by [setTorch] alone, and a subscriber that
     * needs it to be authoritative registers its own torch callback for as long as it is showing.
     */
    val isTorchOn: Boolean get() = torchEnabled

    /** Toggles the physical camera flash and reports whether this device could accept the operation. */
    fun toggleFlashlight(context: Context): Boolean = setTorch(context, !torchEnabled)

    /**
     * Drives the physical camera flash to [enabled] and reports whether the device accepted it.
     *
     * Safe to call with the state the torch is already in. A screen that lights the torch on
     * entry must extinguish it on exit whoever lit it, and a toggle cannot promise that - it would
     * hand back a lit torch to a caller that found one already burning.
     */
    fun setTorch(context: Context, enabled: Boolean): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val flashCameraId = cameraManager?.let(::findFlashCameraId)
        return when {
            cameraManager == null -> {
                Timber.w("TorchController: CameraManager unavailable, flashlight ignored")
                false
            }
            flashCameraId == null -> {
                Timber.w("TorchController: no flash unit on this device, flashlight ignored")
                false
            }
            else -> runCatching {
                cameraManager.setTorchMode(flashCameraId, enabled)
                torchEnabled = enabled
                true
            }.onFailure { Timber.w(it, "TorchController: setTorchMode failed") }.getOrDefault(false)
        }
    }

    /** The first camera carrying a flash unit, or null when this device has none. */
    private fun findFlashCameraId(manager: CameraManager): String? = runCatching {
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()
}
