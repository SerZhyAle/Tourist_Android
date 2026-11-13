package com.sza.tourist.domain.usecase.sos

import javax.inject.Inject

/**
 * Whether the phone's distress signal is a program on this device at all.
 *
 * In FastMediaSorter this reads the `enable_sos` preference (`AppSettings.enableSos`), because SOS is an
 * optional sub-program inside a media sorter. In this standalone kit SOS is always offered on the start
 * screen and the dashboard (just as `enable_tourist` and the light program switches were dropped in
 * S0004-S0005), so the use case stays in place for [com.sza.tourist.ui.sos.SosCommandFromWatchHandler] and
 * always answers `true`.
 */
class IsSosProgramEnabledUseCase @Inject constructor() {

    operator fun invoke(): Boolean = true
}
