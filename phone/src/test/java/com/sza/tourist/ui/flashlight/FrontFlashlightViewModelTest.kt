package com.sza.tourist.ui.flashlight

import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.repository.SettingsRepository
import com.sza.tourist.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The front flashlight's view model had no test in the source. The port is pinned here: the glow
 * colour is the one piece of state that survives a run, and the picker's pick is written through the
 * settings store.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FrontFlashlightViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private fun repository(flow: MutableStateFlow<AppSettings>): SettingsRepository {
        val editSlot = slot<(AppSettings) -> AppSettings>()
        return mockk {
            every { getSettings() } returns flow
            coEvery { updateSettings(capture(editSlot)) } coAnswers {
                flow.value = editSlot.captured.invoke(flow.value)
            }
        }
    }

    @Test
    fun `the lamp starts opaque white before the store answers`() = runTest(dispatcherRule.testDispatcher) {
        val flow = MutableStateFlow(AppSettings())
        val viewModel = FrontFlashlightViewModel(repository(flow))

        assertEquals(AppSettings.FRONT_FLASHLIGHT_DEFAULT_COLOR, viewModel.glowColor.value)
    }

    @Test
    fun `a stored colour is what the lamp lights up with`() = runTest(dispatcherRule.testDispatcher) {
        val flow = MutableStateFlow(AppSettings(frontFlashlightColor = 0xFF2196F3.toInt()))
        val viewModel = FrontFlashlightViewModel(repository(flow))
        // The lamp's flow runs only while a collector is on it - the screen is that collector.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.glowColor.collect {} }

        advanceUntilIdle()

        assertEquals(0xFF2196F3.toInt(), viewModel.glowColor.value)
    }

    @Test
    fun `a picked colour is written to the store alone`() = runTest(dispatcherRule.testDispatcher) {
        val flow = MutableStateFlow(AppSettings(preventSleep = false))
        val viewModel = FrontFlashlightViewModel(repository(flow))

        viewModel.setGlowColor(0xFFF44336.toInt())
        advanceUntilIdle()

        assertEquals(0xFFF44336.toInt(), flow.value.frontFlashlightColor)
        // Nobody else's field moves with the pick.
        assertEquals(false, flow.value.preventSleep)
    }
}
