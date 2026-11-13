# S0042 - string-table-mojibake-gate

**Ticket:** S0042
**Status:** Implemented
**Status note:** Implemented 2026-11-13: tools/strings gate (lib, check, self-test, README) plus fast-check areas strings and ui and the preflight rows strings and strings-selftest. Gate PASS on the tree (0.37 s), FAIL on the damaged 2026-10-27 copy, self-test 32 cases PASS (1.7 s), -Area docs PASS. Writer: PowerShell 5.1 Get-Content through ANSI 1251 reproduced, the exact session tool not proven. No compile command, no compiled source edited, nothing built; RELEASE: tools/release/preflight.ps1 after tools/strings is committed.
**Complexity:** medium - three new PowerShell files and one README under tools/strings/, wiring into three existing scripts (fast-check, preflight, CLAUDE.md section 7); no compiled source, no shared-contract boundary (assessed at the spec pass 2026-11-13)
**Date:** 2026-11-13

## Captured request and migration scope

Parked from S0041 (2026-11-13, unattended pass). Symptom: phone/src/main/res/values/strings.xml carried 14 double-encoded sequences (degree sign stored as bytes D0 92 C2 B0, bullet stored as D0 B2 D0 82 D1 9E, a cp1251 reading of UTF-8) on 10 lines, written into the working tree on 2026-11-11 and still present after a further write on 2026-11-13 08:29. S0041 repaired the file by a byte-level replacement; nothing stops the next write from doing it again. Wanted: a mechanical gate that fails when any string table under phone/src/main/res, wear/src/main/res or watchface/src/main/res (all locales) holds a double-encoded sequence (for example U+0412 U+00B0, U+0432 U+0402 U+045E, U+00C2 U+00B0, U+00E2 U+20AC), fast enough for the 5 second tier (wire it into tools/fast-check.ps1 -Area ui or a new area, and into the release preflight), with a self-test that proves it fails on a planted sample. Also find which writer re-encodes the file (a PowerShell 5 Get-Content/Set-Content round trip without -Encoding UTF8 is the first suspect) and record the finding. Evidence: S0041 audit block, byte counts 22121 -> 22086.

## Problem

A writer outside the repository tooling twice left the English string table of the phone module double-encoded
(14 sequences on 2026-10-27 in the build copy of the working tree made for ticket S0018, 14 again on 2026-11-11 in the working tree, still
present after a write on 2026-11-13 08:29), and nothing in the repository noticed: the unified-UI gate reads the
tables only as XML and counts keys, so a table full of `D0 92 C2 B0` passes. S0041 repaired the bytes; no check stops
the next write. The English UI would show garbage in every temperature, heading and detail line.

Shared-contract boundary: none (string resources of this app only; no format another product reads).

## Approach

1. **A byte-level gate in a new folder `tools/strings/`**: `lib.ps1` (rules and generated signature tables),
   `check-string-tables.ps1` (the gate, exit 0/1/2), `test-string-tables.ps1` (the self-test), `README.md`.
   Scope: every XML file of every `values*` directory of every source set of phone, wear and watchface - all
   locales, debug source sets included.
2. **Rules**: strict UTF-8 decode (invalid bytes fail), no BOM, no U+FFFD, no C1 control, no NUL, and the
   double-encoding signatures. The signatures are generated, not listed by hand: about 140 typographic symbols and
   Latin-1 letters read through five code pages (cp1251, cp1252, cp1250, cp866, ISO-8859-1), a three-byte symbol also
   by its first two bytes (so U+00E2 U+20AC alone is found), plus runs of three or more re-encoded Cyrillic letters
   for whole-text damage. The two examples of the capture, U+0412 U+00B0 and U+0432 U+0402 U+045E, are the cp1251
   signatures of the degree sign and the bullet.
3. **Wiring**: `tools/fast-check.ps1` gets area `strings` and `-Area ui` runs the gate before the unified-UI gate
   (so the last output line is still the verdict tickets cite); `tools/release/preflight.ps1` carries `strings` and
   `strings-selftest` checks; CLAUDE.md section 7 names the area and the safe way to edit a table.
4. **The writer**: searched, reproduced and recorded in `tools/strings/README.md` (section "The writer").

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0041 (repaired the file this gate guards; parked this ticket), S0014 (the in-app help gate
  that shares the string tables), S0015 (the unified-UI gate that `-Area ui` pairs it with)
- **Shared contract:** none - tooling for this repository only
- **Reach:** no product code, no manifest, no supported platform, locale or device is touched

### Assumed (unattended, 2026-11-13)

1. **Where the gate lives**: a new folder `tools/strings/` and a new fast area `strings`, with `-Area ui` also
   running it. Weighed against: folding the rules into `tools/ui/check-unified-ui.ps1` (a UI gate would then own an
   encoding rule and the self-test would sit beside an unrelated script) and against a new area only (an edit of
   a table would then run it only when someone remembers the area). Owner may overrule.
2. **Signatures, not a decode round trip**: a "re-encode the line and see whether it is valid UTF-8" rule was tried
   on paper and rejected, because the capital Ukrainian pair that opens the words for "he" and "wind" is, read through
   cp1251, the valid UTF-8 pair of the superscript three. The cost is documented in the README: the superscripts two
   and three damaged through cp1251 alone are not detectable; every other symbol and every other reader is. Owner may
   overrule.
3. **A BOM fails the gate.** Every table in the tree is BOM-free, the repository convention is none, and a BOM is
   the fingerprint of the PowerShell 5.1 `Set-Content -Encoding UTF8` writer. Weighed against tolerating it
   (aapt2 accepts a BOM). Owner may overrule.
4. **The self-test runs at the release tier and in the fast tier by hand**: 1.8 s measured, so it could join
   `-Area strings`, but it only matters after an edit of the tool, so it is wired into preflight and named in
   CLAUDE.md instead of being paid on every table edit. Owner may overrule.
5. **The writer is recorded as "not proven"**, with the reproduction table, instead of being chased through more
   transcripts: the transcripts of this project hold no line that writes the damaged bytes. Owner may overrule.

## Done criteria

1. `tools/strings/check-string-tables.ps1` exits 0 on the current tree and prints `string-tables: PASS`.
2. It exits 1 on the 2026-10-27 build copy of the working tree (`-Root` set to that copy), naming file, line and
   string key; the output is kept in `PLAN/S0042_string-table-mojibake-gate/damaged-copy-gate-output.txt`.
3. The self-test exits 0 and covers: the S0041 bytes, every reader of the library, a whole-text damage case per
   reader, invalid UTF-8, U+FFFD, C1, BOM, UTF-16, discovery of wear and watchface tables in other locales, the exit
   codes 0, 1 and 2, and clean Russian, Ukrainian and English text that must stay unflagged.
4. `tools/fast-check.ps1 -Area strings` and `-Area ui` run the gate and exit 0; the gate takes 5 s or less.
5. `tools/release/preflight.ps1` lists the `strings` and `strings-selftest` checks (parse-checked, run at release).
6. `-Area docs` passes with the new README; every edited and new script parses.
7. The writer finding is recorded.

## Writer finding

No repository script writes a string table. Reproduced on 2026-11-13 (bytes kept in `PLAN/S0042_string-table-mojibake-gate/writer-reproduction.txt`): the system
ANSI code page of this machine is 1251, Windows PowerShell 5.1 `Get-Content` without `-Encoding` reads through it, and
`Get-Content -Raw` followed by `[IO.File]::WriteAllText(.., UTF8Encoding($false))` turns the degree sign `C2 B0` into
exactly the `D0 92 C2 B0` S0041 found, with no BOM; `Set-Content -Encoding UTF8` of the same shell gives the same
bytes behind a BOM; a plain `Get-Content | Set-Content` round trip is lossless. The 2026-10-29 session of this project
that edited the tables used `[IO.File]::ReadAllText` and `UTF8Encoding($false)` and is not the writer. The sessions
that damaged the table left no transcript line that shows the write, so the exact tool is not proven; the suspect is
any step that decodes a table through the ANSI page and encodes it as UTF-8. The rule and the table are in
`tools/strings/README.md`.

## Last Audit

**Date:** 26-11-13 08:51 | **Mode:** static | **Outcome:** Verified
**Counts:** PASS 7 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 0 | RELEASE 1

### Evidence
1. Criterion 1: `pwsh -NoProfile -File tools/strings/check-string-tables.ps1` exit 0, 0.37 s, 23 table files, expected `string-tables: PASS` | actual `string-tables: PASS` - PASS.
2. Criterion 2: the same gate with `-Root` set to the 2026-10-27 build copy (full output in `PLAN/S0042_string-table-mojibake-gate/damaged-copy-gate-output.txt`) exit 1, expected FAIL naming file, line and key | actual `string-tables: FAIL (15 finding(s))`, first line `phone/src/main/res/values/strings.xml:49 [tourist_detail_speed] signature: double-encoded: cp1251 reading of U+2022 (U+0432 U+0402 U+045E)` - PASS. The gate fails on a real damaged file, not only on a planted one.
3. Criterion 3: `pwsh -NoProfile -File tools/strings/test-string-tables.ps1` exit 0, 1.69 s, expected `PASS` | actual `string-tables-selftest: PASS (32 cases)`: the S0041 bytes, 5 readers x 21 symbols, a truncated cp1252 sequence, whole-text damage (2 languages x 3 readers), invalid UTF-8, U+FFFD, C1, BOM, UTF-16 with and without a BOM, wear and watchface tables in other locales (exit 1, both files named), a clean tree (exit 0), an empty tree and a missing file (exit 2), and clean Russian, Ukrainian and English text (4 samples plus an empty file) unflagged. The first run of this test failed 2 cases (cp866 signatures dropped by a too coarse letter filter; UTF-16 without a BOM read as valid UTF-8) and both were fixed in the gate, which is the failability proof of the self-test.
4. Criterion 4: `tools/fast-check.ps1 -Area strings` exit 0 in 0.64 s, `-Area ui` exit 0 in 0.98 s (last line still `unified-ui-source: PASS`) - expected exit 0 and 5 s or less | actual 0 and under 1 s - PASS.
5. Criterion 5: `tools/release/preflight.ps1` lines 158 and 162 carry the `strings` and `strings-selftest` gate rows; the file parses (`-Area scripts`, exit 0). The gates run only at the release - RELEASE item below.
6. Criterion 6: `-Area docs` exit 0 in 1.71 s, `docs-check: PASS (5 checks)`, the new `tools/strings/README.md` is covered by the registry record `tool-readmes` (`tools/*/README.md`); `-Area scripts` exit 0 for `tools/fast-check.ps1`, `tools/release/preflight.ps1`, `tools/strings/lib.ps1`, `check-string-tables.ps1`, `test-string-tables.ps1` - PASS.
7. Criterion 7: the "Writer finding" section of this ticket and `tools/strings/README.md` section "The writer" - three reproduced writer variants with their bytes (`PLAN/S0042_string-table-mojibake-gate/writer-reproduction.txt`), the proven part (ANSI page 1251, PowerShell 5.1 `Get-Content` default) and the unproven part (the exact tool of the 2026-10-27 and 2026-11-11 sessions) - PASS.
8. Scope: new files only under `tools/strings/`; edited `tools/fast-check.ps1`, `tools/release/preflight.ps1`, `tools/ui/README.md`, CLAUDE.md section 7; no string table, no product source touched. Documentation context: affected - `tool-readmes` (new README), CLAUDE.md section 7; unchanged - the help map, the port ledger, the contracts pointers (no shared-contract boundary).
9. Hygiene: the three scripts are pure ASCII (0 non-ASCII bytes), no ticket-id probe in code, no long dash in code, the one catch of the library reports a finding and the one of the gate exits 2 with the reason.

### Action items
1. RELEASE - `pwsh -NoProfile -File tools/release/preflight.ps1` runs the new `strings` and `strings-selftest` checks on the judged commit; `tools/strings/` must be committed before the release, because the preflight judges `git archive` of the commit - not run between releases.

### Manual / on-target
- none required; the gate is source-level and fully proven by its own self-test and by the damaged 2026-10-27 copy.

Compile gate: no compiled source was edited, and `commands.compileCheck` is absent in `.sza-profile.json` - no compile command, nothing built, not tested until the release.

