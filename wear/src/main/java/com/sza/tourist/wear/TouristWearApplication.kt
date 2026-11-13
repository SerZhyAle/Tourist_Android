package com.sza.tourist.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * The watch application: it wires DI and plants the debug log tree.
 *
 * Trimmed from FastMediaSorter's application class: the log buffer behind "send logs", the voice-note
 * drain, the exchange runtime, the locale restore, the power-state and heart-rate session observers and
 * the foreground-window holder belong to surfaces this product does not carry.
 */
@HiltAndroidApp
class TouristWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Debug builds log everything; release builds plant nothing, so no log line leaves the process.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Tourist Wear OS app started")
    }
}
