package com.sza.tourist.wear.domain.sos

import com.sza.tourist.wear.domain.model.SosMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * S3216: the bus carries the paired phone's two SOS commands from the Data Layer listener to the screen.
 * A start is state (the command can land before the destination has drawn), a stop is an event (a stop
 * nobody was listening for has already been honoured by the screen not being open).
 *
 * Not in the source, where the bus is uncovered; added here with the port.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SosSyncBusTest {

    private val bus = SosSyncBus()

    @Test
    fun `nothing is pending on a fresh bus`() {
        assertNull(bus.pendingStartMode.value)
    }

    @Test
    fun `a start request is held until a screen takes it`() {
        bus.requestStart(SosMode.LIGHT_ONLY)

        assertEquals(SosMode.LIGHT_ONLY, bus.pendingStartMode.value)
    }

    @Test
    fun `a later start replaces an earlier pending one`() {
        bus.requestStart(SosMode.LIGHT_ONLY)
        bus.requestStart(SosMode.SOUND_ONLY)

        assertEquals(SosMode.SOUND_ONLY, bus.pendingStartMode.value)
    }

    @Test
    fun `consuming the start clears it so a later visit does not re-arm itself`() {
        bus.requestStart(SosMode.ALL)

        bus.consumeStart()

        assertNull(bus.pendingStartMode.value)
    }

    @Test
    fun `a stop clears a start that was never taken`() {
        bus.requestStart(SosMode.ALL)

        bus.requestStop()

        assertNull(bus.pendingStartMode.value)
    }

    @Test
    fun `a stop reaches a screen that is listening`() = runTest {
        val received = mutableListOf<Unit>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.stopRequests.collect { received += it }
        }

        bus.requestStop()

        assertEquals(1, received.size)
        job.cancel()
    }

    @Test
    fun `a stop nobody was listening for is not replayed to a later screen`() = runTest {
        bus.requestStop()

        val received = mutableListOf<Unit>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.stopRequests.collect { received += it }
        }

        assertEquals(0, received.size)
        job.cancel()
    }
}
