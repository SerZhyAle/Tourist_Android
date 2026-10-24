package com.sza.tourist.data.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import com.sza.tourist.domain.model.sensors.SensorCapability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor

/**
 * Hardware only, never permissions - with the one deliberate exception of the step counter, whose
 * capability also needs the permission declared in the merged manifest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class DeviceSensorAvailabilityRepositoryImplTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private fun addSensors(vararg types: Int) = types.forEach { shadowOf(manager).addSensor(ShadowSensor.newInstance(it)) }

    private fun declarePermissions(vararg permissions: String) {
        shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName).requestedPermissions =
            arrayOf(*permissions)
    }

    // Built after the shadows are set up: the answers are resolved once, on first use.
    private fun newRepository() = DeviceSensorAvailabilityRepositoryImpl(context)

    @Test
    fun `a device with no sensors offers none of the sensor capabilities`() {
        val repository = newRepository()

        listOf(
            SensorCapability.COMPASS,
            SensorCapability.STEP_COUNTER,
            SensorCapability.BAROMETER,
            SensorCapability.LIGHT,
            SensorCapability.MAGNETOMETER,
            SensorCapability.LEVEL,
        ).forEach { assertFalse(it.name, repository.isAvailable(it)) }
    }

    @Test
    fun `location follows the platform feature`() {
        assertFalse(newRepository().isAvailable(SensorCapability.LOCATION))

        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_LOCATION, true)
        assertTrue(newRepository().isAvailable(SensorCapability.LOCATION))
    }

    @Test
    fun `the fused rotation vector alone is a compass`() {
        addSensors(Sensor.TYPE_ROTATION_VECTOR)
        assertTrue(newRepository().isAvailable(SensorCapability.COMPASS))
    }

    @Test
    fun `an accelerometer alone is not a compass`() {
        addSensors(Sensor.TYPE_ACCELEROMETER)
        assertFalse(newRepository().isAvailable(SensorCapability.COMPASS))
    }

    @Test
    fun `an accelerometer and a magnetometer together are a compass`() {
        addSensors(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD)
        assertTrue(newRepository().isAvailable(SensorCapability.COMPASS))
    }

    @Test
    fun `barometer light and magnetometer follow their own sensor`() {
        addSensors(Sensor.TYPE_PRESSURE, Sensor.TYPE_LIGHT, Sensor.TYPE_MAGNETIC_FIELD)
        val repository = newRepository()

        assertTrue(repository.isAvailable(SensorCapability.BAROMETER))
        assertTrue(repository.isAvailable(SensorCapability.LIGHT))
        assertTrue(repository.isAvailable(SensorCapability.MAGNETOMETER))
    }

    @Test
    fun `the level works from gravity or from the raw accelerometer`() {
        addSensors(Sensor.TYPE_GRAVITY)
        assertTrue(newRepository().isAvailable(SensorCapability.LEVEL))
    }

    @Test
    fun `the level works from the raw accelerometer alone`() {
        addSensors(Sensor.TYPE_ACCELEROMETER)
        assertTrue(newRepository().isAvailable(SensorCapability.LEVEL))
    }

    @Test
    fun `the step counter needs the sensor and the declared permission`() {
        addSensors(Sensor.TYPE_STEP_COUNTER)
        declarePermissions(Manifest.permission.ACTIVITY_RECOGNITION)

        assertTrue(newRepository().isAvailable(SensorCapability.STEP_COUNTER))
    }

    @Test
    fun `a build that does not declare the permission reports no step counter even with the sensor`() {
        addSensors(Sensor.TYPE_STEP_COUNTER)
        declarePermissions()

        assertFalse(newRepository().isAvailable(SensorCapability.STEP_COUNTER))
    }

    @Test
    fun `the permission without the sensor is no step counter either`() {
        declarePermissions(Manifest.permission.ACTIVITY_RECOGNITION)

        assertFalse(newRepository().isAvailable(SensorCapability.STEP_COUNTER))
    }
}
