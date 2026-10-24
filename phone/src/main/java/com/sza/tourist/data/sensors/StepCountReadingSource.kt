package com.sza.tourist.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.sza.tourist.domain.model.sensors.SensorCapability
import com.sza.tourist.domain.model.sensors.StepReading
import com.sza.tourist.domain.repository.SensorAvailabilityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The system step counter, reported verbatim.
 *
 * `TYPE_STEP_COUNTER` is the only source and an accelerometer algorithm of our own is ruled out, so
 * nothing here derives, smooths or re-bases the number.
 *
 * A device without the sensor gets an empty flow rather than an exception: that case degrades to
 * "not offered", and a collector that somehow still subscribes must not crash the screen.
 */
@Singleton
class StepCountReadingSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val availability: SensorAvailabilityRepository,
) {

    fun readings(): Flow<StepReading> {
        val supported = availability.isAvailable(SensorCapability.STEP_COUNTER)
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        // One return over an if/else rather than a guard-return per condition.
        return if (!supported || manager == null || sensor == null) {
            emptyFlow()
        } else {
            callbackFlow {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val steps = event.values.firstOrNull()?.toLong() ?: return
                        trySend(
                            StepReading(
                                stepsSinceBoot = steps,
                                takenAtMillis = System.currentTimeMillis(),
                            ),
                        )
                    }

                    override fun onAccuracyChanged(changed: Sensor?, status: Int) = Unit
                }

                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                awaitClose { manager.unregisterListener(listener) }
            }.conflate()
        }
    }
}
