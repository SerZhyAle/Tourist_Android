package com.sza.tourist.ui.flashlight.helpers

import android.content.Context
import com.sza.tourist.core.torch.TorchController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Keeps the short-lived trampoline activity free of hardware-operation details. */
class FlashlightToggleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val torchController: TorchController,
) {
    fun toggle() = torchController.toggleFlashlight(context)
}
