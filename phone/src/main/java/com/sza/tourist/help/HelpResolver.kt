package com.sza.tourist.help

/** Where a surface's help is: the page [helpId] of the surface [surfaceId], at [anchor] when set. */
data class HelpTarget(val surfaceId: String, val helpId: String, val anchor: String?)

/**
 * The one resolver of CONTEXT-HELP rule 17: every F1 handler and every help button asks here, and no
 * window carries help code of its own. Pure functions over [HelpMap], so the rules are unit-tested
 * without a device.
 */
object HelpResolver {

    /** The deepest `inherit` chain followed; a map that nests deeper, or loops, resolves to nothing. */
    private const val MAX_INHERIT_DEPTH = 8

    /**
     * Rule 3: the key and the button resolve through the same id and open the same form. [anchor] is
     * an element's anchor on the page; without one the surface's own `anchor` (an `inherit` dialog's
     * entry in its parent's page) applies, and without that the top of the page.
     */
    fun resolve(map: HelpMap, surfaceId: String, anchor: String? = null): HelpTarget? {
        var surface = map.surface(surfaceId) ?: return null
        var ownAnchor = anchor ?: surface.anchor
        repeat(MAX_INHERIT_DEPTH) {
            when (surface.form) {
                FORM_PAGE -> {
                    val helpId = surface.help ?: return null
                    return HelpTarget(surfaceId, helpId, ownAnchor)
                }
                FORM_INHERIT -> {
                    val parent = surface.parent?.let(map::surface) ?: return null
                    ownAnchor = ownAnchor ?: parent.anchor
                    surface = parent
                }
                else -> return null
            }
        }
        return null
    }

    /**
     * The help a window asks for when F1 is pressed or its button is tapped. Null when the class is not a
     * surface of the map, when the map gives it no help, or when the window deliberately does not ask
     * (`ask: off`, the lock-bearing flashlights). [focusedViewEntry] is the resource entry name of the view
     * holding the focus: an element with its own entry sends F1 to that entry's anchor (rule 3).
     */
    fun resolveHost(map: HelpMap, hostClassName: String, focusedViewEntry: String? = null): HelpTarget? {
        val surface = map.surfaceForHost(hostClassName) ?: return null
        if (!surface.asksOnWindow) return null
        val anchor = focusedViewEntry?.let { entry -> surface.elements.firstOrNull { it.view == entry }?.anchor }
        return resolve(map, surface.id, anchor)
    }

    /** The help of a surface named by id, subject to the same `ask` switch as a window. */
    fun resolveAsked(map: HelpMap, surfaceId: String): HelpTarget? {
        val surface = map.surface(surfaceId) ?: return null
        if (!surface.asksOnWindow) return null
        return resolve(map, surfaceId)
    }

    /**
     * Rule 13: a page exists in every shipped locale and is chosen by the application's language, falling
     * back to the source language and never to a missing page.
     */
    fun chooseLocale(available: Set<String>, languageTag: String, sourceLocale: String): String {
        val language = languageTag.substringBefore('-').substringBefore('_').lowercase()
        return if (language in available) language else sourceLocale
    }

    /** The asset path of a page: `help/<locale>/<helpId>.html`, from the map's template. */
    fun pagePath(map: HelpMap, locale: String, helpId: String): String =
        map.helpRoot + "/" + map.pageTemplate.replace("{locale}", locale).replace("{help}", helpId)

    const val FORM_PAGE = "page"
    const val FORM_INHERIT = "inherit"
    const val FORM_NONE = "none"
}
