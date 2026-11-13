package com.sza.tourist.core.capability

import javax.inject.Inject

/**
 * The `noLegal` edition's answer: the watch tiles are offered. Only the noLegal watch carries the Tourist
 * dashboard that reads the heart rate and body temperature and publishes them (owner decision 4.3, S0008).
 */
class NoLegalWatchHealthCapability @Inject constructor() : WatchHealthCapability {
    override val isOffered: Boolean = true
}
