package com.sza.tourist.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Presentation preferences never share the phone/watch settings wire. */
object AppearanceSettings {
    private const val PREFS = "appearance"
    private val modes = listOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
    )

    fun themeIndex(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getInt("theme", 0).takeIf { it in modes.indices } ?: 0

    fun applyStoredTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(modes[themeIndex(context)])
    }

    fun setTheme(context: Context, index: Int) {
        if (index !in modes.indices) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("theme", index).apply()
        AppCompatDelegate.setDefaultNightMode(modes[index])
    }
}
