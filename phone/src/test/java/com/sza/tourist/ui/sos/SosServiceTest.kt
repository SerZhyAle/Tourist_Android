package com.sza.tourist.ui.sos

import android.app.Application
import android.content.Intent
import com.sza.tourist.domain.model.sos.SosMode
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SosServiceTest {

    private val application: Application = RuntimeEnvironment.getApplication()

    @Test
    fun `onStartCommand engages sound and light per SosMode and ACTION_STOP tears both down`() {
        val controller = Robolectric.buildService(SosService::class.java)
        val service = controller.get()
        val sound: SosSoundGenerator = mockk(relaxed = true)
        val torch: SosTorchManager = mockk(relaxed = true)
        service.soundGenerator = sound
        service.torchManager = torch

        // ALL engages both sound and light.
        service.onStartCommand(
            Intent(application, SosService::class.java)
                .setAction(SosService.ACTION_START)
                .putExtra(SosService.EXTRA_MODE, SosMode.ALL.name),
            0,
            1,
        )
        assertEquals(SosMode.ALL, SosService.active.value)
        verify(exactly = 1) { sound.start(service) }
        verify(exactly = 1) { torch.start(service) }

        // SOUND_ONLY keeps sound and stops light.
        service.onStartCommand(
            Intent(application, SosService::class.java)
                .setAction(SosService.ACTION_START)
                .putExtra(SosService.EXTRA_MODE, SosMode.SOUND_ONLY.name),
            0,
            2,
        )
        assertEquals(SosMode.SOUND_ONLY, SosService.active.value)
        verify(exactly = 2) { sound.start(service) }
        verify(exactly = 1) { torch.stop(service) }

        // LIGHT_ONLY stops sound and starts light.
        service.onStartCommand(
            Intent(application, SosService::class.java)
                .setAction(SosService.ACTION_START)
                .putExtra(SosService.EXTRA_MODE, SosMode.LIGHT_ONLY.name),
            0,
            3,
        )
        assertEquals(SosMode.LIGHT_ONLY, SosService.active.value)
        verify(exactly = 1) { sound.stop(service) }
        verify(exactly = 2) { torch.start(service) }

        // ACTION_STOP followed by onDestroy tears down both halves and clears active state.
        service.onStartCommand(
            Intent(application, SosService::class.java).setAction(SosService.ACTION_STOP),
            0,
            4,
        )
        controller.destroy()

        assertNull(SosService.active.value)
        verify(exactly = 2) { sound.stop(service) }
        verify(exactly = 2) { torch.stop(service) }
    }
}
