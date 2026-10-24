package com.sza.tourist

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Wires DI and plants the logging tree. Debug builds log through Timber's DebugTree; release builds
 * plant nothing, so the ported classes' log calls compile out to no-ops.
 */
@HiltAndroidApp
class TouristApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
