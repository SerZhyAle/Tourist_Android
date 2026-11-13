package com.sza.tourist.help

import android.content.Context
import android.content.res.Configuration
import androidx.fragment.app.FragmentActivity
import timber.log.Timber
import java.io.IOException

/**
 * Opens a help page over the live screen (CONTEXT-HELP rule 7): the screen below stays resumed, so a
 * running siren, torch or stopwatch is never stopped by asking, and Back returns to it.
 */
object HelpLauncher {

    private const val TAG = "tourist.help"

    /** Shows [target]'s page, or does nothing when the page is already open or the state is saved. */
    fun open(host: FragmentActivity, target: HelpTarget) {
        val fm = host.supportFragmentManager
        if (fm.isStateSaved || fm.findFragmentByTag(TAG) != null) return
        val map = HelpMapStore.get(host)
        val path = pageAsset(host, map, target.helpId)
        val dark = (host.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        HelpDialogFragment.newInstance(path, target.anchor, dark).show(fm, TAG)
    }

    /**
     * The asset path of the page in the application's language, falling back to the source language and
     * then to the index, never to a missing page (rule 13).
     */
    internal fun pageAsset(context: Context, map: HelpMap, helpId: String): String {
        val locale = HelpResolver.chooseLocale(
            available = shippedLocales(context, map),
            languageTag = context.resources.configuration.locales[0].toLanguageTag(),
            sourceLocale = map.sourceLocale,
        )
        val candidates = listOf(
            HelpResolver.pagePath(map, locale, helpId),
            HelpResolver.pagePath(map, map.sourceLocale, helpId),
            HelpResolver.pagePath(map, map.sourceLocale, map.index),
        )
        return candidates.firstOrNull { assetExists(context, it) } ?: candidates.last()
    }

    /** The locales that have a page directory in the package. */
    internal fun shippedLocales(context: Context, map: HelpMap): Set<String> =
        try {
            context.assets.list(map.helpRoot).orEmpty()
                .filter { context.assets.list("${map.helpRoot}/$it").orEmpty().isNotEmpty() }
                .toSet()
        } catch (e: IOException) {
            Timber.w(e, "help locales unreadable")
            emptySet()
        }

    private fun assetExists(context: Context, path: String): Boolean =
        try {
            context.assets.open(path).close()
            true
        } catch (_: IOException) {
            false
        }
}
