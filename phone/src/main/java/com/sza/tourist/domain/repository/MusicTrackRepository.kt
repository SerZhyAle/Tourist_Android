package com.sza.tourist.domain.repository

import android.net.Uri
import com.sza.tourist.domain.model.stopwatch.MusicTrackOption

/**
 * The device's audio library as a chooser source.
 *
 * Exists because the system document picker cannot guarantee a music-only list on every OEM file
 * provider - the app reads MediaStore audio itself.
 */
interface MusicTrackRepository {

    /** Every audio track of the external media store, ordered by title. */
    suspend fun loadTracks(): List<MusicTrackOption>

    /**
     * The display title of a previously chosen track uri, or null when the uri is not a MediaStore
     * audio track - a legacy SAF uri from before the in-app picker keeps its old label fallback.
     */
    suspend fun titleOf(uri: Uri): String?
}
