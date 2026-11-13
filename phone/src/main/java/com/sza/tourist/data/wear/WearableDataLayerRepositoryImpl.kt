package com.sza.tourist.data.wear

import android.content.Context
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sza.tourist.domain.model.WearEventEnvelope
import com.sza.tourist.domain.model.WearEventEnvelopeCodec
import com.sza.tourist.domain.model.WearNode
import com.sza.tourist.domain.repository.WearableDataLayerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GMS-backed Wear Data Layer bridge for the phone side of the watch pair (S0006, S0008).
 *
 * Maps the GMS [Node] type to the domain [WearNode] at this boundary so the
 * [WearableDataLayerRepository] interface stays free of `com.google.android.gms`.
 *
 * S0008 adds the Data Item half (`putDataItem`, `putEnvelopeDataItem`) the unit-system push and the
 * Tourist weather answer travel by. FastMediaSorter gates every send on its Wear-companion settings
 * switch; this product carries no such switch, so the bridge sends whenever a watch is paired.
 */
@Singleton
class WearableDataLayerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WearableDataLayerRepository {

    private val envelopeCodec = WearEventEnvelopeCodec()

    override suspend fun getConnectedNodes(): List<WearNode> {
        return try {
            Wearable.getNodeClient(context).connectedNodes.await()
                .map { node -> WearNode(id = node.id, displayName = node.displayName) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Expected whenever Play Services Wearable is absent or the pairing is gone - a bridge
            // we cannot ask is a bridge with no nodes. INFO, not ERROR: a normal device state on a
            // watch-less phone must not log as an error.
            Timber.i(e, "Connected-nodes query failed, treating the watch as absent")
            emptyList()
        }
    }

    override suspend fun putDataItem(path: String, payload: ByteArray) {
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putByteArray("payload", payload)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request).await()
        Timber.d("putDataItem: $path (${payload.size} bytes)")
    }

    override suspend fun sendMessage(nodeId: String, path: String, data: ByteArray) {
        Wearable.getMessageClient(context).sendMessage(nodeId, path, data).await()
        Timber.d("sendMessage: $path -> $nodeId")
    }

    override suspend fun putEnvelopeDataItem(path: String, envelope: WearEventEnvelope) {
        val bytes = envelopeCodec.encode(envelope)
        putDataItem(path, bytes)
    }
}
