package com.sza.tourist.domain.usecase.sensors

import com.sza.tourist.data.sensors.OrientationReadingSource
import com.sza.tourist.domain.model.sensors.CompassReading
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Heading for the compass tile.
 *
 * Deliberately thin. It exists so a tile view depends on `domain/usecase` instead of reaching into
 * `data/sensors`, which is the layer chain every other reading follows.
 */
class ObserveCompassUseCase @Inject constructor(
    private val source: OrientationReadingSource,
) {

    operator fun invoke(): Flow<CompassReading> = source.readings()
}
