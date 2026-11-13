package com.sza.tourist.ui.settings

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.sza.tourist.R
import com.sza.tourist.help.HelpButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    private val model: SettingsViewModel by viewModels()
    private val pages = linkedMapOf<String, ScrollView>()
    private val destinations = linkedMapOf<String, TextView>()
    private lateinit var content: FrameLayout
    private var drawer: DrawerLayout? = null
    private var selected = "main"
    private val navigationPrefs by lazy { getSharedPreferences("settings_navigation", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = savedInstanceState?.getString("page") ?: navigationPrefs.getString("page", "main") ?: "main"
        if (selected !in listOf("main", "about")) selected = "main"
        val wide = resources.configuration.screenWidthDp >= 840
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        bar.addView(command(R.string.settings_back, R.drawable.ic_arrow_back) { finish() })
        if (!wide) bar.addView(command(R.string.settings_pages, R.drawable.ic_settings) {
            drawer?.openDrawer(Gravity.START)
        }.apply { id = R.id.settings_menu })
        bar.addView(TextView(this).apply {
            setText(R.string.settings_title)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(HelpButton(this).apply { id = R.id.helpButton }, LinearLayout.LayoutParams(dp(48), dp(48)))
        shell.addView(bar)
        content = FrameLayout(this).apply { id = R.id.settings_content }
        val menu = navigation()
        if (wide) {
            val panes = LinearLayout(this)
            panes.addView(menu, LinearLayout.LayoutParams(dp(220), -1))
            panes.addView(content, LinearLayout.LayoutParams(0, -1, 1f))
            shell.addView(panes, LinearLayout.LayoutParams(-1, 0, 1f))
        } else {
            drawer = DrawerLayout(this).apply {
                addView(content, DrawerLayout.LayoutParams(-1, -1))
                addView(menu, DrawerLayout.LayoutParams(dp(280), -1, Gravity.START))
            }
            shell.addView(drawer, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        setContentView(shell)
        ViewCompat.setOnApplyWindowInsetsListener(shell) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        content.addView(TextView(this).apply { setText(R.string.settings_loading); setPadding(dp(16), dp(16), dp(16), dp(16)) })
        val pageFactory = SettingsPages(this, model)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.settings.collect { settings ->
                    if (settings == null) return@collect
                    if (pages.isEmpty()) {
                        pages["main"] = pageFactory.mainPage(settings)
                        pages["about"] = pageFactory.aboutPage()
                        content.removeAllViews()
                        pages.forEach { (key, page) ->
                            page.visibility = if (key == selected) View.VISIBLE else View.GONE
                            content.addView(page, FrameLayout.LayoutParams(-1, -1))
                            restoreViewport(key, page)
                        }
                        showPage(selected)
                    }
                    pageFactory.bind(settings)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawer?.isDrawerOpen(Gravity.START) == true) drawer?.closeDrawer(Gravity.START) else finish()
            }
        })
    }

    private fun navigation(): ScrollView = ScrollView(this).apply {
        setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
        addView(LinearLayout(this@SettingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            listOf(Triple("main", R.string.settings_main, R.drawable.ic_settings),
                Triple("about", R.string.settings_about, R.drawable.ic_info)).forEach { (key, label, glyph) ->
                val row = androidx.appcompat.widget.AppCompatTextView(this@SettingsActivity).apply {
                    id = if (key == "main") R.id.settings_nav_main else R.id.settings_nav_about
                    setText(label)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                    minimumHeight = dp(48)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    compoundDrawablePadding = dp(8)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(glyph, 0, 0, 0)
                    compoundDrawableTintList = android.content.res.ColorStateList.valueOf(
                        com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
                    isFocusable = true
                    setBackgroundResource(R.drawable.settings_navigation_background)
                    setOnClickListener { showPage(key); drawer?.closeDrawer(Gravity.START) }
                }
                destinations[key] = row
                addView(row, LinearLayout.LayoutParams(-1, -2))
            }
        })
    }

    private fun showPage(key: String) {
        if (pages.isEmpty()) return
        selected = key
        pages.forEach { (id, page) -> page.visibility = if (id == key) View.VISIBLE else View.GONE }
        destinations.forEach { (id, row) ->
            row.isSelected = id == key
            row.setTypeface(null, if (id == key) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            ViewCompat.setStateDescription(row, if (id == key) getString(R.string.settings_selected) else null)
        }
        navigationPrefs.edit().putString("page", key).apply()
        ViewCompat.setAccessibilityPaneTitle(content, getString(if (key == "main") R.string.settings_main else R.string.settings_about))
    }

    private fun restoreViewport(key: String, page: ScrollView) {
        page.doOnLayout {
            val body = page.getChildAt(0) as ViewGroup
            val anchorId = navigationPrefs.getInt("$key.anchor", View.NO_ID)
            val anchor = body.findViewById<View>(anchorId)
            if (anchor != null) page.scrollTo(0, anchor.top - navigationPrefs.getInt("$key.offset", 0))
        }
    }

    override fun onStop() {
        val edit = navigationPrefs.edit()
        pages.forEach { (key, page) ->
            val body = page.getChildAt(0) as ViewGroup
            val anchor = (0 until body.childCount).map { body.getChildAt(it) }
                .firstOrNull { it.bottom > page.scrollY && it.id != View.NO_ID }
            if (anchor != null) edit.putInt("$key.anchor", anchor.id).putInt("$key.offset", anchor.top - page.scrollY)
        }
        edit.apply()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("page", selected)
        super.onSaveInstanceState(outState)
    }

    private fun command(label: Int, glyph: Int, action: () -> Unit) = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        contentDescription = getString(label)
        tooltipText = getString(label)
        text = ""
        iconPadding = 0
        iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        setIconResource(glyph)
        cornerRadius = dp(4)
        minHeight = dp(48)
        minWidth = dp(48)
        minimumWidth = dp(48)
        insetTop = 0
        insetBottom = 0
        setPadding(dp(12), 0, dp(12), 0)
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        setOnClickListener { action() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
}
