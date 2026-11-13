package com.sza.tourist.ui.common.widget

import android.app.Application
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.sza.tourist.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UnifiedUiComponentsTest {
    private lateinit var context: Context

    @Before fun prepare() {
        context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Tourist)
    }

    @Test fun `checkbox activation emits once and silent restore emits nothing`() {
        val row = SettingsCheckboxRow(context)
        var calls = 0
        row.setOnCheckedChangeListener { calls++ }
        row.setCheckedSilently(true)
        assertEquals(0, calls)
        val heading = row.findViewById<View>(R.id.scr_headingArea)
        heading.performClick()
        assertFalse(row.isChecked)
        assertEquals(1, calls)
        row.isEnabled = false
        heading.performClick()
        assertFalse(row.isChecked)
        assertEquals(1, calls)
    }

    @Test fun `explanation and background never edit option`() {
        val row = SettingsCheckboxRow(context)
        row.setSubtitle("An explanation")
        assertFalse(row.performClick())
        assertFalse(row.findViewById<View>(R.id.scr_subtitle).performClick())
        assertFalse(row.isChecked)
    }

    @Test fun `space is consumed on both edges and changes value once`() {
        val row = SettingsCheckboxRow(context)
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        activity.setContentView(row)
        layout(row, 320)
        val heading = row.findViewById<View>(R.id.scr_headingArea)
        heading.isFocusableInTouchMode = true
        heading.requestFocus()
        assertTrue(heading.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE)))
        assertFalse(row.isChecked)
        assertTrue(heading.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SPACE)))
        assertTrue(row.isChecked)
    }

    @Test fun `scroll cancellation cannot complete a pending checkbox tap`() {
        val row = SettingsCheckboxRow(context)
        row.setTitle("Caption")
        layout(row, 320)
        val heading = row.findViewById<View>(R.id.scr_headingArea)
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(0, 0, action, 12f, 12f, 0)
            heading.dispatchTouchEvent(event)
            event.recycle()
        }
        assertFalse(row.isChecked)
    }

    @Test fun `group starts collapsed and does not reset child values`() {
        val group = CollapsibleGroup(context)
        val input = EditText(context).apply { setText("Retained") }
        group.addBodyControl(input)
        assertEquals(View.GONE, group.body.visibility)
        group.toggle()
        assertEquals(View.VISIBLE, group.body.visibility)
        group.toggle()
        group.toggle()
        assertEquals("Retained", input.text.toString())
        assertFalse(group.body.performClick())
        assertTrue(group.isExpanded)
    }

    @Test fun `group choices persist separately by stable key`() {
        val key = "test.${System.nanoTime()}"
        val first = CollapsibleGroup(context).apply { setRememberKey(key); toggle() }
        assertTrue(first.isExpanded)
        assertTrue(CollapsibleGroup(context).apply { setRememberKey(key) }.isExpanded)
        assertFalse(CollapsibleGroup(context).apply { setRememberKey("$key.other") }.isExpanded)
    }

    @Test fun `command wrapping equalizes visual rows without stretching widths`() {
        val panel = CommandFlowLayout(context)
        val short = TextView(context).apply { text = "A"; minHeight = 48 }
        val tall = TextView(context).apply { text = "Two\nlines"; minHeight = 48 }
        val next = TextView(context).apply { text = "An entire command that wraps"; minHeight = 48 }
        listOf(short, tall, next).forEach { panel.addView(it) }
        layout(panel, 220)
        assertEquals(short.height, tall.height)
        assertTrue(short.width < panel.width)
        assertTrue(next.top >= short.bottom)
        assertTrue(next.right <= panel.width)
    }

    @Test fun `large text grows checkbox heading without clipping`() {
        val row = SettingsCheckboxRow(context)
        row.setTitle("A long translated caption with several words")
        row.findViewById<TextView>(R.id.scr_title).textSize = 40f
        layout(row, 260)
        val title = row.findViewById<TextView>(R.id.scr_title)
        assertTrue(title.lineCount > 1)
        assertTrue(title.height >= title.layout.height)
    }

    @Test fun `destructive dialog defaults to cancel and retains explicit destructive activation`() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val themed = ContextThemeWrapper(activity, R.style.Theme_Tourist)
        var deletions = 0
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(themed)
            .setTitle("Delete history?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> deletions++ }
            .show()
        DialogCommandPolicy.apply(dialog, destructive = true)
        val cancel = dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE)
        val confirm = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
        assertTrue(cancel.hasFocus())
        assertEquals(cancel, (cancel.parent as android.view.ViewGroup).getChildAt(0))
        assertEquals(0, deletions)
        confirm.performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(1, deletions)
        activity.finish()
    }

    private fun layout(view: View, width: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, width, view.measuredHeight)
    }
}
