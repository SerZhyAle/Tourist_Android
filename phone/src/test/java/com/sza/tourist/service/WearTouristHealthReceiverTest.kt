package com.sza.tourist.service

import com.google.gson.Gson
import com.sza.tourist.data.tourist.WatchHealthRepositoryImpl
import com.sza.tourist.domain.model.WearEventEnvelope
import com.sza.tourist.domain.model.WearEventEnvelopeCodec
import com.sza.tourist.domain.model.tourist.WatchHealthReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * S0008: the phone end of the watch's Tourist health Data Item. New in this product - the source class has
 * no test.
 */
class WearTouristHealthReceiverTest {

    private val gson = Gson()
    private val codec = WearEventEnvelopeCodec()
    private val repository = WatchHealthRepositoryImpl()
    private val receiver = WearTouristHealthReceiver(gson, repository)

    private fun wire(json: String, sentAt: Long = WATCH_CLOCK): ByteArray = codec.encode(
        WearEventEnvelope(
            eventType = WearDataLayerPaths.EVENT_TOURIST_HEALTH,
            sentAt = sentAt,
            data = json.toByteArray(Charsets.UTF_8),
        ),
    )

    @Test
    fun `a report becomes the latest reading stamped with the phone arrival time`() {
        receiver.handle(wire("""{"heartRateBpm":72,"bodyTemperatureCelsius":36.5}"""), ARRIVED_AT)

        assertEquals(WatchHealthReading(72, 36.5f, ARRIVED_AT), repository.latest.value)
    }

    @Test
    fun `the watch clock never reaches the reading`() {
        receiver.handle(wire("""{"heartRateBpm":60,"bodyTemperatureCelsius":null}""", sentAt = 1L), ARRIVED_AT)

        assertEquals(ARRIVED_AT, repository.latest.value!!.receivedAtMillis)
    }

    @Test
    fun `a missing sensor arrives as no value rather than a zero`() {
        receiver.handle(wire("""{"heartRateBpm":null,"bodyTemperatureCelsius":null}"""), ARRIVED_AT)

        val reading = repository.latest.value!!
        assertNull(reading.heartRateBpm)
        assertNull(reading.bodyTemperatureCelsius)
    }

    @Test
    fun `an undecodable payload is dropped and leaves the previous reading in place`() {
        receiver.handle(wire("""{"heartRateBpm":72,"bodyTemperatureCelsius":36.5}"""), ARRIVED_AT)

        receiver.handle(wire("""{"heartRateBpm":"fast"}"""), ARRIVED_AT + 1)
        receiver.handle("not an envelope".toByteArray(), ARRIVED_AT + 2)
        receiver.handle("{".toByteArray(), ARRIVED_AT + 3)
        receiver.handle(wire("null"), ARRIVED_AT + 4)

        assertEquals(WatchHealthReading(72, 36.5f, ARRIVED_AT), repository.latest.value)
    }

    private companion object {
        const val WATCH_CLOCK = 1_700_000_000_000L
        const val ARRIVED_AT = 1_700_000_005_000L
    }
}
