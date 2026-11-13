package com.sza.tourist.wear.ui.apps

import com.sza.tourist.wear.domain.model.WearApp
import com.sza.tourist.wear.domain.model.WearViewMode

/** What the Apps screen draws: the already-filtered, already-ordered programs and nothing else. */
data class AppsUiState(
    val apps: List<WearApp> = emptyList(),
    val viewMode: WearViewMode = WearViewMode.LIST
)
