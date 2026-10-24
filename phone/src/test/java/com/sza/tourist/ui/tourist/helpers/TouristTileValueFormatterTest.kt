package com.sza.tourist.ui.tourist.helpers

import android.content.Context
import com.sza.tourist.core.format.QuantityFormatter
import com.sza.tourist.domain.model.UnitSystem
import com.sza.tourist.domain.model.tourist.GpsAccuracyGrade
import com.sza.tourist.domain.model.tourist.PressureTrend
import com.sza.tourist.domain.model.weather.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pins the tile scale decisions: metric and imperial readings, the large-to-small unit switch of the
 * trip distance, and the spoken-free field split (value and unit travel as two strings).
 */
@RunWith(RobolectricTestRunner::class)
class TouristTileValueFormatterTest {

    private lateinit var context: Context
    private lateinit var quantityFormatter: QuantityFormatter

    @Before
    fun setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication()
        quantityFormatter = QuantityFormatter(context)
    }

    private fun metricFormatter() = TouristTileValueFormatter(context, quantityFormatter) { UnitSystem.METRIC }

    private fun imperialFormatter() = TouristTileValueFormatter(context, quantityFormatter) { UnitSystem.IMPERIAL }

    @Test
    fun `speed keeps kmh on the metric scale`() {
        val reading = metricFormatter().speed(10f)

        assertEquals("10.0", reading.value)
        assertEquals(context.getString(com.sza.tourist.R.string.tourist_unit_kmh), reading.unit)
    }

    @Test
    fun `speed converts to mph on the imperial scale`() {
        val reading = imperialFormatter().speed(100f)

        assertEquals("62.1", reading.value)
        assertEquals(context.getString(com.sza.tourist.R.string.tourist_unit_mph), reading.unit)
    }

    @Test
    fun `speed without a fix is the no-value marker in its unit`() {
        val reading = metricFormatter().speed(null)

        assertEquals(TouristTileValueFormatter.NO_VALUE, reading.value)
        assertEquals(context.getString(com.sza.tourist.R.string.tourist_unit_kmh), reading.unit)
    }

    @Test
    fun `altitude converts metres to feet on the imperial scale`() {
        val reading = imperialFormatter().altitude(1000.0)

        assertEquals("3281", reading.value)
    }

    @Test
    fun `trip distance reads metres under a kilometre and kilometres from one`() {
        assertEquals("500", metricFormatter().tripDistance(500.0).value)
        assertEquals("1.50", metricFormatter().tripDistance(1500.0).value)
    }

    @Test
    fun `temperature converts to Fahrenheit on the imperial scale`() {
        val reading = imperialFormatter().temperature(100f)

        assertEquals("212.0", reading.value)
    }

    @Test
    fun `pressure keeps hPa on metric and converts to inHg on imperial`() {
        assertEquals("1013", metricFormatter().pressure(1013f).value)
        assertEquals("29.91", imperialFormatter().pressure(1013f).value)
    }

    @Test
    fun `pace has no value at a standstill`() {
        val reading = metricFormatter().pace(null)

        assertEquals(TouristTileValueFormatter.NO_VALUE, reading.value)
    }

    @Test
    fun `duration renders h mm ss and no reading renders the marker`() {
        assertEquals("1:01:05", metricFormatter().duration(3_665_000L))
        assertEquals("--:--", metricFormatter().duration(null))
    }

    @Test
    fun `cardinal rounds to the eight compass points`() {
        assertEquals("N", metricFormatter().cardinal(0f))
        assertEquals("NE", metricFormatter().cardinal(45f))
        assertEquals("NW", metricFormatter().cardinal(315f))
        // 350 sits within 22.5 degrees of north, so it rounds to N, not NW.
        assertEquals("N", metricFormatter().cardinal(350f))
        assertEquals("", metricFormatter().cardinal(null))
    }

    @Test
    fun `weather condition maps every named condition and none for unknown`() {
        assertEquals(
            context.getString(com.sza.tourist.R.string.tourist_weather_condition_clear),
            metricFormatter().weatherCondition(WeatherCondition.CLEAR),
        )
        assertEquals(null, metricFormatter().weatherCondition(WeatherCondition.UNKNOWN))
        assertEquals(null, metricFormatter().weatherCondition(null))
    }

    @Test
    fun `gps grade and trend read from resources`() {
        assertEquals(
            context.getString(com.sza.tourist.R.string.tourist_gps_accuracy_good),
            metricFormatter().gpsGrade(GpsAccuracyGrade.GOOD),
        )
        assertEquals(
            context.getString(com.sza.tourist.R.string.tourist_trend_falling),
            metricFormatter().trend(PressureTrend.FALLING),
        )
    }
}
