package com.sza.tourist.help

import android.content.Context
import timber.log.Timber

/** Reads the bundled help map once per process; the map ships inside the package (CONTEXT-HELP rule 17). */
object HelpMapStore {

    const val MAP_ASSET = "help/help-map.json"

    @Volatile
    private var cached: HelpMap? = null

    fun get(context: Context): HelpMap {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: load(context).also { cached = it }
        }
    }

    private fun load(context: Context): HelpMap =
        try {
            val json = context.applicationContext.assets.open(MAP_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            HelpMap.parse(json)
        } catch (e: Exception) {
            // A broken map must not take the screen down: with no surfaces, no window asks for help.
            Timber.e(e, "help map unreadable")
            HelpMap(1, "", "help", "{locale}/{help}.html", "index", "en", emptyList())
        }
}
