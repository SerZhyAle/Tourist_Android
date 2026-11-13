package com.sza.tourist.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The resolver of CONTEXT-HELP rule 17 against the real help map the package ships, and against small
 * synthetic maps for the cases the real one does not contain (a loop, an unknown parent).
 */
class HelpResolverTest {

    private val map: HelpMap = HelpMap.parse(File("src/main/assets/help/help-map.json").readText())

    @Test
    fun `a page row resolves to its own page`() {
        assertEquals(HelpTarget("dashboard", "dashboard", null), HelpResolver.resolve(map, "dashboard"))
    }

    @Test
    fun `an inheriting dialog resolves to its parent's page at its own entry`() {
        assertEquals(HelpTarget("counter.start", "counter", "e-start"), HelpResolver.resolve(map, "counter.start"))
        assertEquals(HelpTarget("stopwatch.music", "stopwatch-settings", "e-track"), HelpResolver.resolve(map, "stopwatch.music"))
    }

    @Test
    fun `a none row and an unknown surface resolve to nothing`() {
        assertNull(HelpResolver.resolve(map, "counter.reset"))
        assertNull(HelpResolver.resolve(map, "widget.stopwatch"))
        assertNull(HelpResolver.resolve(map, "no-such-surface"))
    }

    @Test
    fun `the key and the button resolve to the same target`() {
        val byKey = HelpResolver.resolveHost(map, "com.sza.tourist.ui.flashlight.LightsActivity")
        val byButton = HelpResolver.resolveAsked(map, "lights")
        assertNotNull(byKey)
        assertEquals(byKey, byButton)
    }

    @Test
    fun `the lock-bearing flashlights ask for nothing on their own window`() {
        // Owner decision (S0014): no help control and no F1 on the water and front flashlights.
        assertNull(HelpResolver.resolveHost(map, "com.sza.tourist.ui.flashlight.WaterFlashlightActivity"))
        assertNull(HelpResolver.resolveHost(map, "com.sza.tourist.ui.flashlight.FrontFlashlightActivity"))
        assertNull(HelpResolver.resolveAsked(map, "water-flashlight"))
        // Their help is still a place on the Lights page, for a link that points there.
        assertEquals(HelpTarget("water-flashlight", "lights", "e-water"), HelpResolver.resolve(map, "water-flashlight"))
        assertEquals(HelpTarget("front-flashlight", "lights", "e-front"), HelpResolver.resolve(map, "front-flashlight"))
    }

    @Test
    fun `an element with its own entry sends the key to that entry`() {
        val target = HelpResolver.resolveHost(map, "com.sza.tourist.ui.sos.SosActivity", focusedViewEntry = "btnSosStop")
        assertEquals(HelpTarget("sos", "sos", "e-stop"), target)
        // A focused view without an entry of its own goes to the top of the page.
        assertEquals(
            HelpTarget("sos", "sos", null),
            HelpResolver.resolveHost(map, "com.sza.tourist.ui.sos.SosActivity", focusedViewEntry = "ivSosGlyph"),
        )
    }

    @Test
    fun `a class that is not a surface asks for nothing`() {
        assertNull(HelpResolver.resolveHost(map, "com.sza.tourist.ui.dialog.ColorPickerDialog"))
        assertNull(HelpResolver.resolveHost(map, "com.example.Elsewhere"))
    }

    @Test
    fun `an inherit loop or a missing parent resolves to nothing instead of looping`() {
        val loop = HelpMap.parse(
            """{"surfaces":[
                {"id":"a","form":"inherit","parent":"b","reason":"x"},
                {"id":"b","form":"inherit","parent":"a","reason":"x"},
                {"id":"c","form":"inherit","parent":"missing","reason":"x"}]}""",
        )
        assertNull(HelpResolver.resolve(loop, "a"))
        assertNull(HelpResolver.resolve(loop, "c"))
    }

    @Test
    fun `a page is chosen by the application language and falls back to the source language`() {
        val shipped = setOf("en", "ru", "uk")
        assertEquals("ru", HelpResolver.chooseLocale(shipped, "ru-RU", "en"))
        assertEquals("uk", HelpResolver.chooseLocale(shipped, "uk", "en"))
        assertEquals("en", HelpResolver.chooseLocale(shipped, "de-DE", "en"))
        assertEquals("en", HelpResolver.chooseLocale(shipped, "", "en"))
    }

    @Test
    fun `a page path is built from the map's template`() {
        assertEquals("help/ru/sos.html", HelpResolver.pagePath(map, "ru", "sos"))
    }

    @Test
    fun `every row of the shipped map is well formed`() {
        val ids = map.surfaces.map { it.id }
        assertEquals("surface ids are unique", ids.size, ids.toSet().size)
        for (s in map.surfaces) {
            when (s.form) {
                HelpResolver.FORM_PAGE -> {
                    assertNotNull("${s.id} is a page and needs a help id", s.help)
                    assertTrue("${s.id} lists its elements", s.elements.isNotEmpty())
                    assertEquals("anchors of ${s.id} are unique", s.elements.size, s.elements.map { it.anchor }.toSet().size)
                }
                HelpResolver.FORM_INHERIT -> {
                    assertTrue("${s.id} inherits from a row", s.parent in ids)
                    assertNotNull("${s.id} gives a reason", s.reason)
                }
                HelpResolver.FORM_NONE -> assertNotNull("${s.id} gives a reason", s.reason)
                else -> error("${s.id} has form '${s.form}', which this product does not build")
            }
        }
    }

    @Test
    fun `every shipped locale has a page for every page row`() {
        val locales = File("src/main/assets/help").listFiles { f -> f.isDirectory }.orEmpty().map { it.name }
        assertTrue("the three shipped locales have page folders", locales.containsAll(listOf("en", "ru", "uk")))
        for (locale in locales) {
            for (s in map.surfaces.filter { it.form == HelpResolver.FORM_PAGE }) {
                val page = File("src/main/assets/${HelpResolver.pagePath(map, locale, s.help!!)}")
                assertTrue("missing page $page", page.isFile)
            }
        }
    }
}
