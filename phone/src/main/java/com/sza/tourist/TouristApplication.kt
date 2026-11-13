package com.sza.tourist

import android.app.Application
import com.sza.tourist.core.notification.FlashlightShortcutCoordinator
import com.sza.tourist.data.wear.WearUnitSystemSyncCoordinator
import com.sza.tourist.help.HelpInstaller
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Wires DI and plants the logging tree. Debug builds log through Timber's DebugTree; release builds
 * plant nothing, so the ported classes' log calls compile out to no-ops.
 */
@HiltAndroidApp
class TouristApplication : Application() {

    @Inject
    lateinit var flashlightShortcutCoordinator: FlashlightShortcutCoordinator

    @Inject
    lateinit var wearUnitSystemSyncCoordinator: WearUnitSystemSyncCoordinator

    override fun onCreate() {
        super.onCreate()
        com.sza.tourist.ui.settings.AppearanceSettings.applyStoredTheme(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // The shade shortcut for the camera flashlight follows one setting, and this is where its
        // collector starts. Idempotent, and the collector itself reads the store off the main thread.
        flashlightShortcutCoordinator.start()
        // The paired watch reads its measurement system from the phone (S0008); this collector pushes it
        // at start and on every change. Idempotent.
        wearUnitSystemSyncCoordinator.start()
        // F1 for every screen and dialog, from the one help map (S0014); no screen carries help code itself.
        HelpInstaller.install(this)
    }
}
