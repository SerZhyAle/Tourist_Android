package com.sza.tourist.ui.sos

import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.model.sos.SosMode
import com.sza.tourist.domain.repository.SettingsRepository
import com.sza.tourist.domain.usecase.sos.GetSosModeUseCase
import com.sza.tourist.domain.usecase.sos.SaveSosModeUseCase
import com.sza.tourist.domain.usecase.wear.SendSosCommandToWatchUseCase
import com.sza.tourist.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SosViewModelTest {

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
    fun `resolveInitialMode without watch mode loads stored mode and notifies the watch`() =
        runTest(dispatcherRule.testDispatcher) {
            val flow = MutableStateFlow(AppSettings(sosMode = SosMode.SOUND_ONLY))
            val repo = repository(flow)
            val sendSos: SendSosCommandToWatchUseCase = mockk(relaxed = true)
            val torchManager: SosTorchManager = mockk(relaxed = true) {
                every { isLit } returns MutableStateFlow(false)
            }
            val viewModel = SosViewModel(
                getSosMode = GetSosModeUseCase(repo),
                saveSosMode = SaveSosModeUseCase(repo),
                sendSosCommandToWatch = sendSos,
                torchManager = torchManager,
                applicationScope = this,
            )

            viewModel.resolveInitialMode(requested = null)
            advanceUntilIdle()

            assertEquals(SosMode.SOUND_ONLY, viewModel.mode.value)
            coVerify(exactly = 1) { sendSos.start(SosMode.SOUND_ONLY) }
        }

    @Test
    fun `resolveInitialMode with watch mode adopts and persists it without echoing start back to watch`() =
        runTest(dispatcherRule.testDispatcher) {
            val flow = MutableStateFlow(AppSettings(sosMode = SosMode.ALL))
            val repo = repository(flow)
            val sendSos: SendSosCommandToWatchUseCase = mockk(relaxed = true)
            val torchManager: SosTorchManager = mockk(relaxed = true) {
                every { isLit } returns MutableStateFlow(false)
            }
            val viewModel = SosViewModel(
                getSosMode = GetSosModeUseCase(repo),
                saveSosMode = SaveSosModeUseCase(repo),
                sendSosCommandToWatch = sendSos,
                torchManager = torchManager,
                applicationScope = this,
            )

            viewModel.resolveInitialMode(requested = SosMode.LIGHT_ONLY)
            advanceUntilIdle()

            assertEquals(SosMode.LIGHT_ONLY, viewModel.mode.value)
            assertEquals(SosMode.LIGHT_ONLY, flow.value.sosMode)
            coVerify(exactly = 0) { sendSos.start(any()) }
        }

    @Test
    fun `select updates mode, persists it, and forwards start to watch`() =
        runTest(dispatcherRule.testDispatcher) {
            val flow = MutableStateFlow(AppSettings(sosMode = SosMode.ALL))
            val repo = repository(flow)
            val sendSos: SendSosCommandToWatchUseCase = mockk(relaxed = true)
            val torchManager: SosTorchManager = mockk(relaxed = true) {
                every { isLit } returns MutableStateFlow(false)
            }
            val viewModel = SosViewModel(
                getSosMode = GetSosModeUseCase(repo),
                saveSosMode = SaveSosModeUseCase(repo),
                sendSosCommandToWatch = sendSos,
                torchManager = torchManager,
                applicationScope = this,
            )

            viewModel.select(SosMode.SOUND_ONLY)
            advanceUntilIdle()

            assertEquals(SosMode.SOUND_ONLY, viewModel.mode.value)
            assertEquals(SosMode.SOUND_ONLY, flow.value.sosMode)
            coVerify(exactly = 1) { sendSos.start(SosMode.SOUND_ONLY) }
        }

    @Test
    fun `requestStopEverywhere dispatches stop on applicationScope`() =
        runTest(dispatcherRule.testDispatcher) {
            val flow = MutableStateFlow(AppSettings())
            val repo = repository(flow)
            val sendSos: SendSosCommandToWatchUseCase = mockk(relaxed = true)
            val torchManager: SosTorchManager = mockk(relaxed = true) {
                every { isLit } returns MutableStateFlow(false)
            }
            val viewModel = SosViewModel(
                getSosMode = GetSosModeUseCase(repo),
                saveSosMode = SaveSosModeUseCase(repo),
                sendSosCommandToWatch = sendSos,
                torchManager = torchManager,
                applicationScope = this,
            )

            viewModel.requestStopEverywhere()
            advanceUntilIdle()

            coVerify(exactly = 1) { sendSos.stop() }
        }
}
