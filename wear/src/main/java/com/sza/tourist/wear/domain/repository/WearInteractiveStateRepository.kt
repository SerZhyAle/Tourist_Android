package com.sza.tourist.wear.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * S0020: whether the watch is interactive. It is not while the screen shows ambient or is off.
 */
interface WearInteractiveStateRepository {

    /**
     * Emits the current state on collection, then every change for as long as the flow is collected.
     */
    fun observeInteractive(): Flow<Boolean>
}
