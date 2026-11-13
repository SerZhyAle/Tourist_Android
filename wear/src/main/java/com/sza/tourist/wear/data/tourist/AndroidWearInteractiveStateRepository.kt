package com.sza.tourist.wear.data.tourist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.sza.tourist.wear.domain.repository.WearInteractiveStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * S0020: Android implementation of [WearInteractiveStateRepository].
 *
 * The lifecycle cannot answer this: on Wear OS a screen put to sleep goes to ambient with the Activity still
 * resumed (S0020 section 4.1). The broadcasts named for the screen are sent when the interactive state changes,
 * which covers ambient and screen off alike.
 */
@Singleton
class AndroidWearInteractiveStateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : WearInteractiveStateRepository {

    override fun observeInteractive(): Flow<Boolean> = callbackFlow {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        // Read on every broadcast rather than taken from its action: an "off" delivered after the watch woke
        // again must not stop anything. A watch without the service never stops the sensors on this signal.
        val readInteractive = { powerManager?.isInteractive ?: true }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                trySend(readInteractive())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        // Registered before the first read, so a change between the two is not missed.
        context.registerReceiver(receiver, filter)
        trySend(readInteractive())
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
