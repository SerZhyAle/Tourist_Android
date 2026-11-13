package com.sza.tourist.wear.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S2462: proves the three version-skew outcomes on the watch side.
 *
 * Mirrors FastMediaSorter's test of the same name, trimmed to this product's one-key contract
 * (`unitSystem`); the phone module carries the matching proof for its copy of the decoder, because the
 * two modules share no source and the contract exists twice.
 *
 * Every fixture is hand-written JSON rather than a serialized [WearSettingsPayload]. That is the point:
 * this build's own payload class cannot express an omitted key or a key of the wrong type, so a fixture
 * built by serializing it could not reproduce the situation the decoder exists to survive.
 */
class WearSettingsPayloadDecoderTest {

    private val decoder = WearSettingsPayloadDecoder()

    @Test
    fun `full payload reports every key present and no divergence`() {
        val result = decoder.decode("""{"unitSystem":"IMPERIAL"}""")

        assertNotNull(result.payload)
        assertEquals("IMPERIAL", result.payload?.unitSystem)
        assertEquals(WearSettingsPayloadDecoder.CONTRACT_FIELDS, result.presentFields)
        assertTrue(result.divergences.toString(), result.divergences.isEmpty())
    }

    @Test
    fun `the contract names exactly the unit system`() {
        assertEquals(setOf("unitSystem"), WearSettingsPayloadDecoder.CONTRACT_FIELDS)
    }

    @Test
    fun `omitted key is reported missing and excluded from present fields`() {
        val result = decoder.decode("{}")

        assertFalse(result.presentFields.contains("unitSystem"))
        assertNull(result.payload?.unitSystem)
        assertEquals(
            listOf(WearSettingsDivergence("unitSystem", WearSettingsFieldIssue.MISSING)),
            result.divergences
        )
    }

    @Test
    fun `explicit json null counts as not sent rather than as a value`() {
        val result = decoder.decode("""{"unitSystem":null}""")

        assertFalse(result.presentFields.contains("unitSystem"))
        assertEquals(
            listOf(WearSettingsDivergence("unitSystem", WearSettingsFieldIssue.MISSING)),
            result.divergences
        )
    }

    @Test
    fun `wrong typed key costs only itself and the payload still decodes`() {
        val result = decoder.decode("""{"unitSystem":1}""")

        assertNotNull(result.payload)
        assertNull(result.payload?.unitSystem)
        assertFalse(result.presentFields.contains("unitSystem"))
        assertEquals(
            listOf(WearSettingsDivergence("unitSystem", WearSettingsFieldIssue.WRONG_TYPE)),
            result.divergences
        )
    }

    @Test
    fun `key from a newer peer is reported unknown and the rest still decodes`() {
        val result = decoder.decode("""{"unitSystem":"METRIC","hapticsEnabled":true}""")

        assertNotNull(result.payload)
        assertEquals(WearSettingsPayloadDecoder.CONTRACT_FIELDS, result.presentFields)
        assertEquals(
            listOf(WearSettingsDivergence("hapticsEnabled", WearSettingsFieldIssue.UNKNOWN_KEY)),
            result.divergences
        )
    }

    @Test
    fun `a setting of the wider FastMediaSorter contract is unknown here and never applied`() {
        val result = decoder.decode("""{"unitSystem":"METRIC","audioEnabled":true,"viewMode":"LIST"}""")

        assertEquals(WearSettingsPayloadDecoder.CONTRACT_FIELDS, result.presentFields)
        assertEquals(
            listOf(
                WearSettingsDivergence("audioEnabled", WearSettingsFieldIssue.UNKNOWN_KEY),
                WearSettingsDivergence("viewMode", WearSettingsFieldIssue.UNKNOWN_KEY)
            ),
            result.divergences
        )
    }

    @Test
    fun `text that is not a json object yields nothing to apply`() {
        val result = decoder.decode("not json at all")

        assertNull(result.payload)
        assertTrue(result.presentFields.isEmpty())
        assertEquals(WearSettingsPayloadDecoder.ROOT, result.divergences.single().field)
    }
}
