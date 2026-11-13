package com.sza.tourist.widget.helpers

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StopwatchWidgetStateStoreTest {

    private val application: Application = RuntimeEnvironment.getApplication()
    private val widgetId = 101

    @Before
    fun setUp() {
        StopwatchWidgetStateStore.delete(application, widgetId)
    }

    @Test
    fun `new widget starts stopped at zero`() {
        val state = StopwatchWidgetStateStore.read(application, widgetId)

        assertFalse(state.running)
        assertEquals(0L, state.startMark)
        assertEquals(0L, state.accumulatedMillis)
        assertEquals(5_000L, StopwatchWidgetStateStore.chronometerBase(state, nowMillis = 5_000L))
    }

    @Test
    fun `toggled starts and stops while accumulating elapsed time`() {
        val initial = StopwatchWidgetStateStore.read(application, widgetId)
        val started = StopwatchWidgetStateStore.toggled(initial, nowMillis = 1_000L)
        StopwatchWidgetStateStore.write(application, widgetId, started)

        val rereadStarted = StopwatchWidgetStateStore.read(application, widgetId)
        assertTrue(rereadStarted.running)
        assertEquals(1_000L, StopwatchWidgetStateStore.chronometerBase(rereadStarted, nowMillis = 3_500L))

        val stopped = StopwatchWidgetStateStore.toggled(rereadStarted, nowMillis = 3_500L)
        StopwatchWidgetStateStore.write(application, widgetId, stopped)

        val rereadStopped = StopwatchWidgetStateStore.read(application, widgetId)
        assertFalse(rereadStopped.running)
        assertEquals(2_500L, rereadStopped.accumulatedMillis)
        assertEquals(7_500L, StopwatchWidgetStateStore.chronometerBase(rereadStopped, nowMillis = 10_000L))
    }

    @Test
    fun `afterClockRestart halts running widget without losing accumulated time`() {
        var state = StopwatchWidgetStateStore.toggled(StopwatchWidgetStateStore.State(), nowMillis = 1_000L)
        state = StopwatchWidgetStateStore.toggled(state, nowMillis = 4_000L)
        state = StopwatchWidgetStateStore.toggled(state, nowMillis = 5_000L)

        val restarted = StopwatchWidgetStateStore.afterClockRestart(state)

        assertFalse(restarted.running)
        assertEquals(3_000L, restarted.accumulatedMillis)
    }
}
