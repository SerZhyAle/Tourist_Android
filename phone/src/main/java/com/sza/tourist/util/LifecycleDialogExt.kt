package com.sza.tourist.util

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

/**
 * Shows a dialog and dismisses it when the hosting activity is destroyed, so a screen that dies while
 * a confirmation is up cannot leak the window or come back to a stale callback.
 */
fun AlertDialog.Builder.showBoundToHost(context: Context): AlertDialog? {
    val owner = context.findLifecycleOwner()
    if (owner == null) {
        Timber.w("showBoundToHost: no LifecycleOwner behind %s, dialog stays unbound", context.javaClass.simpleName)
        return show()
    }
    return show().showBoundTo(owner)
}

/** [showBoundToHost] for a dialog that was already created - the shape a site takes when it configures the dialog before showing it. */
fun AlertDialog.showBoundToHost(context: Context): AlertDialog? {
    val owner = context.findLifecycleOwner()
    if (owner == null) {
        Timber.w("showBoundToHost: no LifecycleOwner behind %s, dialog stays unbound", context.javaClass.simpleName)
        show()
        return this
    }
    return showBoundTo(owner)
}

/**
 * Show this dialog and close it automatically when [owner] reaches `ON_DESTROY`.
 *
 * @return the shown dialog, or `null` when [owner] is already destroyed and showing would leak at once.
 */
fun AlertDialog.showBoundTo(owner: LifecycleOwner): AlertDialog? {
    if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return null
    show()
    return bindTo(owner)
}

private fun AlertDialog.bindTo(owner: LifecycleOwner): AlertDialog {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) dismiss()
    }
    owner.lifecycle.addObserver(observer)
    setOnDismissListener { owner.lifecycle.removeObserver(observer) }
    return this
}

/**
 * The owner resolved from [context]: the activity behind it, found by unwrapping the [ContextWrapper]
 * chain. A dialog context already has to be an activity - one raised on an application context never
 * gets a window token in the first place.
 */
private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? =
    when (this) {
        is LifecycleOwner -> this
        is ContextWrapper -> baseContext.findLifecycleOwner()
        else -> null
    }
