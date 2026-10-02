# S0008 - Wear app: dashboard with touch lock, SOS pair, water flashlight, stopwatch

**Status:** Draft
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
- **4.3 Store-variant composition (open, asked before this phase):** in FastMediaSorter the watch store build
  hides SOS, the water flashlight and the Tourist dashboard (`offersHealthFeatures=false`,
  `offersScreenTakeoverPrograms=false`, owner decision S3178 of 2026-09-16 in that repository), and the phone
  declares `ACTIVITY_RECOGNITION` in noLegal only. In a product that is the kit, hiding them in the store
  variant leaves an empty watch app. Options: keep the S3178 split (store watch = stopwatch only); lift it for
  this product after a Play policy check of each capability; or ship the store variant phone-only first.
  Decides the manifests, the capability gate port and the Play declarations.

## 4. Acceptance

- [ ] The watch app installs; the Tourist dashboard and its touch lock work; the stopwatch shows its ongoing
      activity.
- [ ] With the Tourist phone app paired, SOS started on either device starts and stops on the other.
- [ ] The store and noLegal merged manifests match the 4.3 answer; both variants walked on the smallest screen.
- [ ] Every ported file has a ledger row; drift check clean or decided.
