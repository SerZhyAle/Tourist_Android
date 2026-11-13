package com.sza.tourist.wear.ui.apps.tourist

import com.sza.tourist.wear.domain.model.UnitSystem
import com.sza.tourist.wear.domain.model.WearTouristWeatherPayload
import com.sza.tourist.wear.domain.repository.WearInteractiveStateRepository
import com.sza.tourist.wear.domain.repository.WearPreferencesRepository
import com.sza.tourist.wear.domain.repository.WearTouristRepository
import com.sza.tourist.wear.domain.tourist.TouristMetricType
import com.sza.tourist.wear.domain.tourist.WearTouristCounterState
import com.sza.tourist.wear.domain.tourist.WearTouristState
import com.sza.tourist.wear.domain.tourist.WearWeatherPosition
import com.sza.tourist.wear.domain.usecase.ObserveWearTouristCounterUseCase
import com.sza.tourist.wear.domain.usecase.ObserveWearTouristDashboardUseCase
import com.sza.tourist.wear.domain.usecase.ObserveWearTouristWeatherUseCase
import com.sza.tourist.wear.domain.usecase.PublishTouristHealthUseCase
import com.sza.tourist.wear.domain.usecase.ResetWearTouristStepsUseCase
import com.sza.tourist.wear.domain.usecase.ResetWearTouristTripUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The source leaves [TouristViewModel] uncovered; this suite pins what the screen relies on without
 * changing the view model: how the state is assembled, that the sensors are subscribed only while the
 * screen collects, that a granted permission re-subscribes, and what is reported to the phone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TouristViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val dashboard = mockk<ObserveWearTouristDashboardUseCase>()
    private val resetTrip = mockk<ResetWearTouristTripUseCase>()
    private val resetSteps = mockk<ResetWearTouristStepsUseCase>()
    private val preferences = mockk<WearPreferencesRepository>()
    private val publishHealth = mockk<PublishTouristHealthUseCase>()
    private val weatherUseCase = mockk<ObserveWearTouristWeatherUseCase>()
    private val counterUseCase = mockk<ObserveWearTouristCounterUseCase>()

    private val positionsSlot = slot<Flow<WearWeatherPosition?>>()
    private var telemetrySubscriptions = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { resetTrip() } just Runs
        every { resetSteps() } just Runs
        coEvery { publishHealth(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        telemetry: Flow<WearTouristState> = flowOf(WearTouristState()),
        unitSystem: UnitSystem = UnitSystem.METRIC,
        counter: WearTouristCounterState = WearTouristCounterState(),
        weather: Flow<WearTouristWeatherPayload?> = flowOf(null),
    ): TouristViewModel {
        every { dashboard() } answers {
            telemetrySubscriptions++
            telemetry
        }
        every { preferences.unitSystem } returns MutableStateFlow(unitSystem)
        every { counterUseCase() } returns MutableStateFlow(counter)
        every { weatherUseCase(capture(positionsSlot)) } returns weather
        return TouristViewModel(
            observeTelemetryUseCase = dashboard,
            resetTripUseCase = resetTrip,
            resetStepsUseCase = resetSteps,
            preferencesRepository = preferences,
            publishTouristHealthUseCase = publishHealth,
            observeWeatherUseCase = weatherUseCase,
            observeCounterUseCase = counterUseCase,
        ).also { created += it }
    }

    private val created = mutableListOf<TouristViewModel>()

    /**
     * `runTest` runs whatever is still scheduled once the body ends, and the view model keeps a repeating
     * `sample` ticker in its own scope - so a test that leaves the view model alive never finishes.
     * Every test body is therefore closed by cancelling the scope of the view models it created.
     */
    private fun runViewModelTest(body: suspend TestScope.() -> Unit) = runTest {
        try {
            body()
        } finally {
            created.forEach { it.viewModelScope.cancel() }
            created.clear()
        }
    }

    /** The screen collecting the state; the telemetry source is subscribed only while this runs. */
    private fun TestScope.collectUiState(viewModel: TouristViewModel) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect { } }
    }

    @Test
    fun `no sensor is subscribed while nobody collects the state`() = runViewModelTest {
        createViewModel()

        assertEquals(0, telemetrySubscriptions)
    }

    @Test
    fun `state carries telemetry, the metric system and the counter value`() = runViewModelTest {
        val viewModel = createViewModel(
            telemetry = flowOf(WearTouristState(speedKmh = 5f, hasGpsFix = true)),
            unitSystem = UnitSystem.IMPERIAL,
            counter = WearTouristCounterState(value = 7L),
        )

        collectUiState(viewModel)

        val state = viewModel.uiState.value
        assertEquals(5f, state.telemetry.speedKmh)
        assertTrue(state.telemetry.hasGpsFix)
        assertFalse(state.isMetricSystem)
        assertEquals(7L, state.counterValue)
        assertEquals(TouristMetricType.SPEED, state.telemetry.focusedMetric)
    }

    @Test
    fun `the metric system is reported for the metric unit system`() = runViewModelTest {
        val viewModel = createViewModel(unitSystem = UnitSystem.METRIC)

        collectUiState(viewModel)

        assertTrue(viewModel.uiState.value.isMetricSystem)
    }

    @Test
    fun `selecting a metric focuses it`() = runViewModelTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.selectMetric(TouristMetricType.ALTITUDE)

        assertEquals(TouristMetricType.ALTITUDE, viewModel.uiState.value.telemetry.focusedMetric)
    }

    @Test
    fun `athlete mode and the screen lock toggle independently`() = runViewModelTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.toggleAthleteMode()
        assertTrue(viewModel.uiState.value.isAthleteMode)
        assertFalse(viewModel.uiState.value.isScreenLocked)

        viewModel.setScreenLocked(true)
        assertTrue(viewModel.uiState.value.isAthleteMode)
        assertTrue(viewModel.uiState.value.isScreenLocked)

        viewModel.toggleAthleteMode()
        viewModel.setScreenLocked(false)
        assertFalse(viewModel.uiState.value.isAthleteMode)
        assertFalse(viewModel.uiState.value.isScreenLocked)
    }

    @Test
    fun `a granted permission re-subscribes the telemetry source`() = runViewModelTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)
        assertEquals(1, telemetrySubscriptions)

        viewModel.refreshPermissionState()

        assertEquals(2, telemetrySubscriptions)
    }

    /** S0020: stale in ambient, as the owner decided; the state must neither reset nor end with the sensors. */
    @Test
    fun `the last frame stays while the watch is not interactive and a wake refreshes it`() = runViewModelTest {
        val interactive = MutableStateFlow(true)
        val interactiveStateRepository = mockk<WearInteractiveStateRepository>()
        every { interactiveStateRepository.observeInteractive() } returns interactive
        var frame = 0f
        val touristRepository = mockk<WearTouristRepository>()
        every { touristRepository.observeTelemetry() } returns flow {
            frame += 1f
            emit(WearTouristState(speedKmh = frame))
            awaitCancellation()
        }
        val viewModel = createViewModel(
            telemetry = ObserveWearTouristDashboardUseCase(touristRepository, interactiveStateRepository)(),
        )
        collectUiState(viewModel)
        assertEquals(1f, viewModel.uiState.value.telemetry.speedKmh)

        interactive.value = false
        runCurrent()
        assertEquals(1f, viewModel.uiState.value.telemetry.speedKmh)

        interactive.value = true
        runCurrent()
        assertEquals(2f, viewModel.uiState.value.telemetry.speedKmh)
    }

    @Test
    fun `resetting the trip and the steps reaches the use cases`() = runViewModelTest {
        val viewModel = createViewModel()

        viewModel.resetTrip()
        viewModel.resetSteps()

        coVerify(exactly = 1) { resetTrip() }
        verify(exactly = 1) { resetSteps() }
    }

    @Test
    fun `the fix drives the weather request position`() = runViewModelTest {
        val viewModel = createViewModel(
            telemetry = flowOf(WearTouristState(latitude = 50.45, longitude = 30.52)),
        )

        collectUiState(viewModel)

        val positions = positionsSlot.captured as StateFlow<*>
        assertEquals(WearWeatherPosition(50.45, 30.52), positions.value)
    }

    @Test
    fun `no weather position exists before the first fix`() = runViewModelTest {
        val viewModel = createViewModel(telemetry = flowOf(WearTouristState(latitude = null, longitude = null)))

        collectUiState(viewModel)

        val positions = positionsSlot.captured as StateFlow<*>
        assertNull(positions.value)
    }

    @Test
    fun `the weather answer is merged into the telemetry`() = runViewModelTest {
        val answer = WearTouristWeatherPayload(
            observedAtMs = 1_000L,
            apparentTemperatureCelsius = 12.5,
            humidityPercent = 60,
            windSpeedKmh = 10.0,
            windGustsKmh = null,
            windDirectionDegrees = null,
            uvIndex = null,
            precipitationProbabilityPercent = null,
            cloudCoverPercent = null,
            visibilityMetres = null,
        )
        val viewModel = createViewModel(weather = flowOf(answer))

        collectUiState(viewModel)

        assertEquals(answer, viewModel.uiState.value.telemetry.weather)
    }

    @Test
    fun `a heart rate reading is reported to the phone once per interval`() = runViewModelTest {
        val viewModel = createViewModel(
            telemetry = flowOf(WearTouristState(heartRateBpm = 72, bodyTemperatureCelsius = 36.6f)),
        )
        collectUiState(viewModel)

        advanceTimeBy(HEALTH_PUBLISH_INTERVAL_MS + 1L)
        runCurrent()

        coVerify(exactly = 1) { publishHealth(72, 36.6f) }
    }

    @Test
    fun `nothing is reported while neither heart rate nor temperature is known`() = runViewModelTest {
        val viewModel = createViewModel(telemetry = flowOf(WearTouristState(speedKmh = 4f)))
        collectUiState(viewModel)

        advanceTimeBy(HEALTH_PUBLISH_INTERVAL_MS * 2)
        runCurrent()

        coVerify(exactly = 0) { publishHealth(any(), any()) }
    }

    private companion object {
        /** Mirrors the view model's private publish interval. */
        const val HEALTH_PUBLISH_INTERVAL_MS = 5_000L
    }
}
