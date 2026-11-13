package com.sza.tourist.ui.contracts

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import com.sza.tourist.BuildConfig
import com.sza.tourist.R
import com.sza.tourist.ui.settings.SettingsActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Device evidence with real draw commits; this runner is present only in debug APKs. */
class UiContractInstrumentation : Instrumentation() {
    private lateinit var arguments: Bundle
    private lateinit var output: File
    private val assertions = JSONArray()
    private val timings = JSONArray()

    override fun onCreate(arguments: Bundle?) {
        this.arguments = arguments ?: Bundle()
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        output = File(targetContext.getExternalFilesDir(null), "ui-contracts/${arguments.getString("scenario", "baseline")}")
        output.mkdirs()
        var failure: String? = null
        try {
            components()
            if (arguments.getString("timings", "true") == "true") navigationTimings()
            else captureSettings()
        } catch (problem: Throwable) {
            failure = problem.stackTraceToString()
        }
        val config = targetContext.resources.configuration
        val report = JSONObject().put("build", BuildConfig.VERSION_NAME).put("buildType", "debug")
            .put("device", android.os.Build.MODEL).put("api", android.os.Build.VERSION.SDK_INT)
            .put("locale", config.locales.toLanguageTags()).put("fontScale", config.fontScale)
            .put("densityDpi", config.densityDpi).put("assertions", assertions).put("timings", timings)
            .put("failure", failure ?: JSONObject.NULL)
        File(output, "results.json").writeText(report.toString(2))
        finish(if (failure == null) Activity.RESULT_OK else Activity.RESULT_CANCELED, Bundle().apply {
            putString("stream", if (failure == null) "ui-contracts: PASS\n${output.absolutePath}" else "ui-contracts: FAIL\n$failure")
        })
    }

    private fun components() {
        val activity = startActivitySync(Intent(targetContext, UiShowcaseActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as UiShowcaseActivity
        waitForIdleSync()
        awaitFocus(activity)
        frame(activity) { activity.window.decorView.invalidate() }
        screenshot("showcase-initial.png")
        onMain {
            val heading = activity.option.findViewById<View>(R.id.scr_headingArea)
            activity.option.setCheckedSilently(false)
            heading.performClick()
            checkThat("caption activation toggles", activity.option.isChecked)
            activity.option.findViewById<View>(R.id.scr_subtitle).performClick()
            checkThat("explanation does not toggle", activity.option.isChecked)
            activity.option.isEnabled = false
            heading.performClick()
            checkThat("disabled activation does not toggle", activity.option.isChecked)
            activity.option.isEnabled = true
            heading.isFocusableInTouchMode = true
            checkThat("checkbox accepts keyboard focus", heading.requestFocus())
            checkThat("focus does not edit", activity.option.isChecked)
        }
        sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE))
        sendKeySync(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SPACE))
        waitForIdleSync()
        onMain { checkThat("hardware space toggles once", !activity.option.isChecked) }
        // A real parent-intercepted scroll starts inside the heading.
        val position = IntArray(2)
        onMain { activity.option.findViewById<View>(R.id.scr_headingArea).getLocationOnScreen(position) }
        val down = SystemClock.uptimeMillis()
        pointer(down, down, MotionEvent.ACTION_DOWN, position[0] + 20f, position[1] + 30f)
        for (step in 1..12) {
            SystemClock.sleep(16)
            pointer(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, position[0] + 20f, position[1] + 30f + step * 12)
        }
        pointer(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, position[0] + 20f, position[1] + 174f)
        waitForIdleSync()
        onMain {
            checkThat("scroll does not toggle", !activity.option.isChecked)
            val group = activity.group
            val header = group.findViewById<View>(R.id.cg_header)
            header.performClick()
            checkThat("whole header collapses", !group.isExpanded && group.body.visibility == View.GONE)
            header.performClick()
            checkThat("whole header expands", group.isExpanded && group.body.visibility == View.VISIBLE)
            group.body.performClick()
            checkThat("body does not collapse", group.isExpanded)
            checkThat("touch target minimum", header.height >= 48 * activity.resources.displayMetrics.density - 1)
            val caption = activity.option.findViewById<TextView>(R.id.scr_title)
            val surface = MaterialColors.getColor(caption, com.google.android.material.R.attr.colorSurface)
            val contrast = ColorUtils.calculateContrast(caption.currentTextColor, surface)
            assertions.put(JSONObject().put("name", "caption contrast").put("ratio", contrast).put("ink", caption.currentTextColor).put("face", surface))
            checkThat("caption contrast >= 4.5", contrast >= 4.5)
            val panel = activity.panel
            val children = (0 until panel.childCount).map { panel.getChildAt(it) }
            children.filterIsInstance<com.google.android.material.button.MaterialButton>().forEachIndexed { index, button ->
                val face = checkNotNull(button.backgroundTintList).getColorForState(button.drawableState, surface)
                val ratio = ColorUtils.calculateContrast(button.currentTextColor, face)
                assertions.put(JSONObject().put("name", "command $index contrast").put("ratio", ratio)
                    .put("ink", button.currentTextColor).put("face", face))
                checkThat("command $index contrast >= 4.5", ratio >= 4.5)
            }
            checkThat("commands stay within panel", children.all { it.left >= 0 && it.right <= panel.width })
            checkThat("commands fit content or available space", children.filterIsInstance<TextView>().all { button ->
                val natural = kotlin.math.ceil(button.paint.measureText(button.text.toString()).toDouble()).toInt() +
                    button.compoundPaddingLeft + button.compoundPaddingRight
                button.width <= maxOf(button.minWidth, natural) + 2
            })
            checkThat("row heights agree", children.groupBy { it.top }.values.all { row -> row.map { it.height }.distinct().size == 1 })
        }
        frame(activity) { activity.window.decorView.invalidate() }
        screenshot("showcase.png")
        onMain { activity.finish() }
        waitForIdleSync()
    }

    private fun navigationTimings() {
        for (target in listOf("main", "about")) {
            repeat(20) { sample ->
                val activity = settings(if (target == "main") "about" else "main")
                val elapsed = frame(activity) { activity.findViewById<View>(destination(target)).performClick() }
                timings.put(JSONObject().put("destination", target).put("kind", "first-visit").put("sample", sample + 1).put("ms", elapsed))
                onMain { activity.finish() }
                waitForIdleSync()
            }
        }
        val activity = settings("main")
        repeat(20) { sample ->
            for (target in listOf("about", "main")) {
                val elapsed = frame(activity) { activity.findViewById<View>(destination(target)).performClick() }
                timings.put(JSONObject().put("destination", target).put("kind", "warm").put("sample", sample + 1).put("ms", elapsed))
            }
        }
        screenshot("settings-main.png")
        frame(activity) { activity.findViewById<View>(R.id.settings_nav_about).performClick() }
        screenshot("settings-about.png")
        onMain { activity.finish() }
    }

    private fun captureSettings() {
        val activity = settings("main")
        val before = targetContext.getSharedPreferences("settings", 0).all
        screenshot("settings-main.png")
        frame(activity) { activity.findViewById<View>(R.id.settings_nav_about).performClick() }
        screenshot("settings-about.png")
        frame(activity) { activity.findViewById<View>(R.id.settings_nav_main).performClick() }
        checkThat("navigation does not edit settings", before == targetContext.getSharedPreferences("settings", 0).all)
        onMain {
            checkThat("selected page is visible", activity.findViewById<View>(R.id.settings_main_page).isShown)
            checkThat("other page is hidden", !activity.findViewById<View>(R.id.settings_about_page).isShown)
            activity.findViewById<View>(R.id.settings_menu)?.performClick()
        }
        waitForIdleSync()
        screenshot("settings-menu.png")
        onMain { activity.finish() }
    }

    private fun settings(page: String): SettingsActivity {
        targetContext.getSharedPreferences("settings_navigation", 0).edit().putString("page", page).commit()
        val activity = startActivitySync(Intent(targetContext, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        awaitFocus(activity)
        val deadline = SystemClock.uptimeMillis() + 10000
        while (true) {
            var ready = false
            onMain { ready = activity.findViewById<View>(R.id.settings_main_page) != null }
            if (ready) break
            check(SystemClock.uptimeMillis() < deadline) { "Settings pages did not become ready" }
            SystemClock.sleep(20)
        }
        frame(activity) { activity.window.decorView.invalidate() }
        return activity
    }

    private fun destination(page: String) = if (page == "main") R.id.settings_nav_main else R.id.settings_nav_about

    private fun frame(activity: Activity, activate: () -> Unit): Double {
        val committed = CountDownLatch(1)
        var start = 0L
        var end = 0L
        onMain {
            val root = activity.window.decorView
            check(android.os.Build.VERSION.SDK_INT >= 29) { "Frame-commit measurement requires API 29+" }
            root.viewTreeObserver.registerFrameCommitCallback { end = SystemClock.elapsedRealtimeNanos(); committed.countDown() }
            start = SystemClock.elapsedRealtimeNanos()
            activate()
            root.invalidate()
        }
        check(committed.await(10, TimeUnit.SECONDS)) { "No committed frame" }
        return (end - start) / 1_000_000.0
    }

    private fun screenshot(name: String) {
        waitForIdleSync()
        SystemClock.sleep(250)
        val bitmap = checkNotNull(uiAutomation.takeScreenshot())
        File(output, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun pointer(down: Long, time: Long, action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(down, time, action, x, y, 0)
        sendPointerSync(event)
        event.recycle()
    }
    private fun awaitFocus(activity: Activity) {
        val deadline = SystemClock.uptimeMillis() + 10000
        while (true) {
            var focused = false
            onMain { focused = activity.hasWindowFocus() }
            if (focused) return
            check(SystemClock.uptimeMillis() < deadline) { "Activity did not obtain window focus" }
            SystemClock.sleep(20)
        }
    }

    private fun onMain(block: () -> Unit) {
        var failure: Throwable? = null
        runOnMainSync { try { block() } catch (problem: Throwable) { failure = problem } }
        failure?.let { throw it }
    }
    private fun checkThat(name: String, condition: Boolean) {
        assertions.put(JSONObject().put("name", name).put("pass", condition))
        check(condition) { name }
    }
}
