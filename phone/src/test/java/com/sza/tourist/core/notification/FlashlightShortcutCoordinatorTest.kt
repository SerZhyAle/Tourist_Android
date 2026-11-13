package com.sza.tourist.core.notification

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.sza.tourist.core.torch.TorchController
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCameraCharacteristics

/**
 * The shade-shortcut coordinator had no test in the source. The port is pinned here: the setting
 * alone decides the entry, and taking the entry away also puts a posted-and-lit torch out.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class FlashlightShortcutCoordinatorTest {

    @get:Rule
    val dispatcherRule = com.sza.tourist.testing.MainDispatcherRule()

    private val application: Application = RuntimeEnvironment.getApplication()
    private val cameraManager = application.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private fun addFlashCamera() {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics).set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true)
        shadowOf(cameraManager).addCamera("0", characteristics)
    }

    private fun settingsFlow(vararg settings: AppSettings) =
        MutableStateFlow(settings.first()).apply { settings.drop(1).forEach { value = it } }

    @Test
    fun `switching the setting on posts the shade entry`() = runTest(dispatcherRule.testDispatcher) {
        addFlashCamera()
        val settings = MutableStateFlow(AppSettings(flashlightShortcutNotificationEnabled = true))
        val repository = mockk<SettingsRepository> { every { getSettings() } returns settings }
        val notifier = mockk<FlashlightShortcutNotifier>(relaxed = true) {
            every { hasFlashUnit() } returns true
        }
        val coordinator = FlashlightShortcutCoordinator(
            application, repository, notifier, TorchController(),
            CoroutineScope(StandardTestDispatcher(testScheduler)),
        )

        coordinator.start()
        advanceUntilIdle()

        verify(exactly = 1) { notifier.show() }
        verify(exactly = 0) { notifier.hide() }
    }

    @Test
    fun `switching the setting off takes the entry away and extinguishes a lit torch it owned`() =
        runTest(dispatcherRule.testDispatcher) {
            addFlashCamera()
            val torch = TorchController()
            val settings = MutableStateFlow(
                AppSettings(flashlightShortcutNotificationEnabled = true)
            )
            val repository = mockk<SettingsRepository> { every { getSettings() } returns settings }
            val notifier = mockk<FlashlightShortcutNotifier>(relaxed = true) {
                every { hasFlashUnit() } returns true
                every { isShown } returns true
            }
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            FlashlightShortcutCoordinator(application, repository, notifier, torch, scope).start()
            advanceUntilIdle()

            // A torch lit through the shortcut itself: the entry is on screen and the flash is on.
            assertTrue(torch.setTorch(application, true))
            settings.value = AppSettings(flashlightShortcutNotificationEnabled = false)
            advanceUntilIdle()

            verify(exactly = 1) { notifier.hide() }
            assertTrue(!torch.isTorchOn)
        }

    @Test
    fun `taking the entry away never reaches into a torch another surface lit`() =
        runTest(dispatcherRule.testDispatcher) {
            addFlashCamera()
            val torch = TorchController()
            val settings = MutableStateFlow(
                AppSettings(flashlightShortcutNotificationEnabled = false)
            )
            val repository = mockk<SettingsRepository> { every { getSettings() } returns settings }
            // Entry never posted: isShown stays false, so the torch belongs to somebody else.
            val notifier = mockk<FlashlightShortcutNotifier>(relaxed = true) {
                every { hasFlashUnit() } returns true
                every { isShown } returns false
            }
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            FlashlightShortcutCoordinator(application, repository, notifier, torch, scope).start()
            advanceUntilIdle()
            assertTrue(torch.setTorch(application, true))

            settings.value = AppSettings()
            advanceUntilIdle()

            verify(atLeast = 1) { notifier.hide() }
            assertTrue(torch.isTorchOn)
        }

    @Test
    fun `a device without a flash never gets the entry, whatever the setting says`() =
        runTest(dispatcherRule.testDispatcher) {
            val settings = MutableStateFlow(
                AppSettings(flashlightShortcutNotificationEnabled = true)
            )
            val repository = mockk<SettingsRepository> { every { getSettings() } returns settings }
            val notifier = mockk<FlashlightShortcutNotifier>(relaxed = true) {
                every { hasFlashUnit() } returns false
            }
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            FlashlightShortcutCoordinator(application, repository, notifier, TorchController(), scope).start()
            advanceUntilIdle()

            verify(exactly = 0) { notifier.show() }
            verify(atLeast = 1) { notifier.hide() }
        }

    @Test
    fun `starting twice adds no second collector`() = runTest(dispatcherRule.testDispatcher) {
        addFlashCamera()
        val settings = MutableStateFlow(AppSettings(flashlightShortcutNotificationEnabled = true))
        val repository = mockk<SettingsRepository> { every { getSettings() } returns settings }
        val notifier = mockk<FlashlightShortcutNotifier>(relaxed = true) {
            every { hasFlashUnit() } returns true
        }
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val coordinator = FlashlightShortcutCoordinator(
            application, repository, notifier, TorchController(), scope,
        )

        coordinator.start()
        coordinator.start()
        advanceUntilIdle()

        verify(exactly = 1) { notifier.show() }
    }

    @Test
    fun `syncOnce applies the stored setting once, for the boot path`() =
        runTest(dispatcherRule.testDispatcher) {
            addFlashCamera()
            val repository = mockk<SettingsRepository> {
                every { getSettings() } returns settingsFlow(
                    AppSettings(flashlightShortcutNotificationEnabled = true)
                )
            }
            val notifier = mockk<FlashlightShortcutNotifier>(relaxed = true) {
                every { hasFlashUnit() } returns true
            }
            val coordinator = FlashlightShortcutCoordinator(
                application, repository, notifier, TorchController(),
                CoroutineScope(StandardTestDispatcher(testScheduler)),
            )

            coordinator.syncOnce()

            verify(exactly = 1) { notifier.show() }
        }
}
