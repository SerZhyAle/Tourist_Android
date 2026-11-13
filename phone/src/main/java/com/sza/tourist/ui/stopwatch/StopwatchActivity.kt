package com.sza.tourist.ui.stopwatch

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.google.android.material.slider.Slider
import com.sza.tourist.R
import com.sza.tourist.core.ui.BaseActivity
import com.sza.tourist.databinding.ActivityStopwatchBinding
import com.sza.tourist.ui.stopwatch.helpers.MediaPlayerStopwatchAudioOutput
import com.sza.tourist.ui.stopwatch.helpers.StopwatchAudioManager
import com.sza.tourist.ui.stopwatch.helpers.StopwatchCommand
import com.sza.tourist.ui.stopwatch.helpers.StopwatchInputManager
import com.sza.tourist.ui.stopwatch.helpers.StopwatchRegionBinder
import com.sza.tourist.utils.applySystemBarInsetPadding
import com.sza.tourist.utils.collectOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The stopwatch screen.
 *
 * It holds view wiring only: the measurement lives in [StopwatchViewModel] so it survives rotation and
 * backgrounding, the drawing lives in [StopwatchRegionBinder] so the participant count decides the
 * layout and the focus order in one place, and the key-to-command translation lives in
 * [StopwatchInputManager].
 *
 * This is the only screen in the application that consumes a volume key, which is why the escape hatch
 * and the on-screen volume control are part of the same class as the interception.
 */
@AndroidEntryPoint
class StopwatchActivity : BaseActivity<ActivityStopwatchBinding>() {

    private val viewModel: StopwatchViewModel by viewModels()

    private lateinit var regionBinder: StopwatchRegionBinder

    private val inputManager = StopwatchInputManager(
        participantCount = { viewModel.state.value.participantCount },
    )

    private var volumeSliderListener: Slider.OnChangeListener? = null

    /**
     * The accompaniment. Created lazily rather than in a field initialiser because it reaches the
     * platform through this Activity's context, and released in [onDestroy].
     */
    private val audioManager: StopwatchAudioManager by lazy {
        StopwatchAudioManager(MediaPlayerStopwatchAudioOutput(this))
    }

    /** The volume-key escape hatch, read from the tool's own setting rather than assumed. */
    private val volumeKeysDriveMeasurement: Boolean
        get() = viewModel.volumeKeysDriveMeasurement.value

    override fun getViewBinding(): ActivityStopwatchBinding =
        ActivityStopwatchBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.root.applySystemBarInsetPadding()
        binding.toolbar.setUpNavigation(this)
        regionBinder = StopwatchRegionBinder(
            binding = binding,
            onStartOrLap = viewModel::startOrLap,
            onStopOrReset = ::stopOrReset,
        )
        regionBinder.attachListeners()
        binding.btnStopwatchResetAll.setOnClickListener { viewModel.resetAll() }
        binding.btnStopwatchStartAll.setOnClickListener {
            if (viewModel.state.value.anyRunning) viewModel.stopAll() else viewModel.startAll()
        }
        binding.btnStopwatchResult.setOnClickListener { openResultDialog() }
        setupVolumeSlider()
        binding.btnStopwatchSettings.setOnClickListener {
            StopwatchSettingsDialogFragment.newInstance()
                .show(supportFragmentManager, StopwatchSettingsDialogFragment.TAG)
        }
    }

    override fun observeData() {
        collectOnLifecycle(
            combine(viewModel.state, viewModel.now) { state, nowMillis -> state to nowMillis },
        ) { (state, nowMillis) ->
            regionBinder.bind(state, nowMillis)
        }
        // The accompaniment is driven by the state rather than by the button handlers, so a run started
        // by a volume key or a keyboard sounds the same as one started by a tap, and four participants
        // running together still produce one player.
        collectOnLifecycle(
            viewModel.state.map { it.anyRunning }.distinctUntilChanged(),
        ) { anyRunning ->
            audioManager.syncWith(anyRunning)
        }
        collectOnLifecycle(viewModel.trackUri) { uri -> audioManager.selectTrack(uri) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val command = inputManager.handleKeyEvent(event, addressedRegion())
        return when {
            command != null -> consume(command)
            handleVolumeKeyDown(event) -> true
            // Consuming only the ACTION_DOWN half of a volume key would leave the system to act on
            // the unmatched ACTION_UP and change the volume anyway, so the release is swallowed too.
            isInterceptedVolumeRelease(event) -> true
            else -> super.dispatchKeyEvent(event)
        }
    }

    /**
     * Volume up starts or laps, volume down stops. With the escape hatch off the system gets the
     * keys back.
     */
    private fun handleVolumeKeyDown(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val participantId = volumeKeyRegion()
        if (!volumeKeysDriveMeasurement || participantId == StopwatchRegionBinder.NO_REGION) {
            return false
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> consume(StopwatchCommand.StartOrLap(participantId))
            KeyEvent.KEYCODE_VOLUME_DOWN -> consume(StopwatchCommand.Stop(participantId))
            else -> false
        }
    }

    override fun onStop() {
        // The accompaniment belongs to this screen. The measurement keeps running on its own
        // timestamps, but music from a screen the user has left carries no visible control to silence
        // it and no notification. Coming back re-emits the running state and the collector starts it again.
        audioManager.syncWith(anyRunning = false)
        super.onStop()
    }

    override fun onDestroy() {
        volumeSliderListener?.let { binding.sliderStopwatchVolume.removeOnChangeListener(it) }
        volumeSliderListener = null
        audioManager.release()
        super.onDestroy()
    }

    /**
     * Gives the volume back on screen, because the hardware keys no longer change it.
     */
    private fun setupVolumeSlider() {
        val maxVolume = audioManager.maxStreamVolume.toFloat()
        val available = viewModel.audioAvailable && maxVolume > 0f
        binding.stopwatchVolumeRow.isVisible = available
        if (!available) return

        val slider = binding.sliderStopwatchVolume
        slider.valueFrom = 0f
        slider.valueTo = maxVolume
        slider.value = audioManager.streamVolume.toFloat().coerceIn(0f, maxVolume)
        val listener = Slider.OnChangeListener { _, value, fromUser ->
            if (fromUser) {
                audioManager.streamVolume = value.toInt()
            }
        }
        slider.addOnChangeListener(listener)
        volumeSliderListener = listener
    }

    /** Applies [command] and reports the event as consumed, so no other handler sees it. */
    private fun consume(command: StopwatchCommand): Boolean {
        when (command) {
            is StopwatchCommand.StartOrLap -> viewModel.startOrLap(command.participantId)
            is StopwatchCommand.Stop -> viewModel.stop(command.participantId)
            is StopwatchCommand.Reset -> viewModel.reset(command.participantId)
            is StopwatchCommand.FocusRegion -> regionBinder.focusParticipant(command.participantId)
        }
        return true
    }

    /**
     * The other half of an intercepted press. It repeats the addressability test rather than trusting
     * that the press was taken, so a release whose press went to the system is not swallowed here.
     */
    private fun isInterceptedVolumeRelease(event: KeyEvent): Boolean =
        volumeKeysDriveMeasurement &&
            event.keyCode in VOLUME_KEYS &&
            event.action != KeyEvent.ACTION_DOWN &&
            volumeKeyRegion() != StopwatchRegionBinder.NO_REGION

    /**
     * The region a region-less centre/enter key belongs to. The volume vocabulary resolves through
     * [volumeKeyRegion] instead.
     *
     * Key dispatch can outrun [setupViews] on a cold start, so the binder is asked only once it exists.
     */
    private fun addressedRegion(): Int {
        if (!::regionBinder.isInitialized || !binding.stopwatchContentGroup.isVisible) {
            return StopwatchRegionBinder.NO_REGION
        }
        val participantId = regionBinder.focusedParticipantId()
        return participantId.takeIf { it in 0 until viewModel.state.value.participantCount }
            ?: StopwatchRegionBinder.NO_REGION
    }

    /**
     * The region a volume key drives: the resolution carries a touch-mode default instead of requiring
     * focus.
     */
    private fun volumeKeyRegion(): Int {
        if (!::regionBinder.isInitialized || !binding.stopwatchContentGroup.isVisible) {
            return StopwatchRegionBinder.NO_REGION
        }
        val participantId = regionBinder.volumeKeyParticipantId()
        return participantId.takeIf { it in 0 until viewModel.state.value.participantCount }
            ?: StopwatchRegionBinder.NO_REGION
    }

    /**
     * Opens the export surface, or says why there is nothing to export.
     */
    private fun openResultDialog() {
        if (viewModel.state.value.visibleParticipants.none { it.hasProgress }) {
            Toast.makeText(this, R.string.stopwatch_result_empty, Toast.LENGTH_SHORT).show()
            return
        }
        StopwatchResultDialogFragment.newInstance()
            .show(supportFragmentManager, StopwatchResultDialogFragment.TAG)
    }

    /** One secondary button for two jobs: it halts a running measurement, and clears a halted one. */
    private fun stopOrReset(participantId: Int) {
        val participant = viewModel.state.value.participants.getOrNull(participantId)
        if (participant?.running == true) {
            viewModel.stop(participantId)
        } else {
            viewModel.reset(participantId)
        }
    }

    companion object {

        private val VOLUME_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
        )

        fun createIntent(context: Context): Intent = Intent(context, StopwatchActivity::class.java)
    }
}
