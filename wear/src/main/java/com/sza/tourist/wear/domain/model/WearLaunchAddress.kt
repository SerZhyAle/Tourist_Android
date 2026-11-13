package com.sza.tourist.wear.domain.model

/**
 * S2751: what an outside caller asked to open, described without naming a navigation address.
 *
 * The resolver answers with one of these; turning the answer into a route is the navigation branch's job.
 * Keeping the two apart is what stops the domain from importing the screens: a resolver that returned a
 * route string would have to know the route table, and that table is presentation (strategic ADR-2).
 *
 * A target that no longer resolves is a null answer rather than a member here - "the thing you pinned is
 * gone" and "here is a screen" are different answers and the caller has to be able to tell them apart.
 *
 * Trimmed to the one member this product uses: the tile, source and playback addresses of FastMediaSorter
 * belong to surfaces this product does not carry.
 */
sealed interface WearLaunchAddress {

    /** A fixed screen, named by the destination the program list and the shortcuts already address. */
    data class Screen(val destination: WearDestinationId) : WearLaunchAddress
}
