package com.sza.tourist.wear.domain.repository

import com.sza.tourist.wear.domain.repository.preferences.WearAppearancePreferences
import com.sza.tourist.wear.domain.repository.preferences.WearMiniAppPreferences

/**
 * Every Wear OS application preference, as one type.
 *
 * S2655: declares nothing of its own - the members live in the themed interfaces it extends, each of
 * which is counted separately by detekt's `TooManyFunctions`.
 *
 * Trimmed to the two themes the kit's screens read; FastMediaSorter's media-type, playback, browse,
 * streams, document and settings-sync themes are not part of this product.
 */
interface WearPreferencesRepository :
    WearAppearancePreferences,
    WearMiniAppPreferences
