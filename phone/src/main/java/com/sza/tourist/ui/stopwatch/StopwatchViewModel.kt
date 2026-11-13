package com.sza.tourist.ui.stopwatch

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sza.tourist.core.capability.CapabilityAvailability
import com.sza.tourist.core.util.ElapsedClock
import com.sza.tourist.domain.model.stopwatch.StopwatchScreenState
import com.sza.tourist.domain.model.stopwatch.StopwatchSettings
import com.sza.tourist.domain.usecase.stopwatch.ObserveStopwatchSettingsUseCase
import com.sza.tourist.ui.stopwatch.helpers.StopwatchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the measurement for every participant.
 *
 * The screen survives rotation because the state lives here and elapsed time is a function of the
 * stored marks and a fresh clock reading. [now] exists only to repaint - it is never the source of an
 * elapsed value, so a dropped or delayed tick costs a frame and never a millisecond of the measurement.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StopwatchViewModel @Inject constructor(
    private val clock: ElapsedClock,
    private val observeStopwatchSettings: ObserveStopwatchSettingsUseCase,
    mediaCapabilities: CapabilityAvailability,
) : ViewModel() {

    private val _state = MutableStateFlow(StopwatchScreenState())
    val state: StateFlow<StopwatchScreenState> = _state.asStateFlow()

    /**
     * Whether this build can play the accompaniment at all.
     */
    val audioAvailable: Boolean = mediaCapabilities.supportsAudio

    private val _trackUri = MutableStateFlow<Uri?>(null)

    /** The accompaniment's track, resolved from the tool's own settings and the build's capability. */
    val trackUri: StateFlow<Uri?> = _trackUri.asStateFlow()

    private val _volumeKeysDriveMeasurement = MutableStateFlow(true)

    /** The volume-key escape hatch, stored in the stopwatch's settings. */
    val volumeKeysDriveMeasurement: StateFlow<Boolean> = _volumeKeysDriveMeasurement.asStateFlow()

    init {
        viewModelScope.launch {
            var autostartPending = true
            observeStopwatchSettings().collect { settings ->
                applySettings(settings)
                // Wait for the saved region count; later settings edits must respect a user's stop.
                if (autostartPending) {
                    autostartPending = false
                    startAll()
                }
            }
        }
    }

    private fun applySettings(settings: StopwatchSettings) {
        setParticipantCount(settings.participantCount)
        _volumeKeysDriveMeasurement.value = settings.volumeKeysDriveMeasurement
        val wanted = settings.musicUri
        _trackUri.value = if (audioAvailable && settings.musicEnabled && wanted.isNotEmpty()) {
            wanted.toUri()
        } else {
            null
        }
    }

    /**
     * Repaint clock, cold on purpose. It emits once for a still screen and then ticks only while a
     * collector is attached and something is running, so a screen the user left stops repainting
     * instead of waking thirty times a second to redraw views nobody is looking at.
     */
    val now: Flow<Long> = state
        .map { it.anyRunning }
        .distinctUntilChanged()
        .flatMapLatest { running ->
            flow {
                emit(clock.nowMillis())
                while (running) {
                    delay(REPAINT_INTERVAL_MILLIS)
                    emit(clock.nowMillis())
                }
            }
        }

    /** The one command every input source maps onto: run it, or record a split if it already runs. */
    fun startOrLap(participantId: Int) {
        val nowMillis = clock.nowMillis()
        _state.value = if (_state.value.participants.getOrNull(participantId)?.running == true) {
            StopwatchEngine.lap(_state.value, participantId, nowMillis)
        } else {
            StopwatchEngine.start(_state.value, participantId, nowMillis, System.currentTimeMillis())
        }
    }

    fun stop(participantId: Int) {
        _state.value = StopwatchEngine.stop(_state.value, participantId, clock.nowMillis())
    }

    fun reset(participantId: Int) {
        _state.value = StopwatchEngine.reset(_state.value, participantId)
    }

    fun resetAll() {
        _state.value = StopwatchEngine.resetAll(_state.value)
    }

    /**
     * The wall-clock stamp is read beside the monotonic one here and nowhere else, so interval math
     * keeps reading only [ElapsedClock] while the result text gets its calendar date.
     */
    fun startAll() {
        _state.value = StopwatchEngine.startAll(_state.value, clock.nowMillis(), System.currentTimeMillis())
    }

    fun stopAll() {
        _state.value = StopwatchEngine.stopAll(_state.value, clock.nowMillis())
    }

    fun setParticipantCount(count: Int) {
        _state.value = StopwatchEngine.withParticipantCount(_state.value, count)
    }

    /** Current elapsed value for one participant, for a caller that needs it outside a repaint. */
    fun elapsedOf(participantId: Int): Long =
        _state.value.participants.getOrNull(participantId)?.elapsedAt(clock.nowMillis()) ?: 0L

    /**
     * One clock reading, for a caller that renders every participant at once outside a repaint.
     */
    fun nowMillis(): Long = clock.nowMillis()

    private companion object {
        /** About thirty repaints a second - enough for a hundredths reading not to look stepped. */
        const val REPAINT_INTERVAL_MILLIS = 33L
    }
}
