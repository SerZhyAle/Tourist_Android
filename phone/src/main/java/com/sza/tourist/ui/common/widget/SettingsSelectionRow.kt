package com.sza.tourist.ui.common.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.use
import androidx.core.view.updateLayoutParams
import com.sza.tourist.R
import com.sza.tourist.databinding.ViewSettingsSelectionRowBinding

/**
 * Reusable clickable selection row for settings fragments and forms.
 *
 * Layout: optional leading icon, text group (title, subtitle below), trailing value text, optional
 * trailing control slot, and a trailing chevron.
 *
 * The whole row is one focus/click target. The help-icon chrome of the source stays behind with the
 * in-app help (S0014).
 *
 * Public XML attributes use the `ssr_` prefix (see `attrs.xml`).
 */
class SettingsSelectionRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSettingsSelectionRowBinding.inflate(LayoutInflater.from(context), this)

    private val iconView: ImageView = binding.ssrIcon
    private val titleView: TextView = binding.ssrTitle
    private val subtitleView: TextView = binding.ssrSubtitle
    private val valueView: TextView = binding.ssrValue
    private val chevronView: ImageView = binding.ssrChevron
    private val trailingSlot: FrameLayout = binding.ssrTrailingSlot

    private var rowClickListener: ((View) -> Unit)? = null

    /** Whether this row asked to hug its content to the left; honoured only while it has no subtitle. */
    private var hugContentRequested = false

    /** Last form [applyTextGroupWidth] applied; null until the first call. */
    private var textGroupHugged: Boolean? = null

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.button_height)
        isClickable = true
        isFocusable = true
        if (background == null) {
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }
        bindRowClick()
        applyAttributes(attrs, defStyleAttr)
    }

    /**
     * Sets the row title from a raw text value.
     */
    fun setTitle(text: CharSequence) {
        titleView.text = text
        contentDescription = text
    }

    /**
     * Sets the row title from a string resource.
     */
    fun setTitle(@StringRes resId: Int) {
        setTitle(context.getText(resId))
    }

    /**
     * Sets the trailing value text. Empty/null hides it without breaking row layout.
     */
    fun setValue(text: CharSequence?) {
        if (text.isNullOrEmpty()) {
            valueView.text = ""
            valueView.visibility = View.GONE
        } else {
            valueView.text = text
            valueView.visibility = View.VISIBLE
        }
    }

    /**
     * Sets the subtitle text. Empty/null hides the subtitle without breaking row layout.
     */
    fun setSubtitle(text: CharSequence?) {
        if (text.isNullOrEmpty()) {
            subtitleView.text = ""
            subtitleView.visibility = View.GONE
        } else {
            subtitleView.text = text
            subtitleView.visibility = View.VISIBLE
        }
        applyTextGroupWidth()
    }

    /**
     * Sets the subtitle from a string resource.
     */
    fun setSubtitle(@StringRes resId: Int) {
        setSubtitle(context.getText(resId))
    }

    /**
     * Sets an optional leading icon. Passing `null` hides the icon slot.
     */
    fun setIcon(icon: Drawable?) {
        iconView.setImageDrawable(icon)
        iconView.visibility = if (icon == null) View.GONE else View.VISIBLE
    }

    /**
     * Sets an optional leading icon from a drawable resource.
     */
    fun setIcon(@DrawableRes resId: Int) {
        iconView.setImageResource(resId)
        iconView.visibility = View.VISIBLE
    }

    /**
     * Shows or hides the trailing chevron.
     */
    fun setChevronVisible(visible: Boolean) {
        chevronView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Switches the trailing glyph between value mode (default chevron) and navigation mode
     * (real forward arrow).
     */
    fun setNavigationMode(enabled: Boolean) {
        if (enabled) {
            chevronView.setImageResource(R.drawable.ic_arrow_forward)
            chevronView.visibility = View.VISIBLE
            applyInlineLayout()
        } else {
            chevronView.setImageResource(R.drawable.ic_chevron_right)
        }
    }

    /**
     * Registers the listener invoked when the row is clicked. Replaces any previous listener.
     */
    fun setOnRowClickListener(listener: ((View) -> Unit)?) {
        rowClickListener = listener
    }

    /**
     * Injects or removes an optional trailing control hosted by this row.
     * Passing `null` clears the slot and hides it.
     */
    fun setTrailingControl(view: View?) {
        trailingSlot.removeAllViews()
        if (view == null) {
            trailingSlot.visibility = View.GONE
            return
        }
        (view.parent as? ViewGroup)?.removeView(view)
        trailingSlot.addView(view)
        trailingSlot.visibility = View.VISIBLE
    }

    /**
     * Forwards enabled state to children so the row reads as one control.
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        titleView.isEnabled = enabled
        subtitleView.isEnabled = enabled
        valueView.isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
    }

    private fun bindRowClick() {
        setOnClickListener { view ->
            if (!isEnabled) return@setOnClickListener
            rowClickListener?.invoke(view)
        }
    }

    private fun applyAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs == null) return
        context.obtainStyledAttributes(attrs, R.styleable.SettingsSelectionRow, defStyleAttr, 0).use { typedArray ->
            setTitle(typedArray.getText(R.styleable.SettingsSelectionRow_ssr_title) ?: "")
            setValue(typedArray.getText(R.styleable.SettingsSelectionRow_ssr_value))
            setSubtitle(typedArray.getText(R.styleable.SettingsSelectionRow_ssr_subtitle))
            val iconRes = typedArray.getResourceId(R.styleable.SettingsSelectionRow_ssr_icon, 0)
            if (iconRes != 0) setIcon(iconRes)
            val showChevron = typedArray.getBoolean(R.styleable.SettingsSelectionRow_ssr_showChevron, true)
            chevronView.visibility = if (showChevron) View.VISIBLE else View.GONE
            if (subtitleView.visibility == View.GONE) {
                collapseContentToLeft()
            }
            if (typedArray.getBoolean(R.styleable.SettingsSelectionRow_ssr_inline, false)) {
                applyInlineLayout()
            }
            if (typedArray.getBoolean(R.styleable.SettingsSelectionRow_ssr_navMode, false)) {
                setNavigationMode(true)
            }
        }
    }

    private fun applyInlineLayout() {
        minimumHeight = 0
        collapseContentToLeft()
    }

    private fun collapseContentToLeft() {
        hugContentRequested = true
        applyTextGroupWidth()
    }

    private fun applyTextGroupWidth() {
        val hug = hugContentRequested && subtitleView.visibility == View.GONE
        if (hug == textGroupHugged) return
        textGroupHugged = hug
        binding.ssrTextGroup.updateLayoutParams<LayoutParams> {
            width = if (hug) LayoutParams.WRAP_CONTENT else 0
            weight = if (hug) 0f else 1f
        }
        binding.ssrTitleLine.updateLayoutParams<ViewGroup.LayoutParams> {
            width = if (hug) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.ssrTitleLineSpacer.visibility = if (hug) View.GONE else View.VISIBLE
    }
}
