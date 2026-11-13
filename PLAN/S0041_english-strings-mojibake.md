# S0041 - english-strings-mojibake

**Ticket:** S0041
**Status:** Implemented
**Status note:** Implemented 2026-11-13: 14 double-encoded sequences (7 degree, 7 bullet) on 10 lines of phone values/strings.xml repaired by byte replacement, 10 lines equal HEAD text, diff vs HEAD additions only, XML parses, all three locale tables clean, fast-check ui PASS. No compile command declared, nothing built; RELEASE: tools/release/preflight.ps1. Recurrence guard parked as S0042.
**Complexity:** low - one resource file, 14 byte-sequence replacements, no code, no new files (primitive-spec rung, assessed at the spec pass 2026-11-13)
**Date:** 2026-11-13

## Captured request and migration scope

Parked from S0037 (2026-11-13, unattended pass). Symptom: phone/src/main/res/values/strings.xml in the working tree carries 10 double-encoded (mojibake) lines - for example tourist_unit_degrees reads "Â°"-style bytes 0xD0 0x92 0xC2 0xB0 ("В°") instead of "°", and the bullet "•" reads "вЂў". The committed HEAD version of the same lines is clean (0 mojibake lines in git show HEAD). Affected names include tourist_detail_speed, tourist_detail_coordinates, tourist_detail_sun_time, tourist_unit_celsius, tourist_unit_fahrenheit, tourist_unit_degrees, tourist_detail_true_heading, tourist_detail_elevation_loss, tourist_detail_wind, tourist_detail_level. The English UI would show garbage for degree and temperature units. Evidence: grep -c "В°\|вЂ" phone/src/main/res/values/strings.xml = 10; same grep on HEAD = 0; file mtime 2026-11-11 04:36. Fix: restore those 10 lines to their HEAD text without touching the other uncommitted additions in the file.

## Problem

The English string table `phone/src/main/res/values/strings.xml` carried 14 double-encoded sequences on 10 lines: 7 copies of the degree sign stored as the bytes `D0 92 C2 B0` ("В°" in the file, a cp1251 reading of the UTF-8 pair `C2 B0`) and 7 copies of the bullet stored as `D0 B2 D0 82 D1 9E` ("вЂў", a cp1251 reading of `E2 80 A2`). The English UI would have shown garbage in every temperature, heading, level and detail line. HEAD held none of it (0 lines); the corruption lived only in the uncommitted working-tree additions. The Russian and Ukrainian tables were clean. The file was last written at 2026-11-13 08:29 and still carried the damage, so a writer on this tree re-encodes this file through a single-byte code page; the root cause is not in this ticket's scope (see Parked).

Shared-contract boundary: none (string resources of this app only).

## Approach

A byte-level replacement in the one file, so the other 136 uncommitted added lines, the line endings (LF) and the missing BOM stay exactly as they were: `D0 92 C2 B0` -> `C2 B0` (7 hits), `D0 B2 D0 82 D1 9E` -> `E2 80 A2` (7 hits). No line was retyped by hand and no locale other than `values` was touched. Chosen over restoring the 10 lines from HEAD with an editor: HEAD's text and the repaired text are byte-identical for every affected name, and the byte rule cannot clip a neighbouring edit.

## Done criteria

1. `grep -c "В°\|вЂ"` on `strings.xml` = 0.
2. The 10 affected `<string>` lines equal their HEAD text byte for byte.
3. `git diff --numstat` for the file shows additions only (no deleted line, so no pre-existing HEAD line is altered).
4. The file parses as XML and the three locale tables hold no `В°`, `вЂ` or `Â°` sequence.
5. The unified-UI source gate passes.

## Parked

- Recurrence guard: no gate scans the string tables for a double-encoded sequence, and the damage survived at least two writes. Captured as the separate Draft ticket S0042 (string-table-mojibake-gate).

## Last Audit

**Date:** 26-11-13 08:50 | **Mode:** static | **Outcome:** Verified
**Counts:** PASS 5 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 0 | RELEASE 1

### Evidence
1. Criterion 1: mojibake lines in `strings.xml` before 10 (14 sequences: 7 degree, 7 bullet), after 0 - expected 0 | actual 0 - PASS.
2. Criterion 2: loop over the 10 names compared with `git show HEAD:...` - 10 OK, 0 DIFF - PASS.
3. Criterion 3: `git diff --numstat -- phone/src/main/res/values/strings.xml` before 146/10, after 136/0 - PASS.
4. Criterion 4: `System.Xml.XmlDocument.Load` ok, 291 strings; mojibake count `values` 0, `values-ru` 0, `values-uk` 0; tree scan of `phone/src`, `wear/src`, `watchface/src`, `docs` returned no hit - PASS.
5. Criterion 5: `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui` exit 0, "unified-ui-source: PASS" - PASS.
6. Byte arithmetic: file 22121 -> 22086 bytes, delta 35 = 7*2 + 7*3 - consistent with exactly the 14 replacements.

### Action items
1. RELEASE - `pwsh -NoProfile -File tools/release/preflight.ps1` (full Gradle build with resource merge and the unit suites) - not built, not tested until the release.

### Manual / on-target
- none required; an optional look at the dashboard detail lines on a device (degree sign, bullet) closes nothing that the byte comparison with HEAD has not already closed.

Compile gate: `commands.compileCheck` is absent in `.sza-profile.json`, so the repo declares no compile command - no compile command, nothing was built.
