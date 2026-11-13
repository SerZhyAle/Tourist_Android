package com.sza.tourist.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sza.tourist.R
import kotlin.math.max

/** BUTTONS sections 3-4: wrap whole commands, then equalize only each visual row's heights. */
class CommandFlowLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {
    private val gap get() = resources.getDimensionPixelSize(R.dimen.token_spacing_8)
    private data class Row(val children: List<View>, val width: Int, val height: Int)
    private var rows = emptyList<Row>()

    override fun generateDefaultLayoutParams() = MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    override fun generateLayoutParams(attrs: AttributeSet) = MarginLayoutParams(context, attrs)
    override fun generateLayoutParams(p: LayoutParams) = MarginLayoutParams(p)
    override fun checkLayoutParams(p: LayoutParams) = p is MarginLayoutParams

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
        val limit = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE / 4 else available
        val prepared = mutableListOf<Row>()
        var current = mutableListOf<View>()
        var rowWidth = 0
        var rowHeight = 0
        fun finishRow() {
            if (current.isEmpty()) return
            prepared += Row(current.toList(), rowWidth, rowHeight)
            current = mutableListOf(); rowWidth = 0; rowHeight = 0
        }
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == GONE) continue
            val margins = child.layoutParams as MarginLayoutParams
            // Command margins come from the shared panel gap rather than old weighted-row XML.
            val childLimit = limit.coerceAtLeast(0)
            child.measure(MeasureSpec.makeMeasureSpec(childLimit, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            if (current.isNotEmpty() && rowWidth + gap + child.measuredWidth > limit) finishRow()
            if (current.isNotEmpty()) rowWidth += gap
            current += child
            rowWidth += child.measuredWidth
            rowHeight = max(rowHeight, child.measuredHeight + margins.topMargin + margins.bottomMargin)
        }
        finishRow()
        prepared.forEach { row -> row.children.forEach { child ->
            val margins = child.layoutParams as MarginLayoutParams
            child.measure(MeasureSpec.makeMeasureSpec(child.measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(row.height - margins.topMargin - margins.bottomMargin, MeasureSpec.EXACTLY))
        } }
        rows = prepared
        val height = prepared.sumOf { it.height } + gap * (prepared.size - 1).coerceAtLeast(0) + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize((prepared.maxOfOrNull { it.width } ?: 0) + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        var y = paddingTop
        rows.forEach { row ->
            var x = if (rtl) width - paddingRight else paddingLeft
            row.children.forEach { child ->
                val margins = child.layoutParams as MarginLayoutParams
                val childLeft = if (rtl) x - child.measuredWidth else x
                child.layout(childLeft, y + margins.topMargin, childLeft + child.measuredWidth, y + margins.topMargin + child.measuredHeight)
                x += (child.measuredWidth + gap) * if (rtl) -1 else 1
            }
            y += row.height + gap
        }
    }

    companion object {
        /** Reserve known action states once at the current type scale, never container width. */
        fun reserveStates(button: TextView, labels: List<CharSequence>) {
            val textWidth = labels.maxOfOrNull { button.paint.measureText(it.toString()).toDouble() } ?: 0.0
            button.minWidth = max(button.minWidth, kotlin.math.ceil(textWidth).toInt() + button.compoundPaddingLeft + button.compoundPaddingRight)
        }
    }
}
