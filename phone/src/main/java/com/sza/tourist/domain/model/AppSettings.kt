package com.sza.tourist.domain.model

/**
 * The product's stored preferences, as far as the ported core reads them.
 *
 * This is the seam, not a copy of FastMediaSorter's sorter-wide settings record: it holds only what a
 * ported class reads today and grows with each phase that ports a reader of another preference. The
 * persisted implementation arrived with the phone shell (S0004): [unitSystem] and [preventSleep] in the
 * `settings` preferences file.
 */
data class AppSettings(
    val unitSystem: UnitSystem = UnitSystem.DEFAULT,
    val preventSleep: Boolean = true,
)
