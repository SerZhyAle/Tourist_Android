package com.sza.tourist.utils

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.sza.tourist.R

/**
 * Returns the status bar height in pixels.
 *
 * Three-tier fallback for OEM Android 8.x (API 26/27) devices where
 * WindowInsetsCompat.Type.statusBars() may report 0 despite a visible status bar:
 * 1. Modern typed API (correct on API 30+ and well-behaved OEMs).
 * 2. Deprecated systemWindowInsetTop (broader OEM compatibility on API 20-29).
 * 3. System resource "status_bar_height" (always available, OEM-independent).
 */
@Suppress("DEPRECATION")
fun WindowInsetsCompat.getStatusBarHeightSafe(resources: Resources): Int {
    val fromType = getInsets(WindowInsetsCompat.Type.statusBars()).top
    if (fromType > 0) return fromType
    val fromSystemWindow = systemWindowInsetTop
    if (fromSystemWindow > 0) return fromSystemWindow
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
}

/**
 * Keep important content and action targets inside system-bar safe bounds.
 *
 * Ordinary screens must apply these insets even when they do not explicitly opt into fullscreen UI.
 * Insets are passed through (not consumed) so sibling views in the same window still receive them.
 * [onApplied] reports the per-edge inset deltas that were added on top of the captured base padding
 * (0 for any disabled edge); callers use it for diagnostics.
 *
 * [useStatusBarHeightFallback] protects screens that would otherwise slide under a bar the window has
 * not measured yet; for a surface that hides the bar on purpose, pass false - the fallback would
 * reserve a band for something that is no longer there.
 *
 * The base padding is captured on the FIRST call for a given view and reused by every later one, so
 * re-applying with a different edge mask recomputes from the bare view instead of stacking one inset
 * on top of the one it added last time. Re-applying only to pick up a changed inset still needs no
 * call at all: the registered listener does it, and `ViewCompat.requestApplyInsets` asks for a fresh
 * dispatch.
 */
fun View.applySystemBarInsetPadding(
    applyLeft: Boolean = true,
    applyTop: Boolean = true,
    applyRight: Boolean = true,
    applyBottom: Boolean = true,
    useStatusBarHeightFallback: Boolean = true,
    onApplied: ((left: Int, top: Int, right: Int, bottom: Int) -> Unit)? = null,
) {
    val base = systemBarInsetBasePadding()
    val baseLeft = base.left
    val baseTop = base.top
    val baseRight = base.right
    val baseBottom = base.bottom

    fun apply(insets: WindowInsetsCompat) {
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val left = if (applyLeft) maxOf(systemBars.left, cutout.left) else 0
        val top = if (applyTop) {
            val statusBarTop = if (useStatusBarHeightFallback) {
                insets.getStatusBarHeightSafe(resources)
            } else {
                systemBars.top
            }
            maxOf(systemBars.top, cutout.top, statusBarTop)
        } else {
            0
        }
        val right = if (applyRight) maxOf(systemBars.right, cutout.right) else 0
        val bottom = if (applyBottom) maxOf(systemBars.bottom, cutout.bottom) else 0
        updatePadding(
            left = baseLeft + left,
            top = baseTop + top,
            right = baseRight + right,
            bottom = baseBottom + bottom,
        )
        onApplied?.invoke(left, top, right, bottom)
    }

    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        apply(insets)
        insets
    }
    ViewCompat.getRootWindowInsets(this)?.let(::apply) ?: ViewCompat.requestApplyInsets(this)
}

/**
 * The view's padding as it stood before any inset was added to it, remembered on the view itself.
 *
 * Read on every [applySystemBarInsetPadding] call and written only by the first one: the padding the
 * later calls see is the previous call's own output, and measuring from that is what turns a repeated
 * application into a compounding one.
 */
private fun View.systemBarInsetBasePadding(): Rect {
    val remembered = getTag(R.id.systemBarInsetBasePadding) as? Rect
    if (remembered != null) {
        return remembered
    }
    return Rect(paddingLeft, paddingTop, paddingRight, paddingBottom)
        .also { setTag(R.id.systemBarInsetBasePadding, it) }
}
