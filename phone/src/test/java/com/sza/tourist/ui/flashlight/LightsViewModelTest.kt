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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The lights screen is this product's own seat for the shade-shortcut switch, so its model gets its
 * test here: the switch renders the store, and a flip writes exactly the one field.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LightsViewModelTest {

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
    fun `the switch renders off until the store says otherwise`() = runTest(dispatcherRule.testDispatcher) {
        val flow = MutableStateFlow(AppSettings(flashlightShortcutNotificationEnabled = true))
        val viewModel = LightsViewModel(repository(flow))
        // The switch renders from a flow that runs only while the screen collects it.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.settings.collect {} }

        advanceUntilIdle()

        assertTrue(viewModel.settings.value.flashlightShortcutNotificationEnabled)
    }

    @Test
    fun `switching the shortcut on writes the one field`() = runTest(dispatcherRule.testDispatcher) {
        val flow = MutableStateFlow(AppSettings(frontFlashlightColor = 0xFF000000.toInt()))
        val viewModel = LightsViewModel(repository(flow))

        viewModel.setFlashlightShortcutEnabled(true)
        advanceUntilIdle()

        assertTrue(flow.value.flashlightShortcutNotificationEnabled)
        assertEquals(0xFF000000.toInt(), flow.value.frontFlashlightColor)
    }

    @Test
    fun `switching the shortcut off writes the one field`() = runTest(dispatcherRule.testDispatcher) {
        val flow = MutableStateFlow(AppSettings(flashlightShortcutNotificationEnabled = true))
        val viewModel = LightsViewModel(repository(flow))

        viewModel.setFlashlightShortcutEnabled(false)
        advanceUntilIdle()

        assertFalse(flow.value.flashlightShortcutNotificationEnabled)
    }
}
