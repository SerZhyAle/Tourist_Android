package com.sza.tourist.buildlogic

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * The one reading of the build machine's local wall clock, as `yyMMddHHmm` (PACKAGE-VERSIONING P1).
 *
 * A ValueSource and not a bare `LocalDateTime.now()` where the version is declared: a clock read directly
 * at configuration time is serialised into a configuration-cache entry and replayed unchanged minutes
 * later, which would stamp a stale instant without a warning. A ValueSource is re-obtained before an
 * entry is reused and invalidates it when the value differs.
 */
abstract class BuildClockValueSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"))
}
