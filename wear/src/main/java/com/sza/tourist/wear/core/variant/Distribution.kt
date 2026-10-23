package com.sza.tourist.wear.core.variant

/**
 * The distribution edition a build belongs to. The two editions share one application id and one
 * signature, so they update each other and the Data Layer pairs a phone with a watch across them.
 *
 * The wear module keeps its own copy of the seam: the product has no shared code module between phone
 * and watch (handoff section 8 lists exactly three modules), and the interface is two declarations.
 */
enum class Distribution {
    STORE,
    NO_LEGAL,
}

/**
 * The skeleton seam of the variant mechanism (handoff section 3): the interface lives in `src/main`,
 * each variant's implementation in its own source set, one DI module per variant binding it. What the
 * variants hide is owner decision 4.3 (S0008); nothing is hidden yet, and no default implementation
 * exists in `src/main` - a default would diverge silently from the variant that overrides it.
 */
interface DistributionInfo {
    val distribution: Distribution
}
