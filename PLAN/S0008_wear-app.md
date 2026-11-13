# S0008 - Wear app: dashboard with touch lock, SOS pair, water flashlight, stopwatch

**Status:** BlockNeedUserTest
**Status note:** Implemented 2026-10-25: 175 watch + 13 phone ledger rows (drift clean, 367 in step); wear store 204 / noLegal 203, phone 262 / 262 unit tests green; all four release variants assemble through R8; both watch variants walked on Wear_OS_Small_Round (API 37) and the store variant on Wear OS 2 (API 28). Owner check: SOS start/stop across a paired phone and watch - no pair could be built here
**Phase:** 8 of 10 (handoff section 8)
**Verification level:** device - the watch app on a Wear OS emulator or watch, store and noLegal variants, with
the Tourist phone app paired for the SOS check; the store variant's merged manifest read.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 8, section 5 (watch module; Data Layer; permissions), sections 3,
4.3, 6a.

## 1. Goal

The Wear OS app: the glanceable Tourist dashboard with the hardware-key touch lock, SOS started on either side
reaching the other over this product's own Data Layer pair, the water flashlight and the stopwatch with its
ongoing activity; store and noLegal gating settled per owner decision 4.3.

## 2. Scope

Source paths relative to `P:\ANDROID\FastMediaSorter_mob_v2` (read-only), `W` =
`wear/src/main/java/com/sza/fastmediasorter/wear`.

- Tourist: `W/ui/apps/tourist/` (incl. `TouristLockOverlay`, the hardware-key touch lock), `W/domain/tourist/*`
  with the three Observe/Reset use cases, `W/data/tourist/AndroidWearTouristRepository.kt` (altitude from the
  barometer, temperature from an ambient or vendor sensor), `W/di/WearTouristModule.kt`.
- SOS: `W/ui/apps/sos/*`, the `Sos*` models in `W/domain/model`, `W/domain/sos/SosSyncBus.kt`,
  `SendSosCommandToPhoneUseCase`; watch-side routing in `W/data/wear/WatchWearListenerService.kt` and the
  mirrored `W/data/wear/WearDataLayerPaths.kt` (lines 187-201 at hand-off), equal to the phone's block chosen in
  S0006 - the check written there covers both.
- Water flashlight: `W/ui/apps/waterflashlight/` (`WaterFlashlightExitGate` - a held key or three presses). The
  watch has no camera torch by design.
- Stopwatch: `W/ui/apps/stopwatch/*`, `W/domain/stopwatch/*`, the session repository and two use cases, incl.
  `WearStopwatchOngoingNotificationManager`.
- Heading: `W/data/heading/CompassHeadingDataSource.kt` - check whether the ported screens need it.
- Unit system: port only `WearSettingsPayload.unitSystem` and its use from `PushWearSettingsUseCase`, not the
  whole settings sync. Imperial units and the 12-hour clock on the watch too.
- Seams, each its own step: `W/MainActivity.kt` routes, `WearRoutes` / `WearLaunchRoutes`, `WearAppCatalog`,
  `WearApp`, `WearDestinationId`, `WearRestrictedCapabilities` (the store-variant gate of 4.3), the theme and
  icon/accent catalogs, `KeepScreenOnEffect`, `WearUnitDateTimeFormatter`; libraries `accompanist-permissions`
  and `androidx.wear.ongoing`.
- Watch permissions: `VIBRATE`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, fine and coarse location (foreground only);
  noLegal adds `BODY_SENSORS`, `health.READ_HEART_RATE`, `ACTIVITY_RECOGNITION`, `USE_FULL_SCREEN_INTENT`,
  which the store manifest removes (`wear/src/standard/AndroidManifest.xml` in the source). What this product
  keeps is decided by 4.3.
- Stored state: `wear_unit_system`, `wear_stopwatch_participant_count`, `wear_stopwatch_last_result`,
  `wear_stopwatch_session`.
- Unit tests ported: `WearSolarCalculatorTest`, `SosSirenWaveformTest`, `WaterFlashlightExitGateTest`, the six
  stopwatch tests, `WearAppCatalogTest`, and the variant boundary tests `StoreBoundaryTest` /
  `NoLegalBoundaryTest` as the model for this product's own. Uncovered in the source: the watch
  `TouristViewModel` and `AndroidWearTouristRepository` - ported first, tested here afterwards.
- A secondary form-factor artifact is judged against its own store guidelines: a criterion that rests on a
  permission or a notification (the ongoing activity) is claimed only after reading the store variant's merged
  manifest and walking that variant on the smallest supported screen and the oldest supported OS.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002 (skeleton), S0003 (core), S0006 (the phone side of the SOS pair).
- **4.3 Store-variant composition (owner 2026-10-25, asked before this phase):** in FastMediaSorter the watch
  store build hides SOS, the water flashlight and the Tourist dashboard (`offersHealthFeatures=false`,
  `offersScreenTakeoverPrograms=false`, owner decision S3178 of 2026-09-16 in that repository), and the phone
  declares `ACTIVITY_RECOGNITION` in noLegal only. Answers, one per capability:
  - **Tourist dashboard on the watch: noLegal only** (the S3178 split kept for it). The store watch carries no
    dashboard, so it declares none of `BODY_SENSORS`, `health.READ_HEART_RATE`, `ACTIVITY_RECOGNITION` and
    the location pair either - the dashboard is the only reader of all five. The phone side is a separate
    answer, already given in S0004 (`ACTIVITY_RECOGNITION` in both variants); the watch-health tiles of the
    phone dashboard are therefore fed by the noLegal watch only.
  - **SOS and the water flashlight: both variants, ported as they are** - the "no wet touch can switch it off"
    rule is the product's signature (CLAUDE.md section 3) and is not weakened for the store. Known review risk,
    accepted by the owner: Wear OS review items WO-V3 (swipe to close) and WO-V13 (full white screen) and the
    ~3 Hz strobe at maximum alarm volume. `offersScreenTakeoverPrograms` therefore answers `true` in both
    variants but stays a real capability with its own boundary test, so a store review that refuses the pair
    is one line to turn back into the S3178 behaviour.
  - Stopwatch: both variants (needs no permission beyond the ongoing-activity notification).
  Consequence for the manifests: store watch = `POST_NOTIFICATIONS` (API 33+) alone; noLegal adds
  `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `BODY_SENSORS` (up to API 35), `health.READ_HEART_RATE`
  (API 36+), `ACTIVITY_RECOGNITION`. `USE_FULL_SCREEN_INTENT` is not declared in either: it served the
  sorter's automatic listening request, which this product does not carry. `VIBRATE` and `WAKE_LOCK`, which
  the handoff lists for the watch, are not declared either - see tactical decision 3 in section 5.
  - **Shade lock (S2812) follows the source, not the question:** the answer to "hold the system shade shut
    under the water flashlight" was not asked and is carried as FastMediaSorter has it - sideload yes, store
    no (Play admits lock task mode for dedicated devices only). It completes the signature rule for the
    shade; the store edition keeps the rule for the screen and the keys without it.

## 4. Acceptance

- [x] The watch app installs; the Tourist dashboard and its touch lock work; the stopwatch shows its ongoing
      activity.
      Verified on the Wear OS emulator `Wear_OS_Small_Round` (384x384, API 37, noLegal debug, 2026-10-25;
      screenshots `01`-`05` in `PLAN/S0008_wear-app/`): the program list lists Tourist, Water flashlight, SOS
      and Stopwatch; the dashboard asked for the heart-rate grant itself and showed speed, GPS altitude
      (94 m at a `geo fix`) and compass (359 N); the lock button engaged "Press a watch button to unlock", a
      tap and a swipe left the screen where it was and `KEYCODE_STEM_1` lifted the lock. The stopwatch opened
      through the launch-target intent and `dumpsys notification` showed id 4201, channel
      `wear_stopwatch_ongoing`, category `stopwatch`, flags `ONGOING_EVENT`; Stop removed it. The water
      flashlight survived a tap, a swipe and a key, and three quick back presses left it (`05`). SOS
      armed a `USAGE_ALARM` `AudioTrack` (state started) and the strobe, and three back presses stopped both
      (`06`, `07`). Not done on a physical watch: a personal watch is the owner's strict form of this check.
- [ ] With the Tourist phone app paired, SOS started on either device starts and stops on the other.
      **Not verified end to end.** The Data Layer needs a paired phone and watch, and neither emulator image
      here carries the Wear OS companion app (the phone emulator has no Google sign-in), so the pair could
      not be built. What is verified instead: the watch side alone (above), and each half of the wire by unit
      tests - the phone's handling of a watch command (`SosCommandFromWatchHandlerTest`, `SosServiceTest`;
      `PhoneWearListenerService` itself has no routing test), the watch's `WatchWearListenerService` routing
      (`WatchWearListenerServiceTest`: start with a mode, default and unknown modes, stop, refused when
      `offersScreenTakeoverPrograms` is false) and `WearDataLayerPathsParityTest`, which asserts the two
      mirrored path tables are identical. The owner's check: install the phone and the watch builds on a
      paired pair, start SOS on one, expect it on the other, stop on either.
- [x] The store and noLegal merged manifests match the 4.3 answer; both variants walked on the smallest screen.
      Merged release manifests read back from `wear/build/intermediates/merged_manifests/`: **store**
      declares `POST_NOTIFICATIONS` (min API 33) only, **noLegal** adds `ACCESS_FINE_LOCATION`,
      `ACCESS_COARSE_LOCATION`, `BODY_SENSORS` (max API 35), `health.READ_HEART_RATE` and
      `ACTIVITY_RECOGNITION`; components in both: `MainActivity` (exported), `WatchWearListenerService`
      (exported, `/tourist/phone` and `/tourist/wear`), `GoogleApiActivity`, the profile installer and the
      startup provider. Both are guarded by `StoreBoundaryTest` / `NoLegalBoundaryTest`, which also read the
      source manifests. Walked: noLegal on `Wear_OS_Small_Round` (above); store on the same emulator
      (screenshot `08`: no Tourist row; a launch intent naming `TOURIST` left the screen unchanged, i.e.
      `ResolveWearLaunchAddressUseCase` refused it; the install over the noLegal build was an update, same
      id and key) and **on the oldest supported OS**, `Wear_OS2_API28` (Wear OS 2, API 28, 384x384,
      screenshots `09`-`11`): the program list without Tourist, the stopwatch posted its ongoing notification
      (id 4201, `ONGOING_EVENT`), the water flashlight stayed locked under a tap and a swipe, SOS sounded
      and stopped, no crash in the log. The store variant claims no permission the manifest does not carry.
- [x] Every ported file has a ledger row; drift check clean or decided.
      367 rows; `tools/port-ledger/check-drift.ps1`: `OK: nothing uncarried (367 in step, 0 decided)`.
      Fresh runs (`--rerun-tasks`, exit 0) of `:wear:testStoreDebugUnitTest` 204 tests, `:wear:testNoLegalDebugUnitTest`
      203, `:phone:testStoreDebugUnitTest` 262 and `:phone:testNoLegalDebugUnitTest` 262, all with 0 failures;
      `:wear:` and `:phone:` `assembleStoreRelease` / `assembleNoLegalRelease` all assemble through R8 (exit 0).

### 4.1 Open for the owner

- The paired SOS check above, on a real phone and watch.
- The Wear OS review risk the owner accepted in 4.3: WO-V3 (swipe to close) and WO-V13 (white screen) on the
  water flashlight and SOS in the store variant; `offersScreenTakeoverPrograms` is the one line to turn back.
- The unit-system push reaches a watch that connects while the phone app is closed only at the phone app's
  next start (tactical decision 6).
- The shade lock (S2812) is carried as FastMediaSorter has it (sideload yes, store no); say so if the store
  should differ.

## 5. Record of the phase (2026-10-25)

Source `W` = `wear/src/main/java/com/sza/fastmediasorter/wear` at FastMediaSorter HEAD `1e70b4c09`
(2026-10-25); destination under `wear/src/main/java/com/sza/tourist/wear`. 367 ledger rows in all after this
phase (175 watch rows, 26 of them tests, and 13 phone rows for the bridge); `tools/port-ledger/check-drift.ps1`
reports `OK: nothing uncarried (367 in step, 0 decided)`. The mechanical half of each port (copy, package
rename, the ledger pair) was done by one script and the cuts by hand, so a file's KDoc names what it does
not carry.

### 5.1 What was ported, and where

| Area | Source (`W/...`) | Destination (under `.../tourist/wear/...`) |
| --- | --- | --- |
| Tourist dashboard | `ui/apps/tourist/**` (incl. `TouristLockOverlay`, counter), `domain/tourist/**`, `data/tourist/**`, `di/WearTouristModule`, the three repository interfaces and seven use cases, `domain/bodysensor/HeartRatePermission` | same relative paths |
| SOS and water flashlight | `ui/apps/sos/**`, `ui/apps/waterflashlight/**`, `domain/sos/SosSyncBus`, `domain/model/{SosMode,SosMorseCadence,SosSirenWaveform}`, `SendSosCommandToPhoneUseCase` | same |
| Stopwatch | `ui/apps/stopwatch/**` (incl. `WearStopwatchOngoingNotificationManager`), `domain/stopwatch/**`, session repository + impl, two use cases, `di/WearStopwatchModule` | same |
| Data Layer on the watch | `data/wear/WatchWearListenerService` (SOS start/stop, unit-system push), `WearDataLayerPaths` (S0006, extended), `WearSettingsPayload` + decoder + `ApplyWearSettingsUseCase` (unit system only), `WearEventEnvelope` + codec, health and weather payloads | same |
| Seams | `MainActivity` -> `MainActivity` (composition root), `FastMediaSorterWearApp` -> `TouristWearApplication`, `WearAppCatalog`, `WearApp`, `WearDestinationId`, `WearRoutes`, `WearLaunchRoutes`, `WearLaunchTarget`/`Address`, `ResolveWearLaunchAddressUseCase`, `WearRestrictedCapabilities`, theme, icon/accent catalogs, `KeepScreenOnEffect`, `WearUnitDateTimeFormatter`/`WearUnitScale`, the preference layer, `ui/common/**` helpers the screens read | same |
| Phone side of the bridge | `app_v2/src/wearGms/.../WearTouristHealthReceiver`, `WearTouristWeatherRequestHandler`, `PushWearSettingsUseCase`, payload classes and envelope codec | `phone/src/main/java/com/sza/tourist/{service,domain}/...` |

Resources: the strings (EN/RU/UK), colours and vector drawables the ported code names were copied by name
from the source module; the watch carries 128 strings in each of the three locales. Per-file ledger rows do
not exist for resources (the phase tickets before this one did not ledger them either).

### 5.2 Left behind

- Everything the sorter's watch carries outside the kit: browse, players, network resources, voice notes,
  broadcast, clipboard, calculator, game, system info, blood pressure, motion monitor, body sensor,
  tiles, complications and watch-face slots (the face is S0009), onboarding, settings screens, the log buffer.
- `CompassHeadingDataSource` / `ObserveHeadingUseCase` / `HeadingReading`: read only by the dim overlay in
  the source; the dashboard computes its own azimuth. Ported, found unused, removed again with their ledger rows.
- The wallpaper layer of `WearScreenScaffold` (S2000), the geometry-mode setting (the screen keeps the
  reviewed `STORE` geometry), the colour-scheme, view-mode and language settings.
- The two-way settings exchange (S2093): only `unitSystem` crosses, phone -> watch, one way.

### 5.3 Tactical decisions of this ticket

1. **Start screen = the program list** (`AppsScreen`, route `home`), in the phone's order (Dashboard, Lights,
   SOS, Stopwatch). The routes a capability withholds are not registered at all, and the exported activity
   refuses a destination name the build does not offer (`ResolveWearLaunchAddressUseCase`).
2. **Capability interface kept to three members** - `locksSystemShade`, `offersHealthFeatures`,
   `offersScreenTakeoverPrograms` - plus the per-variant classes `StoreWearRestrictedCapabilities` /
   `NoLegalWearRestrictedCapabilities`, each with a boundary test in its own test source set.
3. **Permissions match runtime use:** `VIBRATE` and `WAKE_LOCK` (listed in the handoff for the watch) are not
   declared - no code calls a `Vibrator` or takes a wake lock (haptics go through the view; the screen is held
   by a window flag). Same ruling as S0006 took for the phone's `VIBRATE`.
4. **The dashboard asks for its sensor grants itself.** FastMediaSorter asks for `BODY_SENSORS` /
   `READ_HEART_RATE` and `ACTIVITY_RECOGNITION` in its first-run walk, which this product does not carry; the
   dashboard's one permission request now lists them with the location pair, asked once per visit. Declining
   leaves only the heart-rate and step tiles empty.
5. **Weather and health bridge are in scope** (S0004's ruling: "until S0008 wires the Data Layer"). The watch
   sends `TOURIST_HEALTH` and `TOURIST_WEATHER_REQUEST`, the phone answers with `TOURIST_WEATHER`; the phone's
   watch-health capability is now `WatchHealthCapability`, true in noLegal and false in store.
6. **Unit-system push has a trigger of its own:** FastMediaSorter pushes from a "send" button on its Wear
   companion screen; this product pushes on app start and on every change (`WearUnitSystemSyncCoordinator`).
   Known gap: a watch that connects while the phone app stays closed gets the push at the next app start.
7. **New stores not in the list of section 2:** `wear_tourist_counter` (SharedPreferences, the tally counter
   S4185 ported with the dashboard directory), alongside `wear_unit_system`, `wear_stopwatch_*`,
   `wear_tourist_trip` of `wear_settings`.
8. **R8 keep rules** for the Gson wire classes on both modules (`wear/proguard-rules.pro`,
   `phone/proguard-rules.pro`); the watch payloads carry no `@SerializedName`, so their field names are the
   wire contract and `WearPayloadWireParityTest` on the phone compares both sides.
9. **Ledger discipline:** one script (`port-file.ps1`) copies a file, renames the package and records the
   pair; the ledger rows were written in one pass at the end (`record-port.ps1`), hash taken from the source
   as it was on 2026-10-25.

## Last Audit

2026-10-25, mode pre-handoff (the task re-read from this file on disk, sections 1, 2, 3.3 and 4; `/spec-check` is
not run because it would flip the ticket out of `BlockNeedUserTest` and delete the probes the device pass needs).
Probes for that pass: `Timber.d("S0008: watch received a Data Layer message at ..")` in
`WatchWearListenerService.onMessageReceived` and `Timber.d("S0008: phone received a Data Layer message at ..")` in
`PhoneWearListenerService.onMessageReceived`.

Coverage per goal of section 1:

- **Glanceable dashboard with the hardware-key touch lock** - covered: ported whole (directory rule of S0003),
  noLegal only per 4.3; observed on the emulator, lock holds against tap and swipe, lifted by a key.
- **SOS reaching the other device over the product's own Data Layer pair** - covered in code and by unit tests on
  both halves, **not observed across two devices** (below). The watch half runs alone on the emulator.
- **Water flashlight** - covered and observed on both emulators, both variants offer it.
- **Stopwatch with its ongoing activity** - covered and observed on API 37 and on API 28; the notification is
  `ONGOING_EVENT`, category `stopwatch`.
- **Store/noLegal gating per 4.3** - covered: capability classes, route registration, launch-address refusal,
  both manifests (merged release manifests read back) and the two boundary tests.
- **Scope items** - heading source cut with the reason (5.2); unit system carried one way, phone to watch
  (decision 6); seams each ported or cut and named (5.1, 5.2); `accompanist-permissions` used by the dashboard,
  `androidx.wear.ongoing` by the stopwatch; stored state `wear_unit_system`, `wear_stopwatch_*` plus the two
  stores the source added (decision 7); tests ported and the uncovered classes (`TouristViewModel`,
  `AndroidWearTouristRepository`) now covered.

Verdict: the work covers the task except for the observations the owner has to make. Unobserved, each as it stands:

- [ ] SOS started on the phone starts on the watch, and the reverse, and each stop reaches the other (needs a paired
      pair; none could be built on the emulators).
- [ ] The unit system chosen on the phone reaches the watch: switch the phone to imperial, expect miles per hour,
      feet, Fahrenheit and the 12-hour clock on the watch dashboard and flashlight clock.
- [ ] The dashboard's heart-rate and step tiles with real sensors (the emulator reports none) and the weather tiles
      answered by the phone over the Data Layer.
- [ ] The phone dashboard's watch-health tiles fill from a real watch in the noLegal build, and stay hidden in store.
- [ ] A physical watch: the touch lock, the water flashlight and SOS with a wet wrist, the ongoing stopwatch chip on
      the watch face.
