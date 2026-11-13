package com.sza.tourist.core.notification

import androidx.annotation.DrawableRes
import com.sza.tourist.R

/**
 * The single status-bar icon for every notification this app posts.
 *
 * One value removes the choice: a new notification cannot pick the wrong icon because it is not asked
 * to pick one. Deliberately one value and not a per-family map - families stay distinguishable by
 * their title, text and transport buttons. A family that ever needs its own icon becomes a named
 * exception here, next to the reason.
 */
object NotificationIcons {

    /**
     * Always this, on every notification. The drawable is alpha-only and carries no theme attribute,
     * which is what keeps a post from throwing outside the app theme. The system renders it as a
     * silhouette, so the white fill is the shape rather than the colour.
     */
    @DrawableRes
    val STATUS_BAR: Int = R.drawable.ic_notification
}
