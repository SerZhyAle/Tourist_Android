package com.sza.tourist.ui.settings

import android.os.Build
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.sza.tourist.BuildConfig
import com.sza.tourist.R
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.model.UnitSystem
import com.sza.tourist.help.HelpLauncher
import com.sza.tourist.help.HelpTarget
import com.sza.tourist.ui.common.widget.CollapsibleGroup
import com.sza.tourist.ui.common.widget.SettingsCheckboxRow
import com.sza.tourist.ui.common.widget.SettingsDropdownRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Both destinations use the same shell and are prepared before navigation is enabled. */
internal class SettingsPages(private val host: SettingsActivity, private val model: SettingsViewModel) {
    private lateinit var units: SettingsDropdownRow
    private lateinit var awake: SettingsCheckboxRow
    private var exporting = false
    private var exportButton: MaterialButton? = null
    private val export = host.registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) { setExporting(false); return@registerForActivityResult }
        host.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val stream = host.contentResolver.openOutputStream(uri) ?: throw IOException("No output stream")
                    DiagnosticBundle.write(stream, BuildConfig.VERSION_NAME, Build.VERSION.SDK_INT,
                        Runtime.getRuntime().maxMemory() / (1024 * 1024))
                    true
                } catch (failure: IOException) {
                    timber.log.Timber.w(failure, "Diagnostic export failed")
                    false
                } catch (failure: SecurityException) {
                    timber.log.Timber.w(failure, "Diagnostic export permission denied")
                    false
                }
            }
            setExporting(false)
            Toast.makeText(host, if (success) R.string.settings_export_saved else R.string.settings_export_failed, Toast.LENGTH_LONG).show()
        }
    }

    fun mainPage(settings: AppSettings): ScrollView = page(R.id.settings_main_page, R.string.settings_main) { body ->
        body.addView(dropdown(R.id.settings_theme, R.string.settings_theme,
            listOf(R.string.settings_system, R.string.settings_light, R.string.settings_dark).map(host::getString),
            AppearanceSettings.themeIndex(host)) { AppearanceSettings.setTheme(host, it) })
        val tags = listOf("", "en", "ru", "uk")
        body.addView(dropdown(R.id.settings_language, R.string.settings_language,
            listOf(host.getString(R.string.settings_system), "English", "Русский", "Українська"),
            tags.indexOf(AppCompatDelegate.getApplicationLocales().toLanguageTags()).coerceAtLeast(0)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[it]))
        })
        units = dropdown(R.id.settings_units, R.string.settings_units,
            listOf(host.getString(R.string.settings_metric), host.getString(R.string.settings_imperial)),
            UnitSystem.entries.indexOf(settings.unitSystem)) { model.setUnitSystem(UnitSystem.entries[it]) }
        body.addView(units)
        awake = SettingsCheckboxRow(host).apply {
            id = R.id.settings_awake
            setTitle(R.string.settings_awake)
            setSubtitle(R.string.settings_awake_description)
            setCheckedSilently(settings.preventSleep)
            setOnCheckedChangeListener(model::setPreventSleep)
        }
        body.addView(awake)
    }

    fun aboutPage(): ScrollView = page(R.id.settings_about_page, R.string.settings_about) { body ->
        body.addView(text(host.getString(R.string.settings_description), R.id.settings_description))
        body.addView(text(host.getString(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), R.id.settings_version))
        body.addView(group("about.license", R.string.settings_license_title, R.id.settings_license, false) { legal ->
            legal.addView(text(host.getString(R.string.settings_license_text) + "\n\n" + BuildConfig.LICENSE_TEXT, View.NO_ID))
        })
        body.addView(group("about.privacy", R.string.settings_privacy_title, R.id.settings_privacy, false) { privacy ->
            privacy.addView(text(host.getString(R.string.settings_privacy_text), View.NO_ID))
        })
        body.addView(group("about.resources", R.string.settings_resources, R.id.settings_resources) { resources ->
            resources.addView(command(R.string.settings_help, R.drawable.ic_help_outline, R.id.settings_help) {
                HelpLauncher.open(host, HelpTarget("home", "home", null))
            })
            resources.addView(text(host.getString(R.string.settings_diagnostics_description), R.id.settings_diagnostics_description))
            resources.addView(command(R.string.settings_export, R.drawable.ic_save, R.id.settings_export) {
                if (exporting) return@command
                setExporting(true)
                try {
                    export.launch("Tourist-logs-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())}.zip")
                } catch (failure: android.content.ActivityNotFoundException) {
                    setExporting(false)
                    Toast.makeText(host, R.string.settings_export_failed, Toast.LENGTH_LONG).show()
                }
            }.also { exportButton = it })
            resources.addView(text(host.getString(R.string.settings_resource_index), R.id.settings_resource_index))
        })
        body.addView(group("about.related", R.string.settings_related, R.id.settings_related) { related ->
            related.addView(text(host.getString(R.string.settings_related_description), R.id.settings_related_description))
        })
    }

    fun bind(settings: AppSettings) {
        if (!::units.isInitialized) return
        units.setSelection(UnitSystem.entries.indexOf(settings.unitSystem))
        awake.setCheckedSilently(settings.preventSleep)
    }

    private fun page(id: Int, title: Int, build: (LinearLayout) -> Unit): ScrollView = ScrollView(host).apply {
        this.id = id
        isFillViewport = true
        addView(column().apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            addView(text(host.getString(title), View.NO_ID).apply {
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall)
                androidx.core.view.ViewCompat.setAccessibilityHeading(this, true)
            })
            build(this)
        })
    }

    private fun column() = LinearLayout(host).apply { orientation = LinearLayout.VERTICAL }

    private fun text(value: String, id: Int) = TextView(host).apply {
        this.id = id
        text = value
        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun dropdown(id: Int, title: Int, entries: List<String>, selection: Int, onSelect: (Int) -> Unit) =
        SettingsDropdownRow(host).apply {
            this.id = id
            setTitle(title)
            setEntries(entries)
            setSelection(selection)
            setOnItemSelectedListener(onSelect)
            setPadding(0, dp(8), 0, dp(8))
        }

    private fun command(title: Int, glyph: Int, id: Int, action: () -> Unit) =
        MaterialButton(android.view.ContextThemeWrapper(host, R.style.Widget_Tourist_Button_Command), null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            this.id = id
            setText(title)
            setIconResource(glyph)
            cornerRadius = dp(4)
            minHeight = dp(48)
            maxLines = Int.MAX_VALUE
            ellipsize = null
            layoutParams = LinearLayout.LayoutParams(-2, -2)
            setOnClickListener { action() }
        }

    private fun group(key: String, title: Int, id: Int, initiallyExpanded: Boolean = true, build: (LinearLayout) -> Unit) = CollapsibleGroup(host).apply {
        this.id = id
        setTitle(host.getString(title))
        setExpanded(initiallyExpanded)
        setRememberKey(key)
        addBodyControl(column().also(build))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
    }

    private fun setExporting(value: Boolean) { exporting = value; exportButton?.isEnabled = !value }
    private fun dp(value: Int) = (value * host.resources.displayMetrics.density + .5f).toInt()
}
