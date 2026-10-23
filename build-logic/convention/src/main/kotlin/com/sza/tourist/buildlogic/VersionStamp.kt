package com.sza.tourist.buildlogic

import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * The package families of this product, as PACKAGE-VERSIONING (Android profile, rule 3) has them. The three
 * Tourist rows are filed with the catalog as PROPOSAL-2026-10-23-tourist-android-bands and join the band
 * table when its owner folds the proposal in; the maps below are the ones proposed there.
 *
 * Phone and watch share one application id, so their bands of the last versionCode digit are disjoint; the
 * watch face has an application id of its own and keeps its own namespace of codes. The `partition` of a
 * family is a function of the build instant alone: no family ever needs another family's code to stay clear
 * of it, which is what an independent release cadence requires.
 */
enum class PackageFamily(val moduleName: String) {
    /** `com.sza.tourist`, band 0..5, a 10 minute slot. */
    PHONE("phone"),

    /** `com.sza.tourist` (shared id), band 6..9, a 15 minute slot. */
    WATCH("wear"),

    /** `com.sza.tourist.watchface`, own namespace, band 0..5, a 10 minute slot. */
    WATCH_FACE("watchface");

    fun partition(minute: Int): Int = when (this) {
        PHONE, WATCH_FACE -> minute / 10
        WATCH -> 6 + minute / 15
    }

    companion object {
        fun forModule(moduleName: String): PackageFamily =
            entries.firstOrNull { it.moduleName == moduleName }
                ?: throw IllegalArgumentException(
                    "Module '$moduleName' is not a registered package family " +
                        "(${entries.joinToString { it.moduleName }})."
                )
    }
}

enum class VersionSource { PINNED, STAMPED, SENTINEL }

data class ResolvedVersion(val name: String, val code: Int, val source: VersionSource)

/**
 * versionName and versionCode of one package, two renderings of one instant (PACKAGE-VERSIONING rules
 * 1-3). Nothing here reads the clock: the instant is always an argument, which is what lets a
 * fixed-instant test pin the whole derivation.
 *
 *   versionName  `Y.YM.MDDH.Hmm` - a pure timestamp encoding, `2.60.9262.322` for 2026-09-26 23:22.
 *   versionCode  `yyMMddHH * 10 + p` - nine digits, `p` the family's partition of the minute.
 */
object VersionStamp {
    /**
     * The checked-in constant of rule 7: a non-releasable sentinel. It is derived by the same formula
     * from a fixed, past instant (the day this skeleton was stamped, S0002), so it cannot drift from
     * the formula; an artifact that carries it is an artifact nobody stamped and never reaches a channel.
     */
    const val SENTINEL_INSTANT = "2610230000"

    private val instantFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuMMddHHmm").withResolverStyle(ResolverStyle.STRICT)

    private val nameShape = Regex("""^(\d)\.(\d{2})\.(\d{4})\.(\d{3})$""")

    /** P1: the instant must be a real calendar date and time, `yyMMddHHmm`. */
    fun parseInstant(raw: String): LocalDateTime {
        require(raw.length == 10 && raw.all(Char::isDigit)) {
            "Build instant '$raw' is not ten digits (yyMMddHHmm)."
        }
        return try {
            LocalDateTime.parse(raw, instantFormat)
        } catch (e: DateTimeException) {
            throw IllegalArgumentException("Build instant '$raw' is not a real calendar date and time.", e)
        }
    }

    fun versionName(raw: String): String {
        parseInstant(raw)
        val yy = raw.substring(0, 2)
        val mon = raw.substring(2, 4)
        val dd = raw.substring(4, 6)
        val hh = raw.substring(6, 8)
        val mm = raw.substring(8, 10)
        return "${yy[0]}.${yy[1]}${mon[0]}.${mon[1]}$dd${hh[0]}.${hh[1]}$mm"
    }

    fun versionCode(family: PackageFamily, raw: String): Int {
        val instant = parseInstant(raw)
        return raw.substring(0, 8).toInt() * 10 + family.partition(instant.minute)
    }

    /** The inverse of [versionName]: the instant a name encodes, as `yyMMddHHmm`. */
    fun instantOfName(name: String): String {
        val match = nameShape.matchEntire(name)
            ?: throw IllegalArgumentException("versionName '$name' does not have the shape Y.YM.MDDH.Hmm.")
        val (decade, yearAndMonth, monthDayHour, hourMinute) = match.destructured
        val raw = decade + yearAndMonth[0] + yearAndMonth[1] + monthDayHour[0] +
            monthDayHour.substring(1, 3) + monthDayHour[3] + hourMinute
        parseInstant(raw)
        return raw
    }

    /**
     * The three sources of a version, in this order (PACKAGE-VERSIONING rules 5-7):
     *  1. a pinned pair, which always wins - both halves or neither, and the pair must agree with itself
     *     and with the family's partition (a one-sided or self-contradicting pin is refused);
     *  2. the in-build stamp of [stampInstant], when this invocation packages and nobody pinned;
     *  3. the sentinel.
     *
     * Gradle configures every module of the build, built or not, and a pin names one family's code. A
     * module that [built] says this invocation does not build therefore ignores the pin's code (its
     * artifact is never produced); a pin that is one-sided or whose name is no instant is refused for
     * every module, because that mistake is the invocation's and not one family's.
     */
    fun resolve(
        family: PackageFamily,
        pinnedName: String?,
        pinnedCode: String?,
        stampInstant: String?,
        built: Boolean = true,
    ): ResolvedVersion {
        require((pinnedName == null) == (pinnedCode == null)) {
            "Pass -Ptourist.versionName and -Ptourist.versionCode together or neither. Got " +
                "versionName=${pinnedName ?: "<absent>"}, versionCode=${pinnedCode ?: "<absent>"}."
        }
        if (pinnedName != null && pinnedCode != null) {
            if (!built) {
                return resolve(family, null, null, null)
            }
            val pinnedInstant = instantOfName(pinnedName)
            val code = pinnedCode.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid -Ptourist.versionCode value: '$pinnedCode'.")
            val expected = versionCode(family, pinnedInstant)
            require(code == expected) {
                "Pinned pair does not agree with itself for ${family.moduleName}: versionName " +
                    "$pinnedName encodes a build whose versionCode is $expected, not $code. A pin names " +
                    "one family's code, so build one module per invocation (for example :${family.moduleName}:assemble...)."
            }
            return ResolvedVersion(pinnedName, code, VersionSource.PINNED)
        }
        val instant = stampInstant ?: SENTINEL_INSTANT
        return ResolvedVersion(
            versionName(instant),
            versionCode(family, instant),
            if (stampInstant != null) VersionSource.STAMPED else VersionSource.SENTINEL,
        )
    }
}

/** When does an invocation package an artifact, and so owe a stamp? */
object StampPolicy {
    /**
     * `startParameter.taskNames` carries task options and their values as well as task names, so
     * `--tests "*Install*"` would make a unit-test-only run look like a packaging run. A task name
     * never contains `*`, `.` or `#` and never starts with `-`; those shapes are dropped first.
     *
     * `connected` and `baselineprofile` count as packaging because those tasks INSTALL what they build
     * on a real device, and an installed artifact must never carry the sentinel. `uninstall` produces
     * nothing and is excluded before the `install` test.
     */
    fun packagingRequested(taskNames: List<String>): Boolean =
        taskNames
            .filter { !it.startsWith("-") && !it.contains("*") && !it.contains(".") && !it.contains("#") }
            .any { requested ->
                val name = requested.lowercase()
                if (name.contains("uninstall")) {
                    false
                } else {
                    listOf("assemble", "bundle", "install", "package", "connected", "baselineprofile")
                        .any(name::contains)
                }
            }

    /**
     * Does this invocation build [moduleName]? A task qualified with another project's path
     * (`:phone:assembleStoreDebug`) does not; an unqualified task (`assembleRelease`) runs in every
     * module. No task at all - an IDE sync, `help` - builds nothing.
     */
    fun targetsModule(taskNames: List<String>, moduleName: String): Boolean =
        taskNames
            .filter { !it.startsWith("-") && !it.contains("*") && !it.contains(".") && !it.contains("#") }
            .any { requested -> !requested.startsWith(":") || requested.startsWith(":$moduleName:") }

    fun releasePackagingRequested(taskNames: List<String>): Boolean =
        packagingRequested(taskNames) && taskNames.any { it.lowercase().contains("release") }
}
