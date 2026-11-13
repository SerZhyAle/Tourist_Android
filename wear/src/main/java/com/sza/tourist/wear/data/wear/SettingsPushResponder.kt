package com.sza.tourist.wear.data.wear

import com.sza.tourist.wear.domain.model.WearEventEnvelopeCodec
import com.sza.tourist.wear.domain.model.WearSettingsDivergence
import com.sza.tourist.wear.domain.model.WearSettingsFieldIssue
import com.sza.tourist.wear.domain.model.WearSettingsPayloadDecoder
import com.sza.tourist.wear.domain.usecase.ApplyWearSettingsUseCase
import com.sza.tourist.wear.util.errorUnlessCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * S2461: applies a settings push from the phone on a scope the application owns rather than the one of
 * the listener service that received it.
 *
 * **The scope is the whole point of the class**, as it is for the SOS commands' application scope. The
 * platform destroys [WatchWearListenerService] as soon as nothing is bound to it, shortly after the
 * callback returns, and until S2915 the service cancelled its own scope in onDestroy, taking the work in
 * flight with it. A cancelled job is not an error, so the work in flight vanished without a report, an
 * error or a single log line.
 *
 * Trimmed to the kit: FastMediaSorter's responder also answers a push with the watch's own settings
 * report (the S2093 two-way exchange) and publishes failures to its settings screen's error flow. This
 * product's push is one way and has no settings screen, so a failure is logged and nothing more.
 */
@Singleton
class SettingsPushResponder @Inject constructor(
    private val applyWearSettingsUseCase: ApplyWearSettingsUseCase
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val envelopeCodec = WearEventEnvelopeCodec()
    private val settingsPayloadDecoder = WearSettingsPayloadDecoder()

    // The scope now outlives any one service instance, so two pushes in quick succession could apply
    // side by side. One exchange at a time keeps every read-apply sequence whole.
    private val exchangeLock = Mutex()

    fun respond(payloadBytes: ByteArray) {
        scope.launch {
            exchangeLock.withLock {
                runCatching { apply(payloadBytes) }
                    .onFailure { e -> e.errorUnlessCancellation("Failed to apply settings push") }
            }
        }
    }

    private suspend fun apply(payloadBytes: ByteArray) {
        val envelope = envelopeCodec.decode(payloadBytes)
        // S2462: decoded key by key rather than straight into the payload class. A phone on a different
        // build may omit a key or send it as another type, and a single typed fromJson answers both with
        // an exception that drops the event - one incompatible field silencing the whole exchange.
        val decoded = settingsPayloadDecoder.decode(envelope.data.decodeToString())
        logSettingsDivergences(decoded.divergences)
        val payload = decoded.payload
        if (payload == null) {
            Timber.w("Settings push carried nothing decodable")
            return
        }
        applyWearSettingsUseCase(payload, decoded.presentFields)
    }

    /**
     * S2462: says which keys did not survive the decode, split by what the reason implies.
     *
     * A wrong type is a defect in the contract and is worth a warning; a key the peer never sent or a
     * key this build does not know are the normal shape of two devices on different versions, so they
     * stay at debug rather than crying wolf on every exchange with an older phone.
     */
    private fun logSettingsDivergences(divergences: List<WearSettingsDivergence>) {
        if (divergences.isEmpty()) return
        val mistyped = divergences.filter { it.issue == WearSettingsFieldIssue.WRONG_TYPE }
        if (mistyped.isNotEmpty()) {
            Timber.w("Settings push: %d field(s) of unexpected type - %s", mistyped.size, mistyped)
        }
        val skewed = divergences - mistyped.toSet()
        if (skewed.isNotEmpty()) {
            Timber.d("Settings push: %d field(s) not exchanged - %s", skewed.size, skewed)
        }
    }
}
