package com.sza.tourist.domain.repository

import com.sza.tourist.domain.model.tourist.TouristTripSnapshot

/**
 * Where the Tourist trip survives a process restart. An absent store loads as
 * [TouristTripSnapshot.EMPTY] - the ordinary "no trip yet" case, not an error.
 */
interface TouristTripRepository {

    suspend fun load(): TouristTripSnapshot

    suspend fun save(snapshot: TouristTripSnapshot)
}
