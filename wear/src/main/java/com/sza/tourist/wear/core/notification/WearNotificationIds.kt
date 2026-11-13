package com.sza.tourist.wear.core.notification

/**
 * S1961: every notification this module can post, in one place.
 *
 * Android keys a notification by (package, id) when no tag is given, so two features picking the same
 * number means the later one replaces the earlier and cancelling either clears both. A registry is the
 * only way to see the collision before a user does.
 *
 * This product's own registry, not a copy of FastMediaSorter's: the watch installs its own APK, so the
 * numbering is private to it, and it carries only the notifications the kit posts.
 */
object WearNotificationIds {

    /** S3529: stopwatch ongoing activity notification for Wear OS compliance. */
    const val STOPWATCH_ONGOING = 4201
}
