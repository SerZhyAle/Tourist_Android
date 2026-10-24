package com.sza.tourist.domain.usecase.tourist

import com.sza.tourist.domain.model.tourist.TouristCounterState
import com.sza.tourist.domain.repository.TouristCounterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The Tourist tally counter as the dashboard tile and the counter screen show it. */
class ObserveTouristCounterUseCase @Inject constructor(
    private val repository: TouristCounterRepository,
) {

    operator fun invoke(): Flow<TouristCounterState> = repository.observe()
}
