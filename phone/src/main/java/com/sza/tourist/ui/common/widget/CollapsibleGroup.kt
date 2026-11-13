package com.sza.tourist.ui.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.use
import androidx.core.view.ViewCompat
import com.sza.tourist.R

/**
 * The product's collapsible group (COLLAPSIBLE-GROUP 1.1, S0015): a semantic container that brings
 * together the controls of one topic behind a distinct header.
 *
 * The header - triangle, caption and empty header space alike - toggles the group exactly once on
 * an intentional tap; the body and its controls never toggle it. The triangle is a vector
 * current-state marker: toward reading end while collapsed, down while expanded, mirrored in RTL
 * by the drawable itself. Expansion inserts the body immediately below its own header in normal
 * flow; collapsing removes it, and focus that lived inside returns to the header. Values and state
 * of the included controls are never touched.
 *
 * The initial state comes from XML (`cg_expanded`, read once); when [setRememberKey] names a stable
 * group id, later user choices persist per that key and are restored on the next construction, so
 * a remembered expansion is never treated as an edit.
 *
 * Public XML attributes use the `cg_` prefix (see `attrs.xml`).
 */
class CollapsibleGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val headerView: LinearLayout
    private val markerView: ImageView
    private val titleView: TextView
    private val bodyHost: FrameLayout

    private var rememberKey: String? = null

    /** The host every included control is added to. */
    val body: ViewGroup get() = bodyHost

    /**
     * Whether the group currently shows its body.
     */
    var isExpanded: Boolean = false
        private set

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_collapsible_group, this, true)

        headerView = findViewById(R.id.cg_header)
        markerView = findViewById(R.id.cg_marker)
        titleView = findViewById(R.id.cg_title)
        bodyHost = findViewById(R.id.cg_body)
        bodyHost.visibility = View.GONE
        headerView.isFocusable = true

        bindHeaderActivation()
        exposeHeaderSemantics()
        applyAttributes(attrs)
    }

    /**
     * Sets the group's short localized topic heading.
     */
    fun setTitle(text: CharSequence) {
        titleView.text = text
    }

    /**
     * Adds a control to the group body.
     */
    fun addBodyControl(view: View) {
        bodyHost.addView(view)
    }

    /**
     * Names the stable group id whose expansion the product remembers across openings. Passing
     * `null` keeps the group stateless (XML/programmatic only). Unknown or retired keys are
     * tolerated: a missing record keeps the initial state.
     */
    fun setRememberKey(key: String?) {
        rememberKey = key
        val stored = key?.let { prefs.getBoolean(it, isExpanded) } ?: return
        setExpandedInternal(stored, persist = false, notify = false)
    }

    /**
     * Toggles the group; the whole header is one intentional activation target.
     */
    fun toggle() {
        setExpandedInternal(!isExpanded, persist = true, notify = true)
    }

    /**
     * Expands or collapses without user activation (initial restore, search/error navigation).
     * Never runs preference-change handlers and never touches the included controls' values.
     */
    fun setExpanded(expanded: Boolean) {
        setExpandedInternal(expanded, persist = false, notify = true)
    }

    private fun setExpandedInternal(expanded: Boolean, persist: Boolean, notify: Boolean) {
        if (isExpanded == expanded) return
        val anchor = IntArray(2)
        headerView.getLocationOnScreen(anchor)
        isExpanded = expanded
        renderState()
        if (expanded) {
            bodyHost.visibility = View.VISIBLE
        } else {
            // A focused descendant hides with the body; focus returns to this group's header.
            if (bodyHost.hasFocus()) headerView.requestFocus()
            bodyHost.visibility = View.GONE
        }
        if (persist && rememberKey != null) {
            prefs.edit().putBoolean(rememberKey!!, expanded).apply()
        }
        if (notify) announceStateChange()
        if (isAttachedToWindow) headerView.post {
            val after = IntArray(2)
            headerView.getLocationOnScreen(after)
            var ancestor = parent
            while (ancestor is View) {
                if (ancestor is android.widget.ScrollView) {
                    ancestor.scrollBy(0, after[1] - anchor[1])
                    break
                }
                ancestor = ancestor.parent
            }
        }
    }

    /**
     * Triangle direction and header state description follow the current state; rotation is
     * direction-aware so an RTL layout's marker still ends up pointing down when expanded.
     */
    private fun renderState() {
        val rtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        markerView.rotation = when {
            isExpanded -> if (rtl) -90f else 90f
            else -> 0f
        }
        headerView.isSelected = isExpanded
    }

    private fun bindHeaderActivation() {
        headerView.setOnClickListener { toggle() }
        headerView.setOnKeyListener { _, keyCode, event ->
            if (keyCode != android.view.KeyEvent.KEYCODE_SPACE) false else {
                if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) headerView.performClick()
                true
            }
        }
    }

    /**
     * The header exposes its topic, expanded state and body relationship: a button-role node whose
     * state description names the current state, with the framework expand/collapse actions.
     */
    private fun exposeHeaderSemantics() {
        headerView.setAccessibilityDelegate(object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo,
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = android.widget.Button::class.java.name
                info.text = titleView.text
                val action = if (isExpanded) {
                    AccessibilityNodeInfo.ACTION_COLLAPSE
                } else {
                    AccessibilityNodeInfo.ACTION_EXPAND
                }
                info.addAction(action)
            }

            override fun performAccessibilityAction(host: View, action: Int, arguments: android.os.Bundle?): Boolean {
                return when (action) {
                    AccessibilityNodeInfo.ACTION_EXPAND -> {
                        setExpandedInternal(true, persist = true, notify = true); true
                    }
                    AccessibilityNodeInfo.ACTION_COLLAPSE -> {
                        setExpandedInternal(false, persist = true, notify = true); true
                    }
                    else -> super.performAccessibilityAction(host, action, arguments)
                }
            }
        })
    }

    private fun announceStateChange() {
        ViewCompat.setStateDescription(headerView, context.getText(
            if (isExpanded) R.string.group_state_expanded else R.string.group_state_collapsed,
        ))
        headerView.sendAccessibilityEvent(
            android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        )
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        if (attrs == null) return
        context.obtainStyledAttributes(attrs, R.styleable.CollapsibleGroup).use { typedArray ->
            setTitle(typedArray.getText(R.styleable.CollapsibleGroup_cg_title) ?: "")
            isExpanded = typedArray.getBoolean(R.styleable.CollapsibleGroup_cg_expanded, false)
            renderState()
            bodyHost.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        renderState()
    }

    private val prefs get() =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val PREFS_NAME = "collapsible_group_states"
    }
}
