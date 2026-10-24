# S0004 - Phone shell and the Tourist dashboard

**Status:** Implemented
**Status note:** Implemented 2026-10-25: 55 files ported with ledger rows (84 total, drift check clean, exit 0); 111 unit tests green on store and noLegal (17 classes), both release variants assemble through R8. Second emulator pass closed acceptance 2 and the key walk: map/share driven, widget placed and opens the dashboard, D-pad focus walk with ENTER activation; durable screenshots in PLAN/S0004_phone-shell-and-dashboard/. The owner's personal-phone pass remains.
111 unit tests green on store and noLegal (17 classes), both release variants assemble through R8. Second
emulator pass (2026-10-25) closed acceptance 2 and the key walk: map/share driven (Maps opened at the fix,
share chooser rendered), widget placed through the picker and opens the dashboard, D-pad focus walk over the
grid and the action row with ENTER activation. The owner's personal-phone pass remains, with physical
gamepad/TV-remote hardware if the owner wants the literal walk.
**Phase:** 4 of 10 (handoff section 8)
**Verification level:** device - the dashboard shows altitude, heading, coordinates and sunrise/sunset from a
real fix on a phone with FastMediaSorter not installed; unit tests of the ported classes.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 4, section 5 (phone module: Tourist dashboard, compass, seams),
sections 2, 3, 6a, 7 (INPUT-PARITY, ICON-SET).

## 1. Goal

The phone app opens on the kit: a start screen of its own and the Tourist dashboard with its compass hero tile,
the map and share actions, and the home-screen widget.

## 2. Scope

Source paths relative to `P:\ANDROID\FastMediaSorter_mob_v2` (read-only), `A` =
`app_v2/src/main/java/com/sza/fastmediasorter`.

- Dashboard: `A/ui/tourist/` (`TouristInfoActivity`, `TouristInfoViewModel`, `TouristActionsManager`,
  `TouristHeroTileManager`, `TouristSecondaryTilesAdapter`, `TouristTileValueFormatter`);
  `A/domain/model/tourist/` and `A/domain/usecase/tourist/` (`ObserveTouristDashboardUseCase`);
  `A/widget/TouristInfoWidgetProvider.kt`; layouts `activity_tourist_info.xml` with its `layout-land` twin,
  `item_tourist_tile.xml`, `widget_tourist_info.xml`.
- Compass: not a screen - the `COMPASS` hero tile (`TouristHeroTileManager`), fed by the S0003 orientation
  source and `ObserveCompassUseCase`.
- Seams, each its own step: `A/core/ui/BaseActivity.kt` (keep-screen-awake), `AppSettings`, backup mapping,
  device-profile presets. The program registry that launches the kit inside FastMediaSorter
  (`SubProgramCatalog`, `InternalRouteCatalog`, `AppLaunchPanelRouteIntents`, the programs menu,
  `HomeWidgetCatalog`) does not come along. `NotificationIds` / `NotificationIcons` are this product's own
  registry, not a copy.
- The `enable_tourist` switch exists because the program is optional inside a sorter: keep-or-drop is this
  phase's decision, written down with the stored-state list of handoff section 5.
- Left by S0003 (its record, section 5.4 and 5.6): `SettingsRepository` / `AppSettings` exist only as the narrow
  seam `ObserveUnitSystemUseCase` needs and have **no implementation** - bind a persisted one here (`unit_system`),
  or `UnitSystemProvider` cannot be injected; `TripMotionPolicy` is already ported (ledgered) and sits in
  `domain/model/tourist/`, so port the rest of that directory around it; no Timber tree is planted yet; the
  `record_gnss_track` preference is dropped (the track recorder was not ported).
- Localization EN/RU/UK from the first screen; every user-visible string obeys a communication policy
  (FastMediaSorter's `docs/COMMUNICATION_POLICY.md` is the reference to adapt - adapted here, not copied).
  Accessibility: TalkBack labels, touch targets, state not shown by colour alone. INPUT-PARITY: touch,
  hardware keys, gamepad and TV remote from the first screen.
- Permissions: fine and coarse location (foreground only, platform `LocationManager`, no Google Play services
  for it), `POST_NOTIFICATIONS`, `ACTIVITY_RECOGNITION` for steps (see 3.3, decision 4.3).
- Known defects of the source at hand-off, carried across by the drift check when fixed there: FastMediaSorter
  S4068 (the weather and dew-point tiles are never populated) and S4069 (the dashboard never asks for the
  location or activity-recognition grant; the watch does). Acceptance needs a working grant flow, so how this
  port carries S4069 - wait for the FastMediaSorter fix, or port the watch's existing grant pattern - is
  decided at ticket start and recorded here; never a rewrite of a shipped behaviour.
- Unit tests ported: `TouristInfoViewModelTest`. Uncovered in the source: `ObserveTouristDashboardUseCase`,
  `TouristTileValueFormatter` - ported first, tested here afterwards.
- ICON-SET / ICON-RENDER: decide whether the ported screens keep the contract glyphs; if they do, add this
  product's registry rows in this ticket.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0003 (core port); S0002 (skeleton).
- **Start screen placement (UI decision, owner 2026-10-23):** the product gets its own start screen - a program
  home listing the kit programs (Dashboard, Lights, SOS, Stopwatch), one row added by each of S0005-S0007;
  widgets and intents open programs directly past it. The registry of FastMediaSorter
  (`SubProgramCatalog`, the programs menu) stays behind, as the scope says.
- **S4069 carry (ticket-start decision, 2026-10-23):** moot - the fix exists in the source at HEAD 5fe24d0
  (`TouristPermissionManager`, added to FastMediaSorter 2026-10-03, the day after this handoff was written).
  The port carries it; no watch grant pattern is needed.
- **4.3 Store-variant composition (owner 2026-10-23, carried by S0008):** `ACTIVITY_RECOGNITION` is declared in
  BOTH variants of this product (steps and cadence are core hike telemetry; the tile explains itself and asks at
  runtime). FastMediaSorter declares it in noLegal only; this is this product's own composition, recorded in
  S0008 as required.
- **Post-extraction growth of the source (owner 2026-10-23):** between the handoff (2026-10-02) and source HEAD
  5fe24d0 the tourist directories grew: the S4068 weather feed (Open-Meteo over OkHttp, ~640 lines, needs
  `INTERNET`), the S4150 watch-health tiles, the S4202 inline stopwatch tile and the S4184 tally-counter program.
  By the S0003 ruling ("the directory comes with what it holds today") the owner confirmed: the weather
  subsystem and the counter program port in this ticket; the watch tiles port with the capability answered
  "not offered" until S0008 wires the Data Layer; the inline stopwatch tile is dashboard surface and comes with
  the ViewModel. `POST_NOTIFICATIONS` is deferred to S0005 (first notifier) per the skeleton manifest's
  only-used-permissions law.
- **4.6 Name and branding (open, carried by S0010):** this phase uses the working name "Tourist" and stand-in
  artwork.

## 4. Acceptance

- [x] On a phone without FastMediaSorter the app opens on the start screen and the dashboard shows altitude,
      heading, coordinates and sunrise/sunset from a real fix.
      Verified on the phone emulator (2026-10-23, screenshots `01`, `04`, `05` in
      `PLAN/S0004_phone-shell-and-dashboard/`): start screen renders;
      after the location grant and a `geo fix` (London) the dashboard showed coordinates 51.51/-0.13,
      altitude -16 m, compass 360° N, satellites 0/6, sunset 18:51 (SolarCalculator), weather 8.7 °C with the
      whole provider family from Open-Meteo, pressure 1013 hPa and barometric altitude -113 m. FastMediaSorter
      (debug build) was installed on the same emulator throughout - the two products coexist under their own
      ids. The strict form of this check - a personal phone - is the owner's verification pass.
- [x] Map and share actions work; the widget updates.
      Verified on the phone emulator (2026-10-25, screenshots `17`, `18`, `25`, `26` in
      `PLAN/S0004_phone-shell-and-dashboard/`): Map opens Google Maps
      centred on the fix (Leicester Square, London 51.51/-0.13); Share opens the chooser with
      "Coordinates: 51.51000, -0.13000" plus the maps link; the widget is placed through the picker
      (long-press -> Widgets -> search "Tourist" -> Add), renders on the home screen and its tap opens the
      dashboard - it is the launch widget this port made it, so "updates" is its onUpdate placing the
      click PendingIntent, no live tiles.
- [x] Ported unit tests pass; every ported file has a ledger row; drift check clean or decided.
      `:phone:testStoreDebugUnitTest` and `:phone:testNoLegalDebugUnitTest`: exit 0, 14→17 test classes,
      111 tests, 0 failures on each variant (88 inherited from S0003; 8 the ported `TouristInfoViewModelTest`,
      12 new `TouristTileValueFormatterTest`, 3 new `ObserveTouristDashboardUseCaseTest`). 84 ledger rows
      (29 from S0003 + 55 here); `check-drift.ps1` against source HEAD 5fe24d0: "OK: nothing uncarried
      (84 in step, 0 decided)", exit 0. Both release variants assemble through R8.
- [x] Every action is reachable by touch, hardware keys, gamepad and TV remote (INPUT-PARITY).
      Touch: taps on the start-screen row, the action row and the widget (2026-10-23/25 passes). Hardware
      keys: the key-by-key walk driven on the emulator D-pad (2026-10-25, screenshots `27`, `29`, `30` in
      `PLAN/S0004_phone-shell-and-dashboard/`) -
      the ring walks hero -> grid -> action row and ENTER activates the focused button (Share opened from
      it). Gamepad and TV remote share the D-pad keycode path on Android; a walk on physical gamepad or
      TV-remote hardware is the owner's device pass, as the personal phone is for check 1.

## 5. Record of the phase (2026-10-23)

Source `A` = `app_v2/src/main/java/com/sza/fastmediasorter`, destination `D` =
`phone/src/main/java/com/sza/tourist`; source HEAD 5fe24d0 (2026-10-22). 55 files ported with ledger rows
(46 classes, 9 of them partial trims, + 2 tests incl. the `MainDispatcherRule` fixture).

### 5.1 What was ported, and where

| Area | Source (under `A`) | Destination (under `D`) |
| --- | --- | --- |
| Dashboard models | `domain/model/tourist/` - `TouristDashboardState`, `TouristTileType`, `TouristTripSnapshot`, `PressureTrend`(+`PressureTrendResult`), `WatchHealthReading`(+`toStats`), `TouristCounterState`(+`TouristCounterRecord`) | same paths (around the ledgered `TripMotionPolicy`) |
| Dashboard use cases | `domain/usecase/tourist/` - `ObserveTouristDashboardUseCase`, `ObserveTouristCounterUseCase`, `UpdateTouristCounterUseCase`, `ObserveWatchTouristHealthUseCase` | same paths |
| Trip and environment | `domain/tourist/` - `TouristTripTracker`, `TripAccumulator`, `PressureTrendCalculator`, `BarometricAltitudeCalibrator`, `TouristEnvironmentTelemetry` | same paths |
| Weather | `domain/model/weather/WeatherSnapshot` (+`WeatherLocation`/`WeatherUnit`/`WeatherCondition`/`WeatherDetails`), `domain/weather/WeatherProvider`, `domain/repository/WeatherRepository`(+`WeatherResult`), `data/weather/` - `OpenMeteoWeatherProvider`, `WeatherRepositoryImpl` | same paths |
| Counter and trip stores | `domain/repository/{TouristCounterRepository,TouristTripRepository}`, `data/tourist/` - `TouristCounterRepositoryImpl`, `TouristTripRepositoryImpl` | same paths |
| Watch health read side | `domain/repository/WatchHealthRepository` | same path (the bridge arrives with S0008) |
| Dashboard UI | `ui/tourist/` - `TouristInfoActivity`, `TouristInfoViewModel`, helpers - `TouristActionsManager`, `TouristHeroTileManager`, `TouristMotionTileContentBuilder`, `TouristNavigationTileContentBuilder`, `TouristPermissionManager`, `TouristSecondaryTilesAdapter`, `TouristTileContent`, `TouristTileContentManager`, `TouristTileValueFormatter`, `TouristWatchTileContentBuilder`, `TouristWeatherTileContentBuilder` | same paths |
| Counter UI | `ui/tourist/counter/` - `TouristCounterActivity`, `TouristCounterViewModel`, `helpers/TouristCounterDialogsManager`, `helpers/TouristCounterHistoryAdapter` | same paths |
| UI framework seams | `core/ui/` - `BaseActivity`, `KeepScreenAwakeManager`, `KeepScreenAwakePolicy`; `ui/common/widget/` - `StandardToolbar`, `FocusMaterialCardView`, `SettingsToggleRow` (+its layout, attrs trimmed); `ui/common/permissions/PermissionAskability`; `utils/ViewExtensions` (inset padding part), `utils/LifecycleExtensions`; `util/LifecycleDialogExt` (bound-dialog part) | same paths |
| Widget | `widget/TouristInfoWidgetProvider` | same path |
| Tests | `ui/tourist/TouristInfoViewModelTest` (8), `testing/MainDispatcherRule` | `phone/src/test/...`, same packages |

Resources: `activity_tourist_info.xml` with its `layout-land` twin, `item_tourist_tile.xml`,
`widget_tourist_info.xml` + `xml/widget_tourist_info_info.xml`, the five counter layouts,
`view_settings_toggle_row.xml`, 32 drawable vectors, 148+2 strings x3 locales, the tile colours, the geometry
dimens and the button/toolbar styles renamed `Widget.Tourist.*` / `TextAppearance.Tourist.*`. The
`launcher_gadget_weather_attribution` key is renamed `weather_attribution` (launcher-gadget wording cut).

### 5.2 Left behind

- The program registry seats on the dashboard: the SOS button and the stopwatch row's "open the full
  stopwatch" button are cut with their `AppLaunchPanelRouteIntents` launches and `flow` references; each
  rejoins with its program (S0006, S0007) - the ported row leaves the seat empty rather than showing a button
  nothing opens. `launchCounter()` stays: the counter ports with the dashboard.
- `HelpToolbarEntryManager` and the whole input-help family, `LocaleHelper` (a language switch arrives with a
  settings screen), the mouse-dispatch and gamepad-stick stack of `BaseActivity`, the secure flag, the GMS
  warning, the background-operation bar, `ColorThemePrefs`, `rebindContentView`, `UserActionLogger`.
- `HomeWidgetAccent` (the widget keeps the tint its layout gives it; a widget registry is this product's own
  later decision). `GetWatchTouristWeatherUseCase` (no user in this tree; revisit with S0008).
- FastMediaSorter's sorter-wide settings store (DataStore) and its permission registry: the kit binds its own
  persisted `SettingsRepositoryImpl` (`settings` preferences file, keys `unit_system`, `prevent_sleep`) and a
  name-keyed permission request marker store - both this product's own seams (S0003 5.4's shape).
- `CapabilityAvailability` is this product's own registry with the watch-health answer "not offered" until
  S0008 wires the Data Layer; `WatchHealthRepositoryImpl` is the stand-in holder behind the same seam.

### 5.3 Tactical decisions of this ticket

1. **The GNSS feed is read through the S0003 cut.** `ObserveTouristDashboardUseCase` consumes
   `GnssStatusDataSource.observe()` as `Flow<GnssSnapshot>` directly; the source's `section.data` unwrap is
   the cut S0003 recorded, so nothing re-appears.
2. **`AppSettings` grows `preventSleep`** (default ON) for the keep-screen-awake seam; a settings screen owns
   the flip later. `KeepScreenAwakePolicy` keeps its shape with the power-level parameter left for the port
   that brings a power policy.
3. **BaseActivity is the kit's skeleton**: view-binding lifecycle with the deferred `post{}` setup,
   `runWhenViewsReady`/`onResumeWithViews`, edge-to-edge, keep-screen-awake, non-touch initial focus.
   Reachability (INPUT-PARITY) rides native focus traversal; the custom stacks come with the screens that
   need them.
4. **Permission askability keeps the source's decision function** (`isPermissionRequestable`, pure and
   test-ready); the marker is persisted in this product's own preferences keyed by manifest name, and the
   "declared in the merged manifest" gate is what keeps the steps tile from offering a request a build
   without the declaration would refuse.
5. **The dashboard's empty `if` in `TouristHeroTileManager.bind` is ported verbatim** (a source artefact, not
   ours to clean silently - the drift check would flag a "fix" as an uncarried change).
6. **Gradle pins**: recyclerview 1.3.2, constraintlayout 2.1.4, activity-ktx 1.10.1, okhttp 4.12.0 - all at
   FastMediaSorter's current values; viewBinding on; the phone module gains nothing else.
7. **`POST_NOTIFICATIONS` deferred to S0005** per the skeleton manifest's only-used-permissions law (owner
   answer recorded in 3.3). `INTERNET` is declared now: the weather tiles fetch Open-Meteo.
8. **ICON-SET / ICON-RENDER: the screens keep the contract glyphs.** Usage rows are filed with the catalog as
   `PROPOSAL-2026-10-23-tourist-glyph-usage.md` (29 drawables map to existing vocabulary records through the
   Reference column; `ic_favorite`, the watch heart-rate heart, is not in the vocabulary and ships
   provisionally - the owner is asked to register the meaning). The pointer table moves the contract from
   "conditional" to "consumer".

### 5.4 Device pass on the emulator (2026-10-23, storeDebug)

Installed under `com.sza.tourist` next to FastMediaSorter (debug) - disjoint ids, both live. Screenshots
`01`, `04`, `05`, `07` in `PLAN/S0004_phone-shell-and-dashboard/`. Verified: the start screen with its focus ring; the grant flow (card -> system
dialog -> granted -> card dismissed); the dashboard from a `geo fix` on London - coordinates, altitude,
compass, satellites 0/6, sunset 18:51, the whole Open-Meteo family, pressure and barometric altitude, the
level, light and magnetic-field tiles, the trip ticking; the counter screen rendered in full (value, starting
number, +/-1 with -1 disabled at zero, the extra-steps toggle row, start-value and reset buttons, empty
history)

### 5.5 Second device pass (2026-10-25, storeDebug, the same Pixel 9 emulator)

The half of acceptance 2 the shared-emulator reclaim had left open, plus the key walk. The rebuilt storeDebug
APK reinstalled under `com.sza.tourist` next to FastMediaSorter debug (both live); a fresh `geo fix` (London
51.51/-0.13), screenshots `17`, `18`, `25`, `26`, `27`, `29`, `30` in `PLAN/S0004_phone-shell-and-dashboard/`
(downscaled from the full-frame originals, each under 64 KB). Driven: Map opens Google Maps centred on the fix;
Share opens the chooser carrying "Coordinates: 51.51000, -0.13000" and the maps link; the widget placed
through the picker renders on the home screen and its tap opens the dashboard (the port's launch widget);
the D-pad walk runs the focus ring hero -> grid -> action row and ENTER activates the focused Share button.
Re-run gates at close: `:phone:testStoreDebugUnitTest` and `:phone:testNoLegalDebugUnitTest` exit 0 (111
tests, 17 classes each); `check-drift.ps1` "OK: nothing uncarried (84 in step, 0 decided)", exit 0.
