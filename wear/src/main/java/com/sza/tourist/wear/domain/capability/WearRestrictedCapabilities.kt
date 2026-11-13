package com.sza.tourist.wear.domain.capability

/**
 * S2486 / S3178: the capabilities a distribution may withhold, answered once per build.
 *
 * The answers are BUILD-TIME: the implementation is chosen by the variant source set (`store` or `noLegal`)
 * and cannot change while the app runs. The two-sided binding has no default in `src/main` on purpose - a
 * default would diverge silently from the variant that overrides it.
 *
 * Trimmed from FastMediaSorter's interface to the two answers this product's screens ask for. The other
 * capabilities of that interface (media access, voice recording, remote sources, diagnostics, content
 * transfer and the rest) gate surfaces this product does not carry. The composition below is owner
 * decision 4.3 of 2026-10-25 (S0008): the Tourist dashboard stays sideload-only as in FastMediaSorter's
 * S3178, while SOS and the water flashlight are offered by both variants.
 */
interface WearRestrictedCapabilities {

    /**
     * Whether this build may hold the system shade shut while a screen asks for it - the pull-down that
     * reaches airplane mode and brightness over a screen the user cannot touch away. The water flashlight
     * asks for it, and it is what completes "no wet touch can switch it off" for the shade.
     *
     * The mechanism is lock task mode, which leaves a task with no shade at all; an ordinary app entering
     * it is screen pinning. Play reviews a kiosk-shaped capability against a short list of admitted uses -
     * dedicated devices, enterprise deployment, guided access - and a hike kit matches none of them, so the
     * store build does not ask for it (S2812, carried from FastMediaSorter with the same answer). The overlay
     * route that would otherwise cover the shade is closed twice over: Wear OS 4 removed the system UI
     * granting `SYSTEM_ALERT_WINDOW`, and an application overlay may not draw over the status bar in any case.
     */
    val locksSystemShade: Boolean

    /**
     * S2995 / S3042: whether this build offers the Tourist dashboard, the one reader of the heart-rate,
     * activity-recognition and location permissions.
     *
     * Play Console declarations prohibit health features in store builds unless declared in Play Console.
     * Withheld in `store`, offered in `noLegal`; when it is false the program row, the route, the launch
     * address and the permissions of the dashboard all close together.
     */
    val offersHealthFeatures: Boolean

    /**
     * S3362: whether this build offers the programs that take the screen over - the water flashlight
     * and the distress signal.
     *
     * Both are built so that no touch and no swipe leaves them: every pointer event is consumed on
     * the initial pass and the exit is a hardware key. Wear OS review item WO-V3 asks for swipe to
     * close from almost all screens, and WO-V13 does not like a full white screen; the signal also
     * strobes at about three flashes a second with the alarm stream raised to its maximum.
     *
     * FastMediaSorter withholds both in its store build (owner ruling of 2026-09-21 there). This
     * product's owner ruled the opposite on 2026-10-25 (S0008, decision 4.3): the "no wet touch can
     * switch it off" rule is the product's signature, so both variants answer true. The answer stays a
     * capability with its own boundary test, so a store review that refuses the pair is one line to turn
     * back into the withholding behaviour.
     *
     * Unlike a capability with saved content behind it, this decides whether the programs appear at
     * all: the program row, the route, the launch address and the phone's SOS start all close together.
     */
    val offersScreenTakeoverPrograms: Boolean
}
