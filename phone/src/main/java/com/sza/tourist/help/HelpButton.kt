package com.sza.tourist.help

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.R as AppCompatR
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sza.tourist.R

/**
 * The `?` button of CONTEXT-HELP rule 2: the `app.help` glyph, named "Help (F1)" in the UI language, standing
 * in the trailing corner of a surface and added beside what is there.
 *
 * It wires itself. A screen declares nothing: placed in a layout, the button asks the help map what the host
 * Activity's help is and hides itself when the map gives none, so no window carries help code of its own
 * (rule 17). A dialog title names its surface with [bindSurface] because a dialog has no class of its own
 * in the map's host column.
 */
class HelpButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = AppCompatR.attr.imageButtonStyle,
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    private var surfaceId: String? = null

    init {
        setImageResource(R.drawable.ic_help_outline)
        val borderless = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, borderless, true)) {
            setBackgroundResource(borderless.resourceId)
        } else {
            background = null
        }
        contentDescription = context.getString(R.string.help_action)
        TooltipCompat.setTooltipText(this, contentDescription)
        isFocusable = true
        minimumWidth = resources.getDimensionPixelSize(R.dimen.touch_target_min_height)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.touch_target_min_height)
    }

    /** Names the surface this button asks about, instead of taking the host Activity's. */
    fun bindSurface(id: String) {
        surfaceId = id
        if (isAttachedToWindow) refresh()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        keepCornerClearOfSystemBars()
        refresh()
    }

    /**
     * A button standing alone in the top corner of a full-bleed screen (the SOS screen draws behind the system
     * bars) must not sit under the status bar, where it could not be tapped. A button inside a title row
     * or a toolbar is placed by that row, and is left alone.
     */
    private fun keepCornerClearOfSystemBars() {
        val params = layoutParams as? FrameLayout.LayoutParams ?: return
        if (params.gravity and Gravity.VERTICAL_GRAVITY_MASK != Gravity.TOP) return
        val baseTop = params.topMargin
        val baseEnd = params.marginEnd
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val endInset = if (view.layoutDirection == LAYOUT_DIRECTION_RTL) bars.left else bars.right
            (view.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = baseTop + bars.top
                marginEnd = baseEnd + endInset
            }
            view.requestLayout()
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    private fun refresh() {
        val host = HelpUi.hostActivity(context)
        val target = host?.let { HelpUi.targetFor(it, surfaceId) }
        if (host == null || target == null) {
            visibility = View.GONE
            setOnClickListener(null)
            return
        }
        visibility = View.VISIBLE
        setOnClickListener { HelpLauncher.open(host, target) }
    }
}
