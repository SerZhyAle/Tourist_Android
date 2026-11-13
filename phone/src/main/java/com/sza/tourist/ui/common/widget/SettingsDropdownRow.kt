package com.sza.tourist.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.res.use
import androidx.core.view.updateLayoutParams
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.sza.tourist.R
import com.sza.tourist.databinding.ViewSettingsDropdownRowBinding
import kotlin.math.ceil

/**
 * Reusable dropdown row for settings and dialog surfaces.
 *
 * Layout: title, then a Material outlined field (TextInputLayout + read-only TextInputEditText).
 * The row opens its own modal [ListPopupWindow] so the option list receives D-pad keys and appears
 * in the accessibility window walk.
 *
 * The help-icon chrome of the source stays behind with the in-app help (S0014). Public XML attributes
 * use the `sdr_` prefix (see `attrs.xml`).
 */
class SettingsDropdownRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSettingsDropdownRowBinding.inflate(LayoutInflater.from(context), this)

    private val textGroup: LinearLayout = binding.sdrTextGroup
    private val titleView: TextView = binding.sdrTitle
    private val subtitleView: TextView = binding.sdrSubtitle
    private val inputLayout: TextInputLayout = binding.sdrInputLayout
    private val valueView: TextInputEditText = binding.sdrValue
    private val valueTextView: TextView = binding.sdrValueText
    private val valueTextIcon: ImageView = binding.sdrValueTextIcon

    private var itemSelectedListener: ((Int) -> Unit)? = null
    private var entries: List<CharSequence> = emptyList()
    private var selectedIndex: Int = -1
    private var optionsPopup: ListPopupWindow? = null

    private var fieldWidthPx: Int = LayoutParams.MATCH_PARENT
    private var valueAsText: Boolean = false
    private var labelColumnWidthPx: Int = 0

    private var valueAsTextHugged: Boolean? = null
    private var valueStacked: Boolean = false

    init {
        orientation = VERTICAL

        bindItemSelection()
        applyAttributes(attrs, defStyleAttr)
    }

    /**
     * Sets the row title from a raw text value.
     */
    fun setTitle(text: CharSequence) {
        titleView.text = text
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
        if (valueAsText) syncValueAsTextTextGroup()
    }

    /**
     * Sets the subtitle from a string resource.
     */
    fun setSubtitle(@StringRes resId: Int) {
        setSubtitle(context.getText(resId))
    }

    /**
     * Sets the option list shown by the row and refreshes the displayed value.
     */
    fun setEntries(items: List<CharSequence>) {
        entries = items
        dismissOptions()
        if (selectedIndex in items.indices) {
            showValue(items[selectedIndex])
        }
    }

    /**
     * Selects the entry at [index] without invoking the selection listener.
     */
    fun setSelection(index: Int) {
        selectedIndex = index
        if (index in entries.indices) {
            showValue(entries[index])
        }
    }

    private fun showValue(text: CharSequence) {
        valueView.setText(text)
        valueTextView.text = text
    }

    /**
     * Currently selected entry index, or -1 when nothing is selected.
     */
    fun getSelectedIndex(): Int = selectedIndex

    /**
     * Registers the listener invoked when a dropdown entry is chosen. Replaces any previous listener.
     */
    fun setOnItemSelectedListener(listener: ((Int) -> Unit)?) {
        itemSelectedListener = listener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        titleView.isEnabled = enabled
        subtitleView.isEnabled = enabled
        inputLayout.isEnabled = enabled
        valueView.isEnabled = enabled
        valueTextView.isEnabled = enabled
        valueTextIcon.isEnabled = enabled
        alpha = if (enabled) 1f else 0.5f
        if (!enabled) dismissOptions()
    }

    override fun onDetachedFromWindow() {
        dismissOptions()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (valueAsText && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val gap = resources.getDimensionPixelSize(R.dimen.settings_help_icon_margin)
            val glyph = resources.getDimensionPixelSize(R.dimen.settings_help_icon_size) + gap
            val content = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
            val label = if (labelColumnWidthPx > 0) {
                labelColumnWidthPx + resources.getDimensionPixelSize(R.dimen.margin_medium)
            } else {
                measureLabelNaturalWidth()
            }
            val besideRoom = content - label - glyph - gap
            val stack = labelColumnWidthPx <= 0 &&
                besideRoom < measureValueNaturalWidth() &&
                besideRoom < content / STACK_VALUE_ROOM_DIVISOR
            setValueStacked(stack)
            val room = if (stack) content - glyph - gap else besideRoom
            val cap = room.coerceAtLeast(0)
            if (valueTextView.maxWidth != cap) {
                valueTextView.maxWidth = cap
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun bindItemSelection() {
        valueView.showSoftInputOnFocus = false
        valueView.setOnClickListener { showOptions() }
        inputLayout.setEndIconOnClickListener { showOptions() }
        valueView.setOnKeyListener { _, keyCode, event ->
            if (!isConfirmKey(keyCode)) return@setOnKeyListener false
            if (event.action == KeyEvent.ACTION_UP) showOptions()
            true
        }
    }

    private fun isConfirmKey(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == KeyEvent.KEYCODE_ENTER ||
        keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

    private fun showOptions() {
        if (!isEnabled || entries.isEmpty() || optionsPopup != null) return
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, entries)
        val popup = ListPopupWindow(context).apply {
            anchorView = if (valueAsText) this@SettingsDropdownRow else inputLayout
            isModal = true
            width = measurePopupContentWidth(context, adapter, minWidthPx = if (valueAsText) 0 else inputLayout.width)
            height = ListPopupWindow.WRAP_CONTENT
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                dismiss()
                applySelection(position)
            }
            setOnDismissListener { optionsPopup = null }
        }
        optionsPopup = popup
        popup.show()
        if (selectedIndex in entries.indices) popup.listView?.setSelection(selectedIndex)
    }

    private fun dismissOptions() {
        optionsPopup?.dismiss()
        optionsPopup = null
    }

    private fun applySelection(position: Int) {
        setSelection(position)
        itemSelectedListener?.invoke(position)
    }

    private fun applyAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs == null) return
        context.obtainStyledAttributes(attrs, R.styleable.SettingsDropdownRow, defStyleAttr, 0).use { typedArray ->
            setTitle(typedArray.getText(R.styleable.SettingsDropdownRow_sdr_title) ?: "")
            setSubtitle(typedArray.getText(R.styleable.SettingsDropdownRow_sdr_subtitle))
            val entriesRes = typedArray.getResourceId(R.styleable.SettingsDropdownRow_sdr_entries, 0)
            if (entriesRes != 0) {
                setEntries(resources.getTextArray(entriesRes).toList())
            }
            fieldWidthPx = typedArray.getDimensionPixelSize(
                R.styleable.SettingsDropdownRow_sdr_fieldWidth,
                LayoutParams.MATCH_PARENT,
            )
            val fieldMaxWidthPx = typedArray.getDimensionPixelSize(R.styleable.SettingsDropdownRow_sdr_fieldMaxWidth, 0)
            if (fieldMaxWidthPx > 0) inputLayout.maxWidth = fieldMaxWidthPx
            valueAsText = typedArray.getBoolean(R.styleable.SettingsDropdownRow_sdr_valueAsText, false)
            if (valueAsText) {
                applyValueAsTextLayout()
            } else if (typedArray.getBoolean(R.styleable.SettingsDropdownRow_sdr_inline, false)) {
                applyInlineLayout()
            } else if (fieldWidthPx != LayoutParams.MATCH_PARENT) {
                inputLayout.updateLayoutParams<LayoutParams> { width = fieldWidthPx }
            }
        }
    }

    private fun applyInlineLayout() {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        textGroup.updateLayoutParams<LayoutParams> {
            width = 0
            weight = 1f
            marginEnd = resources.getDimensionPixelSize(R.dimen.settings_help_icon_margin)
        }
        binding.sdrTitleLineSpacer.visibility = View.GONE
        inputLayout.updateLayoutParams<LayoutParams> {
            if (fieldWidthPx != LayoutParams.MATCH_PARENT) {
                width = fieldWidthPx
                weight = 0f
            } else {
                width = 0
                weight = 1f
            }
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
    }

    private fun applyValueAsTextLayout() {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        syncValueAsTextTextGroup()
        inputLayout.visibility = View.GONE
        valueTextView.visibility = View.VISIBLE
        valueTextIcon.visibility = View.VISIBLE
        isClickable = true
        isFocusable = true
        setOnClickListener { showOptions() }
        setOnKeyListener { _, keyCode, event ->
            if (!isConfirmKey(keyCode)) return@setOnKeyListener false
            if (event.action == KeyEvent.ACTION_UP) showOptions()
            true
        }
        val background = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        if (background.resourceId != 0) setBackgroundResource(background.resourceId)
    }

    private fun syncValueAsTextTextGroup() {
        val hug = subtitleView.visibility == View.GONE && !valueStacked
        if (hug == valueAsTextHugged) return
        valueAsTextHugged = hug
        textGroup.updateLayoutParams<LayoutParams> {
            width = if (hug) LayoutParams.WRAP_CONTENT else 0
            weight = if (hug) 0f else 1f
        }
        binding.sdrTitleLine.updateLayoutParams<ViewGroup.LayoutParams> {
            width = if (hug) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.sdrTitleLineSpacer.visibility = if (hug || valueStacked) View.GONE else View.VISIBLE
    }

    private fun setValueStacked(stacked: Boolean) {
        if (stacked == valueStacked) return
        valueStacked = stacked
        binding.sdrTitleLine.orientation = if (stacked) VERTICAL else HORIZONTAL
        valueTextView.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = if (stacked) 0 else resources.getDimensionPixelSize(R.dimen.settings_help_icon_margin)
        }
        syncValueAsTextTextGroup()
        binding.sdrTitleLineSpacer.visibility =
            if (valueAsTextHugged == true || stacked) View.GONE else View.VISIBLE
    }

    private fun measureValueNaturalWidth(): Int {
        val text = valueTextView.text ?: ""
        return ceil(valueTextView.paint.measureText(text, 0, text.length)).toInt() +
            valueTextView.paddingStart + valueTextView.paddingEnd
    }

    private fun measureLabelNaturalWidth(): Int {
        val unbounded = MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        titleView.measure(unbounded, unbounded)
        return titleView.measuredWidth
    }

    private companion object {
        const val STACK_VALUE_ROOM_DIVISOR = 3
    }
}
