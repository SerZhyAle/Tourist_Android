package com.sza.tourist.domain.usecase.tourist

import android.app.Application
import com.sza.tourist.data.gnss.GnssStatusDataSource
import com.sza.tourist.data.sensors.EnvironmentSensorReadingSource
import com.sza.tourist.data.sensors.MagneticDeclinationSource
import com.sza.tourist.data.sensors.MotionReadingSource
import com.sza.tourist.data.sensors.OrientationReadingSource
import com.sza.tourist.data.sensors.StepCountReadingSource
import com.sza.tourist.domain.model.tourist.TouristTripSnapshot
import com.sza.tourist.domain.model.sensors.SensorCapability
import com.sza.tourist.domain.repository.SensorAvailabilityRepository
import com.sza.tourist.domain.repository.TouristTripRepository
import com.sza.tourist.domain.repository.WeatherRepository
import com.sza.tourist.domain.tourist.TouristEnvironmentTelemetry
import com.sza.tourist.domain.tourist.TouristTripTracker
import com.sza.tourist.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pins what the dashboard flow opens with: the capability answer folded into the state, the grant
 * flags, and the one-emission contract of an unsubscribed feed. Robolectric starts with no location
 * grant, which is exactly the on-first-open situation of the screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ObserveTouristDashboardUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val application: Application = org.robolectric.RuntimeEnvironment.getApplication()

    private val tripRepository = mockk<TouristTripRepository>(relaxed = true).also {
        coEvery { it.load() } returns TouristTripSnapshot.EMPTY
    }
    private val tripTracker = TouristTripTracker(tripRepository)
    private val environmentTelemetry = TouristEnvironmentTelemetry(
        mockk<EnvironmentSensorReadingSource>().also {
            every { it.pressure() } returns emptyFlow()
            every { it.light() } returns emptyFlow()
            every { it.magneticField() } returns emptyFlow()
            every { it.level() } returns emptyFlow()
        },
        mockk<MagneticDeclinationSource>(),
        tripTracker,
    )

    private fun newUseCase(
        available: Set<SensorCapability> = emptySet(),
        gnss: GnssStatusDataSource = mockk { every { observe() } returns emptyFlow() },
    ): ObserveTouristDashboardUseCase {
        val availability = mockk<SensorAvailabilityRepository>().also {
            every { it.isAvailable(any()) } answers { firstArg<SensorCapability>() in available }
        }
        return ObserveTouristDashboardUseCase(
            context = application,
            motionReadingSource = mockk { every { readings() } returns emptyFlow() },
            orientationReadingSource = mockk { every { readings() } returns emptyFlow() },
            stepCountReadingSource = mockk { every { readings() } returns emptyFlow() },
            gnssStatusDataSource = gnss,
            sensorAvailabilityRepository = availability,
            weatherRepository = mockk<WeatherRepository>(),
            tripTracker = tripTracker,
            environmentTelemetry = environmentTelemetry,
        )
    }

    @Test
    fun `dashboard opens with capabilities and no grants`() = runTest {
        val useCase = newUseCase(available = setOf(SensorCapability.STEP_COUNTER))

        val states = mutableListOf<com.sza.tourist.domain.model.tourist.TouristDashboardState>()
        val job = launch { useCase().collect { states.add(it) } }
        advanceTimeBy(1_000)
        runCurrent()
        job.cancelAndJoin()

        assertTrue(states.isNotEmpty())
        val first = states.first()
        assertFalse(first.hasLocationPermission)
        assertFalse(first.hasActivityRecognitionPermission)
        assertTrue(first.stepsAvailable)
        assertEquals(setOf(SensorCapability.STEP_COUNTER), first.availableCapabilities)
        assertEquals(com.sza.tourist.domain.model.tourist.TouristTileType.SPEED, first.focusedTile)
    }

    @Test
    fun `steps tile is not offered without the capability`() = runTest {
        val useCase = newUseCase()

        val states = mutableListOf<com.sza.tourist.domain.model.tourist.TouristDashboardState>()
        val job = launch { useCase().collect { states.add(it) } }
        advanceTimeBy(1_000)
        runCurrent()
        job.cancelAndJoin()

        assertTrue(states.isNotEmpty())
        val first = states.first()
        assertFalse(first.stepsAvailable)
        assertNull(first.satellitesTotal)
    }

    @Test
    fun `resetTrip writes the empty trip through the store`() = runTest {
        val useCase = newUseCase()

        useCase.resetTrip()

        coVerify {
            tripRepository.save(match { it.startedAtMillis == null && it.distanceMeters == 0.0 })
        }
    }
}
