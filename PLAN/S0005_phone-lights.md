# S0005 - Phone lights: water flashlight, front flashlight, camera torch

**Status:** Draft
**Phase:** 5 of 10 (handoff section 8)
**Verification level:** device - the water flashlight survives wet touches and leaves only by its exit rule;
the camera torch toggles; unit tests of the ported classes.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 5, section 5 (phone module: flashlights), sections 3 and 6a.

## 1. Goal

The three lights of the kit work on the phone: the water flashlight with its lockdown, the front (screen)
flashlight, and the camera torch - with the notification-shade torch and the widgets.

## 2. Scope

Source paths relative to `P:\ANDROID\FastMediaSorter_mob_v2` (read-only), `A` =
`app_v2/src/main/java/com/sza/fastmediasorter`.

- `A/ui/flashlight/`: `WaterFlashlightActivity` with `WaterFlashlightLockdownManager`,
  `FrontFlashlightActivity` and its ViewModel, `FlashlightToggleActivity`.
- The notification-shade torch: `A/core/notification/FlashlightShortcut*` and
  `A/ui/settings/helpers/OperationsFlashlightShortcutPermissionManager.kt`.
- Widgets: `A/widget/FrontFlashlightWidgetProvider.kt`, `A/widget/WaterFlashlightWidgetProvider.kt`.
- The torch itself comes from S0003 (extracted from `DeviceActionHandler`).
- **The signature rule:** the water flashlight's "no wet touch can switch it off" behaviour is ported exactly,
  with its exit rule; not reinterpreted.
- Permissions: `CAMERA` (torch), `RECEIVE_BOOT_COMPLETED` (torch shortcut), `POST_NOTIFICATIONS`, `WAKE_LOCK`;
  declared only as used, each with a one-sentence justification.
- Stored state keep-or-drop, written down: `water_flashlight_enabled`, `front_flashlight_*`,
  `flashlight_shortcut_notification_enabled`.
- The flashlight classes have no unit tests in the source: ported first, tested here afterwards, never changing
  behaviour to make a test pass.
- INPUT-PARITY and accessibility as in S0004; EN/RU/UK strings under the communication policy.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0003 (torch extraction), S0004 (start screen the lights are launched from).
- **Owner questions:** n/a - no open owner decision of handoff section 4 is needed by this phase.

## 4. Acceptance

- [ ] The water flashlight survives touches and leaves only by its exit rule, on a device.
- [ ] The camera torch toggles from the app, the shade shortcut and the widget; the front flashlight lights the
      screen.
- [ ] Every ported file has a ledger row; drift check clean or decided.
