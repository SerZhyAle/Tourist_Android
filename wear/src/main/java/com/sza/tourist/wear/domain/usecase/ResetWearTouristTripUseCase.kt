package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.repository.WearTouristRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * S3007: resets the accumulated trip distance on Wear OS.
 */
@Singleton
class ResetWearTouristTripUseCase @Inject constructor(
    private val touristRepository: WearTouristRepository,
) {

    suspend operator fun invoke() {
        touristRepository.resetTrip()
    }
}
