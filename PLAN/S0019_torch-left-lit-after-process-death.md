# S0019 - Camera torch stays lit after the app process dies, and after a toggle storm

**Status:** BlockNeedUserTest
**Status note:** Implemented 2026-10-27. Item 2 fixed (flash lookup remembered, Lights row toggles in place) and measured 3 of 3 by hand on an emulator, 298 unit tests green. Item 1 is not an app defect: the camera service switches off a dead client's torch (HAL AVAILABLE_OFF 9 ms after the kill); the stand's reader misreports it. Open: one physical-phone check (owner), stand F8/D1 re-run (S0013 profile busy), reader fix in the S0013 stand.
**Phase:** finding of S0013 (deep dynamic audit)
**Verification level:** device - reproduced by script on the S0013 disposable phone profile

## 0. Source

- S0013 scenes F8 (hard kill mid-feature) and D1 (torch and water toggle storm), budgets 8.3d and 8.4b.
- `WaterFlashlightActivity.onStop` switches the torch off; `TorchController` drives `CameraManager.setTorchMode`.

## 1. Problem

1. **Process death.** With the water flashlight lit, the app process was killed with SIGKILL (the way the
   low-memory killer ends a process). The camera service still reported the torch on 4 s later and at the end
   of the scene: outcome `torch-left-lit`, 3 of 3 runs (`20261025-base-phone`, `20261025-verify-phone`). No
   code of the app can run after SIGKILL, so the light stays on until something else turns it off - in a
   pocket, until the battery is gone.
2. **Toggle storm.** After 100 taps on the Lights screen's camera-torch row at about 0.3 s apart (an even
   number), the torch was still lit more than 2 s after the storm ended, 3 of 3 runs. The stand cannot tell
   whether taps were dropped or the torch state and the row's state parted; the next step is to read the row's
   state against the camera service's.

The emulator's camera HAL is the only torch measured (decision 8.2): a physical device's camera service may
release the torch of a dead client, so item 1 needs one hardware check before any fix.

## 2. Evidence

- Scene evidence: `temp/S0013/runs/20261025-verify-phone/scenes/F8-phone-r{1,2,3}/` and `D1-phone-r{1,2,3}/`
  (`samples.csv` torch column, `evidence.json` faults and notes).
- Re-run: `run-matrix.ps1 -Scenes F8,D1 -Devices phone -Repeat 3`.

## 3. Acceptance

- [ ] Item 1 checked once on a physical phone; if the torch survives there too, a way is found that does not
      leave the light burning after the process dies, re-measured by F8 (3 of 3).
      *Emulator: the torch does not survive - see section 4.1. The physical-phone check is the owner's.*
- [x] Item 2: the torch state after a toggle storm equals the row's state, re-measured by D1 (3 of 3).
      *By hand on a private emulator, 3 of 3 (section 4.3); the stand's own D1 has not been re-run (section 4.4).*

## 4. Record of the work (2026-10-27)

Evidence is in `temp/S0019/` (gitignored). Logs quoted below are the S0013 run `20261025-verify-phone`.

### 4.1 Item 1 is not a defect of the app - the audit instrument misreports it

- F8 (all three runs) kills the app with `am force-stop`. 9 to 32 ms later the camera HAL logs
  `CameraProviderManager: Camera device device@1.1/internal/1 torch status is now AVAILABLE_OFF`: the camera
  service switched off the torch of its dead client, as it does on every Android that has
  `CameraService::handleTorchClientBinderDied`.
- The stand reads the torch with `dumpsys media.camera | grep "Torch for camera"` (`Get-TorchOn` in
  `scenes.ps1`, the `torch` case of `_instruments.ps1`). That list is an event log of client calls only; the
  camera service's own release of a dead client's torch is not an entry in it. So the last entry stays
  `turned on for client PID <dead pid>` and the sample column `torchOn` stays 1 to the end of the scene.
- Re-checked on a fresh API 33 `google_apis` emulator with the fixed build: torch lit from the Lights row,
  `am force-stop`, 9 ms later `AVAILABLE_OFF`; 4 s later `dumpsys media.camera` still prints
  `Torch for camera id 1 turned on for client PID 6913` (PID long dead).
- Consequence: nothing in the app can or needs to change for item 1, and F8 reports `torch-left-lit` forever
  until the reader counts an `on` entry only while its client PID is alive.

### 4.2 Item 2 had two causes in the app and one in the stand

Counted from the S0013 logcat of D1 (taps delivered = the launcher's `TaplEvents` ACTION_DOWN lines in the
`torch-storm` window; starts = `ActivityTaskManager: START ... FlashlightToggleActivity`):

| run | taps | trampoline starts | what went wrong |
| --- | --- | --- | --- |
| r1 | 100 | 100 | one start answered `TorchController: no flash unit on this device` and a Toast: the flash lookup (`cameraIdList` + `getCameraCharacteristics` on every call, any exception swallowed to null) failed once; the state parted by one tap and the burst ended lit |
| r2 | 100 | 99 | one tap never started the trampoline: the row opened `FlashlightToggleActivity`, a transparent window that lives ~220 ms (r1 events: create 1536.702, destroy 1536.922) and takes the next tap |
| r3 | 100 | 100 | clean: 50 on / 50 off, ended dark. The 8.3d breach is the stand's: the `water-storm` opens the water flashlight (`Tap row_water_flashlight`, which lights the torch) before `Begin-Feature water-storm`, so a lit torch lands in the quiet window of `torch-storm` |

### 4.3 Fix and measurement

- `TorchController`: the flash camera id is looked up once and remembered; a failed lookup is logged and never
  remembered, so the next call looks again; the memory is dropped on `IllegalArgumentException` or
  `CAMERA_DISCONNECTED`. Four unit tests added (11 in the class, green).
- `LightsActivity`: the camera-torch row toggles through `FlashlightToggleManager` in place instead of
  starting `FlashlightToggleActivity`. The trampoline stays for surfaces without a screen of their own.
- Measured by hand on a private emulator (`S0019_phone`, clone of the S0013 phone profile, API 33
  `google_apis`, `emulator-5602`): Lights screen, 2 warm-up taps, then 100 `input tap` in a row on the row.

| build | run | trampoline starts | torch on / off events | last event |
| --- | --- | --- | --- | --- |
| before (S0013 `noLegal` debug, built 15:21) | 1 | 64 | 32 / 32 | off |
| before | 2 | 64 | 32 / 32 | **on** |
| before | 3 | 63 | 32 / 31 | **on** |
| after (`store` debug) | 1 | 0 | 50 / 50 | off |
| after | 2 | 0 | 50 / 50 | off |
| after | 3 | 0 | 50 / 50 | off |

  After the fix no `TorchController` warning appears in any run. The three "after" runs were repeated with the
  final build (the `Timber.d("S0019: ..")` probe in the row's click handler added for the status gate): the same
  50 / 50, last event off, 0 warnings, 100 probe lines per run; and the kill check again gave `AVAILABLE_OFF`
  11 ms after the force-stop.

### 4.4 Left open

1. **Physical phone (acceptance item 1).** One check: light the torch from the Lights row, then
   `adb shell am force-stop com.sza.tourist` (or swipe the app away); the lamp must go out within a second.
2. **Stand re-run (F8, D1, 3 of 3).** Not run: `run-matrix.ps1` destroys and recreates the `S0013_phone`
   profile, which another session was using (`temp/S0013/profiles.json`, created 2026-10-27 14:24). For S0013:
   (a) count a `turned on for client PID <p>` entry only while `<p>` is alive; (b) judge D1's `torch-storm`
   quiet window after the water flashlight is opened, or open it before the span begins.
3. **FastMediaSorter** carries the same lookup (`DeviceActionHandler`) and the same trampoline from its menu
   row; carrying the fix back is for its own session (the source is read-only from here).

## Last Audit

2026-10-27, mode pre-handoff: sections 1, 3 and 4 re-read from this file on disk after the final build. The
numbers in section 4.3 are fresh runs of the same day on a private emulator (API 33 `google_apis`), not the
cited S0013 evidence; the S0013 logcat is cited only for the causes in 4.1 and 4.2.

Coverage per problem item of section 1:

- **Item 1, torch lit after the process dies** - covered on the emulator: the camera service turns off the dead
  client's torch (HAL `AVAILABLE_OFF` 9 and 11 ms after `am force-stop`); the lit torch F8 reports is the audit
  reader counting an event-log entry of a dead PID. No change to the app was needed or made for it.
- **Item 2, toggle storm** - covered: both causes in the app removed (lookup remembered; row toggles in place),
  100 taps give 50 on / 50 off and a dark torch 3 of 3 with the final build, against 2 lit of 3 before; the
  third cause (D1's quiet window) is the stand's.
- **Tests** - covered: 298 unit tests green on `store` (11 in `TorchControllerTest`, four of them new).

Verdict: the work covers the task except for what needs hardware this machine does not have and the stand
re-run. Unobserved, each as it stands:

- [ ] A physical phone: torch lit from the Lights row, then `adb shell am force-stop com.sza.tourist`; the lamp
      goes out within a second (the literal form of acceptance item 1).
- [ ] The stand's own F8 and D1, 3 of 3 each (the literal form of both acceptance criteria), once S0013's torch
      reader counts an `on` entry only while its client PID is alive and D1's quiet window is judged after the
      water flashlight is opened.
