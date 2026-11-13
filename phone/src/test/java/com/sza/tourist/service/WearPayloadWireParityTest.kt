package com.sza.tourist.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * S0008: the payloads of the watch pair exist twice, in `:phone` and in `:wear`, because the two modules
 * share no Kotlin source set (the same reason `WearDataLayerPaths` is mirrored). The wire is Gson, so the
 * contract is the key of every constructor property - the `@SerializedName` value where one is declared,
 * the property name where it is not (the watch keeps those classes unobfuscated).
 *
 * This test reads both source files and asserts the key lists are identical per class, in order. A drift is
 * a silent runtime failure on the Data Layer - a field that arrives as null - so it is checked at build time.
 */
class WearPayloadWireParityTest {

    private val root = locateRepoRoot()

    @Test
    fun `the health payload has the same keys on both sides`() = assertParity("WearTouristHealthPayload")

    @Test
    fun `the weather payloads have the same keys on both sides`() = assertParity("WearTouristWeatherPayload")

    @Test
    fun `the weather request payload has the same keys on both sides`() = assertParity(
        file = "WearTouristWeatherPayload",
        className = "WearTouristWeatherRequestPayload",
    )

    @Test
    fun `the settings payload has the same keys on both sides`() = assertParity("WearSettingsPayload")

    @Test
    fun `the envelope has the same keys and the same codec field names on both sides`() {
        assertParity("WearEventEnvelope")
        assertEquals(codecFieldNames(phoneFile("WearEventEnvelopeCodec")), codecFieldNames(wearFile("WearEventEnvelopeCodec")))
    }

    private fun assertParity(file: String, className: String = file) {
        val phoneKeys = wireKeys(phoneFile(file), className)
        val wearKeys = wireKeys(wearFile(file), className)
        assertTrue("$className declares no constructor property on the phone", phoneKeys.isNotEmpty())
        assertEquals("wire keys of $className", phoneKeys, wearKeys)
    }

    private fun phoneFile(name: String) = File(root, "phone/src/main/java/com/sza/tourist/domain/model/$name.kt")

    private fun wearFile(name: String) = File(root, "wear/src/main/java/com/sza/tourist/wear/domain/model/$name.kt")

    /** The keys of the constructor properties of [className], in declaration order. */
    private fun wireKeys(file: File, className: String): List<String> {
        assertTrue("Missing ${file.absolutePath}", file.isFile)
        val text = file.readText()
        val start = Regex("""data class $className\(""").find(text)?.range?.last
            ?: error("No data class $className in ${file.name}")
        // The parameter list closes on a line that starts with the paren.
        val end = Regex("""\n\)""").find(text, start)?.range?.first ?: error("Unterminated class $className")
        return Regex("""(?:@SerializedName\("(\w+)"\)\s+)?\bval\s+(\w+)\s*:""")
            .findAll(text.substring(start, end))
            .map { match -> match.groupValues[1].ifEmpty { match.groupValues[2] } }
            .toList()
    }

    private fun codecFieldNames(file: File): Map<String, String> {
        assertTrue("Missing ${file.absolutePath}", file.isFile)
        return Regex("""const\s+val\s+([A-Z_]+)\s*=\s*"(\w+)"""")
            .findAll(file.readText())
            .associate { match -> match.groupValues[1] to match.groupValues[2] }
    }

    private fun locateRepoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Could not locate repository root from ${System.getProperty("user.dir")}")
    }
}
