package com.sza.tourist.data.settings

import android.app.Application
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.model.UnitSystem
import com.sza.tourist.domain.model.sos.SosMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The write side of the settings store arrives with the lights (S0005): the two new keys persist
 * under FastMediaSorter's names, and an edit never loses another field.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Robolectric maxSdkVersion=34; targetSdkVersion=36 needs an explicit pin.
class SettingsRepositoryImplTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    fun `an empty store hands back every default`() = runTest {
        val repository = SettingsRepositoryImpl(application)

        val settings = repository.getSettings().first()

        assertEquals(UnitSystem.DEFAULT, settings.unitSystem)
        assertTrue(settings.preventSleep)
        assertEquals(AppSettings.FRONT_FLASHLIGHT_DEFAULT_COLOR, settings.frontFlashlightColor)
        assertFalse(settings.flashlightShortcutNotificationEnabled)
        assertEquals(SosMode.ALL, settings.sosMode)
        assertEquals(AppSettings.STOPWATCH_DEFAULT_PARTICIPANTS, settings.stopwatchParticipantCount)
        assertFalse(settings.stopwatchMusicEnabled)
        assertEquals("", settings.stopwatchMusicUri)
        assertTrue(settings.stopwatchVolumeKeysControl)
    }

    @Test
    fun `an edit persists under the source's key names and survives the process`() = runTest {
        SettingsRepositoryImpl(application).updateSettings {
            it.copy(
                unitSystem = UnitSystem.IMPERIAL,
                frontFlashlightColor = 0xFF2196F3.toInt(),
                flashlightShortcutNotificationEnabled = true,
                sosMode = SosMode.LIGHT_ONLY,
                stopwatchParticipantCount = 4,
                stopwatchMusicEnabled = true,
                stopwatchMusicUri = "content://media/external/audio/media/42",
                stopwatchVolumeKeysControl = false,
            )
        }

        val reread = SettingsRepositoryImpl(application).getSettings().first()

        assertEquals(UnitSystem.IMPERIAL, reread.unitSystem)
        assertEquals(0xFF2196F3.toInt(), reread.frontFlashlightColor)
        assertTrue(reread.flashlightShortcutNotificationEnabled)
        assertEquals(SosMode.LIGHT_ONLY, reread.sosMode)
        assertEquals(4, reread.stopwatchParticipantCount)
        assertTrue(reread.stopwatchMusicEnabled)
        assertEquals("content://media/external/audio/media/42", reread.stopwatchMusicUri)
        assertFalse(reread.stopwatchVolumeKeysControl)
    }

    @Test
    fun `an edit never loses a field it did not touch`() = runTest {
        val repository = SettingsRepositoryImpl(application)
        repository.updateSettings {
            it.copy(preventSleep = false, frontFlashlightColor = 0xFF000000.toInt())
        }

        repository.updateSettings { it.copy(flashlightShortcutNotificationEnabled = true) }
        val settings = repository.getSettings().first()

        assertFalse(settings.preventSleep)
        assertEquals(0xFF000000.toInt(), settings.frontFlashlightColor)
        assertTrue(settings.flashlightShortcutNotificationEnabled)
    }

    @Test
    fun `two concurrent edits both land`() = runTest {
        val repository = SettingsRepositoryImpl(application)

        kotlinx.coroutines.coroutineScope {
            launch { repository.updateSettings { it.copy(preventSleep = false) } }
            launch { repository.updateSettings { it.copy(flashlightShortcutNotificationEnabled = true) } }
        }
        val settings = repository.getSettings().first()

        assertFalse(settings.preventSleep)
        assertTrue(settings.flashlightShortcutNotificationEnabled)
    }
}
