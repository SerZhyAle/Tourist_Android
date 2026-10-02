# Bootstrap handoff - Tourist for Android

The starting brief for this repository, written on 2026-10-02 by the session that decided the repository
boundary (FastMediaSorter ticket S4070, `P:\ANDROID\FastMediaSorter_mob_v2\PLAN\S4070_tourist-standalone-product.md`).
It is an input, not a living document: once this repository has its own ticket store, the open items
below become tickets here, and this file is kept as the record of where the product came from.

"Tourist" is the working name (Russian "Турист"); the store name is an open owner decision (section 4.6).

## 1. Decision

- Tourist for Android lives in this separate repository (ADR-1 of S4070, accepted by the owner on
  2026-10-02, by the precedent of StreamsPlayer for Android, `P:\ANDROID\StreamsPlayer_mob`).
- Installable packages: phone app and Wear OS app (one application id, one Data Layer pair), plus a watch
  face if section 4.4 keeps it in the first release.
- The only coupling to FastMediaSorter is the shared contracts catalog. No shared code, no shared Gradle
  module, no shared release.
- Why not a module inside FastMediaSorter: its release orchestrator stamps one version pair for the whole
  repository and its release-scope gates measure the whole tree, so a second product inside would double
  every release run and tie two release calendars together for good. The reasoning is the same as for
  StreamsPlayer: `P:\ANDROID\FastMediaSorter_mob_v2\PLAN\S4053_streamsplayer-standalone-product\research\01__repo-boundary.md`.
- Why a kit and not one program: FastMediaSorter ticket S4024 (section 5b) found the tourist dashboard
  alone too thin for a store listing (compasses and altimeters crowd the category), and the coherent
  "for a hike" kit - dashboard, compass, SOS, a light no wet touch can switch off, a flashlight, a
  stopwatch, plus the watch - a real differentiator.

## 2. Product

Goals:

1. A standalone phone app that opens on the kit: the Tourist dashboard (speed, max speed, altitude, heading,
   coordinates, satellites, sunrise and sunset, daylight, steps and trip distance, open in an external map,
   share the location), SOS, the water flashlight, the front (screen) flashlight, the camera torch, the
   stopwatch - without FastMediaSorter installed.
2. A standalone Wear OS app: the glanceable Tourist dashboard with the hardware-key touch lock, SOS, the
   water flashlight and the stopwatch with its ongoing activity; SOS started on either side reaches the
   other over the product's own Data Layer pair.
3. Independent release: own application ids, own signing key, own release cycle.
4. **No reinvented wheel, no drift.** Every class of the kit is ported from FastMediaSorter in full, not
   rewritten, and stays in step with it afterwards (section 6a).
5. Imperial units and the 12-hour clock as FastMediaSorter ships them, on phone and watch.

Non-goals:

- Media sorting, browsing, players, streams, cloud and network resources - Tourist is not a sorter.
- FastMediaSorter launcher-mode gadgets (compass, weather, sunrise and dew point on the desktop) - they
  live in the launcher, not in the kit.
- New tourist capabilities that exist nowhere today: weather service, embedded maps, route tracking,
  health metrics, tourist watch complications. This is an extraction, not new development; a new
  capability is a ticket of its own after the first release.
- Rewriting a behaviour that FastMediaSorter already ships.
- Automatic migration of FastMediaSorter user data (the kit stores only a handful of preferences and no
  database rows - section 5, "Stored state").

## 3. Owner decisions already taken

- Variants: two - a store variant and a noLegal sideload variant, after the FastMediaSorter watch-module
  precedent. The 8-flavor FastMediaSorter matrix is not inherited. Flavor isolation stays: the interface in
  `src/main`, the implementation in the variant source set, one DI module per variant.
- API level: minSdk is a tactical decision with 26 (phone) and 28 (watch) as the reference; targetSdk is
  whatever Play requires at the first release.
- Wear: the phone and watch apps share one application id (Data Layer requires the same id and signing key
  on both sides).
- Localization: EN/RU/UK from the first release; other locales optional. Every user-visible string obeys a
  communication policy (FastMediaSorter's `docs/COMMUNICATION_POLICY.md` is the reference to adapt).
- Accessibility: TalkBack labels, touch targets, state not shown by colour alone; input parity for touch,
  hardware keys, gamepad and TV remote (INPUT-PARITY adopter). The water flashlight's "no wet touch can
  switch it off" rule is the product's signature and is ported exactly.
- Location is foreground-only, read through the platform `LocationManager` while a screen is open - no
  background location, no Google Play services dependency for it.

## 4. Open owner decisions

Ask each one once, in question-with-options form, before the phase that needs it - never before.

1. **Frozen anchors.** Proposed: `com.sza.tourist` for phone + watch (shared id),
   `com.sza.tourist.watchface` for the watch face if it ships. Frozen forever after the first shipped
   package; confirm before the first build that leaves this machine.
2. **Signing.** A new key, kept outside the repository. Precedents: `P:\ANDROID\FastMediaSorter_credentials`,
   `P:\ANDROID\StreamsPlayer_credentials`; proposed `P:\ANDROID\Tourist_credentials`. Play App Signing or
   self-managed - owner's call.
3. **Store-variant composition.** In FastMediaSorter the watch store build hides SOS, the water flashlight
   and the Tourist dashboard (`offersHealthFeatures=false`, `offersScreenTakeoverPrograms=false`, owner
   decision S3178 of 2026-09-16), and the phone declares `ACTIVITY_RECOGNITION` (steps) in noLegal only.
   In a product that *is* the kit, hiding them in the store variant leaves an empty watch app. Options:
   keep the S3178 split (store watch = stopwatch only), lift it for this product after a Play policy check
   of each capability, or ship the store variant phone-only first. Decides the manifests, the capability
   gate port and the Play declarations; asked before the wear phase.
4. **Watch face.** FastMediaSorter's WFF watch face has no tourist content (no altitude, compass, sunrise
   or steps complication; the only tourist link is a shortcut slot that opens the program). Options: no
   watch face in the first release; a rebranded clone; a clone plus tourist complications (new code - an
   explicit exception to the "extraction only" non-goal).
5. **Distribution of the first release.** Play (phone + wear) plus a sideload variant at once, or
   GitHub/sideload first. Decides the signing flow, the version family registration and whether an
   INSTALL-TRUST page is needed. Note from S4024: a personal developer account created after November 2023
   puts every new app through a closed test (12 testers, 14 days) before production.
6. **Name and branding.** "Tourist" is the working name. The store name, icon and listing text - and
   whether the product names FastMediaSorter ("all this and more in FastMediaSorter", the S4024 Spam-policy
   advice) - are the owner's.
7. **GitHub remote.** Repository name and visibility. Nothing is pushed until the owner names it.
8. **License.** FastMediaSorter's sibling StreamsPlayer for Android chose MIT.
9. **Site.** Whether the product gets a page and joins the family map (SITE-FAMILY-MAP) - an owner
   decision, not a consequence of adopting the canon.

Whether FastMediaSorter keeps its own kit programs after Tourist ships is decided in the FastMediaSorter
repository, not here. Nothing in this product depends on it.

## 5. Extraction source map

Source repository: `P:\ANDROID\FastMediaSorter_mob_v2` - **read-only from this repository's sessions**.
Copy, then clean: port a layer, rename the package `com.sza.fastmediasorter` -> `com.sza.tourist` (after
anchor 1 is confirmed), cut every reference to sorter surfaces, port its unit tests with it. Line counts
below were measured on disk on 2026-10-02; re-measure, do not trust them later.
`A` = `app_v2/src/main/java/com/sza/fastmediasorter`, `W` = `wear/src/main/java/com/sza/fastmediasorter/wear`.

Where to read first in the source:

- `docs/ALL_FEATURES.jsonl` - the shipped capability inventory. About 30 records mention the kit (areas
  `Programs & Tools`, `Wear OS`, `Localization`, `Permissions`); filter `status == "active"` and read the
  `flavors` field. Records with area `Launcher` describe desktop gadgets and stay behind.
- `docs/FLAVOR_MATRIX.md` - no capability flag gates the kit on the phone (all eight flavors);
  `SUPPORT_WEAR_COMPANION` gates the SOS relay.
- Class catalog: `pwsh -NoProfile -File dev/CATALOG/scripts/query.ps1 -ClassMatches "*Tourist*"` (and
  `*Sos*`, `*Flashlight*`, `*Stopwatch*`) from the source repo root (regenerate first with
  `scripts/catalog_sync.ps1 -Module app_v2` / `-Module wear` if the index is missing; it is a gitignored
  local file).

Phone module (about 7.6k lines in the kit packages, before the seams):

- Tourist dashboard: `A/ui/tourist/` (6 files, 841 lines: `TouristInfoActivity`, `TouristInfoViewModel`,
  helpers `TouristActionsManager`, `TouristHeroTileManager`, `TouristSecondaryTilesAdapter`,
  `TouristTileValueFormatter`); `A/domain/model/tourist/` + `A/domain/usecase/tourist/` (3 files, 281
  lines, `ObserveTouristDashboardUseCase`); `A/widget/TouristInfoWidgetProvider.kt`; layouts
  `activity_tourist_info.xml` with its `layout-land` twin, `item_tourist_tile.xml`, `widget_tourist_info.xml`.
- Compass: not a screen - the `COMPASS` hero tile of the dashboard (`TouristHeroTileManager`), fed by
  `A/data/sensors/OrientationReadingSource.kt` and `A/domain/usecase/sensors/ObserveCompassUseCase.kt`.
- Sensors and sun: `A/data/sensors/` (5 files, 399 lines: motion, orientation, step count, availability),
  `A/domain/usecase/sensors/` + `A/domain/model/sensors/` (9 files, 283 lines), `A/domain/util/SolarCalculator.kt`.
  `SensorSeriesRepositoryImpl` and the `SensorSeriesPointEntity` Room table serve launcher gadgets only -
  leave them behind.
- GNSS (satellite count, used satellites): `GnssStatusDataSource` and its use cases live under Network
  Monitor (7 files, 667 lines with the track recorder) - port what the dashboard reads, cut the
  `MonitorSection` / `SectionAvailability` coupling.
- SOS: `A/ui/sos/` (7 files, 936 lines: `SosActivity`, `SosService` - foreground type `mediaPlayback`,
  `SosViewModel`, `SosSoundGenerator`, `SosTorchManager`, `SosCommandFromWatchHandler`,
  `SosStartFromWatchNotifier`); `A/domain/model/sos/`, `A/domain/usecase/sos/`,
  `A/domain/usecase/wear/SendSosCommandToWatchUseCase.kt` (7 files, 368 lines).
- Flashlights: `A/ui/flashlight/` (7 files, 566 lines: `WaterFlashlightActivity` with
  `WaterFlashlightLockdownManager`, `FrontFlashlightActivity` and its ViewModel, `FlashlightToggleActivity`);
  the notification-shade torch `A/core/notification/FlashlightShortcut*` +
  `A/ui/settings/helpers/OperationsFlashlightShortcutPermissionManager.kt` (5 files, 418 lines);
  `A/widget/FrontFlashlightWidgetProvider.kt`, `A/widget/WaterFlashlightWidgetProvider.kt`.
- Stopwatch: `A/ui/stopwatch/` (13 files, 1922 lines); `A/domain/model/stopwatch/`,
  `A/domain/usecase/stopwatch/`, `StopWatchListeningUseCase.kt`,
  `A/data/repository/settings/StopwatchSettingsStore.kt` (7 files, 226 lines);
  `A/widget/StopwatchWidgetProvider.kt` + `A/widget/helpers/StopwatchWidgetStateStore.kt`; the music
  picker `MediaStoreMusicTrackRepository`, `MusicTrackRepository`, `A/di/MusicTrackModule.kt`.
- Units: `A/core/format/QuantityFormatter.kt`, `A/domain/model/UnitScale.kt`, `A/domain/model/Quantity.kt`,
  `A/domain/unit/UnitSystemProvider.kt`, `ObserveUnitSystemUseCase`.

Watch module (about 5k lines, Compose):

- Tourist: `W/ui/apps/tourist/` (8 files, 1424 lines, incl. `TouristLockOverlay` - the hardware-key touch
  lock); `W/domain/tourist/*` and three Observe/Reset use cases (7 files, 299 lines);
  `W/data/tourist/AndroidWearTouristRepository.kt` (altitude from the barometer, temperature from an ambient
  or vendor sensor), `W/di/WearTouristModule.kt`.
- SOS: `W/ui/apps/sos/*`, the `Sos*` models in `W/domain/model`, `W/domain/sos/SosSyncBus.kt`,
  `SendSosCommandToPhoneUseCase` (8 files, 900 lines).
- Water flashlight: `W/ui/apps/waterflashlight/` (3 files, 322 lines; `WaterFlashlightExitGate` - a held
  key or three presses). The watch has no camera torch by design.
- Stopwatch: `W/ui/apps/stopwatch/*`, `W/domain/stopwatch/*`, the session repository and two use cases
  (19 files, 1615 lines), incl. `WearStopwatchOngoingNotificationManager`.
- Heading: `W/data/heading/CompassHeadingDataSource.kt` - check whether the ported screens need it.

Phone <-> watch Data Layer - SOS only:

- Message paths `/fms/watch/sos/start`, `/fms/watch/sos/stop`, `/fms/phone/sos/start`,
  `/fms/phone/sos/stop`; payload the `SosMode` name (`ALL`, `SOUND_ONLY`, `LIGHT_ONLY`).
- Hand-mirrored constants: `A/service/WearDataLayerPaths.kt:191-205` and `W/data/wear/WearDataLayerPaths.kt:187-201`.
  Incoming routing: phone `app_v2/src/wearGms/.../PhoneWearListenerService.kt`, watch
  `W/data/wear/WatchWearListenerService.kt`.
- The unit system reaches the watch through FastMediaSorter's settings push (`WearSettingsPayload.unitSystem`,
  `PushWearSettingsUseCase`) - port only that field, not the whole settings sync.
- The paths are private to one application id, so the `/fms/` prefix becomes this product's prefix during
  the port; the shape and payload stay.

Permissions and services:

- Phone: `CAMERA` (torch), fine and coarse location, `WAKE_LOCK`, `VIBRATE`, `FOREGROUND_SERVICE` +
  `mediaPlayback` (SOS), `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` (torch shortcut), `ACTIVITY_RECOGNITION`
  (noLegal only today). No SMS, no call, no background location.
- Watch: `VIBRATE`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, fine and coarse location (foreground only); noLegal adds
  `BODY_SENSORS`, `health.READ_HEART_RATE`, `ACTIVITY_RECOGNITION`, `USE_FULL_SCREEN_INTENT`, which the store
  manifest removes (`wear/src/standard/AndroidManifest.xml`). Section 4.3 decides what this product keeps.

Stored state (no Room table belongs to the kit):

- Phone preferences: `ProgramsSettingsStore.kt:22-49` (`enable_tourist`, `enable_sos`, `sos_mode`,
  `front_flashlight_*`, `water_flashlight_enabled`, `flashlight_shortcut_notification_enabled`,
  `enable_stopwatch`, `record_gnss_track`), `unit_system`, `StopwatchSettingsStore.kt:20-23`,
  `StopwatchWidgetStateStore.kt:24-26`. The `enable_*` switches exist because the programs are optional
  inside a sorter; in a product that is the kit, each is a keep-or-drop decision of its phase.
- Watch preferences: `wear_unit_system`, `wear_stopwatch_participant_count`, `wear_stopwatch_last_result`,
  `wear_stopwatch_session` (`WearPreferenceKeys.kt`).
- Trip distance, max speed and the step base are in memory on both sides.

Unit tests to port with their classes:

- Phone: `TouristInfoViewModelTest`, `SosSirenWaveformTest`, `SosSoundGeneratorTest`, `StopwatchViewModelTest`
  and the five `ui/stopwatch/helpers` tests, `SolarCalculatorTest`, `QuantityFormatterTest`, `UnitScaleTest`,
  `UnitSystemProviderTest`, `GnssTrackRecorderTest` if the recorder is ported.
- Watch: `WearSolarCalculatorTest`, `SosSirenWaveformTest`, `WaterFlashlightExitGateTest`, the six stopwatch
  tests, `WearAppCatalogTest`, and the variant boundary tests `StoreBoundaryTest` / `NoLegalBoundaryTest` as
  the model for this product's own.
- Uncovered in the source today: `ObserveTouristDashboardUseCase`, `TouristTileValueFormatter`, the SOS
  service and view models, the flashlight classes, the three reading sources, `AndroidWearTouristRepository`,
  the watch `TouristViewModel`. Port first, then add tests here; never change behaviour to make a test pass.

The seams a name search cannot see - plan each as its own step:

- The camera torch lives in `A/core/screencapture/gesture/DeviceActionHandler.kt`, shared by
  `SosTorchManager`, `WaterFlashlightActivity` and the torch toggle, next to unrelated gesture and
  brightness code - extract the torch part only.
- `A/core/ui/BaseActivity.kt` (keep-screen-awake), `AppSettings`, backup mapping, device-profile presets.
- The program registry that launches the kit inside FastMediaSorter (`SubProgramCatalog`,
  `InternalRouteCatalog`, `AppLaunchPanelRouteIntents`, the programs menu, `HomeWidgetCatalog`) does not
  come along: this product needs its own start screen - a UI placement decision settled before its phase.
- Watch: `W/MainActivity.kt` routes, `WearRoutes` / `WearLaunchRoutes`, `WearAppCatalog`, `WearApp`,
  `WearDestinationId`, `WearRestrictedCapabilities` (the store-variant gate of section 4.3), the theme and
  icon/accent catalogs, `KeepScreenOnEffect`, `WearUnitDateTimeFormatter`; libraries
  `accompanist-permissions` and `androidx.wear.ongoing`.
- `NotificationIds` / `NotificationIcons` - this product's own registry, not a copy of the sorter's.

Known defects of the source at hand-off (FastMediaSorter tickets, carried across by the drift check when
fixed there):

- S4068 - the phone dashboard's weather and dew-point tiles are never populated.
- S4069 - the phone dashboard never asks for the location or activity-recognition grant (the watch does).

Tech stack reference: the source repo's `gradle/libs.versions.toml`, `docs/TECH_STACK.md` and its pins.
Re-read the file for the current values. Android SDK on this machine: `C:\Users\serzh\AppData\Local\Android\Sdk`.

## 6. Sibling product

`P:\ANDROID\StreamsPlayer_mob` - StreamsPlayer for Android, extracted from FastMediaSorter the same day by
the same method (S4053). **Read-only reference** for how a sibling adopted the canon, laid out its ticket
store, its port ledger and its drift-check script; reuse its decisions where they fit, never its code.

## 6a. Staying in step with FastMediaSorter

The owner's requirement for these extractions: the product gets the whole code of what it carries, and its
behaviour never quietly diverges from FastMediaSorter's. ADR-1 rules out shared code, so the guarantee
comes from mechanisms, not from a shared module:

1. **Full port, never a rewrite.** Every class listed in section 5 is copied into this repository's source
   tree and cleaned, with its unit tests. Reimplementing a ported behaviour from scratch is refused in
   review.
2. **Port ledger and drift check.** Every ported file is recorded in a committed ledger: source path in
   FastMediaSorter, the source file's SHA-256 at port time, the destination path, the port date. A script
   in this repository re-hashes the source files and lists every one that changed in FastMediaSorter since
   its port - the to-do list for carrying a fix across. It runs at the start of every release of this
   product; a change left uncarried is a decision written next to its ledger row, never silence.
3. **No behaviour contract yet.** Unlike streams and broadcast, the kit's behaviour is written in no
   catalog contract, so the ledger is the whole guarantee. If a behaviour must be shared with a third
   product (for example the SOS signal or its Data Layer messages), it is written as a contract in the
   catalog first - not as a document in this repository.

## 7. Contracts

The catalog is `P:\Contracts`; name it in exactly one place in this repository's rules, cite contracts by
id everywhere else. Versions as read from `P:\Contracts\_meta\REGISTRY.md` on 2026-10-02 - re-read the
registry, do not trust these numbers later.

- `PACKAGE-VERSIONING` 0.2 (`package-versioning/`) - Android profile. The package families (phone and watch
  share one application id and therefore need disjoint bands; the watch face, if it ships, has its own id)
  must be added to the band table **before the first package ships** (a MINOR amendment, written in the
  catalog first).
- `INPUT-PARITY` 0.3 - adopter.
- `ICON-SET` / `ICON-RENDER` - consumer, if the ported screens keep FastMediaSorter's contract glyphs.
- `DIAGNOSTIC-REPORT` 0.12 - producer, only if the product exports a diagnostic log bundle.
- `INSTALL-TRUST` 1.1 - only if a sideload page ships (section 4.5).
- `REPO-STAMP` / `HARNESS-PROFILE` / `REPO-LAYOUT` / `RULE-DELIVERY` (`rule-adoption/`) - this repository is
  an adopter; the canon adoption run covers them.
- `PAGE-CONTENT` / `PAGE-STYLE` / `SITE-FAMILY-MAP` - only if the owner decides on a site (section 4.9).

## 8. Suggested phase order

Each phase is its own ticket in this repository's store, with its own verification level.

1. Canon adoption (`/sza:adopt-canon`, new-repo mode), contract pointers (`/sza:contract-sync`), ticket
   store and release queue.
2. Gradle skeleton: phone and wear modules (and the watch face module if section 4.4 keeps it), two
   variants, version stamping per PACKAGE-VERSIONING, empty apps that build and install. Frozen anchors
   confirmed first (section 4.1). The port ledger and its drift-check script (section 6a) exist before the
   first ported file.
3. Core: sensor reading sources, GNSS status, solar calculator, units and formatting, the torch extracted
   from `DeviceActionHandler` - ported with their unit tests, no UI.
4. Phone shell and the Tourist dashboard: the start screen (placement settled before implementation), the
   dashboard with its compass hero tile, map and share actions, its home-screen widget.
5. Phone lights: water flashlight with its lockdown, front flashlight, camera torch toggle, the
   notification-shade torch, their widgets.
6. Phone SOS: activity, foreground service, siren, torch blinking.
7. Phone stopwatch: multi-participant screen, music, widget.
8. Wear app: Tourist dashboard with the touch lock, SOS with the Data Layer pair (start and stop from either
   side), water flashlight, stopwatch with its ongoing activity; store/noLegal gating per section 4.3.
9. Watch face (only if section 4.4 keeps it).
10. Release pipeline and a first release dry-run (build, pre-release sweep, drift check, no upload).

## 9. Acceptance - the product is done when

1. The phone app is installed under its own application id with FastMediaSorter not installed: the start
   screen opens; the dashboard shows altitude, heading, coordinates and sunrise/sunset from a real fix; the
   water flashlight survives touches and leaves only by its exit rule; the torch toggles; SOS sounds and
   blinks; the stopwatch runs.
2. The watch app is installed: the Tourist dashboard and its touch lock work, the stopwatch shows its
   ongoing activity, and with the Tourist phone app paired an SOS started on either device starts and stops
   on the other.
3. The watch face, if it ships, installs and shows the time with the product branding.
4. Every package carries versionName/versionCode from its registered PACKAGE-VERSIONING family, is signed
   with the product's own key, and installs and updates independently of FastMediaSorter.
5. Building and releasing Tourist needs no FastMediaSorter build, and the other way round.
6. The port ledger covers every ported file, and the drift check run at the release candidate reports zero
   uncarried FastMediaSorter changes without a written decision.

## 10. Do not

- Edit `P:\ANDROID\FastMediaSorter_mob_v2` or `P:\ANDROID\StreamsPlayer_mob` from this repository's sessions.
- Edit a contract anywhere but the catalog, and never silently - follow `P:\Contracts\_meta\RULES.md`.
- Copy a contract document into this repository - a pointer under `docs/contracts/` only.
- Reimplement a kit behaviour that can be ported.
- Hand-copy the FastMediaSorter harness scripts (`scripts/spec_catalog`, locks, leases, chat). The harness
  ships with the `sza` plugin and is adopted, not copied.
- Copy the FastMediaSorter flavor matrix, its launcher, sorter, player or network-resource code.
- Commit signing material or secrets.
- Tag, upload or publish anything unless the owner asked for that exact release.
