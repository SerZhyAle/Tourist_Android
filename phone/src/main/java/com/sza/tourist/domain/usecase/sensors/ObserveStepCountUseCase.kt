package com.sza.tourist.domain.usecase.sensors

import com.sza.tourist.data.sensors.StepCountReadingSource
import com.sza.tourist.domain.model.sensors.StepReading
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The system step count for the steps tile.
 *
 * Passes the counter through untouched - this feature derives no step total of its own, so there is
 * nothing for this layer to compute.
 */
class ObserveStepCountUseCase @Inject constructor(
    private val source: StepCountReadingSource,
) {

    operator fun invoke(): Flow<StepReading> = source.readings()
}
