package com.sza.tourist.ui.common.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.use
import com.google.android.material.checkbox.MaterialCheckBox
import com.sza.tourist.R

/**
 * The product's boolean option row (BOOLEAN-OPTION 2.0, S0015): a clearly visible square checkbox at
 * reading start, one heading line (optional semantic glyph + caption) beside it, and an optional
 * explanation below at the cell's reading-start inset.
 *
 * Activation: an intentional tap on the checkbox, glyph or caption toggles exactly once; the
 * explanation, the gaps and the row background never toggle. Scrolling is left to the hosting
 * scroll container - a drag it recognizes as a scroll cancels the pending tap natively, and the
 * heading stays a deliberate tap target. The row exposes one checkbox to accessibility services:
 * the heading area reports checkbox semantics with the caption as its name and the explanation as
 * its description; the painted [MaterialCheckBox] is decoration only. Space and Enter/D-pad centre
 * toggle the focused row.
 *
 * The view owns visual state only - checked persistence stays outside the component. The optional
 * option-local help button is not built yet; it arrives with the in-app help (S0014) and its
 * omission is allowed while caption plus explanation carry both states.
 *
 * Public XML attributes use the `scr_` prefix (see `attrs.xml`).
 */
class SettingsCheckboxRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val checkboxView: MaterialCheckBox
    private val iconView: ImageView
    private val titleView: TextView
    private val subtitleView: TextView
    private val headingArea: View

    private var checkedChangeListener: ((Boolean) -> Unit)? = null

    /**
     * Current checked state of the option.
     */
    var isChecked: Boolean
        get() = checkboxView.isChecked
        set(value) {
            checkboxView.isChecked = value
            headingArea.isSelected = value
            refreshAccessibilityState()
        }

    init {
        orientation = VERTICAL
        isFocusable = false
        LayoutInflater.from(context).inflate(R.layout.view_settings_checkbox_row, this, true)

        checkboxView = findViewById(R.id.scr_checkbox)
        iconView = findViewById(R.id.scr_icon)
        titleView = findViewById(R.id.scr_title)
        subtitleView = findViewById(R.id.scr_subtitle)
        headingArea = findViewById(R.id.scr_headingArea)

        bindActivation()
        exposeSingleCheckboxSemantics()
        alignExplanationToCellInset()
        applyAttributes(attrs)
    }

    /**
     * Sets the option caption from a raw text value.
     */
    fun setTitle(text: CharSequence) {
        titleView.text = text
    }

    /**
     * Sets the option caption from a string resource.
     */
    fun setTitle(@StringRes resId: Int) {
        setTitle(context.getText(resId))
    }

    /**
     * Sets the explanation. Empty/null hides it without reserving a second row; an obvious option
     * omits a redundant explanation.
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
     * Sets the explanation from a string resource.
     */
    fun setSubtitle(@StringRes resId: Int) {
        setSubtitle(context.getText(resId))
    }

    /**
     * Sets the optional leading semantic glyph; `null` hides the slot without reserving it.
     */
    fun setIcon(icon: Drawable?) {
        iconView.setImageDrawable(icon)
        iconView.visibility = if (icon == null) View.GONE else View.VISIBLE
        updateGlyphGap()
    }

    /**
     * Sets the optional leading semantic glyph from a drawable resource.
     */
    fun setIcon(@DrawableRes resId: Int) {
        iconView.setImageResource(resId)
        iconView.visibility = View.VISIBLE
        updateGlyphGap()
    }

    private fun updateGlyphGap() {
        val params = titleView.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = if (iconView.visibility == View.VISIBLE) resources.getDimensionPixelSize(R.dimen.boolean_glyph_gap) else 0
        titleView.layoutParams = params
    }

    /**
     * Registers the listener invoked when the option changes state; replaces any previous one.
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
        isChecked = value
        checkedChangeListener = listener
    }

    /**
     * A disabled row never toggles from any activation path; the reason stays in the explanation,
     * and the state is conveyed by more than the reduced alpha.
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        headingArea.isEnabled = enabled
        checkboxView.isEnabled = enabled
        iconView.isEnabled = enabled
        titleView.isEnabled = enabled
        subtitleView.isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
        refreshAccessibilityState()
    }

    /**
     * Toggles exactly once per intentional activation of the heading area.
     */
    private fun bindActivation() {
        headingArea.isFocusable = true
        // Only the painted checkbox, glyph and caption start a touch activation. Native View
        // handling still owns the gesture and receives CANCEL when the scroll parent intercepts.
        var rejectedTouch = false
        headingArea.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                rejectedTouch = listOf(checkboxView, iconView, titleView).none { child ->
                    if (child.visibility != View.VISIBLE) false else {
                        val rect = Rect()
                        child.getDrawingRect(rect)
                        (headingArea as ViewGroup).offsetDescendantRectToMyCoords(child, rect)
                        rect.contains(event.x.toInt(), event.y.toInt())
                    }
                }
            }
            rejectedTouch
        }
        headingArea.setOnClickListener {
            if (!isEnabled) return@setOnClickListener
            isChecked = !isChecked
            checkedChangeListener?.invoke(isChecked)
        }
        headingArea.setOnKeyListener { _, keyCode, event ->
            // Space toggles the focused enabled checkbox; the view consumes it so the host page
            // never scrolls on the same press.
            if (keyCode != android.view.KeyEvent.KEYCODE_SPACE) false else {
                if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) headingArea.performClick()
                true
            }
        }
    }

    /**
     * One boolean option for accessibility services: the heading area reports checkbox semantics
     * with the caption as its name and the explanation as its description, and the painted
     * MaterialCheckBox is marked out (layout attribute) so no second checkbox competes with it.
     */
    private fun exposeSingleCheckboxSemantics() {
        headingAreaAccessibilityDelegate()
    }

    private fun headingAreaAccessibilityDelegate() {
        headingArea.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo,
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = MaterialCheckBox::class.java.name
                info.isCheckable = true
                info.isChecked = checkboxView.isChecked
                info.isEnabled = isEnabled
                info.text = titleView.text
                if (subtitleView.visibility == View.VISIBLE) {
                    info.hintText = subtitleView.text
                }
            }
        })
    }

    private fun refreshAccessibilityState() {
        headingArea.sendAccessibilityEvent(
            android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        )
    }

    /**
     * The explanation starts at the cell's reading-start inset: the laid-out checkbox plus the
     * 8-unit gap. Recomputed on every checkbox layout so font scaling keeps the two aligned.
     */
    private fun alignExplanationToCellInset() {
        val gap = resources.getDimensionPixelSize(R.dimen.boolean_checkbox_gap)
        checkboxView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val inset = checkboxView.width + gap
            subtitleView.applyCellInset(inset)
        }
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        if (attrs == null) return
        context.obtainStyledAttributes(attrs, R.styleable.SettingsCheckboxRow).use { typedArray ->
            setTitle(typedArray.getText(R.styleable.SettingsCheckboxRow_scr_title) ?: "")
            setSubtitle(typedArray.getText(R.styleable.SettingsCheckboxRow_scr_subtitle))
            isChecked = typedArray.getBoolean(R.styleable.SettingsCheckboxRow_scr_checked, false)
            val iconRes = typedArray.getResourceId(R.styleable.SettingsCheckboxRow_scr_icon, 0)
            if (iconRes != 0) setIcon(iconRes)
        }
    }

    /**
     * Applies the cell's reading-start inset ([start]) as the explanation's start margin.
     */
    private fun TextView.applyCellInset(start: Int) {
        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        params.marginStart = start
        layoutParams = params
    }
}
