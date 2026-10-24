package com.sza.tourist.core.ui

import com.sza.tourist.domain.model.AppSettings

/**
 * The single answer to "must this window keep the screen on".
 *
 * The kit's answer is the user setting alone for now: the power-saving level FastMediaSorter also
 * folds in arrives with a power-policy port, if one ever does - the parameter is where it would plug
 * in. Pure and free of Android types, so it is unit-testable.
 */
object KeepScreenAwakePolicy {

    fun shouldKeepScreenAwake(preventSleep: Boolean): Boolean = preventSleep
}
