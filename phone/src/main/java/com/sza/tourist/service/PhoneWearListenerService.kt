package com.sza.tourist.service

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sza.tourist.core.di.ApplicationScope
import com.sza.tourist.ui.sos.SosCommandFromWatchHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Wear OS Data Layer listener on the phone (S0006, S0008).
 *
 * Ported from FastMediaSorter's `PhoneWearListenerService` and trimmed to the kit's routes: the paired
 * watch's SOS start and stop commands go to [SosCommandFromWatchHandler] (S0006); its Tourist health
 * report and weather request, which arrive as Data Items, go to [WearTouristHealthReceiver] and
 * [WearTouristWeatherRequestHandler] (S0008).
 */
@AndroidEntryPoint
class PhoneWearListenerService : WearableListenerService() {

    @Inject
    lateinit var sosCommandFromWatchHandler: SosCommandFromWatchHandler

    @Inject
    lateinit var wearTouristHealthReceiver: WearTouristHealthReceiver

    @Inject
    lateinit var wearTouristWeatherRequestHandler: WearTouristWeatherRequestHandler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onMessageReceived(event: MessageEvent) {
        // Probe of S0008: the device pass reads this line to tell that the message reached the phone.
        Timber.d("S0008: phone received a Data Layer message at ${event.path}")
        applicationScope.launch {
            sosCommandFromWatchHandler.handle(event.path, event.data)
        }
    }

    override fun onDataChanged(events: DataEventBuffer) {
        val receivedAt = System.currentTimeMillis()
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            dispatchDataEvent(event, receivedAt)
        }
        events.release()
    }

    // The per-event work is a function rather than the loop body so that skipping a payload-less
    // event is a return here instead of a second continue in the walk above.
    private fun dispatchDataEvent(event: DataEvent, receivedAtEpochMillis: Long) {
        val payloadBytes = DataMapItem.fromDataItem(event.dataItem)
            .dataMap.getByteArray("payload") ?: return
        when (event.dataItem.uri.path) {
            WearDataLayerPaths.TOURIST_HEALTH -> wearTouristHealthReceiver.handle(payloadBytes, receivedAtEpochMillis)
            WearDataLayerPaths.TOURIST_WEATHER_REQUEST -> applicationScope.launch {
                wearTouristWeatherRequestHandler.handle(payloadBytes)
            }
        }
    }
}
