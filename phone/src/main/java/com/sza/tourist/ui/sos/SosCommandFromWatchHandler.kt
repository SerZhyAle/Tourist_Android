package com.sza.tourist.ui.sos

import android.content.Context
import android.content.Intent
import com.sza.tourist.domain.model.sos.SosMode
import com.sza.tourist.domain.usecase.sos.IsSosProgramEnabledUseCase
import com.sza.tourist.service.WearDataLayerPaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * S3216: turns the watch's two SOS commands into a running, or stopped, signal on this phone.
 *
 * A collaborator of its own rather than two branches inside the Data Layer listener, matching the source
 * architecture: starting a siren is a decision with a precondition rather than a bare dispatch.
 */
@Singleton
class SosCommandFromWatchHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isSosProgramEnabled: IsSosProgramEnabledUseCase,
    private val startFromWatchNotifier: SosStartFromWatchNotifier,
) {

    /**
     * Answers whether [path] is one of the two SOS routes, acting on it when it is.
     */
    suspend fun handle(path: String, payload: ByteArray): Boolean = run {
        handleRoute(path, payload)
    }

    private suspend fun handleRoute(path: String, payload: ByteArray): Boolean = when (path) {
        WearDataLayerPaths.SOS_START_FROM_WATCH -> {
            handleStart(payload)
            true
        }

        WearDataLayerPaths.SOS_STOP_FROM_WATCH -> {
            handleStop()
            true
        }

        else -> false
    }

    private suspend fun handleStart(payload: ByteArray) {
        if (!isSosProgramEnabled()) {
            Timber.i("SOS: a start arrived from the watch but the phone program is switched off")
            return
        }
        val mode = SosMode.fromNameOrDefault(payload.decodeToString().takeIf { it.isNotBlank() })
        Timber.i("SOS: raising the phone signal in mode %s at the watch's request", mode)
        try {
            SosService.start(context, mode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            // S3908: from API 31 a background process may not start a foreground service, and this
            // runs on a scope with no handler, so the throw would kill the process. The caught type is
            // the supertype of ForegroundServiceStartNotAllowedException, which exists only from 31.
            // The notification's tap is the exemption: the window it opens starts the service itself.
            Timber.w(e, "SOS: the system refused the background signal start, offering it as a notification")
            startFromWatchNotifier.show(mode)
        }
        // The window as well as the service: half the signal is the flashing screen, and the owner
        // needs the stop control in front of him rather than only in the notification shade.
        context.startActivity(
            SosActivity.createIntent(context, mode).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Unconditional, unlike [handleStart]: the switch decides whether the phone may START signalling,
     * never whether it may stop. A signal running because the switch was on a moment ago must still be
     * stoppable from the watch.
     */
    private fun handleStop() {
        Timber.i("SOS: dropping the phone signal at the watch's request")
        SosService.stop(context)
    }
}
