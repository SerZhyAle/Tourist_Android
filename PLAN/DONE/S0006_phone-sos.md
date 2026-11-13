# S0006 - Phone SOS: activity, foreground service, siren, torch blinking

**Status:** Implemented
**Status note:** Implemented 2026-10-25: 24 files ported with ledger rows (124 total, drift check clean, exit 0); 169 unit tests green on store and noLegal, both release variants assemble through R8. Verified on phone emulator (storeDebug, screenshots 01-06 in PLAN/S0006_phone-sos/): SOS launches from start screen and dashboard, runs as mediaPlayback foreground service, sounds siren and blinks strobe across ALL, SOUND_ONLY, and LIGHT_ONLY with D-pad navigation, and stops cleanly on command.
**Phase:** 6 of 10 (handoff section 8)
**Verification level:** device - SOS sounds and blinks, stops on command; unit tests of the ported classes.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 6, section 5 (phone module: SOS; Data Layer), sections 3, 6a (item 3:
no behaviour contract yet).

## 1. Goal

SOS works on the phone: the activity, the foreground service, the siren and the torch blinking, and the phone
side of the Data Layer pair that starts and stops SOS from the watch.

## 2. Scope

Source paths relative to `P:\ANDROID\FastMediaSorter_mob_v2` (read-only), `A` =
`app_v2/src/main/java/com/sza/fastmediasorter`.

- `A/ui/sos/`: `SosActivity`, `SosService` (foreground type `mediaPlayback`), `SosViewModel`,
  `SosSoundGenerator`, `SosTorchManager`, `SosCommandFromWatchHandler`, `SosStartFromWatchNotifier`;
  `A/domain/model/sos/`, `A/domain/usecase/sos/`, `A/domain/usecase/wear/SendSosCommandToWatchUseCase.kt`.
- Data Layer, SOS only: message paths `/fms/watch/sos/start`, `/fms/watch/sos/stop`, `/fms/phone/sos/start`,
  `/fms/phone/sos/stop`; payload the `SosMode` name (`ALL`, `SOUND_ONLY`, `LIGHT_ONLY`). The constants are
  hand-mirrored in `A/service/WearDataLayerPaths.kt` (lines 191-205 at hand-off) and on the watch; incoming
  routing on the phone is `PhoneWearListenerService` in the source's `wearGms` source set. The paths are private
  to one application id, so the `/fms/` prefix becomes this product's prefix during the port - chosen here, the
  shape and payload unchanged, mirrored by S0008 - and the two mirrored constant blocks are kept equal by a
  check written with this ticket.
- If the SOS signal or its Data Layer messages must be shared with a third product, they are written as a
  contract in the catalog first (handoff section 6a item 3), never as a document here.
- Permissions: `WAKE_LOCK`, `VIBRATE`, `FOREGROUND_SERVICE` with `mediaPlayback`, `POST_NOTIFICATIONS`, and the
  torch's `CAMERA`; the foreground-service declaration must match runtime use.
- Unit tests ported: `SosSirenWaveformTest`, `SosSoundGeneratorTest`. The SOS service and view models are
  uncovered in the source: ported first, tested here afterwards.
- INPUT-PARITY and accessibility as in S0004; EN/RU/UK strings.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0003 (torch), S0004 (shell); S0008 mirrors the watch side.
- **4.3 Store-variant composition (open, carried by S0008):** SOS is one of the capabilities FastMediaSorter's
  S3178 split hides in its store watch build; the phone has no such flag today. If this phase must gate SOS in
  the store variant, the question is asked then, once, and the answer is recorded in S0008.

## 4. Acceptance

- [x] SOS sounds and blinks on a device in each mode (`ALL`, `SOUND_ONLY`, `LIGHT_ONLY`) and stops on command.
      Verified on the phone emulator (`emulator-5554`, `storeDebug`, 2026-10-25, screenshots `01`-`06` in
      `PLAN/S0006_phone-sos/`): launched from both the start screen (`row_sos`) and the Tourist dashboard
      (`btnSos`); in `ALL` mode `SosService` runs as a foreground service (`isForeground=true`,
      `foregroundServiceType=0x00000002` / `mediaPlayback`), `SosSoundGenerator` raises alarm volume and generates
      the continuous dual-tone siren (`peak 1.000, rms 0.788`), and `SosTorchManager` drives the Morse strobe;
      switching via D-pad to `SOUND_ONLY` stops the torch/screen strobe while keeping the siren; switching via
      D-pad to `LIGHT_ONLY` stops the siren (restoring alarm volume to baseline) and resumes the Morse strobe;
      activating `btnSosStop` via D-pad focus + `KEYCODE_ENTER` tears down `SosService` (`dumpsys activity services`
      reports `(nothing)`) and closes `SosActivity`.
- [x] The foreground-service type and permissions in the merged manifest match runtime use.
      Verified in `phone/build/intermediates/merged_manifests/storeDebug/processStoreDebugManifest/AndroidManifest.xml`:
      `SosService` is declared with `android:foregroundServiceType="mediaPlayback"` and `android:exported="false"`;
      `PhoneWearListenerService` is declared with `android:exported="true"` and `pathPrefix="/tourist/watch"`;
      permissions `WAKE_LOCK`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` are declared with
      one-sentence justifications alongside `CAMERA` and `POST_NOTIFICATIONS`. `VIBRATE` is intentionally omitted
      on the phone because phone SOS does not use `Vibrator` (watch SOS in S0008 does).
- [x] Every ported file has a ledger row; drift check clean or decided.
      24 files ported and recorded in `docs/port-ledger.jsonl` (124 total rows across S0003-S0006);
      `tools/port-ledger/check-drift.ps1` exits 0 with `OK: nothing uncarried (124 in step, 0 decided)`.
      `:phone:testStoreDebugUnitTest` and `:phone:testNoLegalDebugUnitTest` pass (169 unit tests, 0 failures);
      `:phone:assembleStoreRelease` and `:phone:assembleNoLegalRelease` assemble cleanly through R8 and lintVital.

## 5. Record of the phase (2026-10-25)

Source `A` = `app_v2/src/main/java/com/sza/fastmediasorter`, destination `D` =
`phone/src/main/java/com/sza/tourist`; source HEAD `5fe24d0` (2026-10-22). 24 files ported with ledger rows
(22 production files across `:phone` and `:wear`, plus 2 ported unit test files), and 6 new unit test suites
added for the previously uncovered SOS and Data Layer classes.

### 5.1 What was ported, and where

| Area | Source (relative to `FastMediaSorter_mob_v2`) | Destination (relative to `Tourist_mob`) |
| --- | --- | --- |
| Coroutine cancellation helper | `A/core/util/CoroutineExt.kt` | `D/core/util/CoroutineExt.kt` |
| SOS domain models | `A/domain/model/sos/{SosMode,SosMorseCadence,SosSirenWaveform}.kt` | `D/domain/model/sos/{SosMode,SosMorseCadence,SosSirenWaveform}.kt` |
| SOS domain use cases | `A/domain/usecase/sos/{GetSosModeUseCase,SaveSosModeUseCase,IsSosProgramEnabledUseCase}.kt` | `D/domain/usecase/sos/{GetSosModeUseCase,SaveSosModeUseCase,IsSosProgramEnabledUseCase}.kt` |
| Wear Data Layer seam | `A/domain/model/WearNode.kt`, `A/domain/repository/WearableDataLayerRepository.kt`, `app_v2/src/wearGms/java/com/sza/fastmediasorter/data/wear/WearableDataLayerRepositoryImpl.kt`, `app_v2/src/wearGms/java/com/sza/fastmediasorter/di/WearModule.kt` | `D/domain/model/WearNode.kt`, `D/domain/repository/WearableDataLayerRepository.kt`, `D/data/wear/WearableDataLayerRepositoryImpl.kt`, `D/core/di/WearModule.kt` |
| Mirrored Data Layer paths | `A/service/WearDataLayerPaths.kt`, `wear/src/main/java/com/sza/fastmediasorter/wear/data/wear/WearDataLayerPaths.kt` | `D/service/WearDataLayerPaths.kt`, `wear/src/main/java/com/sza/tourist/wear/data/wear/WearDataLayerPaths.kt` |
| Outbound & inbound watch SOS | `A/domain/usecase/wear/SendSosCommandToWatchUseCase.kt`, `app_v2/src/wearGms/java/com/sza/fastmediasorter/service/PhoneWearListenerService.kt` | `D/domain/usecase/wear/SendSosCommandToWatchUseCase.kt`, `D/service/PhoneWearListenerService.kt` |
| SOS UI & foreground service | `A/ui/sos/{SosSoundGenerator,SosTorchManager,SosStartFromWatchNotifier,SosCommandFromWatchHandler,SosService,SosViewModel,SosActivity}.kt` | `D/ui/sos/{SosSoundGenerator,SosTorchManager,SosStartFromWatchNotifier,SosCommandFromWatchHandler,SosService,SosViewModel,SosActivity}.kt` |
| Ported unit tests | `app_v2/src/test/java/com/sza/fastmediasorter/domain/model/sos/SosSirenWaveformTest.kt`, `app_v2/src/test/java/com/sza/fastmediasorter/ui/sos/SosSoundGeneratorTest.kt` | `phone/src/test/java/com/sza/tourist/domain/model/sos/SosSirenWaveformTest.kt`, `phone/src/test/java/com/sza/tourist/ui/sos/SosSoundGeneratorTest.kt` |

Resources and seams: `activity_sos.xml`, `focus_button_background.xml`, 12 SOS strings across `values`,
`values-ru`, and `values-uk`, `color_program_accent_red` and `delete_button` in `colors.xml`, SOS and dialog
button dimensions in `dimens.xml`, `Widget.Tourist.Button.DialogDestructive` in `themes.xml`, `SOS_SIGNAL` and
`SOS_START_FROM_WATCH` in `NotificationIds.kt`, `sosMode` persisted under `"sos_mode"` in `AppSettings` /
`SettingsRepositoryImpl`, `row_sos` on `MainActivity`, and `btnSos` restored on `TouristInfoActivity`
(portrait and landscape) and `TouristActionsManager`.

### 5.2 Left behind

- `HelpOverlayEntryManager` on `SosActivity`: cut along with `HelpToolbarEntryManager` in S0004/S0005; in-app
  help arrives with S0014.
- `DeviceActionHandler`: replaced in `SosTorchManager` by `TorchController` extracted in S0003.
- Non-SOS routes in `WearDataLayerPaths` and `PhoneWearListenerService` (media transfer, watch health sync):
  trimmed to the four SOS paths for S0006; watch health routes join with S0008.

### 5.3 Tactical decisions of this ticket

1. **Product Data Layer path prefix (`/tourist/`):** the private Data Layer routes are renamed from `/fms/` to
   `/tourist/` (`/tourist/watch/sos/start`, `/tourist/watch/sos/stop`, `/tourist/phone/sos/start`,
   `/tourist/phone/sos/stop`) in both `:phone` and `:wear`, guarded against drift by
   `WearDataLayerPathsParityTest`.
2. **Stored state keep-or-drop:** `sos_mode` is kept in `AppSettings.sosMode` (`SettingsRepositoryImpl` key
   `"sos_mode"`, default `SosMode.ALL`). `enable_sos` is dropped from `AppSettings` because SOS is always
   offered in the standalone kit; `IsSosProgramEnabledUseCase` is ported and ledgered, returning `true`.
3. **Permissions match runtime use (`VIBRATE` omitted on phone):** phone SOS drives audio (`AudioTrack` on
   `USAGE_ALARM`) and light (`TorchController` + screen strobe) and never touches `Vibrator`. Per
   `INVARIANTS.md` #13 and acceptance criterion 2, `VIBRATE` is not declared in `phone/src/main/AndroidManifest.xml`
   (it belongs to the watch SOS in S0008).
4. **Wear Data Layer in `main` source set:** because both `store` and `noLegal` variants of Tourist pair with the
   Wear OS app over the Data Layer, `WearableDataLayerRepositoryImpl`, `WearModule`, and
   `PhoneWearListenerService` live in `phone/src/main` rather than a separate flavor source set.
