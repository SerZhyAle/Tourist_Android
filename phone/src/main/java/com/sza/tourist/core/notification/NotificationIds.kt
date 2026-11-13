package com.sza.tourist.core.notification

/**
 * Single registry of every notification id this app posts.
 *
 * Android keys a notification by (package, tag, id) and none of these posters use a tag, so two
 * owners sharing an id silently replace each other's notification - and either one's teardown then
 * cancels it for both. Ids are therefore declared here instead of privately per class.
 *
 * This product's own registry, not a copy of the sorter's: it starts with the lights and grows with
 * each phase that posts (the SOS signal is next).
 */
object NotificationIds {

    /** The permanent shade entry that lights and extinguishes the camera flash on a tap. */
    const val FLASHLIGHT_SHORTCUT = 1

    /** The foreground notification while the distress signal is sounding or blinking. */
    const val SOS_SIGNAL = 2

    /** The heads-up offer when a watch SOS start arrives while the phone is in the background. */
    const val SOS_START_FROM_WATCH = 3
}
