# S0023 - Accessibility: unlabelled Up buttons, a torch row whose state is colour only, small result targets

**Status:** Implemented
**Status note:** Implemented 2026-10-29, not built, not tested until the release: Up arrows labelled in six toolbars, camera-flashlight row speaks on/off from a balanced torch callback, watch action chips carry their own labels, strings EN/RU/UK; result dialog and status-bar rows were already closed (S0015, inset padding). Release: preflight build and tests, locale-a11y C3,C7 phone and C2 watch re-measure. Parked: S0032.
**Complexity:** medium - spec pass (2026-10-29), confirmed: two rows need code (Up labels in six toolbar layouts,
the torch row's state from a balanced torch callback), one needs explicit chip labels on the watch, two were
already closed by later tickets; every proof above a grep is a device re-measure, release-tier
**Phase:** finding of S0013 (deep dynamic audit), Phase 4 understandability sweep
**Verification level:** device - read from the accessibility tree on the S0013 disposable profiles

## 0. Source

- S0013 property 5, budgets 8.5c (targets >= 48 dp phone / 40 dp watch), 8.5d (nothing clipped or unlabelled at
  font 2.0), 8.5e (every interactive node has a label and a state); handoff section 3: TalkBack labels, touch
  targets, state not shown by colour alone.
- The dumps come from the stand's accessibility probe, which reads what TalkBack reads (S0013 INDEX.md,
  deviation 1).

## 1. Problem

| Where | What | Budget |
| --- | --- | --- |
| phone Lights, phone Stopwatch | the toolbar's Up button (`ImageButton`) has no label in any locale or font scale | 8.5e, 8.5d |
| phone Lights | toggling the camera-flashlight row changes none of its accessibility attributes (text, description, checked, selected, state description) - its state is shown by colour alone | 8.5e |
| phone Stopwatch result dialog | the send button measures 13 x 48 dp and 20 x 48 dp; at font 2.0 the Cancel and other dialog buttons are clipped | 8.5c, 8.5d |
| watch Tourist dashboard (after the compass storm) | at font 2.0 an unlabelled clickable `View` appears | 8.5d |
| phone Lights (screenshot) | the status bar's clock is drawn over the toolbar's Up arrow and title | visual, outside the budgets |

## 2. Evidence

- Verdict-bearing extract of the S0013 dumps (C3 and C7 phone, C2 watch), copied 2026-10-29:
  `PLAN/S0023_accessibility-labels-state-targets/s0013-extract.md`. The status-bar row came from a C4x
  screenshot of the same campaign; the current tree answers it (section 4.1 item 5).
- Re-run: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1 -Scenes C3,C7 -Devices phone`.

## 3. Acceptance

- [ ] Every row of the table re-measured by `locale-a11y.ps1` without a breach in EN, RU and UK at font 1.0, 1.3
      and 2.0.
- [x] The strings added for labels and states exist in EN, RU and UK in one edit (canon invariant 17).

## 4. Spec pass (2026-10-29, unattended queue pass)

Rung: complex - more than three files change (six layouts, three string files, one activity, one watch
composable) and a torch subscription is new behaviour. Shared-contract boundary: none - no format, wire shape,
algorithm constant or Data Layer message is touched (`docs/contracts/` pointers checked: ICONOGRAPHY,
INPUT-PARITY, DIAGNOSTIC-REPORT and the rest name nothing here). Research items: none open; every row was
settled from the S0013 dumps and the current tree, below. No user-facing placement is open: the changes add
screen-reader labels and state, nothing moves or appears on screen.

### 4.1 Per row

1. **Up buttons without a label.** Every phone toolbar with an Up arrow (`navigationIcon`) lacks
   `navigationContentDescription`: Lights, Stopwatch (portrait and landscape), Counter and Tourist info
   (portrait and landscape) - six layouts; only the help dialog's bar has one. Counter and info were not in the
   measured scenes but carry the same symptom, deduped into this row. FastMediaSorter labels its Up arrows with
   `app:navigationContentDescription="@string/back"` (`activity_add_resource.xml`,
   `fragment_permissions_management.xml`), so the fix is that same attribute and a `back` string, EN/RU/UK.
2. **Camera-flashlight row has no state.** Nothing in the row changes when the torch toggles (the stand's
   `Test-ToggleExposesState` compares text, description, checked, selected and state description of the row and
   its children). The row is this product's own (no port-ledger row for `LightsActivity`). Fix: the row's
   state description reads "on" or "off", rendered from a torch callback registered in `onStart` and removed in
   `onStop` - the balanced subscription `TorchController` asks of a subscriber that must be authoritative, the
   pattern `FlashlightShortcutNotifier` already uses - so a change from the quick-settings tile or the shade
   shortcut is announced too. The first render uses `TorchController.isTorchOn`; the framework then reports
   the current mode on registration. No visual change: the beam itself is the sighted user's signal, and
   swapping the icon would read as "no flash unit".
3. **Stopwatch result dialog targets and clipping.** The dumps of 2026-10-25 show the actions in a
   `LinearLayout`. Since 2026-10-27 (S0015) the actions sit in `CommandFlowLayout`, which measures each command
   against the full row and wraps a command that does not fit to the next row, and the send button carries
   `minWidth`/`minHeight` of `touch_target_min_height` (48 dp). Nothing left to change; the re-measure is the
   proof (release-tier).
4. **Watch dashboard: unlabelled clickable at font 2.0.** At font 2.0 the dashboard is taller and the Athlete
   chip is cut by the bottom of the list (`[76,374][308,384]`, 10 px visible). Compose leaves a fully clipped
   child out of the accessibility tree, so the chip's label - its `Text` child - is gone while the chip itself is
   still exposed and clickable; a touch on that strip is announced as an unlabelled button. Fix: each command
   chip of `TouristActionsRow` (Reset trip, Reset steps, Athlete, SOS) carries its label as the chip's own
   content description, so it does not depend on a child being visible. `TouristActionsRow.kt` is ported
   (ledger row of 2026-10-25): a local addition, marked as such in the code, the `StandardToolbar` precedent.
5. **Status-bar clock over the Up arrow (Lights).** Closed before this pass: `LightsActivity.setupViews`
   applies `applySystemBarInsetPadding()` to the root (as do Stopwatch, Counter and Tourist info). No change.

### 4.2 Decisions

| Question | Answer | Settled by |
| --- | --- | --- |
| Label for the Up arrow | `@string/back` ("Back" / "Назад" / "Назад") | FastMediaSorter's own idiom, CLAUDE.md section 2 |
| Torch row state source | torch callback while started, `isTorchOn` for the first render | `TorchController` doc comment on authoritative subscribers |
| Visible state on the torch row | none added | no placement change ordered; the beam is the visible state |
| Help prose for the new labels | none | the help describes what is on screen; a label or a state read aloud adds no element and no action |
| Clipped "Reset Steps" label at watch font 2.0 (seen in the same dump, not flagged by the stand) | parked, not fixed here | outside the ticket's rows; touches the shared `WearCommandChip` width rule |

### 4.3 Owner inputs (Approval gate)

- **Related tickets:** S0013 (the deep dynamic audit that found the rows; its stand re-measures them); S0015
  (moved the result dialog's actions into `CommandFlowLayout`, closing row 3); S0019 (made the Lights row toggle
  in place and owns the probe line there); S0031 (stand reader misreads, the same family as the viewport-cut
  label of row 4).
- **Owner decisions needed:** none - every choice in 4.2 is settled by FastMediaSorter's idiom, an existing
  pattern of this tree or the absence of an ordered placement change.

### 4.4 Plan

| Step | Change | Static check |
| --- | --- | --- |
| P1 | `back`, `physical_flashlight_state_on`, `physical_flashlight_state_off` in phone `values`, `values-ru`, `values-uk` | grep: each name once per file (9 lines) |
| P2 | `app:navigationContentDescription="@string/back"` on the six toolbars of 4.1 item 1 | grep: every layout with `navigationIcon` also has `navigationContentDescription` |
| P3 | `LightsActivity`: torch callback in `onStart`/`onStop`, `renderTorchState` sets the row's state description | grep: register and unregister once each; `setStateDescription` on `rowCameraTorch` |
| P4 | `TouristActionsRow`: `semantics { contentDescription = .. }` on the four chips | grep: four `contentDescription =` in the file |
| P5 | none (items 3 and 5 closed before the pass) | grep: `CommandFlowLayout` in `dialog_stopwatch_result.xml`; `applySystemBarInsetPadding` in `LightsActivity` |

Produces/consumes: P1 produces the strings P2 and P3 consume; P3 and P4 are independent. Every step ends in a
grep; compile, unit tests and the device re-measure are release-tier (CLAUDE.md section 7).

### 4.5 Implementation (2026-10-29)

- P1-P4 done as planned; P5 needed no change. Files: phone `values`, `values-ru`, `values-uk` `strings.xml`;
  `activity_lights.xml`, `activity_stopwatch.xml` (portrait and landscape), `activity_tourist_counter.xml`,
  `activity_tourist_info.xml` (portrait and landscape); `LightsActivity.kt`; wear `TouristActionsRow.kt`.
- Phase-boundary audit: the torch callback is registered in `onStart` and removed in `onStop` (balanced, no
  leak); a report already queued on the looper after `onStop` is dropped by a flag, so it never touches a
  binding gone after `onDestroy`. No P0 or P1 left.
- Parked: S0032 (the Reset Steps chip label clipped at watch font 2.0).

## Last Audit

**Date:** 26-10-29 | **Mode:** static | **Outcome:** Verified (static rungs; every proof above a grep is RELEASE)
**Counts:** PASS 8 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 0 | RELEASE 4

- PASS - P1: `back`, `physical_flashlight_state_on`, `physical_flashlight_state_off` once each in `values`,
  `values-ru`, `values-uk` (grep: 3 | 3 | 3; expected 3 per file).
- PASS - P2: every phone layout with `navigationIcon` has `navigationContentDescription` (grep: 7 of 7, the six
  of 4.1 item 1 plus the help dialog).
- PASS - P3: `registerTorchCallback` and `unregisterTorchCallback` once each, `setStateDescription` on
  `rowCameraTorch` (grep, `LightsActivity.kt`).
- PASS - P4: four `contentDescription =` in `TouristActionsRow.kt` (grep; expected 4).
- PASS - P5 / row 3: the result actions are in `CommandFlowLayout`; the send button has `minWidth` and
  `minHeight` `touch_target_min_height` (grep, `dialog_stopwatch_result.xml`).
- PASS - row 5: `binding.lightsRoot.applySystemBarInsetPadding()` in `LightsActivity.setupViews` (grep).
- PASS - UI source gate: `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui` exit 0, 0.542 s,
  `unified-ui-source: PASS` (50 command buttons, three locales).
- PASS - help gate unchanged by this ticket: `pwsh -NoProfile -File tools/fast-check.ps1 -Area help` exit 1,
  1.420 s, exactly the eight known-red findings of S0014 (six `uk/stopwatch-result` bounds, two stale baseline
  entries), none on a surface this ticket touched.
- RELEASE - compile and unit tests: `pwsh -NoProfile -File tools/release/preflight.ps1` (Gradle, both
  variants, phone and wear).
- RELEASE - row 1, 2, 3 re-measure: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1
  -Scenes C3,C7 -Devices phone` - no 8.5c/8.5d/8.5e breach in EN, RU, UK at font 1.0, 1.3, 2.0.
- RELEASE - row 4 re-measure: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1
  -Scenes C2 -Devices watch` - no 8.5d unlabelled node.
- RELEASE - row 2 with a real flash: the device sweep of `tools/release/README.md` reads the row's state
  description after switching the flash from the row and from the quick-settings tile.

No ticket-id probe exists or is needed. Not built, not tested until the release.
