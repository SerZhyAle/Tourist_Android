package com.sza.tourist.wear.data.tourist

import android.content.BroadcastReceiver
import android.content.Context
import android.os.PowerManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * S0020: the class is platform wiring, so this suite drives it through what it registers - the receiver is
 * captured from the mocked context and called - and pins the order of the state it reports. Which actions the
 * filter names cannot be read here, because `IntentFilter` is a stub in a unit test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidWearInteractiveStateRepositoryTest {

    private val context = mockk<Context>()
    private val powerManager = mockk<PowerManager>()
    private val receivers = mutableListOf<BroadcastReceiver>()
    private val reported = mutableListOf<Boolean>()
    private var interactive = true

    @Before
    fun setUp() {
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { powerManager.isInteractive } answers { interactive }
        every { context.registerReceiver(capture(receivers), any()) } returns null
        every { context.unregisterReceiver(any()) } just Runs
    }

    private fun TestScope.collectState(): Job {
        val repository = AndroidWearInteractiveStateRepository(context)
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeInteractive().collect { reported.add(it) }
        }
        runCurrent()
        return collection
    }

    private fun TestScope.deliverBroadcast() {
        receivers.single().onReceive(context, mockk(relaxed = true))
        runCurrent()
    }

    @Test
    fun `the current state comes first`() = runTest {
        interactive = false

        collectState()

        assertEquals(listOf(false), reported)
    }

    @Test
    fun `a broadcast reports the state the power manager holds`() = runTest {
        collectState()

        interactive = false
        deliverBroadcast()
        interactive = true
        deliverBroadcast()

        assertEquals(listOf(true, false, true), reported)
    }

    @Test
    fun `a late off broadcast on an awake watch reports nothing new`() = runTest {
        collectState()

        deliverBroadcast()

        assertEquals(listOf(true), reported)
    }

    @Test
    fun `closing the collection unregisters the receiver`() = runTest {
        val collection = collectState()

        collection.cancel()
        runCurrent()

        verify(exactly = 1) { context.unregisterReceiver(receivers.single()) }
    }

    @Test
    fun `a watch without the power service counts as interactive`() = runTest {
        every { context.getSystemService(Context.POWER_SERVICE) } returns null

        collectState()

        assertEquals(listOf(true), reported)
    }
}
