package com.sza.tourist.core.ui

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.sza.tourist.BuildConfig
import com.sza.tourist.domain.model.AppSettings
import com.sza.tourist.utils.collectOnLifecycle
import timber.log.Timber
import javax.inject.Inject

/**
 * Base Activity carrying what every kit screen shares: the ViewBinding lifecycle, edge-to-edge, and
 * the keep-screen-awake flag driven from persisted settings.
 *
 * Ported from FastMediaSorter's BaseActivity as this product's own seam, cut to what the kit's screens
 * use: the locale overlay, the secure flag, the GMS warning, the background-operation bar, the mouse
 * and gamepad-stick dispatch and the help surfaces stay behind with their sorter-wide surfaces. D-pad,
 * keyboard and TV-remote reachability rides on Android's native focus traversal over focusable views.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    // Field-injected: the base class cannot take constructor parameters, and a host must not reference
    // the data layer directly - the manager holds that reference instead.
    @Inject
    lateinit var keepScreenAwakeManager: KeepScreenAwakeManager

    private var _binding: VB? = null
    protected val binding: VB
        get() = checkNotNull(_binding) { "Binding is only valid between onCreate and onDestroy" }

    abstract fun getViewBinding(): VB
    abstract fun setupViews()
    abstract fun observeData()

    // True once setupViews()/observeData() finished. Required because BaseActivity defers
    // setupViews() to binding.root.post { } so the first frame renders fast - meaning
    // Activity.onResume() can fire BEFORE lateinit managers from setupViews() are initialised.
    private var viewsReady = false
    private var resumePending = false

    // Actions that arrived before setupViews() finished - typically an activity result that the
    // ActivityResultRegistry replays at ON_START of a recreated Activity, before the deferred post.
    private val pendingViewsReadyActions = ArrayDeque<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Content draws behind system bars; each Activity applies its own WindowInsets padding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Timber.d("onCreate: ${this::class.simpleName}")

        _binding = getViewBinding()
        setContentView(binding.root)

        // Apply keep screen awake from the cached decision, then keep it in sync with settings.
        applyKeepScreenAwake()
        collectOnLifecycle(keepScreenAwakeManager.settings) { settings ->
            keepScreenAwakeDecision = keepScreenAwakeFor(settings)
            applyKeepScreenAwake()
        }

        // Defer heavy initialization to allow first frame to render quickly
        val onCreateT0 = if (BuildConfig.DEBUG) SystemClock.uptimeMillis() else 0L
        binding.root.post {
            // The post can outlive the Activity when it is destroyed within the first frame.
            // onDestroy() has already nulled _binding by then, so bail out before setupViews() touches it.
            if (_binding == null || isDestroyed) return@post
            if (BuildConfig.DEBUG) {
                val waitMs = SystemClock.uptimeMillis() - onCreateT0
                Timber.d(
                    "BaseActivity.setupViews[${this::class.simpleName}]: START (waited ${waitMs}ms for first frame)"
                )
            }
            setupViews()
            observeData()
            viewsReady = true
            while (pendingViewsReadyActions.isNotEmpty()) {
                pendingViewsReadyActions.removeFirst().invoke()
            }
            if (resumePending) {
                resumePending = false
                onResumeWithViews()
            }
            // Initial focus on any non-touch input device, so D-pad/keyboard focus never parks nowhere.
            if (isNonTouchInputActive()) {
                (getInitialFocusView() ?: _binding?.root)?.requestFocus()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reapply in case the flag was cleared by the system while stopped.
        applyKeepScreenAwake()
        if (viewsReady) {
            onResumeWithViews()
        } else {
            // setupViews() not run yet - defer until the post{} block finishes.
            resumePending = true
        }
    }

    override fun onPause() {
        super.onPause()
        // Drop a pending resume if the Activity was paused before setupViews() finished:
        // onResumeWithViews() must not fire after the first onPause.
        resumePending = false
    }

    /**
     * Lifecycle hook for resume work that depends on lateinit fields initialised in setupViews().
     * Subclasses should override this instead of onResume() when they need access to those fields.
     * Guaranteed to fire only after setupViews()/observeData() have completed.
     */
    protected open fun onResumeWithViews() {}

    /**
     * Runs [action] now when setupViews()/observeData() have completed, otherwise right after they do.
     * Wrap an activity-result callback in it when the callback touches a lateinit built in setupViews():
     * a result pending across a recreation is dispatched at ON_START, before the deferred setup, and the
     * user's pick must be replayed rather than crash or be dropped. A destroyed Activity never runs it.
     */
    protected fun runWhenViewsReady(action: () -> Unit) {
        if (viewsReady) action() else pendingViewsReadyActions.addLast(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingViewsReadyActions.clear()
        _binding = null
        Timber.d("onDestroy: ${this::class.simpleName}")
    }

    /**
     * The keep-screen-on decision is settings-driven. The kit's screens are outdoor instruments; the
     * default is ON and a future settings screen owns the flip.
     */
    protected open fun keepScreenAwakeFor(settings: AppSettings): Boolean =
        KeepScreenAwakePolicy.shouldKeepScreenAwake(settings.preventSleep)

    // Cached decision so onCreate/onResume can re-apply the flag synchronously between settings emissions.
    private var keepScreenAwakeDecision: Boolean = true

    private fun applyKeepScreenAwake() {
        if (keepScreenAwakeDecision) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * The view that should receive initial focus when the Activity opens on a non-touch device.
     * Default null: focus lands on the content root and Android's traversal takes it from there.
     */
    protected open fun getInitialFocusView(): View? = null

    /**
     * True when the user is driving the UI with a non-touch input device (TV remote / D-pad, gamepad,
     * or hardware keyboard): the dual TV check recommended by the Android TV docs, plus touch-mode and
     * hardware-keyboard signals.
     */
    protected fun isNonTouchInputActive(): Boolean {
        if (isTvDevice()) return true
        val notTouchMode = window?.decorView?.isInTouchMode == false
        val hasHardwareKeyboard = resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
        return notTouchMode || hasHardwareKeyboard
    }

    /**
     * True when the app runs on a TV device (Android TV / Google TV / Fire TV): FEATURE_LEANBACK
     * resists fake-TV boxes, the TV UI mode catches stripped-down systems that omit Leanback.
     */
    private val hasLeanback: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    private fun isTvDevice(): Boolean {
        val isTvUiMode = (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
        return hasLeanback || isTvUiMode
    }
}
