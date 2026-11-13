package com.sza.tourist.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sza.tourist.core.di.bootReceiverEntryPointOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A notification does not survive a reboot, so the shade shortcut is re-posted here.
 *
 * Resolves its graph by hand - see [com.sza.tourist.core.di.BootReceiverEntryPoint] for why a
 * manifest receiver cannot take field injection on this path.
 */
class FlashlightShortcutBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val deps = bootReceiverEntryPointOrNull(context, "FlashlightShortcutBootReceiver") ?: return
        val coordinator = deps.flashlightShortcutCoordinator()
        val pending = goAsync()
        deps.applicationScope().launch {
            try {
                coordinator.syncOnce()
            } catch (e: CancellationException) {
                // First arm on purpose: CancellationException extends IllegalStateException, so the
                // arm below would otherwise swallow a cancelled scope.
                throw e
            } catch (e: IllegalStateException) {
                // Safe default is the shortcut simply not appearing: the setting is untouched and the
                // next process start syncs it again, so a corrupt store must not crash the broadcast.
                Timber.w(e, "FlashlightShortcutBootReceiver: settings store unusable at boot")
            } finally {
                pending.finish()
            }
        }
    }
}
