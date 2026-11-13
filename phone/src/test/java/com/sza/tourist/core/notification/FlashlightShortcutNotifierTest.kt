package com.sza.tourist.core.notification

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import com.sza.tourist.core.torch.TorchController
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The shade-shortcut notifier had no test in the source. The port is pinned here: show/hide own the
 * entry's life, the rendered state follows the torch, and hide cancels even what an earlier process
 * posted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class FlashlightShortcutNotifierTest {

    private val application: Application = RuntimeEnvironment.getApplication()
    private val notificationManager =
        application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val shadowNotificationManager = shadowOf(notificationManager)

    private fun torch(lit: Boolean): TorchController = mockk {
        every { isTorchOn } returns lit
    }

    @Test
    fun `show posts the entry in the state the torch is in`() {
        val notifier = FlashlightShortcutNotifier(application, torch(lit = false))

        notifier.show()

        assertTrue(notifier.isShown)
        assertNotNull(shadowNotificationManager.getNotification(NotificationIds.FLASHLIGHT_SHORTCUT))
    }

    @Test
    fun `hide takes the entry away and forgets it was posted`() {
        val notifier = FlashlightShortcutNotifier(application, torch(lit = true))
        notifier.show()

        notifier.hide()

        assertFalse(notifier.isShown)
        assertNull(shadowNotificationManager.getNotification(NotificationIds.FLASHLIGHT_SHORTCUT))
    }

    @Test
    fun `hide cancels unconditionally - an entry an earlier process posted goes too`() {
        val notifier = FlashlightShortcutNotifier(application, torch(lit = true))

        // Never shown in this process: a fresh boot exactly.
        notifier.hide()

        assertFalse(notifier.isShown)
    }

    @Test
    fun `refresh redraws only while the entry is shown`() {
        val torch = torch(lit = false)
        val notifier = FlashlightShortcutNotifier(application, torch)

        // Not shown yet: a refresh must not post the entry.
        notifier.refresh()
        assertFalse(notifier.isShown)

        notifier.show()
        every { torch.isTorchOn } returns true
        notifier.refresh()
        assertTrue(notifier.isShown)
    }

    @Test
    fun `the flash-unit answer follows the device feature`() {
        val shadowPackageManager = shadowOf(application.packageManager)
        val notifier = FlashlightShortcutNotifier(application, torch(lit = false))

        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, true)
        assertTrue(notifier.hasFlashUnit())

        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, false)
        assertFalse(notifier.hasFlashUnit())
    }
}
