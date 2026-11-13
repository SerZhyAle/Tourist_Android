# S0032 - Watch dashboard: the Reset Steps chip label is clipped at font 2.0

**Ticket:** S0032
**Status:** Implemented
**Status note:** Implemented 2026-10-29, not built, not tested until the release: no code change - the 2026-10-25 dump measured the verbatim port (Row of fixed-height CompactChips); since S0015 (2026-10-27) the resets are WearCommandChips in a FlowRow, which measures each chip against the full line and wraps. Release: locale-a11y C2 watch re-measure, label widths read by hand in the three font-2.0 dumps. Parked: S0034.
**Complexity:** low - spec pass (2026-10-29), confirmed: no code change; the clipping layout left the tree with
S0015's Wear command migration, and the only remaining proof is a device re-measure, release-tier
**Date:** 2026-10-29

## Captured request and migration scope

Parked by S0023 (2026-10-29), seen in the same S0013 dump and not flagged by the stand (its clipping reader
only looks at text nodes cut by their parent or by the screen).

- Symptom: on the watch Tourist dashboard at font scale 2.0, "Reset Trip" and "Reset Steps" stay on one
  `FlowRow` line; the Reset Steps chip is measured 78 px wide and its label 12 px wide, so the text is cut to
  a sliver. Same in EN, RU and UK.
- Evidence: the C2 watch section of `PLAN/S0023_accessibility-labels-state-targets/s0013-extract.md`
  (Reset Steps chip `[252,290][330,368]`, its label `[285,303][297,355]`); at font 1.0
  both chips are whole.
- Suspected cause, not proven: `WearCommandChip` sizes itself with `width(IntrinsicSize.Max)` coerced to the
  incoming constraints, so a chip measured into the remaining width of a line shrinks instead of wrapping.
  `WearCommandChip` is shared by every watch screen, so the fix and its re-measure are wider than the one row.
- Budget: S0013 8.5d (nothing clipped at font 2.0). Done when the C2 watch scene at font 2.0 shows both reset
  chips whole (wrapped or not) in EN, RU and UK.

## Spec pass (2026-10-29, unattended queue pass)

Rung: primitive spec - nothing in the product changes (zero files), so Problem / Approach / Done criteria below
carry the whole pass. Shared-contract boundary: none - a watch layout, no format, wire shape, algorithm constant
or Data Layer message (`docs/contracts/` pointers name nothing here). No research item is open and no placement
is decided here: the row already wraps; no owner input is needed.

### Problem - what the dump measured

- The dump is older than the code the capture blamed. S0013 run `20261025-p5-watch`, scene `C2-watch-r1`,
  `m7-compass-after-storm-en-US-2.xml`, written 2026-10-25 22:06. `WearCommandChip.kt` was written 2026-10-27
  14:34; no C2 watch dump exists after that time. Extract (all three locales, the timeline, the `FlowLayout.kt`
  lines): `PLAN/S0032_watch-command-chip-label-clipped-at-large-font/evidence-extract.md`. In RU and UK the
  Reset Steps chip has no text child at all - the label is cut so far that it leaves the accessibility tree.
- On 2026-10-25 `TouristActionsRow` was the verbatim port: FastMediaSorter's file still hashes `584f6820..`, the
  ledger row of 2026-10-25, and it lays the two resets in a plain `Row` of Wear `CompactChip`s. A `Row` measures
  its children in order and hands the second only what the first left, and `CompactChip` has a fixed height
  (`ChipDefaults.CompactChipHeight`, `compactChipModifier` in compose-material 1.4.1 `Chip.kt`), so a label
  that does not fit is cut, not wrapped. That matches the dump exactly: the two chips abut (`252` ends one and
  starts the other), Reset Trip takes 184 px and Reset Steps gets the 78 px left, its label 12 px wide.

### Approach - why the current tree no longer clips, and why nothing changes

- Since 2026-10-27 (S0015, "migrated Wear .. ordinary command shape") the row is three `FlowRow`s of
  `WearCommandChip` (grep below).
- `FlowRow` does not measure a child into the remaining width. Compose foundation-layout 1.7.6 (BOM
  2024.12.01), `FlowLayout.kt` `breakDownItems`: every fixed-size child is measured once with main-axis
  constraints `0..mainAxisMax`, the full line width (lines 1184-1194, 1214-1222, 1296-1305), and a child that
  does not fit the line's remainder starts the next line. The capture's suspected cause does not hold here.
- `WearCommandChip` takes `width(IntrinsicSize.Max)` - the label's one-line width plus padding - capped by that
  full line width, and the Wear `Chip` under it adds `height(IntrinsicSize.Min)` (`chipSizeModifier`). The labels
  set no `maxLines`. So a chip is never narrower than its one-line label unless the label is wider than the
  whole line, and then the label wraps and the chip grows taller. At font 2.0 the two resets do not fit one line
  together (the old dump already shows Reset Trip alone taking 184 of the 262 px the pair spanned), so they
  stand on two lines, each whole; at font 1.0 they share one line as before.
- The shared chip therefore stays as it is, and no other watch screen is touched. S0023's content descriptions
  on the same chips change no measurement.

### Done criteria

- [x] The clipping layout is gone from the tree: `TouristActionsRow` has no `Row(` and no Wear `CompactChip`; its
      chips are `WearCommandChip` in `FlowRow`s; the chip sizes from `IntrinsicSize.Max` with only a minimum
      height (grep).
- [ ] RELEASE - the C2 watch scene at font 2.0 shows both reset chips whole in EN, RU and UK: re-run the stand and
      read the three font-2.0 dumps by hand, because the stand's verdict cannot see this symptom (parked S0034).

## Last Audit

**Date:** 26-10-29 | **Mode:** static | **Outcome:** Verified (static rungs; the device re-measure is RELEASE)
**Counts:** PASS 4 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 0 | RELEASE 2

- PASS - row layout: in `wear/.../apps/tourist/TouristActionsRow.kt`, `FlowRow(` 3, `Row(` or a
  `layout.Row` import 0, `material.CompactChip` import 0, `CompactChip(` calls 4 - the alias of
  `WearCommandChip` (grep; expected 3 | 0 | 0 | 4).
- PASS - chip sizing: `WearCommandChip.kt` line 30 `modifier.width(IntrinsicSize.Max).heightIn(min = 48.dp)`,
  no fixed `height(` and no `maxLines` in the chip or the row's labels (grep).
- PASS - wrap rule: foundation-layout 1.7.6 sources jar from the Gradle cache, `FlowLayout.kt` lines 1184-1194
  and 1296-1305 measure every fixed-size child against the full line (read; lines quoted in
  `evidence-extract.md` of this ticket's folder).
- PASS - docs gate after this spec and the parked S0034: `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs`
  exit 0, 1.914 s, `docs-check: PASS (5 checks)`, 191 files declared.
- RELEASE - compile: `pwsh -NoProfile -File tools/release/preflight.ps1` (no source changed by this ticket).
- RELEASE - re-measure: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1 -Scenes C2
  -Devices watch`, then in each of `m7-compass-after-storm-en-US-2.xml`, `-ru-RU-2.xml`, `-uk-UA-2.xml` each Reset
  chip's label `TextView` is at least half its chip's width (2026-10-25: 12 of 78 px).

No ticket-id probe exists or is needed. Not built, not tested until the release. Parked: S0034.
