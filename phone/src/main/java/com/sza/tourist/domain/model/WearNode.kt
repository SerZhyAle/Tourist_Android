package com.sza.tourist.domain.model

/**
 * GMS-free representation of a connected Wear OS node (paired watch).
 *
 * Keeps the domain `WearableDataLayerRepository` interface free of the Play Services Wearable SDK
 * so the GMS-backed implementation maps the GMS node type to this at the boundary.
 */
data class WearNode(val id: String, val displayName: String)
