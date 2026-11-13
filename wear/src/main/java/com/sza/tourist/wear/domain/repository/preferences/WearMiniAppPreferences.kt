package com.sza.tourist.wear.domain.repository.preferences

import kotlinx.coroutines.flow.Flow

/**
 * State owned by the watch's programs, kept on the watch and never reconciled with the phone.
 * Trimmed to the stopwatch and the Tourist trip; the calculator and game state are not part of this product.
 */
interface WearMiniAppPreferences {

    /**
     * S2825: the stopwatch's participant count.
     *
     * An absent key is a first run, not a broken store, and a stored value the app no longer offers is
     * snapped into the allowed set rather than reported.
     */
    val stopwatchParticipantCount: Flow<Int>
    suspend fun setStopwatchParticipantCount(count: Int)

    /**
     * S2825: the rendered text of the last finished measurement.
     *
     * Null means no measurement has been finished yet - the watch has no file to export to, so this is
     * where a result survives the program being dismissed.
     */
    val stopwatchLastResult: Flow<String?>
    suspend fun setStopwatchLastResult(value: String?)

    /** S3555: the measurement in progress, opaque here - `WearStopwatchSnapshot` owns its format. */
    val stopwatchSession: Flow<String?>
    suspend fun setStopwatchSession(value: String?)

    /** S4146: the Tourist trip and its pressure samples, opaque here - WearTripSnapshotCodec owns the format. */
    val touristTrip: Flow<String?>
    suspend fun setTouristTrip(value: String?)
}
