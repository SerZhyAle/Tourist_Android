package com.sza.tourist.ui.flashlight

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sza.tourist.ui.common.widget.DialogCommandPolicy
import com.sza.tourist.util.showBoundToHost
import com.sza.tourist.R
import com.sza.tourist.core.format.QuantityFormatter
import com.sza.tourist.core.torch.TorchController
import com.sza.tourist.core.ui.BaseActivity
import com.sza.tourist.databinding.ActivityWaterFlashlightBinding
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.domain.model.Quantity
import com.sza.tourist.domain.unit.UnitSystemProvider
import com.sza.tourist.ui.flashlight.helpers.WaterFlashlightLockdownManager
import com.sza.tourist.utils.collectOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * The water flashlight: torch and window lit together behind a screen a wet finger cannot close.
 *
 * Its neighbour [FrontFlashlightActivity] is closed by a tap and dimmed by a drag. Here a touch must
 * reach nothing at all, which is why this is a second program rather than a mode of that one - the
 * two answer the same gesture in opposite ways. The "no wet touch can switch it off" rule is the
 * product's signature and is ported exactly.
 */
@AndroidEntryPoint
class WaterFlashlightActivity : BaseActivity<ActivityWaterFlashlightBinding>() {

    @Inject
    lateinit var torchController: TorchController

    @Inject
    lateinit var lockdown: WaterFlashlightLockdownManager

    @Inject
    lateinit var quantityFormatter: QuantityFormatter

    @Inject
    lateinit var unitSystemProvider: UnitSystemProvider

    private var armingDialog: AlertDialog? = null

    // Re-posted at each minute boundary rather than on a fixed tick, so the displayed minute changes
    // when it actually changes and the screen is not woken 60 times for one visible update.
    private val clockTick = Runnable { showTimeAndScheduleNext() }

    override fun getViewBinding(): ActivityWaterFlashlightBinding =
        ActivityWaterFlashlightBinding.inflate(layoutInflater)

    /**
     * The lamp may not go dark while it is the light in the water. The flag lives on this window, so
     * [BaseActivity] drops it with the window and the hold cannot outlive the program.
     */
    override fun keepScreenAwakeFor(settings: AppSettings): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockdown.restore(savedInstanceState)
        binding.root.visibility = View.INVISIBLE
        binding.tvLockedHint.visibility = View.INVISIBLE
        // Gesture, three-button and predictive BACK share the armed lamp's touch guard.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
    }

    override fun setupViews() {
        showTimeAndScheduleNext()
    }

    /**
     * The clock only redraws at a minute boundary, so a measurement system switched while the lamp is
     * on screen would otherwise keep the old clock length for up to a minute.
     */
    override fun observeData() {
        collectOnLifecycle(unitSystemProvider.current) { renderClock() }
    }

    /** The arming dialog remains usable if Android's pinning confirmation is dismissed. */
    override fun onResumeWithViews() {
        super.onResumeWithViews()
        if (isFinishing || isDestroyed) return
        if (!lockdown.isPinned(this)) showArmingDialog()
        lockdown.engage(this, ::onLocked)
    }

    private fun showArmingDialog() {
        if (armingDialog?.isShowing == true) return
        armingDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.water_flashlight_pin_title)
            .setMessage(R.string.water_flashlight_pin_message)
            .setPositiveButton(R.string.water_flashlight_start, null)
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .create().apply {
                setCanceledOnTouchOutside(false)
                setOnShowListener {
                    // Keep this dialog open under SystemUI so Cancel remains available after denial.
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        lockdown.retry(this@WaterFlashlightActivity, ::onLocked)
                    }
                    DialogCommandPolicy.apply(this)
                }
                showBoundToHost(this@WaterFlashlightActivity)
            }
    }

    private fun onLocked() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        armingDialog?.dismiss()
        armingDialog = null
        // Only this window's brightness, never the device setting - closing the program must leave the
        // phone exactly as bright as it was, the same rule the front flashlight follows.
        window.attributes = window.attributes.apply { screenBrightness = MAX_BRIGHTNESS }
        binding.root.visibility = View.VISIBLE
        binding.tvLockedHint.visibility = View.VISIBLE
        torchController.setTorch(this, true)
    }

    override fun onPause() {
        lockdown.pause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        lockdown.save(this, outState)
        super.onSaveInstanceState(outState)
    }

    /** Any loss of the foreground, including screen-off, puts the torch out. */
    override fun onStop() {
        torchController.setTorch(this, false)
        super.onStop()
    }

    override fun onDestroy() {
        armingDialog?.dismiss()
        armingDialog = null
        lockdown.destroy(this)
        binding.tvClock.removeCallbacks(clockTick)
        super.onDestroy()
    }

    /** Water on the glass fires touches of its own, so every one of them ends here and means nothing. */
    override fun onTouchEvent(event: MotionEvent): Boolean = true

    override fun onGenericMotionEvent(event: MotionEvent): Boolean = true

    /**
     * Taken at dispatch rather than in `onKeyDown`: the volume keys must leave the lamp rather than
     * change its volume. `KEYCODE_BACK` is absent on purpose - under gesture navigation it arrives
     * from a swipe across the glass and cannot be told apart from the touch this screen ignores. The
     * down event is swallowed too, so the exit press does not also move the volume.
     *
     * Android owns the pinning confirmation before the lamp is armed. Once confirmed, these keys
     * unpin the task before finishing so the previous screen and the launcher remain usable.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return true
        if (event.keyCode !in EXIT_KEYS) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP) {
            Timber.d("water flashlight left by hardware key %d", event.keyCode)
            lockdown.release(this)
            finish()
        }
        return true
    }

    /**
     * The app's measurement system decides the clock length, not the device's 12/24 switch: the lamp's
     * clock must read the same as every other time in the program.
     */
    private fun renderClock() {
        binding.tvClock.text = quantityFormatter.format(
            Quantity.Instant(System.currentTimeMillis()),
            unitSystemProvider.value,
        )
    }

    private fun showTimeAndScheduleNext() {
        renderClock()
        binding.tvClock.postDelayed(clockTick, millisUntilNextMinute())
    }

    private fun millisUntilNextMinute(): Long =
        MINUTE_MS - System.currentTimeMillis() % MINUTE_MS

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, WaterFlashlightActivity::class.java)

        private const val MAX_BRIGHTNESS = 1.0f
        private const val MINUTE_MS = 60_000L

        /**
         * Keys the platform delivers only from hardware. Power is not among them at all - the system
         * consumes it before any app sees it - so it ends the program the other way, by taking it off
         * the foreground.
         */
        private val EXIT_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        )
    }
}
