package com.sza.tourist.ui.flashlight.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.sza.tourist.util.findLifecycleOwner
import timber.log.Timber

/**
 * Owns what the flashlight shade shortcut's toggle row needs from the device - the notification
 * permission, and whether there is a flash unit at all.
 *
 * Registration goes through [androidx.activity.result.ActivityResultRegistry] rather than
 * `registerForActivityResult`, which is refused once the host has started - and this manager is built
 * lazily from the host's binding, well after that.
 */
class FlashlightShortcutPermissionManager(
    private val context: Context,
    viewLifecycleOwner: LifecycleOwner?,
    private val onNotificationPermissionGranted: () -> Unit,
) {

    private val launcher: ActivityResultLauncher<String>? =
        (context.findLifecycleOwner() as? ActivityResultRegistryOwner)
            ?.activityResultRegistry
            ?.register(REGISTRY_KEY, ActivityResultContracts.RequestPermission()) { granted ->
                // Written only on a grant: a denial must leave the row off rather than on and mute.
                if (granted) {
                    onNotificationPermissionGranted()
                }
            }

    init {
        // The registry is the activity's, this manager lives only as long as the host's view; left
        // registered, the activity would keep the destroyed view's callback.
        viewLifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                launcher?.unregister()
                owner.lifecycle.removeObserver(this)
            }
        }) ?: Timber.w("FlashlightShortcutPermission: no view lifecycle, launcher lives with the activity")
    }

    /** False on a device with no flash, where the row would promise what the hardware cannot do. */
    val isFlashUnitAvailable: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    /** True when the row may be switched on now; false means the request is on screen instead. */
    fun ensureGranted(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            // A null launcher means the view's context chain held no activity, which cannot happen
            // from a kit screen; the row then stays off rather than switching on and going mute.
            launcher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                ?: Timber.w("FlashlightShortcutPermission: no activity to ask from, row stays off")
        }
        return granted
    }

    private companion object {
        const val REGISTRY_KEY = "flashlight_shortcut_post_notifications"
    }
}
