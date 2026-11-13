package com.sza.tourist.wear.domain.usecase

import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import com.sza.tourist.wear.domain.model.WearDestinationId
import com.sza.tourist.wear.domain.model.WearLaunchAddress
import com.sza.tourist.wear.domain.model.WearLaunchTarget
import javax.inject.Inject

/**
 * S1955: turns what an outside caller asked for into the address that opens it.
 *
 * A target that no longer resolves returns null rather than a fallback, because "the thing you pinned is
 * gone" and "here is the home screen" are different answers and the caller has to be able to tell them
 * apart (strategic §5.2).
 *
 * S2751: the answer is a [WearLaunchAddress] rather than a navigation route. The route table is
 * presentation, and reaching for it from here was the inverted dependency this ticket removed; the
 * navigation branch maps the address it gets back.
 *
 * Trimmed to the one target this product has: FastMediaSorter's tile, resource, stream and file targets
 * belong to surfaces this product does not carry.
 */
class ResolveWearLaunchAddressUseCase @Inject constructor(
    private val capabilities: WearRestrictedCapabilities,
) {

    operator fun invoke(target: WearLaunchTarget): WearLaunchAddress? = when (target) {
        is WearLaunchTarget.Destination -> screenFor(target.id)
    }

    /**
     * S2511 / S2995: resolves static destination addresses. A restricted capability returns null in
     * the builds where it is disabled.
     *
     * S3178: this is the narrowest place the whole store boundary can be enforced for an OUTSIDE caller.
     * A launch intent from the paired phone, the stopwatch notification and another application's
     * shortcut all arrive here carrying a destination name, none of them having passed through a screen
     * this build drew - so a destination filtered out of the program list is still nameable unless it is
     * refused here too. Null is the existing "the thing you pinned is gone" answer and is the right one.
     */
    private fun screenFor(id: WearDestinationId): WearLaunchAddress.Screen? =
        if (offers(id)) WearLaunchAddress.Screen(id) else null

    /**
     * S3178: whether this build offers the destination, by the same categories as the capability
     * interface. Exhaustive rather than `else -> true`: a destination added later must be classified
     * deliberately, and an unclassified one reaching the store artifact is exactly the silent widening
     * the boundary exists to prevent.
     */
    private fun offers(id: WearDestinationId): Boolean = when (id) {
        // S4147: the dashboard reports activity and heart rate, which the store policy files under health.
        WearDestinationId.TOURIST -> capabilities.offersHealthFeatures

        // S3362: the two screens a swipe cannot leave. WO-V3 is about the gesture, not the manifest.
        WearDestinationId.WATER_FLASHLIGHT,
        WearDestinationId.SOS -> capabilities.offersScreenTakeoverPrograms

        // The store-safe surface: no permission, no user content, no device data.
        WearDestinationId.STOPWATCH,
        WearDestinationId.HOME -> true
    }
}
