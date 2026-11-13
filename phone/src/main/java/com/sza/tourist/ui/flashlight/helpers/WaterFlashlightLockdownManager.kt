package com.sza.tourist.ui.flashlight.helpers

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import timber.log.Timber
import javax.inject.Inject

/**
 * Screen pinning guards Home/Recents; immersive bars alone only postpone the second wet swipe.
 * Android owns the confirmation and the emergency unpin gesture. The lamp starts only after consent.
 */
class WaterFlashlightLockdownManager @Inject constructor() {

    private val handler = Handler(Looper.getMainLooper())
    private var requested = false
    private var ownsPinning = false

    fun restore(state: Bundle?) {
        requested = state?.getBoolean(STATE_REQUESTED) ?: false
        ownsPinning = state?.getBoolean(STATE_OWNED) ?: false
    }

    fun save(activity: Activity, state: Bundle) {
        state.putBoolean(STATE_REQUESTED, requested)
        state.putBoolean(STATE_OWNED, ownsPinning || (requested && isPinned(activity)))
    }

    fun isPinned(activity: Activity): Boolean =
        activity.getSystemService(ActivityManager::class.java).lockTaskModeState !=
            ActivityManager.LOCK_TASK_MODE_NONE

    fun engage(activity: Activity, onLocked: () -> Unit) {
        pause()
        hideSystemBars(activity)
        if (isPinned(activity)) {
            complete(activity, onLocked)
        } else if (ownsPinning) {
            activity.finish()
        } else {
            if (!requested) {
                requested = true
                try {
                    activity.startLockTask()
                } catch (error: IllegalStateException) {
                    Timber.w(error, "Water flashlight pinning could not start")
                    requested = false
                } catch (error: SecurityException) {
                    Timber.w(error, "Water flashlight pinning was denied")
                    requested = false
                }
            }
            if (requested) awaitConfirmation(activity, onLocked)
        }
    }

    fun retry(activity: Activity, onLocked: () -> Unit) {
        if (!isPinned(activity)) requested = false
        engage(activity, onLocked)
    }

    fun pause() {
        handler.removeCallbacksAndMessages(null)
    }

    /** Release only a lock this lamp requested; a task pinned before entry belongs to its caller. */
    fun release(activity: Activity) {
        pause()
        if ((ownsPinning || requested) && isPinned(activity)) activity.stopLockTask()
        ownsPinning = false
        requested = false
    }

    fun destroy(activity: Activity) {
        pause()
        if (!activity.isChangingConfigurations) release(activity)
    }

    private fun awaitConfirmation(activity: Activity, onLocked: () -> Unit) {
        // SystemUI's pinning prompt is not a focusable Activity and offers no result callback.
        // Poll only while awaiting consent and resumed; cancellation leaves the unarmed Start/Cancel
        // dialog available. No timer runs once the lamp is armed or the Activity is paused.
        handler.post(object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed || !requested) return
                if (isPinned(activity)) {
                    complete(activity, onLocked)
                } else {
                    handler.postDelayed(this, CONFIRMATION_POLL_MS)
                }
            }
        })
    }

    private fun complete(activity: Activity, onLocked: () -> Unit) {
        pause()
        if (requested) ownsPinning = true
        requested = false
        hideSystemBars(activity)
        onLocked()
    }

    private fun hideSystemBars(activity: Activity) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private companion object {
        const val STATE_REQUESTED = "water.pinning.requested"
        const val STATE_OWNED = "water.pinning.owned"
        const val CONFIRMATION_POLL_MS = 250L
    }
}
