package com.sza.tourist.wear.data.tourist

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.sza.tourist.wear.domain.tourist.TouristMetricType
import com.sza.tourist.wear.domain.tourist.WearElevationSource
import com.sza.tourist.wear.domain.tourist.WearTouristState
import com.sza.tourist.wear.domain.tourist.WearTouristTripTracker
import com.sza.tourist.wear.domain.tourist.WearTripSnapshot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The source leaves [AndroidWearTouristRepository] uncovered. The class is mostly platform wiring, so this
 * suite drives it through the platform interfaces it registers - the location listener and the sensor
 * listener are captured from the mocked managers and fed events - and pins what the dashboard depends on:
 * which sensors are claimed, how a GPS fix and a network fix differ, the step baseline and its reset, and
 * how a body-temperature sensor is recognised.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidWearTouristRepositoryTest {

    private val context = mockk<Context>(relaxed = true)
    private val sensorManager = mockk<SensorManager>()
    private val locationManager = mockk<LocationManager>(relaxed = true)
    private val tripTracker = mockk<WearTouristTripTracker>(relaxed = true)

    private val locationListeners = mutableListOf<LocationListener>()
    private val sensorListeners = mutableListOf<SensorEventListener>()
    private val sensorsByType = mutableMapOf<Int, Sensor>()
    private var sensorList: List<Sensor> = emptyList()

    private val stepSensor = sensor(Sensor.TYPE_STEP_COUNTER)
    private val heartSensor = sensor(Sensor.TYPE_HEART_RATE)

    @Before
    fun setUp() {
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        grantPermissions(true)

        every { sensorManager.getDefaultSensor(any<Int>()) } answers { sensorsByType[firstArg<Int>()] }
        every { sensorManager.getSensorList(any<Int>()) } answers { sensorList }
        every { sensorManager.registerListener(capture(sensorListeners), any<Sensor>(), any<Int>()) } returns true
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns false
        every { locationManager.getLastKnownLocation(any()) } returns null
        every {
            locationManager.requestLocationUpdates(
                any<String>(),
                any<Long>(),
                any<Float>(),
                capture(locationListeners),
            )
        } just Runs

        every { tripTracker.snapshot } returns MutableStateFlow(WearTripSnapshot.EMPTY)
        every { tripTracker.slopePercent() } returns null
    }

    private fun repository(): AndroidWearTouristRepository = AndroidWearTouristRepository(
        context = context,
        tripTracker = tripTracker,
        environmentSensors = WearTouristEnvironmentSensors(),
        appScope = CoroutineScope(Job() + Dispatchers.Unconfined),
    )

    private fun grantPermissions(granted: Boolean) {
        val result = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        every { context.checkPermission(any(), any(), any()) } returns result
    }

    private fun sensor(sensorType: Int, sensorStringType: String = "", sensorName: String = ""): Sensor = mockk {
        every { type } returns sensorType
        every { stringType } returns sensorStringType
        every { name } returns sensorName
    }

    private fun offer(sensor: Sensor) {
        sensorsByType[sensor.type] = sensor
    }

    private fun location(
        provider: String,
        speedMetresPerSecond: Float,
        accuracy: Float,
        latitude: Double,
        longitude: Double,
        altitude: Double,
    ): Location = mockk(relaxed = true) {
        every { this@mockk.provider } returns provider
        every { hasSpeed() } returns true
        every { speed } returns speedMetresPerSecond
        every { hasAccuracy() } returns true
        every { this@mockk.accuracy } returns accuracy
        every { this@mockk.latitude } returns latitude
        every { this@mockk.longitude } returns longitude
        every { hasAltitude() } returns true
        every { this@mockk.altitude } returns altitude
    }

    /** `SensorEvent` has no public constructor and exposes its payload as public final fields. */
    private fun sensorEvent(sensor: Sensor, vararg values: Float): SensorEvent {
        val event = mockk<SensorEvent>(relaxed = true)
        setField(event, "sensor", sensor)
        setField(event, "values", values)
        return event
    }

    private fun setField(target: Any, name: String, value: Any?) {
        val field = SensorEvent::class.java.getField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    /** Collects the telemetry; the throttle (500 ms) is advanced once so the latest state is delivered. */
    private fun TestScope.collectTelemetry(repository: AndroidWearTouristRepository): List<WearTouristState> {
        val states = mutableListOf<WearTouristState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeTelemetry().collect { states.add(it) }
        }
        return states
    }

    private fun TestScope.deliver() {
        advanceTimeBy(THROTTLE_MS + 1L)
        runCurrent()
    }

    @Test
    fun `the first state claims only the sensors the watch has`() = runTest {
        offer(stepSensor)
        val states = collectTelemetry(repository())

        deliver()

        val first = states.first()
        assertEquals(TouristMetricType.SPEED, first.focusedMetric)
        assertTrue(first.hasStepSensor)
        assertFalse(first.hasPressureSensor)
        assertFalse(first.hasCompassSensor)
        assertFalse(first.hasHeartRateSensor)
        assertFalse(first.hasBodyTemperatureSensor)
        assertFalse(first.hasLightSensor)
        assertTrue(first.hasLocationPermission)
    }

    @Test
    fun `a watch without a location grant starts without a location subscription`() = runTest {
        grantPermissions(false)
        val states = collectTelemetry(repository())

        deliver()

        assertFalse(states.last().hasLocationPermission)
        assertTrue(locationListeners.isEmpty())
    }

    @Test
    fun `a GPS fix feeds the position, the speed, the trip and the elevation`() = runTest {
        val states = collectTelemetry(repository())
        val fix = location(
            provider = LocationManager.GPS_PROVIDER,
            speedMetresPerSecond = 2f,
            accuracy = 5f,
            latitude = 50.45,
            longitude = 30.52,
            altitude = 123.0,
        )

        locationListeners.single().onLocationChanged(fix)
        deliver()

        val state = states.last()
        assertEquals(50.45, state.latitude!!, 0.0)
        assertEquals(30.52, state.longitude!!, 0.0)
        assertEquals(2f * MS_TO_KMH, state.speedKmh!!, 0f)
        assertEquals(5f, state.gpsAccuracyMeters!!, 0f)
        assertTrue(state.hasGpsFix)
        assertEquals(123.0, state.altitudeMeters!!, 0.0)
        assertNotNull(state.sunriseMillis)
        assertNotNull(state.sunsetMillis)
        verify(exactly = 1) { tripTracker.onMotion(2f * MS_TO_KMH, 0.0, 5f, any()) }
        verify(exactly = 1) { tripTracker.onElevation(123.0, WearElevationSource.GPS) }
    }

    @Test
    fun `a network fix places the watch but never feeds the trip`() = runTest {
        every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true
        val states = collectTelemetry(repository())
        val fix = location(
            provider = LocationManager.NETWORK_PROVIDER,
            speedMetresPerSecond = 1f,
            accuracy = 800f,
            latitude = 48.85,
            longitude = 2.35,
            altitude = 35.0,
        )

        locationListeners.first().onLocationChanged(fix)
        deliver()

        val state = states.last()
        assertEquals(48.85, state.latitude!!, 0.0)
        assertNull(state.altitudeMeters)
        verify(exactly = 0) { tripTracker.onMotion(any(), any(), any(), any()) }
        verify(exactly = 0) { tripTracker.onElevation(any(), any()) }
    }

    @Test
    fun `the step count is relative to the first reading and resets on request`() = runTest {
        offer(stepSensor)
        val repository = repository()
        val states = collectTelemetry(repository)
        val listener = sensorListeners.first()

        listener.onSensorChanged(sensorEvent(stepSensor, 1_000f))
        listener.onSensorChanged(sensorEvent(stepSensor, 1_030f))
        deliver()
        assertEquals(30L, states.last().stepCount)

        repository.resetSteps()
        listener.onSensorChanged(sensorEvent(stepSensor, 1_050f))
        advanceTimeBy(THROTTLE_MS + 1L)
        runCurrent()
        assertEquals(0L, states.last().stepCount)

        listener.onSensorChanged(sensorEvent(stepSensor, 1_075f))
        advanceTimeBy(THROTTLE_MS + 1L)
        runCurrent()
        assertEquals(25L, states.last().stepCount)
    }

    @Test
    fun `a heart rate reading is rounded and a zero reading is ignored`() = runTest {
        offer(heartSensor)
        val states = collectTelemetry(repository())
        val listener = sensorListeners.first()

        listener.onSensorChanged(sensorEvent(heartSensor, 71.6f))
        deliver()
        assertEquals(72, states.last().heartRateBpm)

        listener.onSensorChanged(sensorEvent(heartSensor, 0f))
        advanceTimeBy(THROTTLE_MS + 1L)
        runCurrent()
        assertEquals(72, states.last().heartRateBpm)
    }

    @Test
    fun `without the heart rate grant the heart rate sensor is not registered`() = runTest {
        offer(heartSensor)
        every {
            context.checkPermission(eq(android.Manifest.permission.BODY_SENSORS), any(), any())
        } returns PackageManager.PERMISSION_DENIED
        collectTelemetry(repository())

        runCurrent()

        verify(exactly = 0) { sensorManager.registerListener(any<SensorEventListener>(), heartSensor, any<Int>()) }
    }

    @Test
    fun `a vendor sensor whose name says temperature is taken as the body temperature`() = runTest {
        val skin = sensor(VENDOR_SENSOR_TYPE, sensorStringType = "com.vendor.sensor.skin_temperature", sensorName = "Skin")
        sensorList = listOf(skin)
        val states = collectTelemetry(repository())
        val listener = sensorListeners.first()

        listener.onSensorChanged(sensorEvent(skin, 33.4f))
        deliver()

        assertTrue(states.first().hasBodyTemperatureSensor)
        assertEquals(33.4f, states.last().bodyTemperatureCelsius!!, 0f)
    }

    @Test
    fun `a vendor sensor that does not mention temperature is not claimed`() = runTest {
        val other = sensor(VENDOR_SENSOR_TYPE, sensorStringType = "com.vendor.sensor.stress", sensorName = "Stress")
        sensorList = listOf(other)
        val states = collectTelemetry(repository())

        deliver()

        assertFalse(states.first().hasBodyTemperatureSensor)
        assertTrue(sensorListeners.isEmpty())
    }

    private companion object {
        /** The repository samples its output every 500 ms. */
        const val THROTTLE_MS = 500L

        /** The repository's metres-per-second to kilometres-per-hour factor, applied in single precision. */
        const val MS_TO_KMH = 3.6f

        /** A type id the repository has no dedicated branch for, as a vendor sensor reports. */
        const val VENDOR_SENSOR_TYPE = 65_537
    }
}
