# S0003 - Core port: sensors, GNSS, solar calculator, units, torch

**Status:** Implemented
**Status note:** Implemented 2026-10-23: 29 files ported with ledger rows (drift check green, 29 in step, exit 0); 88 unit tests green on store and noLegal (23 ported, 65 new), exit 0; both release variants assemble through R8. Track recorder not ported (decision in 5.3); SettingsRepository has no implementation yet, bound by S0004.
**Phase:** 3 of 10 (handoff section 8)
**Verification level:** unit tests of the ported classes, run green; every ported file has a ledger row.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 3, section 5 (phone module: sensors and sun, GNSS, units, the torch
seam), section 6a (full port, ledger).

## 1. Goal

The kit's non-UI core exists in this repository, ported in full from FastMediaSorter and cleaned, with its unit
tests - so S0004 onward builds screens on ported behaviour, never on a rewrite.

## 2. Scope

Source paths are relative to `P:\ANDROID\FastMediaSorter_mob_v2` (read-only), `A` =
`app_v2/src/main/java/com/sza/fastmediasorter`. Re-measure the line counts before porting.

- Reading sources: `A/data/sensors/` (motion, orientation, step count, availability), the sensor use cases and
  models under `A/domain/usecase/sensors/` and `A/domain/model/sensors/`. `SensorSeriesRepositoryImpl` and the
  `SensorSeriesPointEntity` Room table serve launcher gadgets only - left behind.
- Sun: `A/domain/util/SolarCalculator.kt`.
- GNSS: `GnssStatusDataSource` and its use cases under Network Monitor - port what the dashboard reads (satellite
  count, used satellites), cut the `MonitorSection` / `SectionAvailability` coupling. Whether the track recorder
  (`record_gnss_track`, `GnssTrackRecorderTest`) ports is a tactical decision of this ticket, written next to
  its ledger rows.
- Units: `QuantityFormatter`, `UnitScale`, `Quantity`, `UnitSystemProvider`, `ObserveUnitSystemUseCase`.
  Imperial units and the 12-hour clock as FastMediaSorter ships them (handoff goal 5).
- The torch, extracted from `A/core/screencapture/gesture/DeviceActionHandler.kt` (shared there by
  `SosTorchManager`, `WaterFlashlightActivity` and the torch toggle, next to unrelated gesture and brightness
  code): the torch part only.
- Package rename `com.sza.fastmediasorter` -> `com.sza.tourist`; every sorter reference cut; every file recorded
  in the port ledger (S0002) with its source SHA-256 at port time.
- Unit tests ported with their classes: `SolarCalculatorTest`, `QuantityFormatterTest`, `UnitScaleTest`,
  `UnitSystemProviderTest`, `GnssTrackRecorderTest` if the recorder is ported.
- Classes uncovered in the source (the three reading sources, among them) are ported first and tested here
  afterwards; behaviour is never changed to make a test pass.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002 (skeleton, port ledger and drift check) must be closed first.
- **Owner questions:** n/a - no open owner decision of handoff section 4 is needed by this phase.

## 4. Acceptance

- [x] Every ported file has a ledger row; the drift check runs and lists nothing uncarried or decided.
      29 rows in `docs/port-ledger.jsonl` (25 classes, 4 tests). `check-drift.ps1` against
      `P:\ANDROID\FastMediaSorter_mob_v2` (HEAD 5fe24d0): "OK: nothing uncarried (29 in step, 0 decided)",
      exit 0.
- [x] The ported unit tests pass; exit code and counts cited. Fresh `cleanTest` + `:phone:testStoreDebugUnitTest`
      and `:phone:testNoLegalDebugUnitTest`: BUILD SUCCESSFUL, exit 0, 14 test classes, 88 tests, 0 failures,
      0 errors, 0 skipped, on each variant. Of these, 23 are the four ported tests (`SolarCalculatorTest` 2,
      `QuantityFormatterTest` 9, `UnitScaleTest` 9, `UnitSystemProviderTest` 3); the rest are the tests written
      here for the classes that had none (section 5.5). Both release variants also assemble through R8
      (`assembleStoreRelease`, `assembleNoLegalRelease`, exit 0).
- [x] No sorter surface (`MonitorSection`, program registry, Room series tables) remains in the tree. A grep of
      `phone`, `wear` and `watchface` for `MonitorSection`, `SectionAvailability`, `SubProgramCatalog`,
      `InternalRouteCatalog`, `HomeWidgetCatalog`, `AppLaunchPanel`, `SensorSeries`, `@Entity`, `androidx.room` and
      the package `com.sza.fastmediasorter` finds code in none of them (two KDoc sentences name FastMediaSorter
      to say what the seams are not).

## 5. Record of the port (2026-10-23)

### 5.1 What was ported, and where

Source `A` = `app_v2/src/main/java/com/sza/fastmediasorter`, destination `D` =
`phone/src/main/java/com/sza/tourist`; the package is renamed and every file carries a ledger row.

| Area | Source (under `A`) | Destination (under `D`) |
| --- | --- | --- |
| Reading sources | `data/sensors/` - `DeviceSensorAvailabilityRepositoryImpl`, `EnvironmentSensorReadingSource`, `MagneticDeclinationSource`, `MotionReadingSource`, `OrientationReadingSource`, `StepCountReadingSource` | `data/sensors/` |
| Sensor use cases, models | `domain/usecase/sensors/` - `ObserveCompassUseCase`, `ObserveMotionUseCase`, `ObserveStepCountUseCase`; `domain/model/sensors/` - `SensorCapability`, `SensorReading` | same paths |
| GNSS status | `data/networkmonitor/GnssStatusDataSource`, `domain/model/networkmonitor/GnssSnapshot`, `domain/usecase/networkmonitor/ObserveGnssStatusUseCase` | `data/gnss/`, `domain/model/gnss/`, `domain/usecase/gnss/` |
| Sun | `domain/util/SolarCalculator` | same path |
| Units | `core/format/QuantityFormatter`, `domain/model/{UnitScale,Quantity,UnitSystem}`, `domain/unit/UnitSystemProvider`, `domain/usecase/ObserveUnitSystemUseCase` | same paths |
| Torch | `core/screencapture/gesture/DeviceActionHandler` - the torch part only | `core/torch/TorchController` |
| Dependencies the scope list did not name | `domain/repository/SensorAvailabilityRepository`, `domain/model/tourist/TripMotionPolicy`, `util/PackageManagerCompat` | same paths |
| Tests | `SolarCalculatorTest`, `QuantityFormatterTest`, `UnitScaleTest`, `UnitSystemProviderTest` | `phone/src/test/...`, same packages |

Re-measured, not trusted: `data/sensors/` holds seven files today, not the five of the handoff - the two the
handoff does not count, `EnvironmentSensorReadingSource` and `MagneticDeclinationSource`, are the barometer, light,
magnetic-field, tilt and declination feeds of the dashboard (read only by `TouristEnvironmentTelemetry`), so they
come with the directory.

Comments were cleaned on the way: FastMediaSorter ticket ids (which would collide with this repository's S space),
references to its strategic documents, launcher and gadget wording, and its quality gates are dropped; behaviour
comments are kept. The code of the ported classes is unchanged except the cuts listed below.

### 5.2 Left behind

- `SensorSeriesRepositoryImpl`, `SensorSeriesPointEntity`/`SensorSeriesDao`, `SensorSeriesPoint` / `SensorSeriesId`,
  `SensorSeriesRepository`, and the three series use cases (`ObserveSensorSeriesUseCase`,
  `RecordSensorSeriesPointUseCase`, `ResetSensorSeriesUseCase`) with `RecordSensorSeriesPointUseCaseTest`: they
  serve the launcher chart gadgets only (checked: nothing in the dashboard reads them).
- `MonitorSection`, `SectionAvailability` and the rest of the Network Monitor.
- The launcher-only users of the ported sources (`DimHeadingProvider`, the gadgets).

### 5.3 Tactical decisions of this ticket

1. **The GNSS track recorder is not ported.** `GnssTrackRecorder`, `GnssTrackState`, `RecordGnssTrackUseCase`,
   `ShareGnssTrackUseCase` and `GnssTrackRecorderTest` stay in FastMediaSorter. The dashboard never reads the
   recorder (it reads satellite counts and the coordinate only); the recorder's only UI is the Network Monitor
   GNSS section; it is gated by the `record_gnss_track` setting and shares through a `FileProvider` authority,
   neither of which the kit has; and recording a route is "route tracking", a non-goal of handoff section 2
   ("new capabilities that exist nowhere today"). Consequence for S0004: the `record_gnss_track` key of the
   handoff's stored-state list is dropped with it. There are no ledger rows to write the decision next to, so it
   is written here.
2. **The `MonitorSection` / `SectionAvailability` coupling is cut by emptiness.** `GnssStatusDataSource.observe()`
   returns `Flow<GnssSnapshot>`: a device that can show nothing (no fine-location grant, no GPS provider, API
   below 24) gets an empty flow, a platform refusal after registration emits nothing, and a working receiver emits
   an (empty) snapshot at once - so a reader tells "no receiver" from "no satellites in view" by whether any
   snapshot arrives. The reason an unavailable receiver gives (hardware, permission) was read by the Network
   Monitor section only; the dashboard ignored it (it reads `section.data` and skips null). `GnssSnapshot` loses
   the `coordinateAvailability` field with it (a `coordinate == null` says the same). `ObserveGnssStatusUseCase`
   returns `Flow<GnssSnapshot>` to match. The `register` result is a `Boolean` instead of a reason.
3. **The torch is `TorchController`, with the three members unchanged.** `isTorchOn`, `toggleFlashlight(context)`
   and `setTorch(context, enabled)` keep their names and signatures, so the later ports of `SosTorchManager`,
   `WaterFlashlightActivity`, `FlashlightToggleManager` and the shade shortcut (S0005, S0006) change only the
   injected type. The gesture dispatch (`handle`), the brightness presets and the notification-shade expansion are
   not part of the kit and are not copied. Because the ledger hashes the whole source file, a later change to
   that unrelated code in `DeviceActionHandler` will show as drift on this row; it is then a decision (`decide-drift`),
   not a carry.
4. **Dependencies are ported, not rewritten.** `TripMotionPolicy` (a leaf object that `MotionReadingSource` reads),
   `SensorAvailabilityRepository` and `PackageManagerCompat` (whole file; the three helpers beyond
   `getPackageInfoCompat` are unused here and wait for the map/share code of S0004) are copied and ledgered.
   `TripMotionPolicy` sits in `domain/model/tourist/`, which S0004 ports; its row already exists.
5. **Logging.** The ported classes log through Timber (as in the source), added as a dependency at FastMediaSorter's
   pin (5.0.1). No tree is planted yet, so those warnings are silent until the phone shell sets logging up.
6. **Compiler flag.** `-Xannotation-default-target=param-property` is set on the phone module, as FastMediaSorter
   does: the ported classes annotate constructor properties (`@ApplicationContext private val`) and are written
   for that target.

### 5.4 This product's own seams (no ledger rows)

These are written here, not ported; they are the minimum the ported code needs to compile and wire.

- `domain/model/AppSettings` (only `unitSystem`) and `domain/repository/SettingsRepository` (only `getSettings()`):
  the seam `ObserveUnitSystemUseCase` and `UnitSystemProviderTest` need so both port verbatim. FastMediaSorter's
  versions are sorter-wide and are not copied. **`SettingsRepository` has no implementation yet**: the persisted
  store (and the keep-or-drop list of stored preferences, handoff section 5) is S0004's `AppSettings` seam, and
  nothing may inject `UnitSystemProvider` or `ObserveUnitSystemUseCase` into a built graph before it is bound. The
  Hilt graph was checked with a throw-away entry point: every other ported class resolves; the only missing
  binding is `SettingsRepository`.
- `core/di/CoroutineModule` (`@IoDispatcher`, `@ApplicationScope`, shaped after FastMediaSorter's `AppModule`, only
  what the ported classes ask for) and `core/di/SensorModule` (binds `SensorAvailabilityRepository`).
- The twelve `unit_*` strings of `QuantityFormatter` in `values`, `values-ru`, `values-uk` of the phone module,
  copied verbatim from FastMediaSorter. They are entries of a whole-file resource, so they have no ledger row (a row
  on FastMediaSorter's `strings.xml` would report drift on every unrelated change there); a change to those twelve
  keys there is carried by hand when noticed, and `QuantityFormatterTest` pins the English ones it uses.
- Test dependencies at FastMediaSorter's pins: `mockk` 1.13.9, `robolectric` 4.16.1 (and `timber` 5.0.1 for main).

### 5.5 Tests written here for classes the source left uncovered

Ported first, tested afterwards; no behaviour was changed to make a test pass. 65 tests in ten classes:
`OrientationReadingSourceTest` (9: fused and raw-pair headings, accuracy, throttle, free fall, unregistration),
`MotionReadingSourceTest` (6), `StepCountReadingSourceTest` (4), `EnvironmentSensorReadingSourceTest` (9),
`MagneticDeclinationSourceTest` (2), `DeviceSensorAvailabilityRepositoryImplTest` (11), `GnssStatusDataSourceTest` (6,
pins the cut of 5.3.2), `GnssSnapshotTest` (4), `TorchControllerTest` (7), `SensorModelsTest` (7: accuracy mapping,
speed conversion, `TripMotionPolicy` thresholds). The throttle test waits 1.1 s of real time: the source reads the
wall clock, which Robolectric's clock does not drive.

### 5.6 Open for the next tickets

- S0004: bind `SettingsRepository` (persisted `unit_system`), decide the stored-state list, plant a logging tree,
  port the rest of `domain/model/tourist/` around the already ported `TripMotionPolicy`, and declare
  `ACTIVITY_RECOGNITION` only where decision 4.3 allows (the steps capability reads it from the merged manifest).
- Known source defects carried by the drift check, unchanged by this port: FastMediaSorter S4068, S4069.
