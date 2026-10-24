package com.sza.tourist.domain.repository

import com.sza.tourist.domain.model.weather.WeatherLocation
import com.sza.tourist.domain.model.weather.WeatherSnapshot

/**
 * What the UI is allowed to know about the weather.
 *
 * [WeatherResult.Stale] exists because "no network" must not blank the block - a two-hour-old reading
 * with a staleness marker is still information, while an empty tile reads as a broken screen.
 */
sealed interface WeatherResult {

    data class Fresh(val snapshot: WeatherSnapshot) : WeatherResult

    data class Stale(val snapshot: WeatherSnapshot, val ageMs: Long) : WeatherResult

    data object Unavailable : WeatherResult
}

interface WeatherRepository {

    suspend fun current(location: WeatherLocation, forceRefresh: Boolean = false): WeatherResult

    suspend fun search(query: String): List<WeatherLocation>
}
