package com.sza.tourist.ui.flashlight

import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.sza.tourist.R
import com.sza.tourist.core.torch.TorchController
import com.sza.tourist.core.ui.BaseActivity
import com.sza.tourist.databinding.ActivityLightsBinding
import com.sza.tourist.ui.flashlight.helpers.FlashlightShortcutPermissionManager
import com.sza.tourist.ui.flashlight.helpers.FlashlightToggleManager
import com.sza.tourist.utils.applySystemBarInsetPadding
import com.sza.tourist.utils.collectOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * The lights program: the kit's three lights behind one start-screen row. The water and front
 * flashlights and the camera torch are programs of their own in the source; this screen is this
 * product's own seat for them, and it also hosts the shade-shortcut switch, which the source seats
 * on its operations settings tab (a screen this product does not have).
 */
@AndroidEntryPoint
class LightsActivity : BaseActivity<ActivityLightsBinding>() {

    @Inject
    lateinit var toggleManager: FlashlightToggleManager

    @Inject
    lateinit var torchController: TorchController

    private val viewModel: LightsViewModel by viewModels()

    private lateinit var shortcutPermission: FlashlightShortcutPermissionManager

    /** Flash units the framework reports lit; touched on the main looper only. */
    private val litTorches = mutableSetOf<String>()

    // A report already queued on the looper can arrive after onStop, and the binding is gone after onDestroy.
    private var torchSubscribed = false

    // The camera-flashlight row speaks the real torch state, not this screen's last tap: the quick-settings
    // tile and the shade shortcut switch the same flash. Registered only while the screen is started - the
    // balanced subscription TorchController asks of a subscriber whose state must be authoritative.
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (!torchSubscribed) return
            if (enabled) litTorches += cameraId else litTorches -= cameraId
            renderTorchState(litTorches.isNotEmpty())
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (!torchSubscribed) return
            litTorches -= cameraId
            renderTorchState(litTorches.isNotEmpty())
        }
    }

    private val cameraManager: CameraManager?
        get() = getSystemService(CameraManager::class.java)

    override fun getViewBinding(): ActivityLightsBinding =
        ActivityLightsBinding.inflate(layoutInflater)

    override fun setupViews() {
        // The bar must not sit under the status bar, where its back arrow and the help button could not be tapped
        // (the other toolbar screens do the same).
        binding.lightsRoot.applySystemBarInsetPadding()
        binding.toolbar.setUpNavigation(this)

        // Built from the binding's context, the way the source's row builds its collaborator on the
        // host screen: the launcher must register against this activity's result registry before the
        // switch can ask for the notification permission.
        shortcutPermission = FlashlightShortcutPermissionManager(
            binding.root.context,
            this,
            onNotificationPermissionGranted = { viewModel.setFlashlightShortcutEnabled(true) },
        )

        binding.rowWaterFlashlight.setOnClickListener {
            startActivity(WaterFlashlightActivity.createIntent(this))
        }
        binding.rowFrontFlashlight.setOnClickListener {
            startActivity(FrontFlashlightActivity.createIntent(this))
        }
        binding.rowCameraTorch.setOnClickListener {
            // Toggled here, not through FlashlightToggleActivity: that trampoline is for surfaces with no
            // screen of their own. Started from this row it covers the row with a transparent window for
            // ~200 ms, and a tap landing on it is lost - a burst of an even number of taps then left the
            // torch lit (S0019).
            val accepted = toggleManager.toggle()
            Timber.d("S0019: camera torch row toggled in place, accepted=%b", accepted)
            if (!accepted) {
                Toast.makeText(this, R.string.physical_flashlight_unavailable, Toast.LENGTH_SHORT).show()
            }
        }

        binding.rowShadeShortcut.setOnCheckedChangeListener { isChecked ->
            if (isChecked && !shortcutPermission.ensureGranted()) {
                // The request is now on screen; the row stays off until its grant callback writes the
                // setting, so a denial leaves the switch honest instead of on and producing nothing.
                binding.rowShadeShortcut.setCheckedSilently(false)
                return@setOnCheckedChangeListener
            }
            viewModel.setFlashlightShortcutEnabled(isChecked)
        }
    }

    override fun onStart() {
        super.onStart()
        // The tracker's belief until the framework reports every flash unit's mode on registration.
        litTorches.clear()
        renderTorchState(torchController.isTorchOn)
        torchSubscribed = true
        cameraManager?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
    }

    override fun onStop() {
        torchSubscribed = false
        cameraManager?.unregisterTorchCallback(torchCallback)
        super.onStop()
    }

    /** State shown by more than colour (S0013 budget 8.5e): TalkBack reads it after the row's label. */
    private fun renderTorchState(isOn: Boolean) {
        ViewCompat.setStateDescription(
            binding.rowCameraTorch,
            getString(if (isOn) R.string.physical_flashlight_state_on else R.string.physical_flashlight_state_off),
        )
    }

    override fun onResumeWithViews() {
        // Hidden where the device has no flash unit, so the row never offers what the hardware cannot
        // do. Re-checked on every resume: a peripheral flash can appear and disappear.
        binding.rowShadeShortcut.isVisible = shortcutPermission.isFlashUnitAvailable
    }

    override fun observeData() {
        collectOnLifecycle(viewModel.settings) { settings ->
            if (binding.rowShadeShortcut.isChecked != settings.flashlightShortcutNotificationEnabled) {
                binding.rowShadeShortcut.setCheckedSilently(settings.flashlightShortcutNotificationEnabled)
            }
        }
    }
}
