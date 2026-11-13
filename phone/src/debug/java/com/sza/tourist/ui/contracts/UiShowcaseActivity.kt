package com.sza.tourist.ui.contracts

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.sza.tourist.R
import com.sza.tourist.ui.common.widget.CollapsibleGroup
import com.sza.tourist.ui.common.widget.CommandFlowLayout
import com.sza.tourist.ui.common.widget.SettingsCheckboxRow

/** Reproducible real-control fixture, excluded from release packages. */
class UiShowcaseActivity : AppCompatActivity() {
    lateinit var option: SettingsCheckboxRow
    lateinit var group: CollapsibleGroup
    lateinit var panel: CommandFlowLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        body.addView(TextView(this).apply {
            text = "Tourist UI"
        })
        option = SettingsCheckboxRow(this).apply {
            setTitle(R.string.settings_awake)
            setSubtitle(R.string.settings_awake_description)
            setIcon(R.drawable.ic_settings)
        }
        body.addView(option)
        body.addView(SettingsCheckboxRow(this).apply {
            setTitle(R.string.settings_awake)
            setSubtitle(R.string.settings_awake_description)
            setCheckedSilently(true)
            isEnabled = false
        })
        group = CollapsibleGroup(this).apply {
            setTitle(getString(R.string.settings_resources))
            setExpanded(true)
        }
        val groupBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        panel = CommandFlowLayout(this)
        listOf(R.string.cancel, R.string.settings_help, R.string.settings_export).forEach { label ->
            panel.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                setText(label)
                cornerRadius = (4 * resources.displayMetrics.density).toInt()
                minHeight = (48 * resources.displayMetrics.density).toInt()
                maxLines = Int.MAX_VALUE
                ellipsize = null
            })
        }
        groupBody.addView(panel)
        groupBody.addView(TextView(this).apply { text = getString(R.string.settings_diagnostics_description) })
        group.addBodyControl(groupBody)
        body.addView(group)
        body.addView(CollapsibleGroup(this).apply { setTitle(getString(R.string.settings_related)) })
        body.addView(TextView(this).apply {
            text = List(12) { getString(R.string.settings_awake_description) }.joinToString("\n\n")
        })
        setContentView(ScrollView(this).apply { addView(body) })
    }
}
