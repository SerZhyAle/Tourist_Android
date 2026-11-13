# S0037 - gyroscope-3d-panel

**Ticket:** S0037
**Status:** BlockNeedUserTest
**Status note:** Ported from FastMediaSorter (blocker fms-a:S4560 released): Gyroscope tile with a 3D box turning at sensor rate, 7 new + 11 carried files, local activity collector recorded as a ledger decision. No compile command declared - not built, not tested until the release. Owner device test: focus Gyroscope on a real phone, turn it about each axis, check logcat S0037: probe.
**Complexity:** medium - re-assessed at the plan pass 2026-11-13: the build order is settled and FastMediaSorter has realised the panel, so the work is a port of a finished change set (7 new files, 11 carried, 1 local divergence) with no open design decision; it stays above primitive because it adds files, public types and a sensor path.
**Date:** 2026-11-09

## Captured request and migration scope

Tourist sub app the panels Gyroscope showing the realtime moving in 3 dimensions. After its realisation - add the ticke to Tourist idependet mobile android application

## 1. Spec pass (2026-11-10, unattended queue pass)

### 1.1 Triage

Complex, not primitive: the change adds files (a sensor source, a drawn 3D view), public types (a tile, a capability, a hero visual), a new screen element on the dashboard card and strings in every shipped locale, and it edits far more than three existing files. The criterion that forced it upward is the first of them.

### 1.2 Problem and goals

The Tourist dashboard has no gyroscope panel. The owner asks for a panel that shows the phone's movement in three dimensions in real time. The request is written in the FastMediaSorter vocabulary ("Tourist sub app", "panel") and ends with the instruction that, once it is realised there, the ticket is added to this independent application - the same two-step shape as FastMediaSorter S4524 / S4525, mirrored here as S0038 / S0039.

Goals once the owner inputs below are settled:

- A dashboard tile, offered only on a phone that has the sensor, whose focused card draws a 3D picture that follows the phone's motion in real time.
- The picture costs nothing while it is not on screen: no sensor registration once the dashboard is left or the tile is not the focused one (CLAUDE.md section 3, foreground-only rule, applies to sensors as well).
- Every shipped locale (en, ru, uk) carries the tile name, unit and status lines in the same edit as the code, and the dashboard help page names the new tile.

### 1.3 What the code and the records already answer (resolved without asking)

- **A "panel" is a dashboard tile, and its picture belongs to the focused hero card.** The sibling tickets call the magnetic-field tile "the magnetic-field panel"; the hero card already hosts a picture per tile through `HeroVisual` (`CompassRose`, `Arrow`, `Level`, `Coordinates`) in `phone/src/main/java/com/sza/tourist/ui/tourist/helpers/TouristTileContent.kt`, and the grid ignores it. A 3D picture is one more `HeroVisual`.
- **Nothing exists to port.** No gyroscope source in `phone/src` (case-insensitive grep for `gyro`: 0 matches). In FastMediaSorter the only gyroscope code is the watch-side Motion Monitor mini-program (S2458, numeric readout on the watch); its `app_v2` phone Tourist has none, and its `PLAN/` holds no ticket for a gyroscope 3D panel (searched 2026-11-10, active and archive). So "after its realisation" has not happened yet.
- **Seven files this change must edit are ported files with ledger rows** in `docs/port-ledger.jsonl`: `TouristTileType`, `TouristDashboardState`, `SensorCapability`, `DeviceSensorAvailabilityRepositoryImpl`, `TouristTileContent` (holds `HeroVisual`), `TouristTileContentManager` and `TouristHeroTileManager`. CLAUDE.md section 2 makes any divergence of a ported file from its FastMediaSorter source a decision written next to its ledger row, never silence.
- **The dashboard state is too slow for a moving 3D picture.** `ObserveTouristDashboardUseCase` hands the screen a state sampled every `UPDATE_THROTTLE_MS = 500` ms, the pressure, light, magnetic-field and level sources register at `SENSOR_DELAY_UI`, and `OrientationReadingSource` asks for `UPDATE_INTERVAL_MICROS = 1_000_000` (one second). A picture driven from that state would step twice a second at best. The plan pass therefore gives the picture its own flow at `SENSOR_DELAY_GAME`, bound only while the tile is focused and the screen is open, beside `OrientationReadingSource` (which already reads `TYPE_ROTATION_VECTOR` and reuses its arrays per sample).
- **No new permission.** `SENSOR_DELAY_GAME` stays far below the 200 Hz line above which Android 12 and later require `HIGH_SAMPLING_RATE_SENSORS`, so the manifest, the data-safety form and the permission list stay as they are (invariants 13 and 14). Local rendering only, no network, no storage: "no telemetry" stays true.
- **Reach is not reduced.** A new `SensorCapability` value gates the tile the way `LEVEL` and `MAGNETOMETER` do (hardware only: a phone without the sensor is not shown an empty tile). Minimum platform version, ABIs and device set are untouched (invariant 2).
- **Plan-pass decisions, not owner calls:** which rotation sensor feeds the picture (`TYPE_GAME_ROTATION_VECTOR`, which has no magnetic dependence, with `TYPE_ROTATION_VECTOR` as fallback, is the starting point) and which sensor the capability asks for.

### 1.4 Shared-contract boundary

None identified for the phone-only form: the picture is rendered from local sensor events and nothing another product reads, writes or reproduces changes. If the owner chooses to extend the watch (input 3 below) and values cross the Data Layer, the catalog amendment becomes the plan's first phase, before any boundary code.

## 2. Research items

1. **Build order against FastMediaSorter.** Status: Closed 2026-11-13. Owner call settled: FastMediaSorter first, then port here (section 3.3, Quiz decisions 2026-11-13). The plan of this ticket therefore waits on a FastMediaSorter ticket that does not exist yet.
2. **What the 3D picture draws.** Status: Closed 2026-11-13. Owner call settled: device attitude - a 3D box that turns with the phone, fed by the fused rotation sensor, with the turning speed in degrees per second on the value line (section 3.3, Quiz decisions 2026-11-13).
3. **Watch reach.** Status: Closed 2026-11-13. Owner call settled: phone only; the watch dashboard and the help scope for the watch are untouched (section 3.3, Quiz decisions 2026-11-13).
4. **Panel placement.** Status: Closed. A dashboard tile with its picture in the hero card (section 1.3).
5. **Realtime rate and lifecycle.** Status: Closed. Own flow at `SENSOR_DELAY_GAME`, bound only while the tile is focused and the screen is open (section 1.3).
6. **Permission and reach impact.** Status: Closed. None (section 1.3).

## 3. Acceptance (provisional until the owner inputs land)

- [ ] The tile is offered only on a phone with the sensor, and is absent on one without it.
- [ ] The focused card's 3D picture follows a real phone turned about each axis within one sensor delivery.
- [ ] No sensor registration of the picture remains after the dashboard is left or another tile is focused.
- [x] Tile name, unit and status lines exist in `values`, `values-ru` and `values-uk` in the same edit; the dashboard help page names the tile (help map elements `e-hero` and `e-tiles` already cover the card and the grid).
- [ ] Unit tests for the new sensor source and the tile content builder run green; the divergence of each edited ported file is written next to its ledger row.
- [ ] Device pass by the owner: the picture on a real phone, which an emulator cannot show convincingly.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0038, S0039 (the same two-step shape for the magnetic-field panel, mirrored from FastMediaSorter S4524 / S4525); S0004 (the dashboard shell this tile joins); S0014 (in-app help, whose map and prose the tile touches).
- **Build order:** settled 2026-11-13 - option 1, FastMediaSorter first, then port here (owner accepted the recommendation with the table). Options, recommended first:
  1. FastMediaSorter first, then port here (recommended). It follows CLAUDE.md section 2 and the S4524 / S4525 pattern; the port is a clean copy of seven files and the drift check carries later fixes. Cost: this ticket waits for a FastMediaSorter ticket and session that do not exist yet.
  2. Build here directly. The independent app gets the panel at once. Cost: seven ported files diverge from their source, each with a decision recorded next to its ledger row, and FastMediaSorter realises its own copy later.
- **The 3D picture:** settled 2026-11-13 - option 1, device attitude (owner accepted the recommendation with the table). Options, recommended first:
  1. Device attitude (recommended): a 3D box that turns with the phone in real time, fed by the fused rotation sensor; the value line shows the turning speed in degrees per second from the gyroscope. This is the reading of "moving in 3 dimensions" as a moving object.
  2. Three rotation-rate axes: X, Y and Z bars or arrows in degrees per second straight from the raw gyroscope, no model. Truer to the word gyroscope, but nothing "moves" when the phone is still.
- **Watch reach:** settled 2026-11-13 - option 1, phone only (owner accepted the recommendation with the table). Options, recommended first:
  1. Phone only (recommended): the watch dashboard is a separate Compose surface in `wear/`, FastMediaSorter keeps its watch gyroscope as a separate numeric mini-program, and the help scope excludes the watch.
  2. Phone and watch: a second surface to build, capture and keep in step, and the ambient rule of CLAUDE.md section 3 applies to its sensor registration.

### Quiz decisions (2026-11-13)

- Build order against FastMediaSorter -> FastMediaSorter first, then port here (accepted with the table; follows CLAUDE.md section 2 and the S4524 / S4525 pattern). Consequence recorded for the plan pass: the plan waits on a FastMediaSorter ticket for the gyroscope 3D panel, which does not exist yet (FastMediaSorter_mob_v2 holds only sensor diagnostics in `SensorsDiagnosticsCollector.kt` and the watch Motion Monitor; checked 2026-11-13).
- What the 3D picture draws -> device attitude: a 3D box turning with the phone in real time from the fused rotation sensor, with the turning speed in degrees per second from the gyroscope on the value line (accepted with the table).
- Watch reach -> phone only; the watch dashboard stays as it is and the help scope still excludes the watch (accepted with the table).

## 4. Plan (2026-11-13, unattended pass after the blocker released)

The blocker `fms-a:S4560` was released by the portfolio dispatcher's own test (`Get-PqXrepoBlock -RepoKey tourist-mob -TicketId S0037` -> `$null`, "RELEASED"); FastMediaSorter's working tree carries the realised panel. The plan is a port of that change set (CLAUDE.md section 2), read from FastMediaSorter's source files only, never from its ticket.

Coverage inventory -> phase: goal 1 tile + 3D picture -> P1-P3; goal 2 no registration off-screen -> P2; goal 3 three locales + help -> P4; ledger rows for every edited ported file -> P5; unit tests -> P1/P3 (ported tests); device pass -> out-of-scope for this pass: owner device test (`BlockNeedUserTest`). Topology: P1 produces the model and capability that P2/P3 consume; P3 consumes P2's `attitude` stream; P4 consumes P3's string ids; P5 consumes every destination file. No forward reference.

- [x] P1 - Model and capability: `SensorCapability.ATTITUDE`, its hardware answer in `DeviceSensorAvailabilityRepositoryImpl`, `TouristTileType.GYROSCOPE(ATTITUDE)` appended last, `TouristDashboardState.turnRateDegreesPerSecond`, new `AttitudeReading`. Check: symbols grep in `phone/src/main`.
- [x] P2 - Sensor path: new `AttitudeReadingSource` (game rotation vector, geomagnetic fallback, gyroscope for the rate, `SENSOR_DELAY_GAME`, one listener dropped in `awaitClose`), new `ObserveDeviceAttitudeUseCase`, `TouristInfoViewModel.attitude` keyed on the focused tile (`flatMapLatest`, `WhileSubscribed()`), turn rate sampled every 500 ms into the state. Check: symbols grep; ported `TouristInfoViewModelTest` takes the new constructor argument.
- [x] P3 - Picture and tile: new `AttitudeBoxProjector` + `AttitudeBoxView` (+ ported `AttitudeBoxProjectorTest`), `HeroVisual.Attitude`, `TouristNavigationTileContentBuilder.gyroscope`, `TouristTileContentManager` branch, `TouristTileValueFormatter.degreesPerSecond`, `TouristHeroTileManager.bindAttitude` and visibility, `viewHeroAttitude` in `layout` and `layout-land`, icon `ic_screen_rotation`. Local addition: `TouristInfoActivity.observeData` collects `viewModel.attitude` into `bindAttitude` (see Assumed). Check: id in both layouts, symbols grep.
- [x] P4 - Text: `tourist_tile_gyroscope`, `tourist_unit_degrees_per_second`, `tourist_detail_gyroscope`, `tourist_detail_gyroscope_idle` in `values`, `values-ru`, `values-uk` (FastMediaSorter's wording); the `e-hero` prose of `docs/help/src/dashboard.json` in en/ru/uk; the README product paragraph. Check: grep 3 x 4 names; JSON parses.
- [x] P5 - Ledger: `record-port.ps1` for the 7 new files, `-Refresh` for the 11 carried ones (the three with older committed FastMediaSorter changes - an unused Timber import, an empty `if` - carried those too), `decide-drift.ps1` on `TouristInfoActivity.kt` for the local collector. Check: `check-drift.ps1` lists none of the S0037 files.

### Assumed (unattended, 2026-11-13)

- **The activity collects the attitude stream here, ahead of FastMediaSorter.** FastMediaSorter's ported change exposes `TouristInfoViewModel.attitude` and `TouristHeroTileManager.bindAttitude`, but its `TouristInfoActivity` never collects the stream (grep for `bindAttitude(` / `.attitude` in its `app_v2/src/main/java`: only the definition), so its box never turns. Taken: Tourist adds the three-line collector in `observeData` (lifecycle-bound through `collectOnLifecycle`, so it stops with the screen) and records the divergence with `decide-drift.ps1` next to the activity's ledger row, pinned to the current source version, so the drift check flags the file when FastMediaSorter adds its own collector. Alternative weighed: a byte-exact port that ships a still box - the headline behaviour (owner choice "a 3D box that turns with the phone") would not work. Owner may overrule.
- **Help pages are regenerated at the release, not now.** The prose source is edited; `build-help.ps1` has no recorded time and the pages also need a fresh capture of the dashboard on the emulator, both release items. Alternative: run them now - refused by the 5-second rule. Owner may overrule.

### Implementation record (2026-11-13)

- Files ported new: `domain/model/sensors/AttitudeReading.kt`, `data/sensors/AttitudeReadingSource.kt`, `domain/usecase/sensors/ObserveDeviceAttitudeUseCase.kt`, `ui/tourist/view/AttitudeBoxProjector.kt`, `ui/tourist/view/AttitudeBoxView.kt`, test `ui/tourist/view/AttitudeBoxProjectorTest.kt`, `res/drawable/ic_screen_rotation.xml`. Package renamed; FastMediaSorter ticket ids dropped from comments and the permanent log lines, as in the earlier ports.
- Files carried: `SensorCapability`, `DeviceSensorAvailabilityRepositoryImpl`, `TouristDashboardState`, `TouristTileType`, `TouristInfoViewModel`, `TouristHeroTileManager`, `TouristNavigationTileContentBuilder`, `TouristTileContent`, `TouristTileContentManager`, `TouristTileValueFormatter`, test `TouristInfoViewModelTest`.
- Not ported files (no Tourist counterpart or not in the ledger): both `activity_tourist_info.xml` layouts and the three `strings.xml` were edited by hand with FastMediaSorter's lines; FastMediaSorter's generated help HTML is not ported (Tourist renders its own from `docs/help/src`).
- Parked: S0041 (`english-strings-mojibake`) - 10 double-encoded lines in the working tree's `values/strings.xml`, written by another session on 2026-11-11, not this ticket's.
- Probe: `Timber.d("S0037: attitude flow registered ..")` in `AttitudeReadingSource.attitudeFlow`, present only while the ticket is in `BlockNeedUserTest`.

## 10. Blockers

Blocker: fms-a:S4560

- 2026-11-13, unattended pass: the FastMediaSorter ticket the plan ports from did not exist, so it was filed with `spec-draft.ps1 -Project fms-a` as `fms-a:S4560` (`tourist-gyroscope-3d-panel`, Draft), carrying the owner's request verbatim and the three settled owner choices of section 3.3. Exit 0, line `drafted fms-a:S4560 ... pq=unknown` (the pq eligibility check timed out after 10 s; the capture itself succeeded). This ticket waits in `BlockByOtherTask` until S4560 reaches a release-ready status, then the plan pass ports its files here with ledger rows. No code, no build in this pass.
- 2026-11-13, unattended pass: the blocker was released (dispatcher test `Get-PqXrepoBlock` returned no holding blocker), the plan of section 4 was written and implemented in the same pass. The `Blocker:` token above stays as the record of the dependency; it no longer holds.

## Last Audit

**Date:** 26-11-13 09:30 | **Mode:** static | **Outcome:** Partial
**Counts:** PASS 7 | WARN 0 | FAIL 0 | MANUAL 2 | UNCHECKABLE 0 | EXEMPT 0 | RELEASE 4

- PASS - tile gated by hardware: `TouristTileType.GYROSCOPE(SensorCapability.ATTITUDE)`, `isOffered` reads `availableCapabilities`, `DeviceSensorAvailabilityRepositoryImpl` answers ATTITUDE from the game/geomagnetic rotation vector (grep).
- PASS - lifecycle: `attitude` is `flatMapLatest` on the focused tile with `flowOf(null)` otherwise; the activity collects it through `collectOnLifecycle` (STARTED); the source unregisters in `awaitClose` (static inspection). Note: the sampled turn rate also rides the dashboard state, whose `WhileSubscribed(5_000)` keeps the pair registered up to 5 s after the screen stops - the same grace every dashboard sensor has.
- PASS - strings: 4 names x 3 locales present (grep); help prose `e-hero` names `{ui:tourist_tile_gyroscope}` in en/ru/uk and the JSON parses (`ConvertFrom-Json`, ok).
- PASS - UI source gate: `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui` exit 0, `unified-ui-source: PASS` (0.6 s). expected: PASS | actual: PASS.
- PASS - help gate carries no new finding: `tools/fast-check.ps1 -Area help` exit 1 with 8 findings, all on `stopwatch-result` / `stopwatch` (known red, S0014); none on the dashboard. expected: no dashboard finding | actual: none.
- PASS - port ledger: `check-drift.ps1` 27 -> 16 uncarried rows, none of them an S0037 file; 375 rows (368 + 7). The 16 left are earlier drift for the release's drift check.
- PASS - no permission, no reach change: `SENSOR_DELAY_GAME` (about 50 Hz) is under the `HIGH_SAMPLING_RATE_SENSORS` line; manifest untouched.
- RELEASE - compile: the repository declares no compile command (`commands.compileCheck` absent in `.sza-profile.json`); not built. Release: `pwsh -NoProfile -File tools/release/preflight.ps1`.
- RELEASE - unit suites `AttitudeBoxProjectorTest`, `TouristInfoViewModelTest`, `DeviceSensorAvailabilityRepositoryImplTest`: no recorded time. Release: `tools/release/preflight.ps1` (full Gradle build with every module's unit suite).
- RELEASE - help pages: `tools/help/capture-help.ps1 -Serial emulator-NNNN`, then `tools/help/build-help.ps1`, then `tools/help/check-help.ps1` (freshness on), per `tools/release/README.md`.
- RELEASE - R8: `TouristTileType.GYROSCOPE` is persisted as the focused tile name; proven on the release variant by the preflight build.

### Manual / on-target
- [ ] On a real phone with a gyroscope: focus Gyroscope; the box turns about each axis as the phone turns, the value shows degrees per second, and logcat shows `S0037: attitude flow registered`. Not covered by this pass: any run at all.
- [ ] Focus another tile, then leave the dashboard: the box rests and no new `S0037:` registration appears; on a phone without a rotation sensor the tile is absent.
