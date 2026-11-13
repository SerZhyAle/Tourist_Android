package com.sza.tourist.domain.usecase.sensors

import com.sza.tourist.data.sensors.AttitudeReadingSource
import com.sza.tourist.domain.model.sensors.AttitudeReading
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Device attitude for the Tourist gyroscope tile; cold, so a collector owns the registration. */
class ObserveDeviceAttitudeUseCase @Inject constructor(
    private val source: AttitudeReadingSource,
) {

    operator fun invoke(): Flow<AttitudeReading> = source.readings()
}
