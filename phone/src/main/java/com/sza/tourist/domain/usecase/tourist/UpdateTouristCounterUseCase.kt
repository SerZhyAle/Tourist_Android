package com.sza.tourist.domain.usecase.tourist

import com.sza.tourist.domain.repository.TouristCounterRepository
import javax.inject.Inject

/**
 * Every change the counter screen makes. The clock is read here rather than in the screen so the
 * session start and the archived end share one time source.
 */
class UpdateTouristCounterUseCase @Inject constructor(
    private val repository: TouristCounterRepository,
) {

    suspend fun add(delta: Long) {
        val now = System.currentTimeMillis()
        repository.update { it.plus(delta, now) }
    }

    suspend fun reset() {
        val now = System.currentTimeMillis()
        repository.update { it.reset(now) }
    }

    suspend fun setStartValue(start: Long) {
        repository.update { it.withStartValue(start) }
    }

    suspend fun setExtraStepsVisible(visible: Boolean) {
        repository.update { it.copy(extraStepsVisible = visible) }
    }

    suspend fun clearHistory() {
        repository.update { it.clearHistory() }
    }
}
