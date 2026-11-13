package com.sza.tourist.ui.flashlight.helpers

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class WaterFlashlightLockdownManagerTest {

    private val activity = Robolectric.buildActivity(PinningActivity::class.java).setup().get()
    private val activityManager = activity.getSystemService(ActivityManager::class.java)
    private val manager = WaterFlashlightLockdownManager()
    private var armed = 0
    private val onLocked: () -> Unit = { armed++ }

    @Test
    fun `resume does not arm before consent or repeat its prompt`() {
        manager.engage(activity, onLocked)
        manager.engage(activity, onLocked)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, activity.requests)
        assertEquals(0, armed)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `confirmation arms the lamp and hardware exit can release its pin`() {
        manager.engage(activity, onLocked)
        shadowOf(activityManager).setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_PINNED)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, armed)
        manager.release(activity)
        assertEquals(1, activity.releases)
        assertEquals(ActivityManager.LOCK_TASK_MODE_NONE, activityManager.lockTaskModeState)
    }

    @Test
    fun `cancelling confirmation closes without arming`() {
        manager.engage(activity, onLocked)
        activity.finish()
        manager.release(activity)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(activity.isFinishing)
        assertEquals(0, armed)
        assertEquals(0, activity.releases)
    }

    @Test
    fun `an existing caller pin survives the lamp leaving`() {
        shadowOf(activityManager).setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_PINNED)
        manager.engage(activity, onLocked)
        manager.release(activity)

        assertEquals(1, armed)
        assertEquals(0, activity.requests)
        assertEquals(0, activity.releases)
        assertEquals(ActivityManager.LOCK_TASK_MODE_PINNED, activityManager.lockTaskModeState)
    }

    @Test
    fun `rotation retains ownership without a second prompt`() {
        manager.engage(activity, onLocked)
        shadowOf(activityManager).setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_PINNED)
        shadowOf(Looper.getMainLooper()).idle()
        val state = Bundle()
        manager.save(activity, state)

        val recreated = WaterFlashlightLockdownManager()
        recreated.restore(state)
        recreated.engage(activity, onLocked)
        recreated.release(activity)

        assertEquals(1, activity.requests)
        assertEquals(1, activity.releases)
    }

    @Test
    fun `rotation during consent retains the pending request and its ownership`() {
        manager.engage(activity, onLocked)
        val state = Bundle()
        manager.save(activity, state)

        val recreated = WaterFlashlightLockdownManager()
        recreated.restore(state)
        shadowOf(activityManager).setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_PINNED)
        recreated.engage(activity, onLocked)
        recreated.release(activity)

        assertEquals(1, activity.requests)
        assertEquals(1, activity.releases)
    }

    @Test
    fun `destroy removes a queued confirmation callback`() {
        manager.engage(activity, onLocked)
        shadowOf(activityManager).setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_PINNED)
        manager.destroy(activity)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(0, armed)
        assertEquals(1, activity.releases)
    }

    @Test
    fun `an OS unpin closes the armed session without asking to pin again`() {
        manager.engage(activity, onLocked)
        shadowOf(activityManager).setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_PINNED)
        shadowOf(Looper.getMainLooper()).idle()
        shadowOf(activityManager).setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_NONE)

        manager.engage(activity, onLocked)

        assertTrue(activity.isFinishing)
        assertEquals(1, activity.requests)
        assertEquals(1, armed)
    }

    @Test
    fun `retry as consent completes preserves ownership for hardware exit`() {
        manager.engage(activity, onLocked)
        shadowOf(activityManager).setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_PINNED)

        manager.retry(activity, onLocked)
        manager.release(activity)

        assertEquals(1, armed)
        assertEquals(1, activity.releases)
        assertEquals(ActivityManager.LOCK_TASK_MODE_NONE, activityManager.lockTaskModeState)
    }

    class PinningActivity : Activity() {
        var requests = 0
        var releases = 0

        override fun startLockTask() {
            requests++
            // The real OS stays unpinned until the user responds in its own window.
        }

        override fun stopLockTask() {
            releases++
            shadowOf(getSystemService(ActivityManager::class.java))
                .setLockTaskModeState(ActivityManager.LOCK_TASK_MODE_NONE)
        }
    }
}
