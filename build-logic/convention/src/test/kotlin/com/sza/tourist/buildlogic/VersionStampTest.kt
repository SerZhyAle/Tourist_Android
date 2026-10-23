package com.sza.tourist.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Fixed-instant vectors of the PACKAGE-VERSIONING Android profile (rung 2 of the contract's section 4):
 * the derivation never reads the clock, so one pinned instant pins the whole pair.
 */
class VersionStampTest {
    @Test
    fun `contract example renders the documented name and codes`() {
        // PACKAGE-VERSIONING rule 2: the instant 2609262322 (2026-09-26 23:22) is `2.60.9262.322`.
        assertEquals("2.60.9262.322", VersionStamp.versionName("2609262322"))
        // Rule 3 with this product's proposed bands: phone floor(22/10)=2, watch 6+floor(22/15)=7,
        // watch face floor(22/10)=2.
        assertEquals(260926232, VersionStamp.versionCode(PackageFamily.PHONE, "2609262322"))
        assertEquals(260926237, VersionStamp.versionCode(PackageFamily.WATCH, "2609262322"))
        assertEquals(260926232, VersionStamp.versionCode(PackageFamily.WATCH_FACE, "2609262322"))
    }

    @Test
    fun `an instant is ten digits of real calendar time`() {
        assertThrows(IllegalArgumentException::class.java) { VersionStamp.parseInstant("26092623") }
        assertThrows(IllegalArgumentException::class.java) { VersionStamp.parseInstant("26132 1230") }
        assertThrows(IllegalArgumentException::class.java) { VersionStamp.parseInstant("2609262399") }
        assertThrows(IllegalArgumentException::class.java) { VersionStamp.parseInstant("2610230000x") }
        // 2028 is a leap year, 2026 is not.
        assertEquals("2028-02-29", VersionStamp.parseInstant("2802291200").toLocalDate().toString())
        assertThrows(IllegalArgumentException::class.java) { VersionStamp.parseInstant("2602291200") }
    }

    @Test
    fun `name and code are inverse renderings of one instant`() {
        for (instant in listOf("2609262322", "2610230000", "2612312359", "2701010000")) {
            val name = VersionStamp.versionName(instant)
            assertEquals(instant, VersionStamp.instantOfName(name))
        }
        assertThrows(IllegalArgumentException::class.java) { VersionStamp.instantOfName("1.23.4567.890") }
        assertThrows(IllegalArgumentException::class.java) { VersionStamp.instantOfName("2.61.0230.0000") }
    }

    @Test
    fun `the bands of the shared id never collide`() {
        // Every minute of the hour: phone takes 0..5, watch takes 6..9, so the two families that share
        // `com.sza.tourist` can never produce the same code from one instant.
        for (minute in 0..59) {
            val instant = "26092623%02d".format(minute)
            val phone = VersionStamp.versionCode(PackageFamily.PHONE, instant)
            val watch = VersionStamp.versionCode(PackageFamily.WATCH, instant)
            assertEquals(phone / 10, watch / 10)
            assert(phone % 10 in 0..5) { "minute $minute: phone band spilled into $phone" }
            assert(watch % 10 in 6..9) { "minute $minute: watch band spilled into $watch" }
        }
    }

    @Test
    fun `codes are monotonic across a slot boundary`() {
        val late = VersionStamp.versionCode(PackageFamily.PHONE, "2609262359")
        val next = VersionStamp.versionCode(PackageFamily.PHONE, "2609270000")
        assert(next > late) { "$next did not order above $late" }
    }

    @Test
    fun `the sentinel is the formula applied to a fixed instant`() {
        val sentinel = VersionStamp.resolve(PackageFamily.PHONE, null, null, null)
        assertEquals(VersionSource.SENTINEL, sentinel.source)
        assertEquals("2.61.0230.000", sentinel.name)
        assertEquals(261023000, sentinel.code)
        assertEquals(261023006, VersionStamp.resolve(PackageFamily.WATCH, null, null, null).code)
        assertEquals(261023000, VersionStamp.resolve(PackageFamily.WATCH_FACE, null, null, null).code)
    }

    @Test
    fun `a pin wins, agrees with itself, and never crosses families`() {
        val pinned = VersionStamp.resolve(
            PackageFamily.WATCH, "2.60.9262.322", "260926237", null,
        )
        assertEquals(VersionSource.PINNED, pinned.source)
        assertEquals("2.60.9262.322", pinned.name)
        assertEquals(260926237, pinned.code)

        // One-sided pins are refused for every module.
        assertThrows(IllegalArgumentException::class.java) {
            VersionStamp.resolve(PackageFamily.PHONE, "2.60.9262.322", null, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VersionStamp.resolve(PackageFamily.PHONE, null, "260926237", null)
        }
        // A watch code on the phone family is a self-contradiction and is refused.
        assertThrows(IllegalArgumentException::class.java) {
            VersionStamp.resolve(PackageFamily.PHONE, "2.60.9262.322", "260926237", null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VersionStamp.resolve(PackageFamily.PHONE, "not-a-name", "1", null)
        }
        // A module this invocation does not build ignores the pin's code but still refuses a broken pin.
        val unbuilt = VersionStamp.resolve(PackageFamily.PHONE, "2.60.9262.322", "260926237", null, built = false)
        assertEquals(VersionSource.SENTINEL, unbuilt.source)
    }

    @Test
    fun `a stamp reads through and the sentinel appears only when nobody stamps`() {
        val stamped = VersionStamp.resolve(PackageFamily.PHONE, null, null, "2609262322")
        assertEquals(VersionSource.STAMPED, stamped.source)
        assertEquals("2.60.9262.322", stamped.name)
        assertEquals(260926232, stamped.code)
        assertThrows(IllegalArgumentException::class.java) {
            VersionStamp.resolve(PackageFamily.PHONE, null, null, "9999999999")
        }
    }

    @Test
    fun `packaging and module targeting follow the task names`() {
        assert(StampPolicy.packagingRequested(listOf("assembleStoreDebug")))
        assert(StampPolicy.packagingRequested(listOf(":wear:bundleNoLegalRelease")))
        assert(StampPolicy.packagingRequested(listOf("installPhone")))
        assert(StampPolicy.packagingRequested(listOf("connectedAndroidTest")))
        assert(!StampPolicy.packagingRequested(listOf("test")))
        assert(!StampPolicy.packagingRequested(listOf("--tests", "*Install*")))
        assert(!StampPolicy.packagingRequested(listOf("uninstallAll")))

        assert(StampPolicy.targetsModule(listOf(":phone:assembleStoreDebug"), "phone"))
        assert(StampPolicy.targetsModule(listOf("assembleRelease"), "phone"))
        assert(!StampPolicy.targetsModule(listOf(":wear:assembleStoreDebug"), "phone"))
        assert(!StampPolicy.targetsModule(emptyList(), "phone"))
    }
}
