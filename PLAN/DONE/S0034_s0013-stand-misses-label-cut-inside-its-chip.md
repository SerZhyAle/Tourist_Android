# S0034 - S0013 stand: a label cut inside its own control is not flagged as clipped

**Ticket:** S0034
**Status:** Implemented
**Status note:** Implemented 2026-10-29, not built, not tested until the release: 8.5d reader gains a 3 sp-per-glyph width floor and a dropped-label rule against the font-1.0 dump; the 2026-10-25 C2 watch dumps now report Reset Steps clipped in EN, RU, UK. Release: readers-selftest.ps1, locale-a11y C2/C5. Parked: S0035.
**Complexity:** low - primitive spec (confirmed at the spec pass 2026-10-29)
**Date:** 2026-10-29

## Captured request and migration scope

Parked by S0032 (2026-10-29) as an out-of-scope finding; nothing was changed for it.

- Symptom: the stand's 8.5d clipping reader (`PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1`) flags a
  text node only when its bounds are cut by its parent or by the screen. A label squeezed inside a control that
  is itself too narrow keeps bounds inside its parent and passes, even when the text drawn is a sliver.
- Evidence: `m7-compass-after-storm-en-US-2.xml` of the S0013 run `20261025-p5-watch` (watch, font 2.0), quoted
  in `PLAN/S0023_accessibility-labels-state-targets/s0013-extract.md`: the "Reset Steps" chip
  `[252,290][330,368]` holds its label `[285,303][297,355]` - 12 px for eleven characters - and the cell
  reported `clipped []`. S0023 and S0032 both had to read this by eye.
- Consequence: the stand cannot prove S0032's done criterion; its release re-measure reads the bounds by hand.
- Done when the stand flags a text node whose width cannot hold its text at the scene's font scale (for
  example narrower than one glyph per character of its longest word), and the 2026-10-25 dump above is
  reported as a clipped `Reset Steps` in EN, RU and UK.

## Rung

Primitive spec: three existing stand files change (`_instruments.ps1`, `scenes.ps1`, `readers-selftest.ps1`), no
new file, no new type, no DI, screen or schema, one deterministic threshold, well under 100 lines. No
shared-contract boundary: the stand is this repository's own test tooling; no other product reads its output.

## Problem

The 8.5d reader judges each text node against its parent and the screen only. The 2026-10-25 font-2.0 dumps of
`C2 watch` show the symptom in two shapes, and the reader misses both:

- EN: the label node is present but 12 px wide (`[285,303][297,355]`) for "Reset Steps" - inside its chip.
- RU and UK: the label node is absent - the chip (`[264,295][341,372]` RU, `[276,298][353,375]` UK) is fully on
  screen and holds no text at all, while the same chip at font 1.0 carries "Сброс шагов" / "Скидання кроків".
  Only a cross-scale comparison can see this; a single dump holds nothing to measure.

## Approach

1. Width floor (`Get-A11yFacts -FontScale`): a text node whose width is below `3 sp x font scale` per character of
   its longest word is clipped. 3 sp is under the narrowest legible glyph at the smallest body size in use, so a
   label above the floor is never flagged by it; the rule is off when the caller passes no font scale. A node
   cut by its scroll viewport is not judged, as for touch targets.
2. Dropped label (`Get-DroppedLabels`): a clickable node of the font-1.0 dump that has a label, matched by its
   index path in the font-2.0 dump of the same locale, that is fully inside its scroll viewport there and has no
   label any more, is clipped. A node that touches the viewport edge is scrolling, not squeezed, and is skipped.
3. A clipped text node without a resource id is named by its text, so the finding reads `Reset Steps`, not
   `TextView`.
4. `Invoke-M7Sweep` passes the scale and the locale's font-1.0 dump; `readers-selftest.ps1` gains fixtures for
   both rules and for the scroll exemption.

## Done criteria

- The three 2026-10-25 dumps (`m7-compass-after-storm-{en-US,ru-RU,uk-UA}-2.xml`, run `20261025-p5-watch`), read
  by the changed reader with their font-1.0 dumps, report `Reset Steps`, `Сброс шагов`, `Скидання кроків` as
  clipped; the top chip that scrolls out (`Speed`) is not reported.
- No other font-2.0 dump of the 2026-10-25 runs gains a clipped entry that the dump does not show (read per
  entry).
- `readers-selftest.ps1` carries the fixtures; it runs at the release (no recorded time).

## Last Audit

**Date:** 26-10-29 21:45 | **Mode:** static | **Outcome:** Implemented (not built, not tested until the release)
**Counts:** PASS 6 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 0 | RELEASE 2

- PASS - done criterion 1, direct call of the changed reader on the 2026-10-25 dumps
  (`PLAN/S0034_s0013-stand-misses-label-cut-inside-its-chip/attachments/probe-c2.ps1`, exit 0, 0.8 s): EN `width-on [Reset Steps]` (before: `[]`), RU `dropped [Сброс
  шагов]`, UK `dropped [Скидання кроків]`; the scrolled top chip (`Speed`) is not reported. expected: Reset Steps
  clipped in EN, RU, UK | actual: as listed.
- PASS - done criterion 2, every font-2.0 M7 dump of the 2026-10-25 runs through old and new reader
  (`PLAN/S0034_s0013-stand-misses-label-cut-inside-its-chip/attachments/sweep-runs.ps1`, 63 dumps, exit 0, 1.2 s): the only new entries are the three C2 watch labels above
  and `btnSosModeLightOnly` in `C5-phone-r1` EN/RU/UK, which the dumps show 3 px wide (`[1035,1468][1038,1620]`
  EN) - a real clip, parked as S0035, not a false positive.
- PASS - the new fixture section of `readers-selftest.ps1` run on its own (`PLAN/S0034_s0013-stand-misses-label-cut-inside-its-chip/attachments/section6.ps1`, exit 0,
  0.75 s): 4 of 4 checks ok, including the pre-S0034 reader missing the narrow label.
- PASS - `tools/fast-check.ps1 -Area scripts` on `_instruments.ps1`, `scenes.ps1`, `readers-selftest.ps1`: exit 0
  each (0.28-0.30 s), `PASS (1 PowerShell script(s) parsed)`.
- PASS - `tools/fast-check.ps1 -Area docs`: exit 0, 1.96 s, `docs-check: PASS (5 checks)`; `spec_catalog/validate`
  exit 0, `8 OK, 2 WARN, 0 FAIL`.
- PASS - probe invariant: no `S0034` line in `phone`, `wear`, `watchface` sources; the stand is not shipped code.
- RELEASE - the whole fixture check: `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/readers-selftest.ps1`
  (no recorded time).
- RELEASE - the `Invoke-M7Sweep` wiring (`-FontScale`, `Get-DroppedLabels` against the locale's font-1.0 dump) is
  checked by inspection only; it runs live in
  `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1 -Scenes C2,C5 -Devices watch,phone`.

### Action items
1. None in scope. Parked: S0035 (SOS `Light only` mode button cut to 3 px at font 2.0).
