package com.sza.tourist.wear

import android.content.Intent
import android.os.BadParcelableException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.sza.tourist.wear.core.util.WearUnitDateTimeFormatter
import com.sza.tourist.wear.domain.capability.WearRestrictedCapabilities
import com.sza.tourist.wear.domain.model.UnitSystem
import com.sza.tourist.wear.domain.model.WearLaunchTarget
import com.sza.tourist.wear.domain.model.readWearLaunchTarget
import com.sza.tourist.wear.domain.repository.WearPreferencesRepository
import com.sza.tourist.wear.domain.usecase.ResolveWearLaunchAddressUseCase
import com.sza.tourist.wear.ui.apps.AppsScreen
import com.sza.tourist.wear.ui.apps.sos.SosScreen
import com.sza.tourist.wear.ui.apps.stopwatch.WearStopwatchScreen
import com.sza.tourist.wear.ui.apps.tourist.TouristScreen
import com.sza.tourist.wear.ui.apps.tourist.counter.TouristCounterScreen
import com.sza.tourist.wear.ui.apps.waterflashlight.WaterFlashlightScreen
import com.sza.tourist.wear.ui.common.LocalWearDateTimeFormatter
import com.sza.tourist.wear.ui.common.LocalWearListPositions
import com.sza.tourist.wear.ui.common.LocalWearRotaryFocusStack
import com.sza.tourist.wear.ui.common.LocalWearUnitSystem
import com.sza.tourist.wear.ui.common.WearListPositionStore
import com.sza.tourist.wear.ui.common.WearRotaryFocusStack
import com.sza.tourist.wear.ui.navigation.WearLaunchRoutes
import com.sza.tourist.wear.ui.navigation.WearRoutes
import com.sza.tourist.wear.ui.theme.WearAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import timber.log.Timber
import javax.inject.Inject

/**
 * S1955: the launch-intent wiring, handed down as one thing.
 *
 * The three are useless apart - a resolver with nothing pending resolves nothing, and clearing without
 * both cannot be ordered after the navigation - so they travel together rather than as three parameters
 * repeated at every level between the Activity and the navigation host.
 */
data class WearLaunchEntry(
    val resolveAddress: ResolveWearLaunchAddressUseCase,
    val pendingTarget: StateFlow<WearLaunchTarget?>,
    /** Takes the target it handled, so a newer one that arrived mid-resolution is not cleared unhandled. */
    val onHandled: (WearLaunchTarget) -> Unit,
)

/**
 * The only Activity of the watch app, and the composition root of its Compose UI.
 *
 * It is exported: the stopwatch's ongoing-activity notification, the phone's SOS start and a launcher tap
 * all enter here, and the first two name a destination in the intent (see [WearLaunchTarget]).
 *
 * Trimmed from FastMediaSorter's composition root: the brand frame, onboarding and permission walks, the
 * wallpaper and colour-scheme layers, the rim back and screen-off affordances, the test-launch override
 * and the stream, file and tile entrances belong to surfaces this product does not carry. The program
 * list is the start screen, and the routes the capabilities withhold are not registered at all.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesRepository: WearPreferencesRepository

    // S1955: an outside caller names its target in the launch intent, and resolving it asks the
    // capabilities, so a destination this build withholds does not resolve.
    @Inject lateinit var resolveLaunchAddress: ResolveWearLaunchAddressUseCase

    @Inject lateinit var capabilities: WearRestrictedCapabilities

    // S2543: list positions outlive the screens that produced them, so the store is held by the process
    // and handed to composition here - a screen popped off the back stack takes its own state with it.
    @Inject lateinit var listPositions: WearListPositionStore

    // S2795: handed to composition here because every screen that shows a time needs it, and one shared
    // instance keeps every surface on the same patterns.
    @Inject lateinit var dateTimeFormatter: WearUnitDateTimeFormatter

    /**
     * S1955: what this launch asked to open, until the navigation host has opened it.
     *
     * Held here rather than read inside the host because the host is not composed yet when the intent
     * arrives on a cold start.
     */
    private val pendingLaunchTarget = MutableStateFlow<WearLaunchTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("MainActivity created")

        // Only on a genuine start: a recreation re-delivers the same intent, and the module's
        // configChanges does not cover a locale, font-scale or density change, so re-reading it here
        // would replay the jump the user already took - and again on every restore from Recents.
        if (savedInstanceState == null) {
            pendingLaunchTarget.value = launchTargetFrom(intent)
        }

        setContent {
            // S2763: one rotary stack for the whole watch UI. It has to span the screen and everything
            // drawn over it - dialogs, the action cloud - or each side would believe it owns the crown.
            val rotaryFocus = remember { WearRotaryFocusStack() }
            // S2795: collected above every screen, because the measurement system decides the clock
            // format on surfaces that share nothing else. A push from the phone lands here and
            // recomposes all of them.
            val units by preferencesRepository.unitSystem
                .collectAsStateWithLifecycle(initialValue = UnitSystem.DEFAULT)
            CompositionLocalProvider(
                LocalWearUnitSystem provides units,
                LocalWearListPositions provides listPositions,
                LocalWearRotaryFocusStack provides rotaryFocus,
                LocalWearDateTimeFormatter provides dateTimeFormatter
            ) {
                WearApp(
                    capabilities = capabilities,
                    launchEntry = WearLaunchEntry(
                        resolveAddress = resolveLaunchAddress,
                        pendingTarget = pendingLaunchTarget,
                        onHandled = { handled -> pendingLaunchTarget.compareAndSet(handled, null) }
                    )
                )
            }
        }
    }

    /**
     * S1955: a launch delivered while this Activity is already in front arrives here, not in [onCreate].
     *
     * `setIntent` keeps `getIntent()` agreeing with what was just handled, so anything later reading the
     * Activity's intent sees the target the user actually named rather than the one that started it.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Only overwrite with a real target: tapping the launcher icon while a target is still waiting
        // delivers a bare MAIN intent, and that must not erase it.
        launchTargetFrom(intent)?.let { pendingLaunchTarget.value = it }
    }

    /**
     * Reads the launch target, or nothing when the extras cannot be read.
     *
     * Reading one extra unparcels the whole bundle, and this Activity is exported, so a malformed bundle
     * from any app on the watch would otherwise take the process down before the first frame. Treating it
     * as "no target was named" is the same outcome as an ordinary launch, which is the safe answer.
     */
    private fun launchTargetFrom(intent: Intent): WearLaunchTarget? = try {
        readWearLaunchTarget(intent)
    } catch (e: BadParcelableException) {
        Timber.w(e, "Unreadable launch intent extras - treating as a plain launch")
        null
    }
}

@Composable
fun WearApp(
    capabilities: WearRestrictedCapabilities,
    // S1955: read-only here and cleared through the callback, so the one writer stays the Activity.
    launchEntry: WearLaunchEntry
) {
    WearAppTheme {
        MainNavigation(capabilities = capabilities, launchEntry = launchEntry)
    }
}

@Composable
fun MainNavigation(
    capabilities: WearRestrictedCapabilities,
    launchEntry: WearLaunchEntry,
) {
    val navController = rememberSwipeDismissableNavController()

    OpenLaunchTargetEffect(navController = navController, launchEntry = launchEntry)

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // S2097 - When on WearRoutes.HOME (root destination), intercept Back press to call moveTaskToBack(true)
    // instead of finishing the Activity and overshooting into developer options or settings.
    BackHandler(enabled = currentRoute == WearRoutes.HOME) {
        (navController.context as? android.app.Activity)?.moveTaskToBack(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearRoutes.HOME,
        ) {
            composable(WearRoutes.HOME) {
                AppsScreen(navController = navController)
            }

            programRoutes(navController = navController, capabilities = capabilities)
        }
    }
}

/**
 * S1955: opens the destination an outside caller named, once the host is on screen.
 *
 * The waiting is the point: a notification tap or the phone's SOS start launches a cold process, and the
 * target has to outlive the first composition rather than be read where it arrives.
 *
 * A null resolution navigates nowhere. The target is one this build does not offer, and leaving the user
 * on the start destination is a better answer than jumping at a screen with nothing behind it
 * (strategic §5.2).
 */
@Composable
private fun OpenLaunchTargetEffect(
    navController: NavHostController,
    launchEntry: WearLaunchEntry,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(navController) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launchEntry.pendingTarget.filterNotNull().collect { target ->
                when (val address = launchEntry.resolveAddress(target)) {
                    // The whole path is otherwise silent: a withheld destination and an ordinary launch
                    // look identical from outside the process.
                    null -> Timber.w("Launch target does not resolve in this build, staying put: %s", target)
                    else -> navigateGuarded(navController, WearLaunchRoutes.routeFor(address))
                }
                // Cleared even when nothing resolved, so the jump cannot be replayed.
                launchEntry.onHandled(target)
            }
        }
    }
}

/**
 * Navigates to [route], or logs it when the graph does not carry it.
 *
 * This entry point is reachable from outside the app, so an address the graph cannot serve must not take
 * the process down. Staying on the current screen is the same safe outcome as a target that does not
 * resolve. `launchSingleTop` covers the case of the very same address arriving twice.
 */
private fun navigateGuarded(navController: NavHostController, route: String) {
    try {
        navController.navigate(route) { launchSingleTop = true }
    } catch (e: IllegalArgumentException) {
        Timber.w(e, "Launch route is not in the navigation graph: %s", route)
    }
}

/**
 * The programs of the list. A route a capability withholds is not registered at all (owner decision 4.3,
 * S0008): the store edition carries no Tourist dashboard, and both editions carry the screen takeover pair.
 */
private fun NavGraphBuilder.programRoutes(
    navController: NavHostController,
    capabilities: WearRestrictedCapabilities,
) {
    composable(WearRoutes.STOPWATCH) {
        // S2825: holding the menu control leaves the stopwatch.
        WearStopwatchScreen(onLeave = { navController.popBackStack() })
    }

    // S2995: the dashboard reads heart rate, steps and location - sideload only.
    if (capabilities.offersHealthFeatures) {
        // S4185: Tourist and the counter it opens. The counter is reached only from its Tourist tile,
        // so it is registered wherever Tourist is.
        composable(WearRoutes.TOURIST) {
            TouristScreen(
                onLaunchSos = { navigateGuarded(navController, WearRoutes.SOS) },
                onLaunchStopwatch = { navigateGuarded(navController, WearRoutes.STOPWATCH) },
                onLaunchCounter = { navigateGuarded(navController, WearRoutes.TOURIST_COUNTER) },
            )
        }
        composable(WearRoutes.TOURIST_COUNTER) {
            TouristCounterScreen()
        }
    }

    // S3362: registered only where the screen takeover is offered. Both programs consume every pointer
    // event on the initial pass, so the route is the last place a build can refuse a screen WO-V3 says a
    // swipe must be able to leave. Owner decision 4.3: both editions offer them.
    if (capabilities.offersScreenTakeoverPrograms) {
        composable(WearRoutes.WATER_FLASHLIGHT) {
            // Leaving is the host's word: the screen knows only that some hardware input arrived, never
            // what to navigate to.
            WaterFlashlightScreen(onLeave = { navController.popBackStack() })
        }

        // S3216: leaving is the host's word here too.
        composable(WearRoutes.SOS) {
            SosScreen(onLeave = { navController.popBackStack() })
        }
    }
}
