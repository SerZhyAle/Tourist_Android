# S0030 - Switch off full builds and slow tests between releases (the 5 s line)

**Ticket:** S0030
**Status:** Implemented
**Status note:** Implemented 2026-10-29, not built, not tested until the release: fast tier tools/fast-check.ps1 (ui, help, docs, scripts) is the between-release path; preflight.ps1 adds the full help gate, UI and docs gates, both self-tests and build-logic tests. Help gate stays known-red for S0014.
**Complexity:** hard - assessed at the spec pass: cross-cutting migration of verification paths and release-gate mechanics across scripts, documentation and tickets
**Date:** 2026-10-29

## Captured request and migration scope

Captured from the canon session on behalf of the portfolio owner (2026-10-29). This ticket changes no product code and deletes no test: it moves the full build and every check above 5 seconds to the release path and leaves only the fast tier between releases. It is the portfolio-wide order for every repository; this is the Tourist_mob instance (phone, Wear OS app and watch face).

**Priority: highest - first in this repo's queue, ahead of every other open ticket.**
**Source:** owner order of 2026-10-29 for the whole portfolio, made canon in the same deploy:
TESTING_AND_QA section 2 ("Between releases the ladder stops at the checks that take 5 seconds or less"),
section 5 (the release runs the full build and every tier), the INVARIANTS working stance "Between releases,
no full build and no test longer than 5 seconds", and the `RELEASE` audit outcome in the `spec-to-audit`
skill. Canon version 2026.10.29.2 or later (the `sza` plugin carries it; `claude plugin update sza@sza-unified-rules`).

### Problem

Full builds and multi-minute test suites run while technical pieces, point fixes and small improvements are
being made - after every edit, at every phase gate, at every ticket close, and in branch CI on every push -
while the owner waits. The owner's order: the full build and full testing happen only before or during a
release. Between releases only very fast and fast tests stay; any test, suite or gate longer than 5 seconds
runs only for release actions (or when the owner names that run).

### Do

1. **Inventory every place a build or a test runs outside a release**: the agent-rules file (`CLAUDE.md` /
   `AGENTS.md`), `.claude/commands/` and `.claude/skills/` (build, spec-do, close, audit, sweep commands),
   closure facades and gate scripts (close/complete/validate wrappers, pre-commit or pre-push hooks, watch
   scripts), the task runner's child prompt, and branch CI (`.github/workflows/*` with `push` /
   `pull_request` / `schedule` triggers). List each with what it runs.
2. **Class each test, suite and gate**: *fast* (5 s or less) or *release* (above 5 s). Take the time from
   records that already exist - CI step durations, gate timing logs, dev logs, earlier reports. A check with
   no recorded time is *release* until the next release run measures it. **Do not run a slow suite or a full
   build to time it**; a check expected to be fast may be timed with one direct call.
3. **Make the class machine-readable** where the runner reads it - a test category / tag / filter, a separate
   fast suite or script, or a `-Fast` / `-Release` switch - so "run the fast checks for this area" is one
   command whose run takes 5 s or less. Split a slow suite into a fast filter where that is possible; move a
   slow test to the release tier, never delete it.
4. **Change every between-release path to the fast tier only**: agent rules, project commands and skills,
   closure facades and phase gates, hooks, and the runner's child prompt call only fast checks plus a direct
   call of what changed. Branch CI on push / pull request runs only gates of 5 s or less (docs, links, lint,
   the fast tier) - or nothing; the full build and the release tier move to the release workflow / pre-flight
   so they run against the tag's own tree.
5. **Keep the release whole**: the release pre-flight (release script, release workflow, release doc) runs the
   full build and every tier, the release tier included. Nothing that ran before may disappear from the
   release path. A release gate is never weakened by this ticket.
6. **Write it down in the agent-rules file**: the fast command, the release command, and the measured list
   (check, seconds, date, how measured), plus the rule in one line with a pointer to TESTING_AND_QA section 2.
   A slow rung skipped during a ticket is recorded in its audit as a `RELEASE` item naming the command the
   release will run, and the report says "not built, not tested until the release".

### Must not

- Delete a test, weaken a release gate, or remove a check from the release path.
- Run a full build or any check above 5 s to prove this ticket.
- Trigger a release, push a tag, or change a frozen anchor.

### Done criteria

- The inventory and the fast/release list are in the ticket, each time with its source and date.
- A grep over the agent-rules file, project commands/skills, facades, hooks and branch CI shows no full build
  and no release-tier check on a between-release path (cite the grep and its hits).
- The fast command exists and one call of it for a touched area completes in 5 s or less (cite the timed run,
  its exit code and seconds).
- The release path names the full build and the release tier (cite the lines).
- Report: what was not built and not tested by this ticket (the release run proves the release tier).

### Starting points (seen on a quick look on 2026-10-29, not run - a start for the inventory, not its result)

Agent rules (`CLAUDE.md`; `AGENTS.md` is three lines that delegate to it). This repository carries no `.claude/` directory, no `.github/` directory (no workflows at all; there is no remote yet, `CLAUDE.md` section 4 item 6) and no hooks in `.git/hooks`, so there are no project commands, skills, branch CI or closure facade to change. The between-release paths are:
- `CLAUDE.md` section 6, "In-app help": names the gate and the tools but not when to run them; `tools/help/README.md` ("Order after a change to a screen") says: install the debug build on the emulator, `capture-help.ps1`, `build-help.ps1`, `check-help.ps1`, then rebuild the app - a build, an emulator walk and a rebuild after a screen change.
- `CLAUDE.md` section 5 still says the Gradle modules "arrive with S0002"; it names no build, test, fast or release command (Do, item 6).
- Open and recent tickets carry their own unit-suite done criteria (grep for `gradlew` / `UnitTest`): `PLAN/S0004_*` (lines 104 and 209) and `S0005_*` (lines 70-71) require `:phone:testStoreDebugUnitTest` and `:phone:testNoLegalDebugUnitTest` to exit 0, `S0008_*` (line 127) adds the watch, `S0010_*` (line 107) `gradlew -p build-logic :convention:test`, `S0014_*` (lines 197, 376) `./gradlew :phone:test` (550 tests; `HelpGateTest` runs the help gate inside it). Under the new rule those criteria become a fast-tier check plus a `RELEASE` item naming the suite the release will run. `PLAN/TICKET_TEMPLATE.md` carries no verification step.

Gate, check and self-test scripts that can run between releases:
- `tools/help/check-help.ps1` (the help gate, static half), `tools/help/walk-help.ps1`, `capture-help.ps1` (emulator), `build-help.ps1`.
- `tools/ui/check-unified-ui.ps1` (source properties only - a candidate for the fast tier) and `tools/ui/run-ui-contracts.ps1` (needs the noLegal debug APK built and installed first; 20 first and 20 warm visits per destination across scenarios on a dedicated `S0015_` AVD - release tier).
- `tools/port-ledger/test-port-ledger.ps1` ("after editing these scripts", self-test on a throw-away tree).
- Gradle unit suites of `phone`, `wear`, `watchface` and `build-logic` (`junit`, `mockk`, `robolectric` in the module build files); `HelpGateTest` and `UnifiedUiComponentsTest` live in `phone`.

The release path that must stay whole (do not weaken):
- `tools/release/preflight.ps1` ("start of every release", `tools/release/README.md`): extracts the commit with `git archive` into `temp/rc/src` and runs one Gradle invocation, `gradlew.bat --no-daemon` over `:phone:test :wear:test :watchface:test` and the release and bundle tasks of all three modules (lines 165-173), then judges every package and writes the verdict. `tools/port-ledger/check-drift.ps1` ("start of every release") and `tools/release/update-proof.ps1` belong to the same path.

Times already on record: none found on a quick look. The only figure in the tickets is the S0003 note that one throttle test waits 1.1 s of real time, and `S0019_*` records "298 unit tests green" without a duration. Per Do, item 2, every Gradle suite and every emulator script above is `release` until a recorded time says otherwise; a script expected to be fast (`check-help.ps1`, `check-unified-ui.ps1`, `test-port-ledger.ps1`) may be timed with one direct call.

No runner prompt of this repository's own exists; the unattended runner is the canon's portfolio queue runner (confirm in the inventory). A runner may be on the head ticket when this one is added; this one takes effect from the next pick.


## Owner inputs (Approval gate)

- **Related tickets:** S0014 owns help captures and the existing red help gate; S0010 owns the release pipeline; S0029 owns the document registry. This migration does not change their product decisions.

## Spec assessment and plan

- Rung: complex, because this changes multiple files and release verification mechanics.
- Shared contract boundary: none. Test scheduling changes no shared format, wire shape or reproduced product behaviour. CONTEXT-HELP's existing gate remains mandatory; its findings are not waived.
- Owner input and required research: none open. The order fixes the threshold and preserves all release checks.
- Documentation context: no document registry exists (S0029). The affected records are `CLAUDE.md`, the help, UI, port-ledger and release tool READMEs, and active ticket verification instructions; product README and generated help assets do not change.
- Coverage and topology: P1 inventories and classifies all paths (Do 1-2); P2 supplies fast commands and updates between-release instructions (Do 3-4); P3 carries all checks to release on the candidate tree (Do 5); P4 records commands, timing and audit (Do 6 and all done criteria). P2 consumes P1; P3 consumes P1 and P2; P4 consumes P1-P3. No later artifact is consumed early. No release or product code change.
- Files touched: this ticket; `CLAUDE.md`; `tools/help/README.md`, `tools/ui/README.md`, `tools/port-ledger/README.md`, `tools/release/README.md`, `tools/release/preflight.ps1`; a fast-check script under `tools/`; active tickets whose future gates still prescribe slow tests. No harness or generated file is hand-edited.
- [x] P1 Inventory and classify: inspect commands and recorded timings; verify inventory and classifications are written below.
- [x] P2 Fast path: add a named area-specific command and move between-release instructions to it. Static check: the script declares each area and documents its release counterpart.
- [x] P3 Release path: preserve existing release gates and add missing release-tier checks against the candidate. Static check: every prior check and added check is named in the release instructions and fail-closed verdict.
- [x] P4 Evidence and audit: run the fast command once, inspect the paths and write exact results, skipped release commands and the audit. Static check: all acceptance criteria map to a result.

### Inventory and measurements, 2026-10-29

- `CLAUDE.md` names help tools but no execution timing; `.claude/` and `.github/` are absent; no project branch workflow or child prompt; the shared portfolio harness owns the queue runner. Active hooks: none found. `tools/harness.ps1` forwards to the canon harness; its scripts are not project-owned.
- `tools/help/README.md` instructs a debug install, capture, render, help gate and rebuild after screen edits. `tools/ui/README.md` names the UI static gate, debug build/install and device matrix. `tools/port-ledger/README.md` runs the ledger self-test after edits. `tools/release/preflight.ps1` runs drift, three module test suites and all release assemblies/bundles; `tools/release/update-proof.ps1` is the device update check.
- UI source gate: direct `pwsh -NoProfile -File tools/ui/check-unified-ui.ps1`, exit 0, `unified-ui-source: PASS`, 0.380 s, Stopwatch including process startup: fast.
- Help gate: direct `pwsh -NoProfile -File tools/help/check-help.ps1`, exit 1, `help-check: FAIL (62 finding(s))`, 1.527 s; `-SkipFreshness`, exit 1, `help-check: FAIL (8 finding(s))`, 1.369 s. Both are fast but currently red: stale/missing captures and stale baseline, tracked with help work (S0014; S0018 records the red gate). No findings are waived.
- Port ledger self-test: direct `pwsh -NoProfile -File tools/port-ledger/test-port-ledger.ps1`, exit 0, `20 passed, 0 failed`, 5.685 s: release. The candidate was timed once on the expectation it was fast.
- Gradle module and build-logic suites, full builds, help capture/render/walk, UI device matrix, drift and update proof have no recorded duration at this inventory: release until measured at a release. Historical results in open tickets remain historical; only prospective slow re-run requirements need migration.
### Phase boundary, 2026-10-29

P1 inventory remains open: the initial inventory and timing are recorded, but the complete release-path and active-ticket sweep still needs reconciliation. P2 is in progress: `tools/fast-check.ps1` and the agent, help, UI and port-ledger instructions were changed. The UI and PowerShell areas pass; the help area's direct call returned the existing help failure. The gate stops here. S0014 owns the help captures and baseline: six missing `uk/stopwatch-result` bounds and two stale baseline entries must be repaired before the help fast check can pass. Do not mark P2 done or start P3 until that happens. The release pre-flight has not yet been changed; build-logic tests, full help freshness, the ledger self-test and the device matrix still need an explicit fail-closed release path. The old `S0004` prospective close instruction still needs migration; preserve historical evidence in other tickets.

- `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui`: exit 0, expected `unified-ui-source: PASS` | actual `unified-ui-source: PASS`, 0.569 s including startup.
- `pwsh -NoProfile -File tools/fast-check.ps1 -Area scripts -Path tools/fast-check.ps1`: exit 0, expected parse PASS | actual `fast-check: PASS (1 PowerShell script(s) parsed)`, 0.274 s including startup.
- `pwsh -NoProfile -File tools/fast-check.ps1 -Area help`: exit 1, expected help PASS | actual `help-check: FAIL (8 finding(s))`, 1.437 s including startup. This is not treated as a pass.
- Probe sweep: `rg -n S0030 phone/src wear/src watchface/src` exit 1, expected no ticket probe | actual no hits.
- Documentation boundary: affected records are `CLAUDE.md` and the help, UI, port-ledger and release tool READMEs; the release README remains to be edited. Product README and generated help assets are unchanged. S0029 owns the absent registry.
- RELEASE: `pwsh -NoProfile -File tools/release/preflight.ps1` is not run in this pass; it does not yet include all checks required by P3. The release-tier candidate checks remain incomplete. Not built, not tested until the release.

### Resume and phase boundaries, 2026-10-29 (run-spec-queue pq2)

The block on S0014 is lifted: it misread a tracked known-red as a dependency. P2's static check is that the
fast command declares each area with its release counterpart; the help area runs the help gate in 1.6 s and
reports its verdict, which is the fast tier working. The red belongs to S0014 (six `uk/stopwatch-result`
bounds, two stale baseline entries; S0014 is at `BlockNeedUserTest`), is named as known-red in `CLAUDE.md`
section 7, and stays a FAIL - never waived, never counted as a green. The same reading closed S0023's audit.

**P1 - inventory completed.**

| Path | What it runs | Class | Source of the class |
| --- | --- | --- | --- |
| `CLAUDE.md` sections 6-7, `AGENTS.md` (delegates) | section 7: fast command per area; section 6 names the help tools | fast between releases, release command named | this ticket |
| `.claude/commands/*.md` (8 files: sa, sd, sn, sq, spec-*) | forwarders to the `sza` plugin skills; no build or test (grep `gradle\|test\|build\|preflight`: no hits) | none | grep, 2026-10-29 |
| `.git/hooks` | samples only (no non-sample hook) | none | `ls`, 2026-10-29 |
| `.github/` | absent: no branch CI | none | `ls`, 2026-10-29 |
| closure facade | the canon harness (`tools/harness.ps1` forwarder); `.sza-profile.json` configures no build or test command | none in this repo | profile read, 2026-10-29 |
| runner child prompt | the canon portfolio queue runner (`run-spec-queue`), not project-owned | out-of-scope: canon-owned | lease reason |
| `tools/ui/check-unified-ui.ps1` (`-Area ui`) | UI source gate | fast: 0.380 s direct, 0.572 s wrapped | Stopwatch, 2026-10-29 |
| `tools/help/check-help.ps1 -SkipFreshness` (`-Area help`) | help gate, static half | fast: 1.369 s direct, 1.592 s wrapped (exit 1, known-red) | Stopwatch, 2026-10-29 |
| `tools/help/check-help.ps1` (full) | help gate with freshness | fast by time (1.527 s); runs between releases as `-SkipFreshness`, full at release | Stopwatch, 2026-10-29 |
| `tools/docs/check-docs.ps1 -Check all` (`-Area docs`) | documentation gate | fast: 1.906 s wrapped | Stopwatch, 2026-10-29 |
| `tools/fast-check.ps1 -Area scripts` | parse of named scripts | fast: 0.252-0.274 s | Stopwatch, 2026-10-29 |
| `tools/port-ledger/test-port-ledger.ps1` | ledger self-test | release: 5.685 s | Stopwatch, 2026-10-29 |
| `tools/docs/test-docs.ps1` | docs self-test | release: no recorded time | - |
| `tools/port-ledger/check-drift.ps1` | drift against FastMediaSorter | release (start of every release) | handoff 6a |
| Gradle `:phone:test`, `:wear:test`, `:watchface:test`, release assembles and bundles | unit suites, `HelpGateTest`, `UnifiedUiComponentsTest`, all packages | release: no recorded time | - |
| `gradlew -p build-logic :convention:test` | build-logic unit tests | release: no recorded time | - |
| `capture-help.ps1`, `build-help.ps1`, `walk-help.ps1`, `run-ui-contracts.ps1`, `update-proof.ps1` | emulator and device scripts | release: no recorded time | - |

Tickets: prospective slow instructions searched with `gradlew|UnitTest|:test|assemble|preflight|test-port-ledger|
run-ui-contracts|capture-help` over `PLAN/S0*.md` and `PLAN/TICKET_TEMPLATE.md`. S0004 (lines 104, 209), S0005
(70-71), S0008 (126-128), S0010 (107) and S0014 (197, 376) record runs already made - historical evidence, kept.
S0022 and S0023 already carry `RELEASE` items. S0025 B3 builds a fresh clone once, after the owner's go for the
publication (A·GO): a run the owner names, not a between-release check. The template carries no verification
step. No ticket needed a change.

**P2 - fast path.** `tools/fast-check.ps1` declares `ui`, `help`, `docs`, `scripts` and now documents each area's
release counterpart in its header. Between-release instructions point at it: `CLAUDE.md` section 7,
`tools/help/README.md` line 12, `tools/ui/README.md` line 3, `tools/port-ledger/README.md` line 25,
`tools/docs/README.md` line 36.

**P3 - release path.** `tools/release/preflight.ps1` keeps every prior check (source, drift, plan, leaks, keys,
build with `:phone:test :wear:test :watchface:test` and the ten packages, packages, ids, signatures, version,
families, reach, notices, contracts, privacy, channels) and adds, fail-closed on the judged source: `help`
(full gate with freshness), `ui-source`, `docs` (`-Check all`, in a work tree of its own), `docs-selftest`,
`ledger-selftest`, and `build-logic` (`gradlew -p build-logic :convention:test`). Each FAIL makes the verdict FAIL
(`Write-Verdict`, unchanged). `tools/release/README.md` names them and the device half (help refresh before the
candidate commit, help walk, UI matrix, update proof). `CLAUDE.md` section 7 names the same release contents.

Consequence the owner should know: the help gate is now a release check, so a pre-flight FAILs until S0014
refreshes the captures and the baseline. That is the gate doing its job; it was a FAIL before this ticket too.

**Evidence, this run:**

- `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui`: exit 0, 0.579 s, expected `unified-ui-source: PASS` |
  actual `unified-ui-source: PASS`.
- `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs`: exit 0, 1.87 s, expected `docs-check: PASS` | actual
  `docs-check: PASS (5 checks)`.
- `pwsh -NoProfile -File tools/fast-check.ps1 -Area help`: exit 1, 1.592 s, expected the eight known-red findings
  of S0014 | actual `help-check: FAIL (8 finding(s); 23 surfaces, 9 pages x 3 locales, 612 links, 0 baselined)`,
  the six `uk/stopwatch-result` bounds and two stale baseline entries.
- `pwsh -NoProfile -File tools/fast-check.ps1 -Area scripts -Path tools/release/preflight.ps1`: exit 0, 0.261 s,
  `fast-check: PASS (1 PowerShell script(s) parsed)`; the same for `tools/fast-check.ps1`: exit 0, 0.252 s.
- Direct call of the new gate mechanism on a throw-away working-tree copy (robocopy with the `-WorkingTree` excludes, PLAN kept; deleted afterwards):
  after `git init`, `check-docs.ps1 -Check all -Root <copy>` exit 0, 2.371 s, `docs-check: PASS (5 checks)`;
  `check-unified-ui.ps1` exit 0, 0.355 s, PASS; `check-help.ps1` (full) exit 1, 1.322 s, `help-check: FAIL (62
  finding(s); ..)` - the known-red with freshness; `.git` removed afterwards: True. Without the nested work tree
  the docs gate also printed PASS, because the outer `.gitignore` turned every registry into a local one - a
  green for the wrong reason, which is why the pre-flight gives the copy a work tree of its own.
- Between-release grep: `grep -n -E 'gradlew|:test\b|assemble|bundle[A-Z]|UnitTest|preflight|test-port-ledger|
  test-docs|run-ui-contracts|capture-help|walk-help|build-help' CLAUDE.md AGENTS.md .claude/commands/*.md
  tools/*/README.md tools/fast-check.ps1`: 20 hits, each on a release path (`tools/release/README.md`, rows
  marked "at release" or "start of every release", the "At release" paragraphs of the help and UI READMEs,
  `CLAUDE.md` line 128 "Release:", the tool list of `CLAUDE.md` section 6 line 105, the release column of
  `fast-check.ps1`). None on a between-release path. Non-sample hooks: none.
- Probe sweep: `grep -rn S0030 phone/src wear/src watchface/src`: exit 1, no hits.
- Documentation: `CLAUDE.md`, `tools/release/README.md` edited; both already registered (`-Area docs` PASS above).
  Product README, help assets and other tickets unchanged.

## Last Audit

**Date:** 26-10-29 21:00 | **Mode:** static | **Outcome:** Verified (static and fast rungs; every proof above 5 s is RELEASE)
**Counts:** PASS 8 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 1 | RELEASE 2

- PASS - done 1: the inventory and fast/release list with source and date are in this ticket (table above).
- PASS - done 2: the between-release grep above - 20 hits, every one on a release path; no hook, no CI.
- PASS - done 3: `tools/fast-check.ps1 -Area ui` exit 0 in 0.579 s; `-Area docs` exit 0 in 1.87 s.
- PASS - done 4: the release path names the full build and the release tier - `tools/release/preflight.ps1`
  header lines 11-35 and the `gates` and `build-logic` stages, `tools/release/README.md` lines 45-55 and 70-79,
  `CLAUDE.md` section 7 "Release:".
- PASS - Must not: no test deleted, no release check removed (every prior `Add-Check` id still present), no
  release, tag or anchor touched, nothing above 5 s run (the ledger self-test was not re-run).
- PASS - P2 static check: the area table in the `fast-check.ps1` header, each with its release counterpart.
- PASS - help gate unchanged by this ticket: `-Area help` exit 1 with exactly S0014's eight findings; tracked
  known-red, not counted as green.
- PASS - probe invariant: no `S0030` line in module sources; this ticket changes no product code.
- EXEMPT - the queue runner's child prompt: canon-owned (`run-spec-queue`), not in this repository.
- RELEASE - the extended pre-flight end to end: `pwsh -NoProfile -File tools/release/preflight.ps1` (the new
  `help`, `ui-source`, `docs`, `docs-selftest`, `ledger-selftest` and `build-logic` checks, with the Gradle
  build). Expected FAIL on `help` until S0014 refreshes the captures.
- RELEASE - the device half: `tools/help/walk-help.ps1`, `tools/ui/run-ui-contracts.ps1`,
  `tools/release/update-proof.ps1` per `tools/release/README.md`.

Not built, not tested until the release.