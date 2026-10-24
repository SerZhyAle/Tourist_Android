package com.sza.tourist.domain.usecase.sensors

import com.sza.tourist.data.sensors.MotionReadingSource
import com.sza.tourist.domain.model.sensors.MotionReading
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Speed, altitude and per-sample distance for the speed tile, the compass tile's altitude line and
 * the trip.
 *
 * Several consumers, one stream: they agree about the same fix rather than each opening its own
 * subscription.
 */
class ObserveMotionUseCase @Inject constructor(
    private val source: MotionReadingSource,
) {

    operator fun invoke(): Flow<MotionReading> = source.readings()
}
