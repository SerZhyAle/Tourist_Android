package com.sza.tourist.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import androidx.activity.ComponentActivity
import com.google.android.material.appbar.MaterialToolbar

/**
 * The app's single toolbar entry point.
 *
 * Back navigation has one idiom here: [setUpNavigation] routes it through `onBackPressedDispatcher`,
 * which is the only path that honours the callbacks a host already registered.
 */
class StandardToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.toolbarStyle,
) : MaterialToolbar(context, attrs, defStyleAttr) {

    fun setUpNavigation(activity: ComponentActivity) {
        setNavigationOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
    }
}
