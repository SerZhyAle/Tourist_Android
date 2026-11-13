package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.repository.WearInteractiveStateRepository
import com.sza.tourist.wear.domain.repository.WearTouristRepository
import com.sza.tourist.wear.domain.tourist.WearTouristState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * S0020: the telemetry source, which holds every sensor and location registration, is subscribed only while
 * the watch is interactive. No test here advances the clock, so a release proven here has no grace in it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveWearTouristDashboardUseCaseTest {

    private val interactive = MutableStateFlow(true)
    private val touristRepository = mockk<WearTouristRepository>()
    private val interactiveStateRepository = mockk<WearInteractiveStateRepository>()

    private var subscriptions = 0
    private var held = 0
    private val states = mutableListOf<WearTouristState>()

    @Before
    fun setUp() {
        every { interactiveStateRepository.observeInteractive() } returns interactive
        every { touristRepository.observeTelemetry() } returns callbackFlow {
            subscriptions++
            held++
            trySend(WearTouristState(speedKmh = subscriptions.toFloat()))
            awaitClose { held-- }
        }
    }

    private fun TestScope.collectTelemetry(): Job {
        val useCase = ObserveWearTouristDashboardUseCase(touristRepository, interactiveStateRepository)
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { states.add(it) }
        }
        runCurrent()
        return collection
    }

    @Test
    fun `the source is held while the watch is interactive`() = runTest {
        collectTelemetry()

        assertEquals(1, held)
        assertEquals(1, states.size)
    }

    @Test
    fun `leaving the interactive state releases the source at once`() = runTest {
        collectTelemetry()

        interactive.value = false
        runCurrent()

        assertEquals(0, held)
    }

    @Test
    fun `a wake subscribes the source afresh`() = runTest {
        collectTelemetry()
        interactive.value = false
        runCurrent()

        interactive.value = true
        runCurrent()

        assertEquals(2, subscriptions)
        assertEquals(1, held)
        assertEquals(2f, states.last().speedKmh)
    }

    @Test
    fun `a watch that is not interactive at the start subscribes nothing`() = runTest {
        interactive.value = false

        collectTelemetry()

        assertEquals(0, subscriptions)
        assertTrue(states.isEmpty())
    }

    /** A completed flow here would end the view model's combine, and the wake would have nothing to restart. */
    @Test
    fun `the flow stays open while the watch is not interactive`() = runTest {
        val collection = collectTelemetry()

        interactive.value = false
        runCurrent()

        assertTrue(collection.isActive)
        assertEquals(1, states.size)
    }
}
