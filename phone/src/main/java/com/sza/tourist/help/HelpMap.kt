package com.sza.tourist.help

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * The help map of CONTEXT-HELP rule 17: one row per surface the product draws, read by the resolver at
 * run time and by the gate (`tools/help/check-help.ps1`) at check time - the same file, so the two cannot
 * disagree.
 *
 * Read through Gson's tree and not by reflection into these classes: R8 renames reflected fields, and a
 * tree reader also ignores members it does not know, which is what the contract's `schemaVersion` promises
 * a later shape.
 */
data class HelpMap(
    val schemaVersion: Int,
    val product: String,
    val helpRoot: String,
    val pageTemplate: String,
    val index: String,
    val sourceLocale: String,
    val surfaces: List<HelpSurface>,
) {

    fun surface(id: String): HelpSurface? = surfaces.firstOrNull { it.id == id }

    /** The surface drawn by the Activity or DialogFragment class [hostClassName], if the map names one. */
    fun surfaceForHost(hostClassName: String): HelpSurface? = surfaces.firstOrNull { it.host == hostClassName }

    companion object {
        fun parse(json: String): HelpMap {
            val root = JsonParser.parseString(json).asJsonObject
            return HelpMap(
                schemaVersion = root.int("schemaVersion") ?: 1,
                product = root.string("product").orEmpty(),
                helpRoot = root.string("helpRoot") ?: "help",
                pageTemplate = root.string("pageTemplate") ?: "{locale}/{help}.html",
                index = root.string("index") ?: "index",
                sourceLocale = root.string("sourceLocale") ?: "en",
                surfaces = root.array("surfaces").map { HelpSurface.from(it.asJsonObject) },
            )
        }
    }
}

/**
 * One surface. [form] is `page`, `inherit` or `none` in this product (`layer` and `both` are in the
 * contract but not built here). [host] is the class that draws the surface when it is a screen or a
 * dialog fragment; a surface drawn by a call site carries a `Class#member` name instead.
 * [ask] is `off` for a window that deliberately wires neither F1 nor the button (its [reason] says why).
 */
data class HelpSurface(
    val id: String,
    val name: String,
    val form: String,
    val help: String?,
    val host: String?,
    val parent: String?,
    val reason: String?,
    val ask: String?,
    val anchor: String?,
    val elements: List<HelpElement>,
) {
    val asksOnWindow: Boolean get() = ask != "off"

    companion object {
        fun from(o: JsonObject) = HelpSurface(
            id = o.string("id").orEmpty(),
            name = o.string("name").orEmpty(),
            form = o.string("form").orEmpty(),
            help = o.string("help"),
            host = o.string("host"),
            parent = o.string("parent"),
            reason = o.string("reason"),
            ask = o.string("ask"),
            anchor = o.string("anchor"),
            elements = o.array("elements").map { HelpElement.from(it.asJsonObject) },
        )
    }
}

/** An element with its own entry on the surface's page: [anchor] on the page, [view] the resource entry name. */
data class HelpElement(val anchor: String, val view: String?) {
    companion object {
        fun from(o: JsonObject) = HelpElement(anchor = o.string("anchor").orEmpty(), view = o.string("view"))
    }
}

private fun JsonObject.string(name: String): String? =
    get(name)?.takeUnless(JsonElement::isJsonNull)?.asString

private fun JsonObject.int(name: String): Int? =
    get(name)?.takeUnless(JsonElement::isJsonNull)?.asInt

private fun JsonObject.array(name: String): List<JsonElement> =
    get(name)?.takeIf(JsonElement::isJsonArray)?.asJsonArray?.toList().orEmpty()
