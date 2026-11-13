package com.sza.tourist.wear.ui.apps.sos

import android.content.Context
import com.sza.tourist.wear.domain.model.SosMode
import com.sza.tourist.wear.domain.model.SosMorseCadence
import com.sza.tourist.wear.domain.sos.SosSyncBus
import com.sza.tourist.wear.domain.usecase.SendSosCommandToPhoneUseCase
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * S3216: the watch's distress signal - which mode runs, which span of the Morse cadence the display is
 * in, and what is echoed to the paired phone.
 *
 * Not in the source, where the view model is uncovered; added here with the port. The one rule worth a
 * test of its own is the echo: a start or stop that ARRIVED from the phone must not be sent back, or two
 * devices would start each other forever.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SosViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val context: Context = mockk(relaxed = true)
    private val sound: SosSoundGenerator = mockk(relaxed = true)
    private val sendToPhone: SendSosCommandToPhoneUseCase = mockk(relaxed = true)
    private val bus = SosSyncBus()
    private lateinit var viewModel: SosViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        // The echo to the phone launches on the application scope; unconfined, so it has run by the
        // time the call that asked for it returns.
        viewModel = SosViewModel(
            appContext = context,
            soundGenerator = sound,
            syncBus = bus,
            sendSosCommandToPhone = sendToPhone,
            applicationScope = CoroutineScope(UnconfinedTestDispatcher(scheduler)),
        )
    }

    @After
    fun tearDown() {
        // Ends the cadence loop, which would otherwise keep the shared scheduler busy forever.
        viewModel.stop(echoToPhone = false)
        Dispatchers.resetMain()
    }

    @Test
    fun `the screen starts arming with no mode and a dark display`() {
        assertNull(viewModel.mode.value)
        assertFalse(viewModel.isLit.value)
    }

    @Test
    fun `starting in ALL engages the siren and the strobe and tells the phone`() {
        viewModel.start(SosMode.ALL)
        scheduler.runCurrent()

        assertEquals(SosMode.ALL, viewModel.mode.value)
        verify { sound.start(context) }
        assertTrue("the first span of the cadence is an engaged one", viewModel.isLit.value)
        coVerify(exactly = 1) { sendToPhone.start(SosMode.ALL) }
    }

    @Test
    fun `the display follows the cadence spans`() {
        viewModel.start(SosMode.ALL)
        scheduler.runCurrent()
        assertTrue(viewModel.isLit.value)

        // The cadence opens with a dot and the one-dot gap that holds a letter together.
        scheduler.advanceTimeBy(SosMorseCadence.DOT_MS)
        scheduler.runCurrent()
        assertFalse(viewModel.isLit.value)

        scheduler.advanceTimeBy(SosMorseCadence.DOT_MS)
        scheduler.runCurrent()
        assertTrue(viewModel.isLit.value)
    }

    @Test
    fun `sound only never runs the strobe`() {
        viewModel.start(SosMode.SOUND_ONLY)
        scheduler.runCurrent()

        verify { sound.start(context) }
        assertFalse(viewModel.isLit.value)
    }

    @Test
    fun `light only keeps the siren off and runs the strobe`() {
        viewModel.start(SosMode.LIGHT_ONLY)
        scheduler.runCurrent()

        verify(exactly = 0) { sound.start(any()) }
        verify { sound.stop(context) }
        assertTrue(viewModel.isLit.value)
    }

    @Test
    fun `a start that arrived from the phone is not echoed back`() {
        viewModel.start(SosMode.ALL, echoToPhone = false)

        coVerify(exactly = 0) { sendToPhone.start(any()) }
    }

    @Test
    fun `starting takes the pending mode off the bus`() {
        bus.requestStart(SosMode.ALL)
        assertEquals(SosMode.ALL, viewModel.pendingStartMode.value)

        viewModel.start(SosMode.ALL, echoToPhone = false)

        assertNull(viewModel.pendingStartMode.value)
    }

    @Test
    fun `stopping silences the siren, returns to arming and tells the phone`() {
        viewModel.start(SosMode.ALL)
        scheduler.runCurrent()

        viewModel.stop()

        assertNull(viewModel.mode.value)
        assertFalse(viewModel.isLit.value)
        verify { sound.stop(context) }
        coVerify(exactly = 1) { sendToPhone.stop() }
    }

    @Test
    fun `stopping when nothing is running tells nobody`() {
        viewModel.stop()

        coVerify(exactly = 0) { sendToPhone.stop() }
    }

    @Test
    fun `a stop that arrived from the phone is not echoed back`() {
        viewModel.start(SosMode.ALL, echoToPhone = false)

        viewModel.stop(echoToPhone = false)

        assertNull(viewModel.mode.value)
        coVerify(exactly = 0) { sendToPhone.stop() }
    }
}
