package com.sza.tourist.ui.common.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
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
import com.google.android.material.materialswitch.MaterialSwitch
import com.sza.tourist.R

/**
 * Reusable switch/toggle row for forms: switch (leftmost), text group (title, subtitle below),
 * optional trailing action slot.
 *
 * The view owns visual state only - checked persistence stays outside the component. Tapping anywhere
 * on the row toggles the switch. The help icon of the source stays behind with the in-app help (S0014).
 *
 * Public XML attributes use the `str_` prefix (see `attrs.xml`).
 */
class SettingsToggleRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val switchView: MaterialSwitch
    private val iconView: ImageView
    private val titleView: TextView
    private val subtitleView: TextView
    private val trailingSlot: FrameLayout
    private val textGroup: LinearLayout
    private val titleLineSpacer: View

    private var checkedChangeListener: ((Boolean) -> Unit)? = null

    /**
     * Current checked state of the embedded switch.
     */
    var isChecked: Boolean
        get() = switchView.isChecked
        set(value) {
            switchView.isChecked = value
        }

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.button_height)
        isClickable = true
        isFocusable = true
        if (background == null) {
            // Provide the standard ripple feedback on row taps so the whole row reads as one control.
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }
        LayoutInflater.from(context).inflate(R.layout.view_settings_toggle_row, this, true)

        switchView = findViewById(R.id.str_switch)
        iconView = findViewById(R.id.str_icon)
        titleView = findViewById(R.id.str_title)
        subtitleView = findViewById(R.id.str_subtitle)
        trailingSlot = findViewById(R.id.str_trailingSlot)
        textGroup = findViewById(R.id.str_textGroup)
        titleLineSpacer = findViewById(R.id.str_titleLineSpacer)

        bindRowClicks()
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
    }

    /**
     * Sets the subtitle from a string resource.
     */
    fun setSubtitle(@StringRes resId: Int) {
        setSubtitle(context.getText(resId))
    }

    /**
     * Sets an optional leading feature icon. Passing `null` hides the icon slot.
     * Clears the theme tint so full-color Drawables display in their natural colors rather than being
     * converted into monochrome white silhouettes.
     */
    fun setIcon(icon: Drawable?) {
        iconView.imageTintList = null
        iconView.clearColorFilter()
        iconView.setImageDrawable(icon)
        iconView.visibility = if (icon == null) View.GONE else View.VISIBLE
    }

    /**
     * Sets an optional leading feature icon from a drawable resource; applies the standard themed
     * color for vector glyphs.
     */
    fun setIcon(@DrawableRes resId: Int) {
        val typedValue = android.util.TypedValue()
        context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
        iconView.imageTintList = android.content.res.ColorStateList.valueOf(typedValue.data)
        iconView.setImageResource(resId)
        iconView.visibility = View.VISIBLE
    }

    /**
     * Registers a listener invoked when the embedded switch changes state.
     * Replaces any previously registered listener.
     */
    fun setOnCheckedChangeListener(listener: ((Boolean) -> Unit)?) {
        checkedChangeListener = listener
    }

    /**
     * Sets the checked state without invoking the change listener.
     */
    fun setCheckedSilently(value: Boolean) {
        val listener = checkedChangeListener
        checkedChangeListener = null
        switchView.isChecked = value
        checkedChangeListener = listener
    }

    /**
     * Forwards enabled state to all interactive children so the row reads as one control.
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        switchView.isEnabled = enabled
        iconView.isEnabled = enabled
        titleView.isEnabled = enabled
        subtitleView.isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
    }

    /**
     * Switches the row between the default "stretch-to-parent" layout and a compact layout that hugs
     * the title/subtitle so a trailing sibling sits immediately next to the label rather than at the
     * far right edge of the container.
     */
    fun setHugsTextContent(hug: Boolean) {
        val textGroupParams = textGroup.layoutParams as LayoutParams
        textGroupParams.width = if (hug) LayoutParams.WRAP_CONTENT else 0
        textGroupParams.weight = if (hug) 0f else 1f
        textGroup.layoutParams = textGroupParams

        val spacerParams = titleLineSpacer.layoutParams as LayoutParams
        spacerParams.weight = if (hug) 0f else 1f
        titleLineSpacer.layoutParams = spacerParams

        val subtitleParams = subtitleView.layoutParams as LayoutParams
        subtitleParams.width = if (hug) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
        subtitleView.layoutParams = subtitleParams
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

    private fun bindRowClicks() {
        setOnClickListener {
            if (!isEnabled) return@setOnClickListener
            switchView.isChecked = !switchView.isChecked
        }
        // The native CheckedChangeListener fires for both programmatic and user-driven changes;
        // setCheckedSilently temporarily detaches it to suppress programmatic notifications.
        switchView.setOnCheckedChangeListener { _, value ->
            checkedChangeListener?.invoke(value)
        }
    }

    private fun applyAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs == null) return
        context.obtainStyledAttributes(attrs, R.styleable.SettingsToggleRow, defStyleAttr, 0).use { typedArray ->
            val title = typedArray.getText(R.styleable.SettingsToggleRow_str_title) ?: ""
            setTitle(title)
            setSubtitle(typedArray.getText(R.styleable.SettingsToggleRow_str_subtitle))
            val initiallyChecked = typedArray.getBoolean(R.styleable.SettingsToggleRow_str_checked, false)
            switchView.isChecked = initiallyChecked
            val iconRes = typedArray.getResourceId(R.styleable.SettingsToggleRow_str_icon, 0)
            if (iconRes != 0) setIcon(iconRes)
            applyTextSize(titleView, typedArray, R.styleable.SettingsToggleRow_str_titleTextSize)
            applyTextSize(subtitleView, typedArray, R.styleable.SettingsToggleRow_str_subtitleTextSize)
        }
    }

    private fun applyTextSize(view: TextView, typedArray: TypedArray, index: Int) {
        if (typedArray.hasValue(index)) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, typedArray.getDimension(index, view.textSize))
        }
    }
}
