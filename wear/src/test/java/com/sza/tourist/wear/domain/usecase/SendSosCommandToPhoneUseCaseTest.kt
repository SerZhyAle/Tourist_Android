package com.sza.tourist.wear.domain.usecase

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.sza.tourist.wear.data.wear.WearDataLayerPaths
import com.sza.tourist.wear.domain.model.SosMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * S3216: the watch asks the paired phone to raise, or drop, the same distress signal it is running.
 * Fire and forget: no ack, no retry, and an unreachable phone is not an error.
 *
 * Not in the source, where the use case is uncovered; added here with the port.
 */
class SendSosCommandToPhoneUseCaseTest {

    private val context: Context = mockk(relaxed = true)
    private val nodeClient: NodeClient = mockk()
    private val messageClient: MessageClient = mockk()
    private val useCase = SendSosCommandToPhoneUseCase(context)

    @Before
    fun setUp() {
        mockkStatic(Wearable::class)
        every { Wearable.getNodeClient(context) } returns nodeClient
        every { Wearable.getMessageClient(context) } returns messageClient
        every { messageClient.sendMessage(any(), any(), any()) } returns Tasks.forResult(0)
    }

    @After
    fun tearDown() {
        unmockkStatic(Wearable::class)
    }

    @Test
    fun `start sends the mode name to the connected phone`() = runTest {
        connected(node("phone-1"))

        useCase.start(SosMode.SOUND_ONLY)

        verify(exactly = 1) {
            messageClient.sendMessage(
                "phone-1",
                WearDataLayerPaths.SOS_START_FROM_WATCH,
                match { it.decodeToString() == SosMode.SOUND_ONLY.name }
            )
        }
    }

    @Test
    fun `stop sends an empty payload on the stop path`() = runTest {
        connected(node("phone-1"))

        useCase.stop()

        verify(exactly = 1) {
            messageClient.sendMessage(
                "phone-1",
                WearDataLayerPaths.SOS_STOP_FROM_WATCH,
                match { it.isEmpty() }
            )
        }
    }

    @Test
    fun `no phone in range means the watch signals alone and nothing is sent`() = runTest {
        connected()

        useCase.start(SosMode.ALL)

        verify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `failing to list the phones is swallowed and nothing is sent`() = runTest {
        every { nodeClient.connectedNodes } returns Tasks.forException(IOException("no bluetooth"))

        useCase.start(SosMode.ALL)

        verify(exactly = 0) { messageClient.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `every connected phone is told`() = runTest {
        connected(node("phone-1"), node("phone-2"))

        useCase.start(SosMode.LIGHT_ONLY)

        verify(exactly = 1) { messageClient.sendMessage("phone-1", WearDataLayerPaths.SOS_START_FROM_WATCH, any()) }
        verify(exactly = 1) { messageClient.sendMessage("phone-2", WearDataLayerPaths.SOS_START_FROM_WATCH, any()) }
    }

    @Test
    fun `one phone refusing the message does not stop the others being told`() = runTest {
        connected(node("phone-1"), node("phone-2"))
        every {
            messageClient.sendMessage("phone-1", any(), any())
        } returns Tasks.forException(IOException("channel closed"))

        useCase.start(SosMode.ALL)

        verify(exactly = 1) { messageClient.sendMessage("phone-1", WearDataLayerPaths.SOS_START_FROM_WATCH, any()) }
        verify(exactly = 1) { messageClient.sendMessage("phone-2", WearDataLayerPaths.SOS_START_FROM_WATCH, any()) }
    }

    private fun connected(vararg nodes: Node) {
        every { nodeClient.connectedNodes } returns Tasks.forResult(nodes.toList())
    }

    private fun node(nodeId: String): Node {
        val node = mockk<Node>()
        every { node.id } returns nodeId
        return node
    }
}
