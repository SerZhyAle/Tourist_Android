package com.sza.tourist.help

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The help gate (CONTEXT-HELP rule 19) inside the repository's normal check: `./gradlew :phone:test` runs
 * `tools/help/check-help.ps1` and fails on a surface with no row, a row with no surface, a page missing in a
 * shipped locale, a dead link, a request in a page, or a reason that is missing. Freshness of the screenshots
 * (rung 2) needs the layouts to be unchanged since the capture, so this run skips it and the release pre-flight
 * runs the gate without `-SkipFreshness`. Skipped, not passed, where PowerShell 7 is not installed.
 */
class HelpGateTest {

    @Test
    fun `the help gate passes`() {
        val repoRoot = File("..").canonicalFile
        val script = File(repoRoot, "tools/help/check-help.ps1")
        assumeTrue("the gate script is part of the checkout", script.isFile)
        val process = try {
            ProcessBuilder("pwsh", "-NoProfile", "-File", script.path, "-SkipFreshness")
                .directory(repoRoot)
                .redirectErrorStream(true)
                .start()
        } catch (_: java.io.IOException) {
            assumeTrue("PowerShell 7 (pwsh) is not installed, so the gate could not be run", false)
            return
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(5, TimeUnit.MINUTES)
        println(output.trim())
        assertEquals("the gate did not finish in time", true, finished)
        assertEquals("help gate output:\n$output", 0, process.exitValue())
    }
}
