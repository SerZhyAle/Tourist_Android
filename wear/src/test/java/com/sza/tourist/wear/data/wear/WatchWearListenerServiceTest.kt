package com.sza.tourist.wear.data.wear

import com.google.android.gms.wearable.MessageEvent
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import com.sza.tourist.wear.domain.model.SosMode
import com.sza.tourist.wear.domain.sos.SosSyncBus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * S3216 / S4029: what the watch's Data Layer listener does with the phone's commands.
 *
 * Not in the source, where the listener is uncovered. The platform half (starting the activity, reading a
 * data-item buffer) has no JVM stand-in, so the launch is the service's `launchSos` seam and the data-item
 * routing is `dispatchDataItem`; everything between the wire and those two is exercised as shipped.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchWearListenerServiceTest {

    private val bus = SosSyncBus()
    private val responder: SettingsPushResponder = mockk(relaxed = true)
    private var launches = 0

    @Before
    fun resetLaunches() {
        launches = 0
    }

    @Test
    fun `a start from the phone records the mode and brings the SOS screen up`() {
        val service = serviceOffering(takeoverPrograms = true)

        service.onMessageReceived(message(WearDataLayerPaths.SOS_START_FROM_PHONE, SosMode.SOUND_ONLY.name))

        assertEquals(SosMode.SOUND_ONLY, bus.pendingStartMode.value)
        assertEquals(1, launches)
    }

    @Test
    fun `a start with no payload falls back to the default mode`() {
        val service = serviceOffering(takeoverPrograms = true)

        service.onMessageReceived(message(WearDataLayerPaths.SOS_START_FROM_PHONE, ""))

        assertEquals(SosMode.ALL, bus.pendingStartMode.value)
        assertEquals(1, launches)
    }

    @Test
    fun `a start naming a mode this build does not know falls back to the default mode`() {
        val service = serviceOffering(takeoverPrograms = true)

        service.onMessageReceived(message(WearDataLayerPaths.SOS_START_FROM_PHONE, "STROBE_ONLY"))

        assertEquals(SosMode.ALL, bus.pendingStartMode.value)
    }

    @Test
    fun `a start is refused before the bus is touched where the edition withholds the takeover programs`() {
        val service = serviceOffering(takeoverPrograms = false)

        service.onMessageReceived(message(WearDataLayerPaths.SOS_START_FROM_PHONE, SosMode.ALL.name))

        assertNull(bus.pendingStartMode.value)
        assertEquals(0, launches)
    }

    @Test
    fun `a stop from the phone clears a pending start and reaches a running screen`() = runTest {
        val service = serviceOffering(takeoverPrograms = true)
        bus.requestStart(SosMode.ALL)
        val stops = mutableListOf<Unit>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.stopRequests.collect { stops += it }
        }

        service.onMessageReceived(message(WearDataLayerPaths.SOS_STOP_FROM_PHONE))

        assertNull(bus.pendingStartMode.value)
        assertEquals(1, stops.size)
        assertEquals(0, launches)
        collector.cancel()
    }

    @Test
    fun `a path the watch does not route changes nothing`() {
        val service = serviceOffering(takeoverPrograms = true)

        service.onMessageReceived(message("/tourist/phone/unknown"))

        assertNull(bus.pendingStartMode.value)
        assertEquals(0, launches)
    }

    @Test
    fun `the settings push data item is handed to the responder`() {
        val service = serviceOffering(takeoverPrograms = true)
        val payload = byteArrayOf(1, 2, 3)

        service.dispatchDataItem(WearDataLayerPaths.SETTINGS_PUSH, payload)

        verify(exactly = 1) { responder.respond(payload) }
    }

    @Test
    fun `any other data item is not handed to the responder`() {
        val service = serviceOffering(takeoverPrograms = true)

        service.dispatchDataItem(WearDataLayerPaths.TOURIST_WEATHER, byteArrayOf(1))
        service.dispatchDataItem(null, byteArrayOf(1))

        verify(exactly = 0) { responder.respond(any()) }
    }

    private fun serviceOffering(takeoverPrograms: Boolean): WatchWearListenerService =
        WatchWearListenerService().also { service ->
            service.sosSyncBus = bus
            service.capabilities = FakeCapabilities(takeoverPrograms)
            service.settingsPushResponder = dagger.Lazy { responder }
            service.launchSos = { launches++ }
        }

    private fun message(path: String, payload: String = ""): MessageEvent {
        val event = mockk<MessageEvent>()
        every { event.path } returns path
        every { event.data } returns payload.toByteArray()
        return event
    }

    private class FakeCapabilities(
        override val offersScreenTakeoverPrograms: Boolean,
        override val offersHealthFeatures: Boolean = true,
        override val locksSystemShade: Boolean = false,
    ) : WearRestrictedCapabilities
}
