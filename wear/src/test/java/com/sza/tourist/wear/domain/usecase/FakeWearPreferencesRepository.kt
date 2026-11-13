package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.model.UnitSystem
import com.sza.tourist.wear.domain.repository.WearPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An in-memory [WearPreferencesRepository] for the unit tests.
 *
 * In FastMediaSorter this fake sits at the bottom of `ApplyWearSettingsUseCaseTest.kt` and implements the
 * whole preference surface of that product. This product's surface is the unit system, the stopwatch and
 * the Tourist trip, so the fake is its own small file, shared by every test that needs a store.
 */
internal class FakeWearPreferencesRepository : WearPreferencesRepository {

    private val unit = MutableStateFlow(UnitSystem.DEFAULT)
    private val participantCount = MutableStateFlow(1)
    private val lastResult = MutableStateFlow<String?>(null)
    private val session = MutableStateFlow<String?>(null)
    private val trip = MutableStateFlow<String?>(null)

    /** The value a reader of the store would see now. */
    val unitSystemValue: UnitSystem get() = unit.value
    val touristTripValue: String? get() = trip.value

    override val unitSystem: Flow<UnitSystem> = unit
    override suspend fun setUnitSystem(system: UnitSystem) {
        unit.value = system
    }

    override val stopwatchParticipantCount: Flow<Int> = participantCount
    override suspend fun setStopwatchParticipantCount(count: Int) {
        participantCount.value = count
    }

    override val stopwatchLastResult: Flow<String?> = lastResult
    override suspend fun setStopwatchLastResult(value: String?) {
        lastResult.value = value
    }

    override val stopwatchSession: Flow<String?> = session
    override suspend fun setStopwatchSession(value: String?) {
        session.value = value
    }

    override val touristTrip: Flow<String?> = trip
    override suspend fun setTouristTrip(value: String?) {
        trip.value = value
    }
}
