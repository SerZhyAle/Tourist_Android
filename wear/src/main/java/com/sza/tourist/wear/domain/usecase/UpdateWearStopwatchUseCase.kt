package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.repository.WearStopwatchSessionRepository
import com.sza.tourist.wear.domain.stopwatch.WearStopwatchOngoingIndicator
import com.sza.tourist.wear.domain.stopwatch.WearStopwatchState
import com.sza.tourist.wear.domain.stopwatch.WearStopwatchUpdate
import javax.inject.Inject

/**
 * S3555: every change to the measurement, and the surfaces that follow it.
 *
 * The indicator is re-posted on every change while something runs, so a lap or a second participant is
 * reflected at once, and removed on the change that stops the last runner.
 *
 * Trimmed to the kit: FastMediaSorter's version also asks the programs tile to redraw when running starts
 * or stops. This product carries no tiles, so that call (and its `RequestWearTileRefreshUseCase`) is not
 * ported.
 */
class UpdateWearStopwatchUseCase @Inject constructor(
    private val session: WearStopwatchSessionRepository,
    private val indicator: WearStopwatchOngoingIndicator
) {

    suspend operator fun invoke(
        nowMillis: Long,
        transform: (WearStopwatchState, Long) -> WearStopwatchState
    ): WearStopwatchUpdate {
        val (previous, next) = session.update(nowMillis, transform)
        if (next.anyRunning) indicator.show(next) else indicator.hide()
        return WearStopwatchUpdate(
            state = next,
            indicatorBlocked = next.anyRunning && indicator.blockedByMissingPermission()
        )
    }
}
