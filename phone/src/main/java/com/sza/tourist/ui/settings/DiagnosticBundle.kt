package com.sza.tourist.ui.settings

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** DIAGNOSTIC-REPORT 0.14 sections 2 and 8: an allowlist, never a preference or log dump. */
object DiagnosticBundle {
    fun write(output: OutputStream, version: String, api: Int, memoryMb: Long) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("environment.txt"))
            zip.write(("schema_version=1\nproduct=Tourist\nversion=$version\n" +
                "android_api=$api\nmax_heap_mb=$memoryMb\n" +
                "volume_metrics=unavailable\nsession_logs=unavailable\n").toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("report.txt"))
            zip.write(("Tourist diagnostic summary.\n" +
                "This product does not keep session log files.\n" +
                "No location, trip, counter history, music URI, account, identifier or user text is included.\n" +
                "The archive is created only on request and is not uploaded.\n").toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}
