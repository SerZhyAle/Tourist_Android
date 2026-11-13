package com.sza.tourist.help

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * F1 (CONTEXT-HELP rule 1), installed once for the whole app so that no screen carries help code of its own
 * (rule 17).
 *
 * F1 is read at the window of the product's own screens and dialogs, and only while one of them has the
 * focus; it is never registered system-wide. A hardware keyboard - or a TV remote or gamepad that maps to
 * the key - delivers it, and a phone without one asks through the `?` button instead. The key and the
 * button resolve through the same [HelpResolver], so they open the same page (rule 3).
 */
object HelpInstaller {

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity !is FragmentActivity) return
                wrapWindow(activity.window) { event -> handleKey(activity, event, activity.currentFocus) }
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(DialogKeys(activity), true)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /**
     * Puts the help key in front of [window]'s own key handling. The wrapper answers F1 only when the
     * help map gives the window a help; every other key, and F1 on a window without one, reaches the
     * window exactly as before - the water flashlight's exit keys among them.
     */
    internal fun wrapWindow(window: Window, onKey: (KeyEvent) -> Boolean) {
        val current = window.callback ?: return
        if (current is HelpWindowCallback) return
        window.callback = HelpWindowCallback(current, onKey)
    }

    /**
     * True when [event] is a plain F1 that the map gives a help to, so the caller swallows it. The page opens
     * on the key-up; the key-down and its repeats are swallowed without opening anything.
     */
    internal fun handleKey(
        host: FragmentActivity,
        event: KeyEvent,
        focused: View?,
        surfaceId: String? = null,
        hostClassName: String = host.javaClass.name,
    ): Boolean {
        if (!HelpUi.isPlainF1(event)) return false
        val map = HelpMapStore.get(host)
        val target = if (surfaceId != null) {
            HelpResolver.resolveAsked(map, surfaceId)
        } else {
            HelpResolver.resolveHost(map, hostClassName, HelpUi.entryName(host, focused))
        } ?: return false
        if (event.action == KeyEvent.ACTION_UP) HelpLauncher.open(host, target)
        return true
    }

    private class HelpWindowCallback(
        private val base: Window.Callback,
        private val onKey: (KeyEvent) -> Boolean,
    ) : Window.Callback by base {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean = onKey(event) || base.dispatchKeyEvent(event)

        // Kotlin's delegation skips the two Java default methods below; forward them so the wrapped window
        // keeps the keyboard-shortcut list and pointer-capture callbacks of the Activity it wraps.
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>?, menu: Menu?, deviceId: Int) =
            base.onProvideKeyboardShortcuts(data, menu, deviceId)

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPointerCaptureChanged(hasCapture: Boolean) = base.onPointerCaptureChanged(hasCapture)
    }

    /** A DialogFragment has a window of its own that the Activity's callback never sees. */
    private class DialogKeys(private val host: FragmentActivity) : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
            if (f !is DialogFragment) return
            val dialog = f.dialog ?: return
            val window = dialog.window ?: return
            val className = f.javaClass.name
            wrapWindow(window) { event -> handleKey(host, event, dialog.currentFocus, hostClassName = className) }
        }
    }
}
