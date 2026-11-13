package com.sza.tourist.ui.sos

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.sza.tourist.core.notification.NotificationIds
import com.sza.tourist.domain.model.sos.SosMode
import com.sza.tourist.domain.usecase.sos.IsSosProgramEnabledUseCase
import com.sza.tourist.service.WearDataLayerPaths
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SosCommandFromWatchHandlerTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    fun `SOS_START_FROM_WATCH starts SosService and launches SosActivity with requested mode`() = runTest {
        val notifier = SosStartFromWatchNotifier(application)
        val handler = SosCommandFromWatchHandler(
            context = application,
            isSosProgramEnabled = IsSosProgramEnabledUseCase(),
            startFromWatchNotifier = notifier,
        )

        val handled = handler.handle(
            WearDataLayerPaths.SOS_START_FROM_WATCH,
            SosMode.LIGHT_ONLY.name.encodeToByteArray(),
        )

        assertTrue(handled)
        val serviceIntent = shadowOf(application).nextStartedService
        assertNotNull(serviceIntent)
        assertEquals(SosService::class.java.name, serviceIntent.component?.className)
        assertEquals(SosService.ACTION_START, serviceIntent.action)
        assertEquals(SosMode.LIGHT_ONLY.name, serviceIntent.getStringExtra(SosService.EXTRA_MODE))

        val activityIntent = shadowOf(application).nextStartedActivity
        assertNotNull(activityIntent)
        assertEquals(SosActivity::class.java.name, activityIntent.component?.className)
        assertEquals(SosMode.LIGHT_ONLY.name, activityIntent.getStringExtra(SosService.EXTRA_MODE))
        assertTrue((activityIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun `SOS_START_FROM_WATCH falls back to SosStartFromWatchNotifier when startForegroundService throws IllegalStateException`() = runTest {
        val spyContext: Context = spyk(application) {
            every { startForegroundService(any()) } throws IllegalStateException("Background start not allowed")
        }
        val notifier: SosStartFromWatchNotifier = mockk(relaxed = true)
        val handler = SosCommandFromWatchHandler(
            context = spyContext,
            isSosProgramEnabled = IsSosProgramEnabledUseCase(),
            startFromWatchNotifier = notifier,
        )

        val handled = handler.handle(
            WearDataLayerPaths.SOS_START_FROM_WATCH,
            SosMode.SOUND_ONLY.name.encodeToByteArray(),
        )

        assertTrue(handled)
        verify(exactly = 1) { notifier.show(SosMode.SOUND_ONLY) }
    }

    @Test
    fun `SOS_STOP_FROM_WATCH dispatches ACTION_STOP to SosService`() = runTest {
        val handler = SosCommandFromWatchHandler(
            context = application,
            isSosProgramEnabled = IsSosProgramEnabledUseCase(),
            startFromWatchNotifier = SosStartFromWatchNotifier(application),
        )

        val handled = handler.handle(WearDataLayerPaths.SOS_STOP_FROM_WATCH, ByteArray(0))

        assertTrue(handled)
        val serviceIntent = shadowOf(application).nextStartedService
        assertNotNull(serviceIntent)
        assertEquals(SosService::class.java.name, serviceIntent.component?.className)
        assertEquals(SosService.ACTION_STOP, serviceIntent.action)
    }

    @Test
    fun `unrelated path returns false without starting service or activity`() = runTest {
        val handler = SosCommandFromWatchHandler(
            context = application,
            isSosProgramEnabled = IsSosProgramEnabledUseCase(),
            startFromWatchNotifier = SosStartFromWatchNotifier(application),
        )

        val handled = handler.handle("/tourist/watch/other", ByteArray(0))

        assertFalse(handled)
        assertNull(shadowOf(application).nextStartedService)
        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun `SosStartFromWatchNotifier posts and dismisses SOS_START_FROM_WATCH notification`() {
        val notifier = SosStartFromWatchNotifier(application)
        val manager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = shadowOf(manager)

        notifier.show(SosMode.LIGHT_ONLY)

        val posted = shadowManager.getNotification(NotificationIds.SOS_START_FROM_WATCH)
        assertNotNull(posted)

        notifier.dismiss()
        assertNull(shadowManager.getNotification(NotificationIds.SOS_START_FROM_WATCH))
    }
}
