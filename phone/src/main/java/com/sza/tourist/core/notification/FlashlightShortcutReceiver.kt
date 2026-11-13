package com.sza.tourist.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sza.tourist.core.torch.TorchController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Where a tap on the shade shortcut lands.
 *
 * A receiver rather than the [com.sza.tourist.ui.flashlight.FlashlightToggleActivity] trampoline the
 * other surfaces use: a notification needs no window, and the same pass that flips the torch can
 * repaint the entry.
 */
@AndroidEntryPoint
class FlashlightShortcutReceiver : BroadcastReceiver() {

    @Inject
    lateinit var torchController: TorchController

    @Inject
    lateinit var notifier: FlashlightShortcutNotifier

    override fun onReceive(context: Context, intent: Intent) {
        // No super.onReceive: BroadcastReceiver declares it abstract, and the Hilt Gradle plugin's
        // bytecode transform is what injects the fields below - the same shape every other
        // @AndroidEntryPoint receiver in this module uses.
        if (intent.action != ACTION_TOGGLE) {
            return
        }
        torchController.setTorch(context, !torchController.isTorchOn)
        // Re-drawn whatever the hardware answered, so a device that refused shows the dark state
        // rather than a lit icon over an unlit flash.
        notifier.refresh()
    }

    companion object {
        private const val ACTION_TOGGLE = "com.sza.tourist.action.FLASHLIGHT_SHORTCUT_TOGGLE"

        /** The explicit intent the notification's tap target carries. */
        fun toggleIntent(context: Context): Intent =
            Intent(context, FlashlightShortcutReceiver::class.java).setAction(ACTION_TOGGLE)
    }
}
