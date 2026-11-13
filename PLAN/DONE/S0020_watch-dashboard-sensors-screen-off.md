# S0020 - Watch dashboard keeps sensor and location registrations with the screen off

**Status:** Implemented
**Status note:** Implemented 2026-11-08, not built, not tested until the release: the watch dashboard subscribes its telemetry only while the watch is interactive (PowerManager.isInteractive, told by the screen broadcasts), so ambient and screen off release every sensor and location registration at once, the last frame stays, and a wake restarts them. Release: preflight (compile, DI on the release variants, the new tests), then the B1 watch re-measure, 3 of 3.
**Complexity:** hard - assessed at the spec pass: watch lifecycle and sensor registration changes depend on an unresolved product decision.
**Phase:** finding of S0013 (deep dynamic audit)
**Verification level:** device - reproduced by script on the S0013 disposable watch profile

## 0. Source

- S0013 scene B1 (screen off and on mid-feature), budget 8.3c; related: B5, D2, F3 (release after 7.1-7.5 s).
- CLAUDE.md section 3: location is foreground-only, read while a screen is open.

## 1. Problem

On the watch (noLegal debug, Wear OS API 37 emulator) the Tourist dashboard was open and the screen was put to
sleep (`KEYCODE_SLEEP`). For the whole 12-13 s the screen stayed off, the app's sensor connections and location
registrations stayed active: 3 of 3 runs (`20261025-verify-watch`, B1 r1-r3; also `20261025-base-watch`). On the
phone the same scene released them within the budget. Where the dashboard was left by a key (B5, D2, F3), the
watch released them 7.1-7.5 s after the exit - over the 5 s hypothesis budget, inside the stand's ~2.5 s
sampling step of it.

Whether an ambient (always-on) watch screen counts as "open" is the question to settle first; the phone's
behaviour is the reference.

## 1.1 Spec research and owner input (2026-10-29)

- Triage: complex, because this lifecycle change contains an unresolved product decision; its implementation
  and file count cannot be determined until that decision is made.
- `CLAUDE.md` section 3 requires foreground-only location while a screen is open, but does not define whether
  ambient display is open. No ambient policy was found in the watch implementation or project documentation.
  Existing code is not an owner decision: the dashboard collects its state with lifecycle awareness, and the
  view model retains the upstream subscription for 5 seconds after the last subscriber. The repository has
  sensor and location cleanup on cancellation. These facts do not decide ambient behavior.
- **Owner decision (decided 2026-11-08, quiz): ambient does not count as an open screen.** When the dashboard
  is visible in always-on ambient mode, its live sensor and foreground location registrations stop, and they
  restart on interactive wake.
  - Chosen: **stop all registrations in ambient and restart on interactive wake.** This follows the phone
    screen-off reference and CLAUDE.md section 3; ambient values may be stale until wake.
  - Not chosen: keep both while ambient is visible; keep sensors and stop only location.
- **Budget 8.3c (decided 2026-11-08, quiz): for this ticket's acceptance it is "released no later than 5 s,
  observed within one sampling step of the stand", i.e. <= 7.5 s as measured.** S0013's other `[h]` budgets are
  not re-issued by this decision; S0013 decision 8.1 stays open for them. The three-run B1 device measurement
  and budget verdict belong to the release tier.
- Shared-contract boundary: no wire format or catalog contract change is identified for the requested local
  lifecycle fix. Preserve the FastMediaSorter port and drift discipline in `CLAUDE.md` section 2.

### Quiz decisions (2026-11-08)

- Does a visible always-on ambient dashboard keep its live sensor and foreground location registrations? ->
  Stop all of them in ambient, restart on interactive wake (follows the phone reference and CLAUDE.md section 3;
  the owner took the recommended option).
- Which 8.3c budget is final for this ticket's acceptance? -> Released no later than 5 s, observed within one
  sampling step of the stand (<= 7.5 s as measured); the 5 s view-model grace stays unchanged, so no ported file
  changes for the budget (the owner took the recommended option).
- Resolved without asking (facts from the code, 2026-11-08):
  - The `wear` module has no ambient code: no ambient lifecycle observer and no ambient declaration in its
    manifest; the dashboard does not hold `FLAG_KEEP_SCREEN_ON` (only the SOS, water flashlight and a running
    stopwatch do, through `KeepScreenOnEffect`). The implementation therefore needs an ambient or interactive
    state signal on the watch; its shape is for the plan pass.
  - Watch (`TouristViewModel`) and phone (`TouristInfoViewModel`) both keep the upstream subscription 5 s after
    the last subscriber (`SharingStarted.WhileSubscribed(5_000)`). A key exit read as 7.1-7.5 s by a stand that
    samples about every 2.5 s is the 5 s grace plus one sampling step, which is what the decided budget states.

## 2. Evidence

- Recorded 2026-10-25 in the S0013 runs `20261025-verify-watch` (B1 r1-r3) and `20261025-base-watch`: the `sensors`
  and `locRegs` sample columns against the marks `screen-off` and `open:wake`. The lines that carry the verdict are
  kept in section 4.1.
- Re-run: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Scenes B1 -Devices watch -Repeat 3`.

## 3. Acceptance

- [x] Decided whether ambient counts as an open screen: it does not (owner, 2026-11-08).
- [ ] The registrations clear within budget 8.3c after screen-off, re-measured by B1 (3 of 3). Release tier, by
      the owner's decision in section 1.1.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0013 (the audit this finding came from; its decision 8.1 stays open for the budget rows
  other than 8.3c; B1 is re-run there), S0008 (the watch app that holds the dashboard).
- **Ambient policy:** decided 2026-11-08 (owner, quiz): ambient is not an open screen; sensor and foreground
  location registrations stop in ambient and restart on interactive wake.
- **Budget 8.3c:** decided 2026-11-08 (owner, quiz): released no later than 5 s, observed within one sampling
  step of the stand (<= 7.5 s as measured); the 5 s view-model grace is unchanged.

## 4. Plan (2026-11-08, unattended queue pass)

Complexity stands at `hard`: the plan crosses the domain, data and DI layers and changes a lifecycle.

### 4.1 Cause, measured in the recorded run

B1-watch r1 of the 2026-10-25 verify run, event-log clock in seconds:

| Clock | Event | Reads as |
| --- | --- | --- |
| 140.106 | `power_sleep_requested: 0` | `KEYCODE_SLEEP` reached the power manager |
| 140.148 | `screen_toggled: 0` | the watch is not interactive, 42 ms later |
| 140.152 | `AmbientTaskStateMachine: TaskInteractive -> TaskAmbiactive`, target `MainActivity` | the system keeps the task in ambient by itself; the module declares no ambient support |
| 140.216 | `power_screen_broadcast_done: [0,66,1]` | the off broadcast went round in 66 ms |
| 154.613 | `screen_toggled: 1`, later `power_screen_broadcast_done: [1,69,1]` | the wake, broadcast in 69 ms |
| 158.866 | `wm_on_paused_called` for `MainActivity` | the first lifecycle callback since the open, and it follows the wake and the scene's next key |

All three runs read the same. `screen_toggled: 0`, `screen_toggled: 1` and the first `wm_on_paused_called` for
`MainActivity` fall at 140.148, 154.613, 158.866 (r1), 185.641, 199.851, 204.114 (r2) and 229.430, 243.640, 247.830
(r3): the pause always comes about 4.2 s after the wake. In all three `sensors` reads 2 and `locRegs` 1 from before
the sleep to after the wake.

So the Activity stays `RESUMED` for the whole sleep, `collectAsStateWithLifecycle` keeps collecting and
`SharingStarted.WhileSubscribed` never starts its 5 s. The phone differs exactly here (B1-phone r1 of the
2026-10-25 base run): `screen_toggled: 0` at 466.259 and `wm_on_stop_called` for the dashboard at 466.289, 30 ms
later, which is why it releases in budget.

### 4.2 Approach

- **Signal:** `PowerManager.isInteractive`, told by `ACTION_SCREEN_OFF` and `ACTION_SCREEN_ON`. It is false in
  ambient and with the screen off alike, so one signal answers both, and it needs no manifest entry, permission
  or dependency. `AmbientLifecycleObserver` is not used: attached to the watch's only Activity it would opt every
  screen into always-on, which nobody ordered.
- **Gate:** `ObserveWearTouristDashboardUseCase` subscribes the telemetry source only while the watch is
  interactive. `TouristViewModel` and its 5 s grace stay as ported, and because the gated flow stays open while
  non-interactive, the view model's `combine` keeps the last frame on screen (stale until wake, as decided).
- **Key exits (B5, D2, F3)** are not touched: 7.1-7.5 s is the 5 s grace plus one sampling step, inside the
  decided budget, and `_judges.ps1` already judges `limit + realInterval`.
- **Re-subscribing is an existing path:** `observeTelemetry` drops the GPS anchor on subscribe, the trip
  accumulator counts no gap as a walk, and the step counter keeps its base, so the steps walked in ambient appear
  on wake. A watch with only a step detector does not count them: stale in ambient, as decided.

### 4.3 Coverage inventory

| Line | Maps to |
| --- | --- |
| Ambient is not an open screen; stop in ambient (owner) | steps 1-4; step 6 records it in `CLAUDE.md` section 3 |
| Restart on interactive wake (owner) | step 4, pinned by step 5; release B1 |
| Registrations clear within 8.3c after screen-off, B1 3 of 3 | steps 1-4; the measurement is release tier (owner) |
| 5 s grace unchanged, no budget edit (owner) | step 4 leaves `TouristViewModel` main code untouched |
| Foreground-only location (`CLAUDE.md` section 3) | step 4 |
| Port and drift discipline (`CLAUDE.md` section 2) | step 6, decisions next to the ledger rows |
| Shared contract | out-of-scope: no wire format or catalog contract changes |
| Weather requests while ambient | out-of-scope: not a sensor or location registration and not in the owner's decision; see 5 |
| User-facing text and help | out-of-scope: no string changes, the watch is outside the help, and no surface mentions ambient |

### 4.4 Steps, with what each produces and consumes

Nothing is consumed before it is produced.

- [x] 1. Add `WearInteractiveStateRepository` (domain). Produces the seam; consumes nothing. Check: one
      `fun observeInteractive` declaration.
- [x] 2. Add `AndroidWearInteractiveStateRepository` (data). Consumes 1. Check: one each of
      `Intent.ACTION_SCREEN_OFF`, `Intent.ACTION_SCREEN_ON`, `unregisterReceiver(` in the file.
- [x] 3. Bind it in `WearTouristModule`. Consumes 1, 2. Check: one `bindWearInteractiveStateRepository`.
- [x] 4. Gate `ObserveWearTouristDashboardUseCase`. Consumes 1. Check: one `flatMapLatest` and one `emptyFlow()`
      call in the file; `SUBSCRIPTION_TIMEOUT_MS = 5_000L` still in `TouristViewModel`.
- [x] 5. Add `ObserveWearTouristDashboardUseCaseTest` and `AndroidWearInteractiveStateRepositoryTest`, and one last
      frame test in `TouristViewModelTest`. Consumes 1, 2, 4. Check: the three test names exist.
- [x] 6. Add the ambient clause to `CLAUDE.md` section 3 and the ledger decisions for the two ported files that
      changed. Check: `tools/fast-check.ps1 -Area docs` exits 0; both rows carry a `decision` starting `S0020`.

## 5. Left open

- `ObserveWearTouristWeatherUseCase` runs a 60 s clock while the state is collected, so a watch held in ambient
  can still ask the phone for weather at the policy's interval, from the last position. It holds no sensor or
  location registration and is outside the owner's decision; unchanged here.

## Last Audit

**Date:** 26-11-08 17:21 | **Mode:** static | **Outcome:** Verified (static rungs; the compile, the unit suites and the device re-measure are RELEASE)
**Counts:** PASS 12 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 3 | RELEASE 2

- PASS - cause: event logs of B1-watch r1-r3 (read; expected no pause of `MainActivity` between sleep and wake |
  actual first `wm_on_paused_called` about 4.2 s after the wake, 3 of 3) and of B1-phone r1 (stop 30 ms after
  `screen_toggled: 0`). Lines in section 4.1.
- PASS - signal and binding: `fun observeInteractive` 1, `Intent.ACTION_SCREEN_OFF` 1, `Intent.ACTION_SCREEN_ON` 1,
  `unregisterReceiver(` 1, `fun bindWearInteractiveStateRepository` 1 (grep; expected 1 each | actual 1 each).
- PASS - gate: in `ObserveWearTouristDashboardUseCase.kt`, `.flatMapLatest` 1 and `emptyFlow()` 1 (grep; expected
  1 | 1).
- PASS - grace unchanged: `TouristViewModel.kt` line 33 `SUBSCRIPTION_TIMEOUT_MS = 5_000L`; the file's main code was
  not edited.
- PASS - reach: no manifest, permission or dependency edit; in `wear/src/main`, `AmbientLifecycleObserver` 0 and
  `<uses-permission` 1, the existing `POST_NOTIFICATIONS` (grep).
- PASS - tests written: `ObserveWearTouristDashboardUseCaseTest`, `AndroidWearInteractiveStateRepositoryTest`, and
  `the last frame stays while the watch is not interactive and a wake refreshes it` in `TouristViewModelTest` (grep,
  1 each). They have not run; see RELEASE.
- PASS - ledger: `tools/port-ledger/decide-drift.ps1` exit 0 twice; 2 of 368 rows changed, both carry a `decision`
  starting `S0020 local addition` (diff).
- PASS - stand judge: `_judges.ps1` lines 194-195 end the 8.3c window at the `open:wake` mark and line 214 judges
  `limit + realInterval`, so restarting on wake is not a finding and no stand edit is needed (read).
- PASS - docs gate: `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs` exit 0, `docs-check: PASS (5 checks)`.
- PASS - UI gate: `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui` exit 0, `unified-ui-source: PASS`.
- PASS - ticket gates: `spec_catalog/check-probe-absent -Id S0020` exit 0, `spec_catalog/check-evidence-durable -Id
  S0020` exit 0, `spec_catalog/validate` exit 0 with `0 FAIL`.
- PASS - hygiene: in the changed Kotlin files, long dashes 0, `GlobalScope` 0, direct `Log` calls 0 (grep).
- EXEMPT - shared contract: no wire format or catalog contract changes.
- EXEMPT - user-facing text and help: no string changes, the watch is outside the help, no surface mentions ambient.
- EXEMPT - weather requests while ambient: not a registration and not in the owner's decision (section 5).
- RELEASE - compile, DI on the release variants and the unit suites, including the three tests above:
  `pwsh -NoProfile -File tools/release/preflight.ps1`.
- RELEASE - re-measure: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Scenes B1 -Devices
  watch -Repeat 3`. Passing, 3 of 3: no 8.3c finding for `dashboard`, `release_dashboard_ms` at or below 7500, and
  `sensors` above 0 in the samples after `open:wake` (the restart).

No ticket-id probe exists or is needed. Not built, not tested until the release.
