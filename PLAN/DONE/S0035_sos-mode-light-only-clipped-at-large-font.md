# S0035 - SOS screen: the Light only mode button is cut to 3 px at font 2.0

**Ticket:** S0035
**Status:** Implemented
**Status note:** Implemented 2026-10-29, not built, not tested until the release: the SOS mode row stacks into a column when its three one-line labels do not fit (SosActivity fitModeRow); ledger decision recorded. Release: preflight compile, locale-a11y C5 phone re-measure at font 2.0.
**Complexity:** low - primitive spec (confirmed at the spec pass 2026-10-29): one existing source file and
its ledger row change, no new file, type, resource, DI or screen, about 40 lines
**Date:** 2026-10-29

## Captured request and migration scope

Parked by S0034 (2026-10-29) as an out-of-scope finding; nothing was changed for it.

- Symptom: on the phone SOS screen at font scale 2.0 the third mode button, "Light only"
  (`btnSosModeLightOnly`, `activity_sos.xml`), is laid out 3 px wide at the right edge of its row; the first two
  buttons take the whole line. At font 1.0 the three buttons share the row.
- Evidence: the S0013 run `20261025-p5-phone`, scene `C5-phone-r1`, `m7-sos-running-{en-US,ru-RU,uk-UA}-2.xml`
  (density 420): EN `Light only` `[1035,1468][1038,1620]` beside `Sound only` `[689,1468][1038,1620]`; RU
  `Только свет` `[1035,1511][1038,1663]`. The stand's reader did not flag it before S0034; with S0034's width
  floor it reports `btnSosModeLightOnly` as clipped in all three locales.
- Consequence: at large font a user cannot see or reliably tap the light-only SOS mode.
- Done when at font scale 2.0 every SOS mode button shows its whole label in EN, RU and UK, and the S0013
  locale-a11y C5 phone cell reports no clipped `btnSosModeLightOnly`.

## Spec pass (2026-10-29, unattended queue pass)

Rung: primitive spec - one existing Kotlin file plus its port-ledger decision, no new file, public type,
resource, DI wiring or screen, under 100 lines. Shared-contract boundary: none - a phone layout decision, no
format, wire shape, algorithm constant or Data Layer message (`docs/contracts/` pointers name nothing here). No
research item is open. The one placement call is settled below without the owner: font 1.0 keeps today's
single centred row exactly, and only a row that cannot hold its labels changes shape.

### Problem

- The three modes are a `MaterialButtonToggleGroup` - a horizontal `LinearLayout` - of `wrap_content` buttons,
  itself `wrap_content` and centred in the screen's column. A horizontal `LinearLayout` measures its children in
  order and hands each only what the earlier ones left, so at font 2.0 "Sound and light" and "Sound only" take
  the whole 996 px line (1080 px minus the column's `margin_large` padding at density 420) and "Light only" gets
  the 3 px remainder. The `Widget.Tourist.Button.Choice` style already allows a label to wrap
  (`singleLine=false`), but 3 px holds no character.
- The layout is the verbatim port: FastMediaSorter's `activity_sos.xml` has the same group, differing only in the
  button style; its `SosActivity.kt` still hashes `b85a9c90..`, the ledger row of 2026-10-25.

### Approach

- Fit or stack, decided from the text: `SosActivity` reads the width each mode needs to show its label on one
  line (what `TextView` measures for an unbounded width: `Layout.getDesiredWidth` plus the compound paddings,
  no less than the minimum widths) and compares their sum with the column's inner width. While the sum fits, the
  group stays horizontal with `wrap_content` buttons - today's row, unchanged. When it does not, the group turns
  vertical with `match_parent` buttons, so the three stand in a centred column as wide as the widest label;
  a label wider than the whole line wraps inside its button instead of being cut.
- Run from an `OnLayoutChangeListener` on the column, on every width change including the first layout, so a
  resize moves the row either way; the check reads text, not a measure pass, so it is the same in both
  orientations. Added in `setupViews`, removed in `onDestroy` beside the mode listener.
- Material 1.14.0 `MaterialButtonGroup.setOrientation` marks the child shapes dirty and its `onMeasure`
  re-applies the connected-button margins and corners for the new orientation (read from `classes.jar`
  bytecode). Its own `overflowMode="wrap"` was not used: it refuses a `wrap_content` group width, which would
  move the font-1.0 row off centre.
- Port law: `SosActivity.kt` carries a local addition, written next to its ledger row with
  `tools/port-ledger/decide-drift.ps1`. FastMediaSorter has the same clipping; its tree is read-only here.

### Done criteria

- [x] The mode row is fitted in code: `fitModeRow`, `oneLineWidth` and `modeRowFitListener` exist in
      `SosActivity.kt`; the listener is added in `setupViews` and removed in `onDestroy` (grep).
- [x] The ledger row of `ui/sos/SosActivity.kt` carries the S0035 decision pinned to `b85a9c90..` (read).
- [ ] RELEASE - compile and unit suites: `pwsh -NoProfile -File tools/release/preflight.ps1`.
- [ ] RELEASE - at font 2.0 every SOS mode label is whole in EN, RU and UK and the C5 phone cell reports no
      clipped `btnSosModeLightOnly`: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1
      -Scenes C5 -Devices phone`, then read `m7-sos-running-{en-US,ru-RU,uk-UA}-2.xml` (three buttons stacked,
      each as wide as the column's widest label) and `-1.xml` (three buttons on one line, as on 2026-10-25).

## Last Audit

**Date:** 26-10-29 | **Mode:** static | **Outcome:** Implemented (static rungs; compile and the device re-measure are RELEASE)
**Counts:** PASS 4 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 0 | RELEASE 2

- PASS - fit code: `SosActivity.kt` declares `modeRowFitListener` (line 87), `fitModeRow` (159) and
  `oneLineWidth` (171); `addOnLayoutChangeListener(modeRowFitListener)` in `setupViews` (98) and
  `removeOnLayoutChangeListener(modeRowFitListener)` in `onDestroy` (110) (grep; expected 1 each | actual 1 each).
- PASS - library behaviour: Material 1.14.0 `classes.jar` (Gradle cache), `javap -c`: `setOrientation` sets
  `childShapesDirty`; `onMeasure` runs `adjustChildMarginsAndUpdateLayout` and `updateChildShapes` first; wrap
  overflow throws on an `AT_MOST` (`wrap_content`) width (read).
- PASS - ledger: `decide-drift.ps1 -Source app_v2/.../ui/sos/SosActivity.kt` exit 0, "decision recorded against
  version b85a9c900d2e"; the row carries the S0035 note dated 2026-10-29.
- PASS - fast gates: `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui` exit 0, `unified-ui-source: PASS`,
  0.665 s; `-Area docs` after this spec exit 0, `docs-check: PASS (5 checks)`, 2.100 s.
- RELEASE - compile and unit suites: `pwsh -NoProfile -File tools/release/preflight.ps1`.
- RELEASE - re-measure: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1 -Scenes C5
  -Devices phone`; in `m7-sos-running-{en-US,ru-RU,uk-UA}-2.xml` the three mode buttons are stacked and none is
  reported clipped; in the `-1.xml` dumps they share one line (2026-10-25: `Light only` 3 px wide at font 2.0).

No ticket-id probe exists or is needed (no `BlockNeedUserTest`). Help prose, help map and strings are unchanged:
no element is added or renamed, and the font-1.0 captures keep the single row. Not built, not tested until the
release.
