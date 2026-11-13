package com.sza.tourist.service

import com.google.gson.Gson
import com.sza.tourist.domain.model.WearEventEnvelope
import com.sza.tourist.domain.model.WearEventEnvelopeCodec
import com.sza.tourist.domain.model.WearTouristWeatherPayload
import com.sza.tourist.domain.model.WearTouristWeatherRequestPayload
import com.sza.tourist.domain.repository.WearableDataLayerRepository
import com.sza.tourist.domain.usecase.tourist.GetWatchTouristWeatherUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/**
 * S0008: the phone end of the watch's Tourist weather request. New in this product - the source class has
 * no test.
 */
class WearTouristWeatherRequestHandlerTest {

    private val gson = Gson()
    private val codec = WearEventEnvelopeCodec()
    private val getWeather = mockk<GetWatchTouristWeatherUseCase>()
    private val wearable = mockk<WearableDataLayerRepository>(relaxed = true)
    private val handler = WearTouristWeatherRequestHandler(gson, getWeather, wearable)

    private val answer = WearTouristWeatherPayload(
        observedAtMs = OBSERVED_AT,
        apparentTemperatureCelsius = 8.0,
        humidityPercent = 64,
        windSpeedKmh = 12.5,
        windGustsKmh = 30.0,
        windDirectionDegrees = 230,
        uvIndex = 3.2,
        precipitationProbabilityPercent = 40,
        cloudCoverPercent = 75,
        visibilityMetres = 24_000.0,
    )

    private fun request(json: String): ByteArray = codec.encode(
        WearEventEnvelope(
            eventType = WearDataLayerPaths.EVENT_TOURIST_WEATHER_REQUEST,
            sentAt = 1L,
            data = json.toByteArray(Charsets.UTF_8),
        ),
    )

    private fun requestAt(latitude: Double, longitude: Double): ByteArray =
        request(gson.toJson(WearTouristWeatherRequestPayload(latitude, longitude, requestedAtMs = 5L)))

    @Test
    fun `a request is answered at the asked place with the weather envelope`() = runTest {
        coEvery { getWeather(LAT, LON) } returns answer
        val path = slot<String>()
        val envelope = slot<WearEventEnvelope>()
        coEvery { wearable.putEnvelopeDataItem(capture(path), capture(envelope)) } returns Unit

        handler.handle(requestAt(LAT, LON))

        assertEquals(WearDataLayerPaths.TOURIST_WEATHER, path.captured)
        assertEquals(WearDataLayerPaths.EVENT_TOURIST_WEATHER, envelope.captured.eventType)
        val sent = gson.fromJson(String(envelope.captured.data, Charsets.UTF_8), WearTouristWeatherPayload::class.java)
        assertEquals(answer, sent)
    }

    @Test
    fun `no reading on the phone leaves the request unanswered`() = runTest {
        coEvery { getWeather(LAT, LON) } returns null

        handler.handle(requestAt(LAT, LON))

        coVerify(exactly = 0) { wearable.putEnvelopeDataItem(any(), any()) }
    }

    @Test
    fun `an undecodable request is dropped before the weather is asked for`() = runTest {
        handler.handle(request("""{"latitude":"north"}"""))
        handler.handle("not an envelope".toByteArray())

        coVerify(exactly = 0) { getWeather(any(), any()) }
        coVerify(exactly = 0) { wearable.putEnvelopeDataItem(any(), any()) }
    }

    @Test
    fun `a failed send is logged and does not reach the listener`() = runTest {
        coEvery { getWeather(LAT, LON) } returns answer
        coEvery { wearable.putEnvelopeDataItem(any(), any()) } throws IOException("Data Layer gone")

        handler.handle(requestAt(LAT, LON))
    }

    @Test
    fun `cancellation while sending is rethrown`() = runTest {
        coEvery { getWeather(LAT, LON) } returns answer
        coEvery { wearable.putEnvelopeDataItem(any(), any()) } throws CancellationException("scope closed")

        try {
            handler.handle(requestAt(LAT, LON))
            fail("expected CancellationException")
        } catch (expected: CancellationException) {
            // cooperative cancellation must not be swallowed by the send guard
        }
    }

    private companion object {
        const val LAT = 51.5
        const val LON = -0.12
        const val OBSERVED_AT = 1_700_000_000_000L
    }
}
