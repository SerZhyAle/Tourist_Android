# S0006 - Phone SOS: activity, foreground service, siren, torch blinking

**Status:** Draft
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

- [ ] SOS sounds and blinks on a device in each mode (`ALL`, `SOUND_ONLY`, `LIGHT_ONLY`) and stops on command.
- [ ] The foreground-service type and permissions in the merged manifest match runtime use.
- [ ] Every ported file has a ledger row; drift check clean or decided.
