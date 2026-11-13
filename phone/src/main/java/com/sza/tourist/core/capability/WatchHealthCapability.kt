package com.sza.tourist.core.capability

/**
 * Whether the Tourist dashboard may show the paired watch's heart rate and body temperature (S4150).
 *
 * FastMediaSorter answers this from a multibound set of capability ids that only its `noLegal` flavor
 * contributes to. This product follows its own variant seam instead (handoff section 3): the interface
 * lives in `src/main` and each variant source set carries a real implementation bound by that variant's
 * DI module - there is deliberately no default here, which would diverge silently from the variant that
 * overrides it.
 *
 * The composition is owner decision 4.3 (2026-10-25, S0008): the store watch carries no Tourist
 * dashboard, so it publishes no health report and the phone does not offer the tiles; the noLegal watch
 * does publish, and the phone offers them.
 */
interface WatchHealthCapability {

    /** True when the two watch tiles of the Tourist dashboard are offered by this build. */
    val isOffered: Boolean
}
