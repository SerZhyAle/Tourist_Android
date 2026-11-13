package com.sza.tourist.wear.ui.apps

import androidx.lifecycle.ViewModel
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import com.sza.tourist.wear.domain.catalog.WearAppCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Hands the program list the Apps screen renders.
 *
 * The program set itself is fixed at build time, so nothing changes under the screen while it is open.
 * FastMediaSorter's version also observed a view-mode preference; this product has no writer for it
 * (the settings push carries the unit system only), so the list keeps the single-column default of
 * [AppsUiState] and the view-mode preference is not carried.
 */
@HiltViewModel
class AppsViewModel @Inject constructor(
    capabilities: WearRestrictedCapabilities
) : ViewModel() {

    private val state = MutableStateFlow(AppsUiState(apps = WearAppCatalog.apps(capabilities)))

    val uiState: StateFlow<AppsUiState> = state.asStateFlow()
}
