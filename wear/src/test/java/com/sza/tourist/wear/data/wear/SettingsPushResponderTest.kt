package com.sza.tourist.wear.data.wear

import com.sza.tourist.wear.domain.model.UnitSystem
import com.sza.tourist.wear.domain.model.WearEventEnvelope
import com.sza.tourist.wear.domain.model.WearEventEnvelopeCodec
import com.sza.tourist.wear.domain.model.WearSettingsPayload
import com.sza.tourist.wear.domain.usecase.ApplyWearSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * S2461: the settings push is applied on the responder's own scope, so the exchange completes even when
 * whoever handed it over is gone - the listener service that receives a push is destroyed shortly after
 * its callback returns, and its scope with it.
 *
 * Trimmed from FastMediaSorter's test of the same name: this product's responder only applies a push and
 * has no report to answer it with, so the apply-then-report ordering case is not carried.
 */
class SettingsPushResponderTest {

    private val envelopeCodec = WearEventEnvelopeCodec()
    private lateinit var apply: ApplyWearSettingsUseCase
    private lateinit var responder: SettingsPushResponder

    @Before
    fun setup() {
        apply = mockk()
        coEvery { apply(any(), any()) } returns Unit
        responder = SettingsPushResponder(apply)
    }

    @Test
    fun `a push is decoded and applied with the keys it carried`() {
        responder.respond(push("""{"unitSystem":"IMPERIAL"}"""))

        coVerify(timeout = WAIT_MILLIS) {
            apply(WearSettingsPayload(unitSystem = UnitSystem.IMPERIAL.name), setOf("unitSystem"))
        }
    }

    @Test
    fun `the exchange completes after the caller that handed it over is cancelled`() = runTest {
        val applyStarted = CompletableDeferred<Unit>()
        val applyMayFinish = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        coEvery { apply(any(), any()) } coAnswers {
            applyStarted.complete(Unit)
            applyMayFinish.await()
            finished.complete(Unit)
        }

        // Stands in for the listener service: alive while the push is handed over, then destroyed
        // while the apply is still running.
        val caller = launch {
            responder.respond(push("""{"unitSystem":"METRIC"}"""))
            awaitCancellation()
        }
        applyStarted.await()
        caller.cancelAndJoin()
        applyMayFinish.complete(Unit)

        finished.await()
        coVerify(exactly = 1) { apply(any(), any()) }
    }

    @Test
    fun `a push with nothing decodable is not applied`() {
        responder.respond(push("not json"))
        // Exchanges run one at a time, so once the second one has applied the first has finished.
        responder.respond(push("""{"unitSystem":"METRIC"}"""))

        coVerify(timeout = WAIT_MILLIS) { apply(any(), any()) }
        coVerify(exactly = 1) { apply(any(), any()) }
    }

    @Test
    fun `a payload that is not an envelope is dropped and the next push still applies`() {
        responder.respond("garbage".toByteArray())
        responder.respond(push("""{"unitSystem":"METRIC"}"""))

        coVerify(timeout = WAIT_MILLIS) { apply(any(), any()) }
        coVerify(exactly = 1) { apply(any(), any()) }
    }

    private fun push(json: String): ByteArray = envelopeCodec.encode(
        WearEventEnvelope(
            eventType = WearDataLayerPaths.EVENT_SETTINGS,
            sentAt = SENT_AT,
            data = json.toByteArray()
        )
    )

    private companion object {
        const val SENT_AT = 1_000L
        const val WAIT_MILLIS = 5_000L
    }
}
