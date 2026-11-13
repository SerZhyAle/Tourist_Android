package com.sza.tourist.data.wear

import com.sza.tourist.core.di.ApplicationScope
import com.sza.tourist.domain.model.UnitSystem
import com.sza.tourist.domain.model.WearSettingsPayload
import com.sza.tourist.domain.usecase.ObserveUnitSystemUseCase
import com.sza.tourist.domain.usecase.PushWearSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the watch's measurement system equal to the phone's (S2731), with no screen to press a button on.
 *
 * FastMediaSorter pushes its settings from a "send" action of the Wear companion screen
 * (`WearSyncViewModel.pushSettings`); this product has no such screen and its only mirrored setting is the
 * unit system, so one collector stands in for the button. It pushes when the app starts (the first
 * emission) and on every change of the setting, through the ported [PushWearSettingsUseCase].
 *
 * The push is a Data Item, which the Data Layer delivers to a watch that connects later, so a push made
 * while no watch is reachable is not lost - but the use case refuses to send when no node is connected,
 * as in the source, so such a push is simply retried at the next start or change.
 *
 * New in this product: there is no FastMediaSorter file behind this class, hence no ledger row.
 */
@Singleton
class WearUnitSystemSyncCoordinator @Inject constructor(
    private val observeUnitSystem: ObserveUnitSystemUseCase,
    private val pushWearSettings: PushWearSettingsUseCase,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    private val started = AtomicBoolean(false)

    /** Idempotent: a second call must not add a second collector. */
    fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }
        scope.launch {
            observeUnitSystem().collect { system -> push(system) }
        }
    }

    private suspend fun push(system: UnitSystem) {
        pushWearSettings(WearSettingsPayload(unitSystem = system.name)).fold(
            onSuccess = { Timber.d("Unit system %s pushed to the watch", system) },
            onFailure = { Timber.i("Unit system %s not pushed: %s", system, it.message) },
        )
    }
}
