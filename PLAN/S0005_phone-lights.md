# S0005 - Phone lights: water flashlight, front flashlight, camera torch

**Status:** BlockNeedUserTest
**Status note:** Implemented 2026-10-25: 16 files ported with ledger rows (100 total, drift check clean), 143 unit tests green on store and noLegal, both release variants assemble through R8; the emulator pass closed all three acceptance criteria. Waiting for the owner personal-phone pass: the water flashlight under a wet finger on real glass, and a physical flash LED lit and extinguished at its exit.
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

- [x] The water flashlight survives touches and leaves only by its exit rule, on a device.
      Verified on the phone emulator (2026-10-25, screenshots `07`, `09`, `19` in
      `PLAN/S0005_phone-lights/`): the lamp opens white at full window brightness with the clock and the
      "Locked. Use the buttons" hint, the camera-service log shows the torch lit at `onStart`; a barrage of
      eight injected touches (centre/edge taps, two vertical swipes, a horizontal edge swipe shaped like the
      back gesture) left the lamp in the foreground with the torch still lit; `VOLUME_DOWN` - one of the
      hardware-only exit keys - finished it, and the torch went out at `onStop` (camera-service log, this
      app's PID). `KEYCODE_BACK` stays unhandled on purpose, exactly as the source rules it. The strict form -
      a wet finger on real glass, a physical flash LED - is the owner's personal-phone pass, as in S0004.
- [x] The camera torch toggles from the app, the shade shortcut and the widget; the front flashlight lights
      the screen.
      Torch from the app: the camera-torch row of the lights screen drove `FlashlightToggleActivity` and the
      camera-service log recorded the flash on (and off on the next drive), driven by this app's PID - once by
      touch, once by D-pad `ENTER` on the row. From the shade shortcut: with the switch on, the shade entry
      "Camera flashlight" posted (ongoing, low-importance channel `flashlight_shortcut`, amber large icon); a
      tap on the entry flipped the torch off (camera-service log) and the entry re-rendered itself to
      "Tap to switch the light on" in the same pass. From the widget family: the source has no torch widget
      and this scope ports none - the two widgets the scope names open their lamps (`17`, `18`, `19`: both
      placed through the picker; the water widget's tap opened the water lamp and lit the torch). Front
      flashlight: the screen lights as the lamp - pixel-sampled opaque white `255,255,255` on open, the picked
      red `#F44336` exactly after the picker confirmed, and the same red after close/reopen (the colour is
      the one piece of state that survives a run, `10`, `12`, `14`).
- [x] Every ported file has a ledger row; drift check clean or decided.
      16 new rows in `docs/port-ledger.jsonl` (100 total). `check-drift.ps1` against source HEAD 5fe24d0:
      "OK: nothing uncarried (100 in step, 0 decided)", exit 0. `:phone:testStoreDebugUnitTest` and
      `:phone:testNoLegalDebugUnitTest`: exit 0, 25 test classes, 143 tests, 0 failures on each variant
      (111 inherited from S0004 + 32 new here in 8 classes; the source left all 16 ported classes
      uncovered). Both release variants assemble through R8. One scope note: `WAKE_LOCK` from the scope's
      permission list is NOT declared - no ported class of this phase takes a wake lock (the lamps hold the
      screen through `FLAG_KEEP_SCREEN_ON`, which needs no permission), and the scope's own "declared only
      as used" clause rules it out; S0006's foreground service is the first user and declares it.

## 5. Record of the phase (2026-10-25)

Source `A` = `app_v2/src/main/java/com/sza/fastmediasorter`, destination `D` =
`phone/src/main/java/com/sza/tourist`; source HEAD 5fe24d0. 16 files ported with ledger rows.

### 5.1 What was ported, and where

| Area | Source (under `A`) | Destination (under `D`) |
| --- | --- | --- |
| Water flashlight | `ui/flashlight/WaterFlashlightActivity`, `ui/flashlight/helpers/WaterFlashlightLockdownManager` | same paths |
| Front flashlight | `ui/flashlight/FrontFlashlightActivity`, `ui/flashlight/FrontFlashlightViewModel`, `ui/flashlight/helpers/FrontFlashlightBrightnessManager` | same paths |
| Torch trampoline | `ui/flashlight/FlashlightToggleActivity`, `ui/flashlight/helpers/FlashlightToggleManager` | same paths |
| Shade shortcut | `core/notification/` - `FlashlightShortcutCoordinator`, `FlashlightShortcutNotifier`, `FlashlightShortcutReceiver`, `FlashlightShortcutBootReceiver` | same paths |
| Shade permission | `ui/settings/helpers/OperationsFlashlightShortcutPermissionManager` | `ui/flashlight/helpers/FlashlightShortcutPermissionManager` (renamed: "Operations" names the source's settings tab) |
| Widgets | `widget/FrontFlashlightWidgetProvider`, `widget/WaterFlashlightWidgetProvider` | same paths |
| Colour picker | `ui/dialog/ColorPickerDialog`, `core/util/ColorPalette` | same paths |

Resources: `activity_front_flashlight.xml`, `activity_water_flashlight.xml`,
`dialog_color_picker.xml` + its `layout-land` twin, `item_color.xml`, the two widget layouts + their
`xml/` info files (widget theme set to `Theme.Tourist`), seven drawable vectors, the flashlight
dimens/colours, `Theme.Tourist.Transparent` for the trampoline, `grid_column_count`, and 23+4 strings
x3 locales (widget labels from the source's `strings_widget.xml`, the rest from its `strings.xml`;
`program_lights` x3 and the record comments are this product's own wording).

### 5.2 Left behind

- `HelpOverlayEntryManager` in `FrontFlashlightActivity` and `DialogKeyboardDelegate` in
  `ColorPickerDialog`: the input-help family stays behind with S0004's ruling; the picker keeps
  `btnOk.requestFocus()` so D-pad still lands on the confirming button.
- The `LocaleHelper` wrapper of `FlashlightToggleActivity` (no language switch in this product yet),
  the settings-tab wording of the permission manager's class name, and the launcher accent catalog
  call of the two widgets (the icons keep the indigo baked into their accent drawables, the shape
  S0004's widget took).
- The boot receiver's `IOException` catch arm: that arm guards the source's DataStore read; this
  product's `SharedPreferences` store throws no `IOException`, so the arm is dead here - the
  `CancellationException` rethrow and the corrupt-store `IllegalStateException` arm are ported.

### 5.3 Tactical decisions of this ticket

1. **Stored state, keep-or-drop.** `front_flashlight_color` and `flashlight_shortcut_notification_enabled`
   keep, persisted under the source's key names with the source's defaults (opaque white; off until
   asked). `front_flashlight_enabled` and `water_flashlight_enabled` drop: they are the sorter's
   program switches and the lights are always offered here - the same ruling S0004 made for
   `enable_tourist`.
2. **The shade switch seats on the lights screen.** The source's row lives on its Operations settings
   tab, a screen this product does not have; `LightsActivity` - this product's own program surface,
   the S0004 start screen's "Lights" row - hosts the row, with the ported permission manager asking
   for `POST_NOTIFICATIONS` on the flip. The switch's own semantics (off until granted, a denial
   leaves the row honest, hidden where no flash unit exists) are the source's row wiring verbatim.
3. **The in-app torch toggle is the source's programs-menu shape:** a bare intent into
   `FlashlightToggleActivity` (toggle, toast when refused, finish), reached from the lights screen's
   camera-torch row.
4. **`NotificationIds` / `NotificationIcons` are this product's own registries**, as S0004's scope
   ruled: one id (`FLASHLIGHT_SHORTCUT`) and one alpha-only status-bar glyph - a fork of the compass
   with the colours flattened to white, because a status-bar icon must not carry a theme attribute.
5. **`AppSettings` grows the write side.** `SettingsRepository.updateSettings(edit)` arrives as an
   atomic mutex-held read-modify-write persisting through the existing `settings` preferences file;
   `findLifecycleOwner` opens from `private` to `internal` (the source's visibility) so the ported
   permission manager can resolve the host.
6. **Gradle pin**: `fragment-ktx` 1.6.2 at FastMediaSorter's value - `setFragmentResult` of the ported
   picker needs it; the phone module gains nothing else.
7. **Boot graph**: `core/di/BootReceiverEntryPoint` + `bootReceiverEntryPointOrNull` are this
   product's own seam (the source's shape, minus its WorkManager and SFTP members); the coordinator
   starts from `TouristApplication.onCreate`, as the source starts it from its app class.

### 5.4 Device pass on the emulator (2026-10-25, storeDebug)

The full evidence trail is in section 4. Driven additionally: the start screen's Lights row by touch
and by D-pad; the lights screen rows walked by D-pad with `ENTER` activation (the water row's `ENTER`
opened the water lamp); the front lamp's five brightness cells carry "Brightness N of 5"
descriptions and the tapped step's selection outline follows; the picker renders the current colour
by name ("White" on first open) and D-pad `ENTER` on the colour button opens it - the source's
"focused control consumes the key first" rule, observed exactly (the lamp's activity-level
`ENTER`-close branch needs a focus-free window, which the emulator's always-focused initial control
never produced; the code is the source's verbatim). The switch-off path removed the posted shade
entry. `POST_NOTIFICATIONS` was pre-granted on this emulator, so the ask itself is pinned by the
permission manager's unit tests on both sides of API 33.

### 5.5 Concurrency note

The S0006 session began porting into this tree at 02:15, after this phase's unit-test and R8 gates
had run green on the S0005-only tree. The device pass therefore ran from a scratch snapshot of the
S0005-only tree (built in `temp/S0005_snapshot/`, since deleted); the live tree's shared files
(`AppSettings`, `MainActivity`, `SettingsRepositoryImpl`, the manifest) carry both phases' edits, and
S0006's session is responsible for its own half.

## Last Audit

2026-10-27, mode pre-handoff: sections 1, 2, 3.3, 4 and 5 re-read from this file on disk. No code was changed and
no build was run for this block; the evidence is the cited record of 2026-10-25, not a fresh run.

Coverage per goal of section 1:

- **The water flashlight with its lockdown** - covered: a barrage of eight injected touches (taps, two vertical
  swipes, a swipe shaped like the back gesture) left the lamp in the foreground with the torch lit; `VOLUME_DOWN`
  finished it and the torch went out at `onStop` (camera-service log, this app's PID, screenshots `07`, `09`, `19`).
  `KEYCODE_BACK` stays unhandled on purpose, as the source rules it.
- **The camera torch** - covered: toggled from the app row (by touch and by D-pad `ENTER`) and from the shade
  shortcut (camera-service log); the shade entry posted, re-rendered on its own tap and left with its switch.
- **The front (screen) flashlight** - covered: opaque white on open, the picked red exactly after the picker
  confirmed and again after close and reopen (`10`, `12`, `14`).
- **The widgets** - covered: both placed through the picker, each opens its lamp.
- **Port discipline and tests** - covered: 100 ledger rows, drift check clean, 143 unit tests green on both
  variants, both release variants assemble through R8.

Verdict: the work covers the task except for the observations the owner has to make on a real phone. Unobserved,
each as it stands:

- [ ] The water flashlight under a wet finger on real glass: the strict form of the signature rule, which an
      injected touch cannot reproduce.
- [ ] A physical flash LED: lit by the torch row and the shade shortcut, extinguished at the exit of the water
      flashlight.
