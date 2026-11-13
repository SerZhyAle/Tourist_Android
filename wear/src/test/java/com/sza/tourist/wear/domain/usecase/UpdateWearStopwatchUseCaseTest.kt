package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.repository.WearStopwatchSessionRepository
import com.sza.tourist.wear.domain.stopwatch.WearStopwatchEngine
import com.sza.tourist.wear.domain.stopwatch.WearStopwatchOngoingIndicator
import com.sza.tourist.wear.domain.stopwatch.WearStopwatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Trimmed to the kit: FastMediaSorter's version also verified the programs-tile redraw on a start and a stop.
 * This product carries no tiles, so those assertions (and the tile use case they mocked) are not ported.
 */
class UpdateWearStopwatchUseCaseTest {

    private val session = InMemorySession(WearStopwatchState.initial(2))
    private val indicator = RecordingIndicator()
    private val useCase = UpdateWearStopwatchUseCase(session, indicator)

    @Test
    fun `a start shows the indicator`() = runTest {
        useCase(START) { state, now -> WearStopwatchEngine.startOrLap(state, 0, now) }

        assertEquals(1, indicator.shown)
    }

    @Test
    fun `a lap while running re-posts the indicator`() = runTest {
        useCase(START) { state, now -> WearStopwatchEngine.startOrLap(state, 0, now) }
        useCase(START + LAP_GAP) { state, now -> WearStopwatchEngine.startOrLap(state, 0, now) }

        assertEquals(2, indicator.shown)
    }

    @Test
    fun `stopping the last runner hides the indicator`() = runTest {
        useCase(START) { state, now -> WearStopwatchEngine.startOrLap(state, 0, now) }
        useCase(START + LAP_GAP) { state, now -> WearStopwatchEngine.stopOrReset(state, 0, now) }

        assertEquals(1, indicator.hidden)
    }

    @Test
    fun `the indicator is reported blocked only while running without the permission`() = runTest {
        indicator.permissionMissing = true

        val running = useCase(START) { state, now -> WearStopwatchEngine.startOrLap(state, 0, now) }
        val stopped = useCase(START + LAP_GAP) { state, now -> WearStopwatchEngine.stopOrReset(state, 0, now) }

        assertTrue(running.indicatorBlocked)
        assertFalse(stopped.indicatorBlocked)
    }

    private class InMemorySession(initial: WearStopwatchState) : WearStopwatchSessionRepository {
        private val state = MutableStateFlow(initial)
        override val session: StateFlow<WearStopwatchState> = state

        override suspend fun current(): WearStopwatchState = state.value

        override suspend fun update(
            nowMillis: Long,
            transform: (WearStopwatchState, Long) -> WearStopwatchState
        ): Pair<WearStopwatchState, WearStopwatchState> {
            val previous = state.value
            state.value = transform(previous, nowMillis)
            return previous to state.value
        }
    }

    private class RecordingIndicator : WearStopwatchOngoingIndicator {
        var shown = 0
        var hidden = 0
        var permissionMissing = false

        override fun show(state: WearStopwatchState): Boolean {
            shown += 1
            return !permissionMissing
        }

        override fun hide() {
            hidden += 1
        }

        override fun blockedByMissingPermission(): Boolean = permissionMissing
    }

    private companion object {
        const val START = 10_000L
        const val LAP_GAP = 1_500L
    }
}
