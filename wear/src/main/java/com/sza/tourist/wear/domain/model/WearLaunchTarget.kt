package com.sza.tourist.wear.domain.model

import android.content.Intent

/**
 * S1955: what an outside caller can ask the watch app to open.
 *
 * The module's only exported Activity used to ignore its launch intent completely and always open the home
 * screen, so "open this program" had nowhere to be said. This is that contract. The ongoing-activity
 * notification of the stopwatch and the phone's SOS start both enter through it.
 *
 * Both ends of the wire format live in this file for the reason written at the top of `WearRoutes.kt` - a
 * writer and a reader kept apart are two independent string literals, and a mismatch between them is a
 * silent no-op that nothing in this module can catch.
 *
 * Trimmed to the one member this product uses: FastMediaSorter's tile, file and picker targets belong to
 * surfaces (tiles, resources, file hand-over) this product does not carry.
 */
sealed interface WearLaunchTarget {

    /**
     * S2511: open a named destination of the app - the home screen or a program.
     *
     * A shortcut pins nothing: there is no stored assignment to look up and no target that can go missing,
     * so the id alone is the whole address.
     */
    data class Destination(val id: WearDestinationId) : WearLaunchTarget
}

/**
 * S2511: one extra of the wire format, carrying its own type.
 *
 * An `Intent` erases the type of what was put into it, and the only way back is a deprecated untyped read
 * that answers `Any?`. Naming the type here makes every consumer read the same declared shape instead of
 * guessing at one.
 */
sealed interface WearLaunchExtra {
    data class Text(val value: String) : WearLaunchExtra
    data class Number(val value: Int) : WearLaunchExtra
}

/**
 * The full wire form of [this], in the shape [readWearLaunchTarget] reads back.
 *
 * A null-valued field is omitted rather than written as a null, because both readers ask for it with a
 * getter that answers null for "absent" anyway - so the two spellings are the same answer.
 */
fun WearLaunchTarget.extras(): Map<String, WearLaunchExtra> = buildMap {
    when (this@extras) {
        is WearLaunchTarget.Destination -> {
            put(EXTRA_MODE, WearLaunchExtra.Text(MODE_DESTINATION))
            put(EXTRA_DESTINATION, WearLaunchExtra.Text(id.name))
        }
    }
}

/** Writes [this] into [intent] in the shape [readWearLaunchTarget] reads back. */
fun WearLaunchTarget.writeTo(intent: Intent) {
    extras().forEach { (key, extra) ->
        when (extra) {
            is WearLaunchExtra.Text -> intent.putExtra(key, extra.value)
            is WearLaunchExtra.Number -> intent.putExtra(key, extra.value)
        }
    }
}

/**
 * Reads back what [writeTo] wrote, or null.
 *
 * Null covers three cases that must not be told apart by the caller: no extras at all, an unreadable value,
 * and a partially written set. A half-filled target would resolve to some address rather than to none, and
 * landing on the wrong screen is worse than landing on the usual one.
 */
fun readWearLaunchTarget(intent: Intent): WearLaunchTarget? {
    val mode = intent.getStringExtra(EXTRA_MODE)
    return when (mode) {
        MODE_DESTINATION -> intent.readDestinationTarget()
        else -> null
    }
}

/** An unrecognised name is a target this build does not have, which is the same answer as none at all. */
private fun Intent.readDestinationTarget(): WearLaunchTarget.Destination? =
    getStringExtra(EXTRA_DESTINATION)
        ?.let { name -> WearDestinationId.entries.firstOrNull { it.name == name } }
        ?.let(WearLaunchTarget::Destination)

private const val PREFIX = "com.sza.tourist.wear.launch."

private const val EXTRA_MODE = PREFIX + "mode"
private const val EXTRA_DESTINATION = PREFIX + "destination"

private const val MODE_DESTINATION = "destination"
