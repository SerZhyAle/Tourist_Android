package com.sza.tourist.ui.settings

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class DiagnosticBundleTest {
    @Test fun `archive has versioned environment and contains only allowlisted values`() {
        val bytes = ByteArrayOutputStream()
        DiagnosticBundle.write(bytes, "2.61.0271.000", 36, 256)
        val entries = linkedMapOf<String, String>()
        ZipInputStream(bytes.toByteArray().inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        assertEquals(setOf("environment.txt", "report.txt"), entries.keys)
        assertTrue(entries.getValue("environment.txt").contains("schema_version=1\n"))
        assertTrue(entries.getValue("environment.txt").contains("volume_metrics=unavailable\n"))
        assertTrue(bytes.size() < 4096)
        assertFalse(entries.values.any { it.contains("content://") || it.contains("/storage/") })
    }
}
