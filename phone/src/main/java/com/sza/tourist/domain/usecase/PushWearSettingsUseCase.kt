package com.sza.tourist.domain.usecase

import com.google.gson.Gson
import com.sza.tourist.core.util.rethrowIfCancellation
import com.sza.tourist.domain.model.WearEventEnvelope
import com.sza.tourist.domain.model.WearSettingsPayload
import com.sza.tourist.domain.repository.WearableDataLayerRepository
import com.sza.tourist.service.WearDataLayerPaths
import javax.inject.Inject

/**
 * Pushes the phone's mirrored settings to the paired watch as a Data Item.
 *
 * Trimmed to this product's payload (the unit system only). FastMediaSorter's version also stamps the
 * sender's version name into the packet (S2461) so its two-way settings exchange can tell "the settings
 * did not arrive" from "an older build accepted them"; there is no such exchange here, so the stamp is
 * not carried.
 */
class PushWearSettingsUseCase @Inject constructor(
    private val wearableRepository: WearableDataLayerRepository,
    private val gson: Gson
) {
    suspend operator fun invoke(settings: WearSettingsPayload): Result<Unit> = runCatching {
        val nodes = wearableRepository.getConnectedNodes()
        check(nodes.isNotEmpty()) { "No watch connected" }
        val settingsJson = gson.toJson(settings)
        val settingsBytes = settingsJson.toByteArray(Charsets.UTF_8)
        val envelope = WearEventEnvelope(
            eventType = WearDataLayerPaths.EVENT_SETTINGS,
            sentAt = System.currentTimeMillis(),
            data = settingsBytes
        )
        wearableRepository.putEnvelopeDataItem(WearDataLayerPaths.SETTINGS_PUSH, envelope)
    }.onFailure { it.rethrowIfCancellation() }
}
