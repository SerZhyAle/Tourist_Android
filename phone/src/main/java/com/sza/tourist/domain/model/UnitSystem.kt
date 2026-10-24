package com.sza.tourist.domain.model

/**
 * Which measurement system the user reads values in.
 *
 * Named after the system rather than after the temperature on purpose: a future quantity is meant to
 * become another consumer of this value instead of getting a settings row of its own.
 */
enum class UnitSystem {
    METRIC,
    IMPERIAL,
    ;

    companion object {

        val DEFAULT: UnitSystem = METRIC

        /**
         * A stored value this build does not know resolves to the default rather than throwing, so a
         * settings file written by a newer build keeps working after a downgrade.
         */
        fun fromNameOrDefault(name: String?): UnitSystem =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
