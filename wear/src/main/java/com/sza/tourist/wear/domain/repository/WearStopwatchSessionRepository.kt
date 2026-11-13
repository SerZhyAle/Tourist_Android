package com.sza.tourist.wear.domain.repository

import com.sza.tourist.wear.domain.stopwatch.WearStopwatchState
import kotlinx.coroutines.flow.StateFlow

/**
 * S3555: the one owner of the stopwatch measurement, for the whole process.
 *
 * The screen and the ongoing-activity notification both read this, because the notification outlives the
 * screen - a measurement held by the screen would make it open a reset stopwatch.
 */
interface WearStopwatchSessionRepository {

    /** The measurement; before the stored one is restored it holds a clear stopwatch. */
    val session: StateFlow<WearStopwatchState>

    /** The measurement once the stored one has been restored. */
    suspend fun current(): WearStopwatchState

    /**
     * Applies [transform] at [nowMillis] (the elapsedRealtime of the tap), stores the result and answers
     * the state before and after it, so a caller can tell whether anything started or stopped running.
     */
    suspend fun update(
        nowMillis: Long,
        transform: (WearStopwatchState, Long) -> WearStopwatchState
    ): Pair<WearStopwatchState, WearStopwatchState>
}
