package com.sza.tourist.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import androidx.activity.ComponentActivity
import com.google.android.material.appbar.MaterialToolbar
import com.sza.tourist.help.HelpUi

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

    /**
     * S0014: the `?` action of CONTEXT-HELP rule 2 joins the bar when the help map gives the host screen a
     * help. A local addition to the port, kept to this one call so the file stays close to its source.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        HelpUi.installToolbarAction(this)
    }
}
