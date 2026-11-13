package com.sza.tourist.domain.usecase.stopwatch

import com.sza.tourist.domain.model.stopwatch.MusicTrackOption
import com.sza.tourist.domain.repository.MusicTrackRepository
import javax.inject.Inject

/**
 * Loads the device's music for a chooser dialog.
 *
 * A use case rather than a repository call at the call site because the screen must not import a
 * repository.
 */
class LoadMusicTracksUseCase @Inject constructor(
    private val repository: MusicTrackRepository,
) {

    suspend operator fun invoke(): List<MusicTrackOption> = repository.loadTracks()
}
