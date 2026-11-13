package com.sza.tourist.ui.flashlight.helpers

import android.app.Application
import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The shade-shortcut permission manager had no test in the source. The port is pinned here: the
 * flash-unit answer follows the device, and the permission question only exists on Android 13+.
 */
@RunWith(RobolectricTestRunner::class)
class FlashlightShortcutPermissionManagerTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    @Config(sdk = [31])
    fun `below Android 13 the row switches on without asking`() {
        // An application context holds no activity result registry, which is fine here: below
        // Android 13 no request is ever raised.
        val manager = FlashlightShortcutPermissionManager(
            application.applicationContext,
            viewLifecycleOwner = null,
            onNotificationPermissionGranted = {},
        )

        assertTrue(manager.ensureGranted())
    }

    @Test
    @Config(sdk = [34])
    fun `on Android 13 and later an ungranted permission keeps the row off`() {
        // An application context holds no activity result registry, so the request cannot even be
        // raised - the manager's own answer for a host that has no activity: the row stays off.
        val manager = FlashlightShortcutPermissionManager(
            application.applicationContext,
            viewLifecycleOwner = null,
            onNotificationPermissionGranted = {},
        )

        assertFalse(manager.ensureGranted())
    }

    @Test
    fun `the flash-unit answer follows the device feature`() {
        val manager = FlashlightShortcutPermissionManager(
            application.applicationContext,
            viewLifecycleOwner = null,
            onNotificationPermissionGranted = {},
        )
        val shadowPackageManager = shadowOf(application.packageManager)

        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, true)
        assertTrue(manager.isFlashUnitAvailable)

        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, false)
        assertFalse(manager.isFlashUnitAvailable)
    }
}
