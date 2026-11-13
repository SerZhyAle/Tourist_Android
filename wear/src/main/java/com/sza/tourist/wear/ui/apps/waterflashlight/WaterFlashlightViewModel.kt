package com.sza.tourist.wear.ui.apps.waterflashlight

import androidx.lifecycle.ViewModel
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Carries the one build answer the water flashlight needs: whether this build may hold the system shade
 * shut while the screen is lit (S2812).
 *
 * A plain value rather than a Flow, like the credential-entry answer beside it: the capability is decided
 * at build time and cannot change under an open screen.
 */
@HiltViewModel
class WaterFlashlightViewModel @Inject constructor(
    capabilities: WearRestrictedCapabilities
) : ViewModel() {

    val locksSystemShade: Boolean = capabilities.locksSystemShade
}
