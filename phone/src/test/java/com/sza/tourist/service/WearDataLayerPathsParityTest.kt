package com.sza.tourist.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * S0006: `WearDataLayerPaths` lives in both `:phone` and `:wear` because the two modules share no
 * Kotlin source set. A drift between the two files is a silent runtime failure on the Data Layer, so
 * this test parses both source files directly and asserts their constant tables are identical.
 */
class WearDataLayerPathsParityTest {

    @Test
    fun `phone and wear WearDataLayerPaths declare the exact same constants and paths`() {
        val root = locateRepoRoot()
        val phoneFile = File(root, "phone/src/main/java/com/sza/tourist/service/WearDataLayerPaths.kt")
        val wearFile = File(root, "wear/src/main/java/com/sza/tourist/wear/data/wear/WearDataLayerPaths.kt")

        assertTrue("Missing phone WearDataLayerPaths.kt at ${phoneFile.absolutePath}", phoneFile.isFile)
        assertTrue("Missing wear WearDataLayerPaths.kt at ${wearFile.absolutePath}", wearFile.isFile)

        val phoneConstants = parseConstants(phoneFile)
        val wearConstants = parseConstants(wearFile)

        assertTrue("Expected at least one constant in WearDataLayerPaths", phoneConstants.isNotEmpty())
        assertEquals(phoneConstants, wearConstants)
        // The EVENT_* constants are WearEventEnvelope.eventType tokens, not routes.
        phoneConstants.filterKeys { !it.startsWith("EVENT_") }.forEach { (name, path) ->
            assertTrue("Expected $name ($path) to start with /tourist/", path.startsWith("/tourist/"))
        }
    }

    private fun locateRepoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Could not locate repository root from ${System.getProperty("user.dir")}")
    }

    private fun parseConstants(file: File): Map<String, String> {
        val regex = Regex("""const\s+val\s+([A-Z0-9_]+)\s*=\s*"([^"]+)"""")
        return regex.findAll(file.readText())
            .associate { match -> match.groupValues[1] to match.groupValues[2] }
    }
}
