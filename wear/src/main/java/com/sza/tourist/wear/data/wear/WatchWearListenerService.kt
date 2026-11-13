package com.sza.tourist.wear.data.wear

import android.content.Intent
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sza.tourist.wear.MainActivity
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import com.sza.tourist.wear.domain.model.SosMode
import com.sza.tourist.wear.domain.model.WearDestinationId
import com.sza.tourist.wear.domain.model.WearLaunchTarget
import com.sza.tourist.wear.domain.model.writeTo
import com.sza.tourist.wear.domain.sos.SosSyncBus
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * The watch's Data Layer listener: the paired phone's two distress-signal commands, and the settings push
 * that carries the unit system.
 *
 * Trimmed to the kit. FastMediaSorter's listener also routes the network-source, stream, file-transfer,
 * playback, listening, camera, cast, clipboard, screenshot, clock-style, face-slot, phone-battery,
 * exchange and resource-pass traffic of surfaces this product does not carry, and observes the phone
 * companion capability to drain pending voice notes. None of those paths is declared by this product's
 * manifest filter, so the platform never delivers them here.
 *
 * The tourist weather the phone answers with (`TOURIST_WEATHER`) is not handled here either: the source
 * listener ignores it, and the weather repository reads that Data Item itself.
 */
@AndroidEntryPoint
class WatchWearListenerService : WearableListenerService() {

    // S2461: Lazy for S2626's reason - this service is constructed for every Data Layer message, and
    // only a settings push needs the apply chain behind the responder.
    @Inject lateinit var settingsPushResponder: dagger.Lazy<SettingsPushResponder>

    // S3216: the signal itself belongs to the SOS screen, so this service only relays the phone's two
    // commands to it - the bus is the only thing a service and a composable can both reach.
    @Inject lateinit var sosSyncBus: SosSyncBus

    // S4029: the listener is declared in both editions now, so a command whose screen an edition
    // withholds is refused here as well - a hidden route does not stop a Data Layer message.
    @Inject lateinit var capabilities: WearRestrictedCapabilities

    /**
     * Brings the SOS destination to the front. A seam for the JVM test of the routing below, which has
     * no platform to start an activity on; production never replaces it.
     */
    internal var launchSos: () -> Unit = { startActivity(sosLaunchIntent()) }

    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val payloadBytes = dataMap.getByteArray("payload") ?: continue
            dispatchDataItem(event.dataItem.uri.path, payloadBytes)
        }
        events.release()
    }

    /**
     * Split out of [onDataChanged] so the routing can be exercised without a Data Layer buffer. The
     * payload is read once above rather than per branch: every path carries it under the same key, and
     * a data item without one is nothing any branch could act on.
     */
    internal fun dispatchDataItem(path: String?, payloadBytes: ByteArray) {
        when (path) {
            WearDataLayerPaths.SETTINGS_PUSH -> handleSettingsPush(payloadBytes)
        }
    }

    private fun handleSettingsPush(payloadBytes: ByteArray) {
        settingsPushResponder.get().respond(payloadBytes)
    }

    override fun onMessageReceived(event: MessageEvent) {
        // Probe of S0008: the device pass reads this line to tell that the message reached the watch.
        Timber.d("S0008: watch received a Data Layer message at ${event.path}")
        when (event.path) {
            WearDataLayerPaths.SOS_START_FROM_PHONE -> startSosFromPhone(event.data)

            WearDataLayerPaths.SOS_STOP_FROM_PHONE -> {
                Timber.i("SOS: the phone asks this watch to stop signalling")
                sosSyncBus.requestStop()
            }

            else -> Timber.d("WatchWearListenerService: unhandled message path ${event.path}")
        }
    }

    /**
     * S3216: the phone's distress-signal start. Neither command touches the siren directly. The signal
     * belongs to the screen, so a start becomes a pending mode plus a launch and a stop becomes an
     * event; [SosSyncBus] records why.
     *
     * S4029: refused before the bus is touched where the edition withholds the takeover programs. A
     * pending mode left on the bus would be picked up by any later SOS screen, and the launch would
     * only bounce off the launch-address check after the app had already come to the front.
     */
    private fun startSosFromPhone(data: ByteArray) {
        if (!capabilities.offersScreenTakeoverPrograms) {
            Timber.i("SOS: the phone asked this watch to signal; this edition does not offer it")
            return
        }
        val mode = SosMode.fromNameOrDefault(data.decodeToString().takeIf { it.isNotBlank() })
        Timber.i("SOS: the phone asks this watch to signal in mode %s", mode)
        sosSyncBus.requestStart(mode)
        launchSos()
    }

    /**
     * The app opened at the SOS destination, in the shape `MainActivity` already reads back (S2511).
     *
     * A launch target rather than a route string: a route is an address inside the navigation graph and
     * nothing outside the graph may name one, which is the whole reason `WearDestinationId` exists.
     */
    private fun sosLaunchIntent(): Intent =
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .also { intent -> WearLaunchTarget.Destination(WearDestinationId.SOS).writeTo(intent) }
}
