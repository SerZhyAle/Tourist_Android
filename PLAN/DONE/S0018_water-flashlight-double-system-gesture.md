# S0018 - Water flashlight leaves on two quick system-edge gestures

**Status:** Implemented
**Status note:** Implemented 2026-10-27: Android screen pinning with system confirmation guards the water flashlight; repeated system gestures and BACK no longer leave it, hardware exit keys unpin and close it, the OS unpin escape is preserved. Emulator (API 33, gesture navigation): 11 wet inputs x3 kept the lamp, 13 lifecycle cases pass, 9 unit tests green on store and noLegal. Ledger rows carry the dated decision. Not measured: a physical phone, 3-button navigation.
**Phase:** finding of S0013 (deep dynamic audit), Phase 1 signatures
**Verification level:** device - reproduced by script on the S0013 disposable phone profile

## 0. Source

- S0013 scene C4 (water flashlight full cycle incl. the exit rule), cells P2 and P3; S0013 section 9.
- CLAUDE.md section 3: "the water flashlight's rule - no wet touch can switch it off - is the product's
  signature and is ported exactly".
- `WaterFlashlightActivity` (exit keys: volume, camera, headset hook, media play/pause; `KEYCODE_BACK` absent on
  purpose; system-bar suppression through `WaterFlashlightLockdownManager`).

## 1. Problem

On the phone (noLegal debug 2.61.0251.831, emulator API 33, gesture navigation, `navigation_mode=2`) the water
flashlight survives every single touch the stand injected - tap, long press, a swipe across the glass, one
left-edge or right-edge back gesture, one bottom home gesture, one top shade pull: 0 exits in 21 tries. It
leaves, and the torch goes dark, when two system-edge gestures arrive in quick succession: the first swipe
only reveals the suppressed system bars, the second one is then executed by the system.

| Input (each from a freshly opened lamp) | Lamp left | Runs |
| --- | --- | --- |
| left-edge back swipe twice, 0.4 s apart | yes | 3 of 3 |
| left-edge then right-edge back swipe | yes | 3 of 3 |
| bottom home swipe twice, 0.4 s apart | yes | 3 of 3 |
| `KEYCODE_BACK` (key event, not a touch) | yes | 3 of 3 |
| `KEYCODE_VOLUME_DOWN` (the exit rule) | yes, as designed | 3 of 3 |
| every single touch listed above | no | 0 of 21 |

The scene's wet-touch storm (40 random taps, 8 swipes, both edge swipes, a bottom swipe, a long press) took the
lamp away in 3 of 3 runs (S0013 runs `20261025-base-phone`, `20261025-verify-phone`).

Water running across the glass can plausibly produce two edge swipes within a second; whether that is a "wet
touch" the rule must withstand, and whether a hardware/3-button BACK may leave the lamp, are owner decisions
(S0013 decision 8.7, question `C4-exit`).

## 2. Evidence

- The original S0013 runs of 2026-10-25 were disposable; the finding is reproduced on the fresh baseline of
  section 4, kept as `S0018_water-flashlight-double-system-gesture/evidence-before-water-gestures.json`.
- Re-run: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/water-gestures.ps1` on a live S0013
  phone profile.
- Not measured: a physical phone (decision 8.2 is emulator-only), the store variant (decision 8.6), FastMediaSorter
  (no parity benchmark, S0013 non-goal) - the port ledger says the behaviour is ported, so the source likely
  behaves the same.

## 3. Acceptance

- [x] The owner decides whether a second system gesture is inside the rule.
- [x] The lamp stays lit for every row of the table above except the exit keys, re-measured with
      `water-gestures.ps1` (3 of 3) after confirming Android screen pinning (section 4, "After the fix").
- [x] Any change is carried to FastMediaSorter's behaviour record through the port-ledger mechanism, never as a
      silent divergence (CLAUDE.md section 2): both ported rows carry a dated decision (2026-10-27); the
      upstream carry itself belongs to the FastMediaSorter session, its source being read-only here.

### Owner decision (2026-10-27)

Repeated system gestures and BACK are inside the guard. Leave the water flashlight using its hardware exit
keys. The decision was supplied in this implementation session.

### Implementation boundary

Android does not allow an ordinary app to opt out of Home/quick-switch gestures. Immersive mode alone is
insufficient. Use Android screen pinning with its system confirmation; keep the camera torch off until the
confirmation succeeds. If the system request is dismissed, leave an unarmed Start/Cancel dialog so the user
can retry or return to the previous screen. The SystemUI prompt has no public result callback and does not
take window focus on API 33; observing task state is necessary. Consume BACK through AndroidX's
back dispatcher as well as key dispatch. Release pinning owned by this lamp before a hardware exit, preserve
it through rotation, and never release pinning that was already active before the lamp opened.

Android's own unpin escape remains available (including swipe-up-and-hold under gesture navigation). This is
an OS escape, not an app exit control; the help must explain it without promising an unbreakable kiosk lock.
No device-owner provisioning, accessibility service, overlay permission or private API is introduced.

Platform references: [gesture navigation](https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav)
and [Activity.startLockTask](https://developer.android.com/reference/android/app/Activity#startLockTask()).

## 4. Verification record (2026-10-27)

Fresh baseline: API 33 disposable profile `emulator-5590`, gesture navigation (`navigation_mode=2`), noLegal
debug `2.61.0271.429` (`261027142`), installed APK SHA-256 prefix `a87abf725802`, confirmed by the profile
installer's readback. Command:

```powershell
pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/water-gestures.ps1 -Recipe phone -Repeat 3 -Only 'edge-left-twice,edge-both-sides,bottom-home-twice,key-back,key-volume-down' -RunId 20261027-S0018-before
```

Expected before the fix: reproduce all four failure inputs and retain the hardware-exit control. Actual:
`edge-left-twice`, `edge-both-sides`, `bottom-home-twice`, `key-back` each closed the lamp and extinguished
the torch in 3/3 attempts; volume-down closed it in 3/3 attempts. Exit 1, verdict `S0013 water-gestures: FAIL`.
Results: `S0018_water-flashlight-double-system-gesture/evidence-before-water-gestures.json` (screenshots were
disposable and are not kept).

The input probe now refuses a dark initial torch or an empty selection, checks the torch as well as the
resumed activity, and offers `-RequirePinning` to confirm Android's real dialog before injecting inputs.
`check-lifecycle.ps1` beside this ticket exercises cancellation, rotation, screen-off/wake, the OS unpin escape
and all six hardware exit keys.

### After the fix (2026-10-27)

Build under test: noLegal debug `2.61.0271.520` (`261027152`), sha256 prefix `61f20b5b362e`, installed on the same
`emulator-5590` profile by `profile.ps1 -Action Install` (readback confirmed). Android's real pinning dialog is
confirmed through its own button; no shell lock-task, no device-owner setup.

| Check | Command | Result |
| --- | --- | --- |
| Unit tests, `WaterFlashlightLockdownManagerTest` | `./gradlew :phone:testNoLegalDebugUnitTest :phone:testStoreDebugUnitTest --tests "*WaterFlashlightLockdownManagerTest" --no-daemon` | 9 of 9 on each variant, exit 0 |
| Wet inputs | `water-gestures.ps1 -Recipe phone -Repeat 3 -RequirePinning -RunId 20261027-S0018-after` | PASS, exit 0: all 11 touch and key inputs including `edge-left-twice`, `edge-both-sides`, `bottom-home-twice`, `key-back` kept the lamp and the torch in 3 of 3; `key-volume-down` left in 3 of 3 |
| Lifecycle | `check-lifecycle.ps1` | PASS, exit 0, 13 cases: dismissed consent stays unarmed and dark, Cancel returns to Lights, rotation keeps the pin and the lamp, screen-off puts the torch out and wake restores it, the OS unpin gesture releases the pin, each of the six hardware keys unpins and closes the lamp with the torch off |

Results: `S0018_water-flashlight-double-system-gesture/evidence-after-water-gestures.json` and
`S0018_water-flashlight-double-system-gesture/evidence-after-lifecycle.json` (screenshots were disposable and
are not kept; both scripts re-run against a live S0013 phone profile).

Observations, not defects of the acceptance:

- After the OS unpin gesture (swipe up and hold) the lamp stays on screen with the torch lit until the next
  lifecycle event; touches are still ignored and Home or a hardware exit key leaves it. Closing the lamp at the
  moment of unpinning would need a timer while armed, which the lamp deliberately does not run.
- A first lifecycle run after an ad-hoc probe that ended with `force-stop` in the same shell line failed its two
  consent cases: the stand's torch detector reads the newest "turned on/off" line of `dumpsys media.camera`, and a
  process killed before `onStop` leaves a stale "on". A clean rerun passed 13 of 13; not reproduced since.

Not measured: a physical phone (S0013 decision 8.2 is emulator-only), 3-button navigation (the profile runs
gesture navigation, `navigation_mode=2`), Android versions other than API 33.

The in-app help gate (`tools/help/check-help.ps1`) is red at 62 findings, all of them stale screenshot captures
(strings or layouts changed after the capture) across every help surface, `lights` among them; no content finding
concerns the pinning text. Recapture is the S0014 step and is not part of this ticket. The port-ledger drift check
lists 5 uncarried Wear Data Layer rows, none of them the water flashlight.
