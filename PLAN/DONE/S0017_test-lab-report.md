# S0017 - Report the test lab to the portfolio - equipment, test practice and contract proof (TEST-LAB 0.1)

**Status:** Implemented
**Status note:** Implemented 2026-10-23: TEST-LAB report filed in the catalog as PROPOSAL-2026-10-23-tourist-android-inventory.md (status proposed, parts A-E, owner batch O1-O4), pointer docs/contracts/TEST-LAB.md and the registry row written, spec_catalog/validate exit 0. Documentation only: no code, test or harness changed, no device or emulator touched. Open questions for the platform owner are in report section E2. Not placed in a release package by hand.
**Phase:** outside the handoff phase plan (handoff section 8) - a cross-cutting documentation ticket
**Verification level:** document - the report exists in the catalog in the form of `TEST-LAB` section 3, with a source or `unknown` on every item; no device is touched

## 0. Source

- **Origin.** The portfolio owner ordered on 2026-10-23, through the canon session: collect from every project what
  it already knows about the hardware zoo (devices, watches, machines), the emulators and virtual machines, and the
  way its products are tested, and make inter-project contracts of it. Owners by platform: Android - FMS_A, Windows -
  FMS_Win, web - the sza.od.ua hub. The catalog now holds the draft `TEST-LAB` 0.1 (its home is the `test-lab/`
  folder of the catalog, located through `CLAUDE.md`); catalog `RULES` section 2 records the carve-out that makes
  this a contract.
- **This repository's platform:** Android (phone app, Wear OS app and watch face under one application id; variants
  `store` and `noLegal`). **Platform owner:** FMS_A (`P:\ANDROID\FastMediaSorter_mob_v2`), which writes and owns the
  `TEST-LAB-ANDROID` profile. **This repository's role:** reporter only.
- **Contract.** `TEST-LAB` 0.1, draft, owner shared; this product is a named consumer. Sections used: 1 (scope), 2
  (the family), 3 (the collection form, parts A to E), 4 (the five rules of the collection).
- **Canon.** `TESTING_AND_QA` owns the tiers, the commands and the harness mechanics; 0.1 of the contract does not
  move them, so this ticket does not restate or change them.
- **This repository.** `CLAUDE.md` (agent rules and the one place that names the catalog), `docs/contracts/README.md`
  (the pointer index), `PLAN/` (tickets and the archive in `PLAN/DONE/`).

## 1. Goal

This repository files one report of what it knows, in the form of `TEST-LAB` section 3, so the platform owner can
fold it into the Android profile. Nothing more is changed: no harness, no test, no behaviour.

Why it matters here: Tourist is an extraction with little built so far (S0002 is the only implemented ticket), so most
of its test practice is still written as plans (S0013, S0014 and the device acceptance lines of S0004 to S0009). The
report therefore separates what was done on the two emulators in S0002 from what is only planned, and records the
planned equipment as `planned`, never as owned. A hike kit also depends on conditions a desk does not give - a
satellite fix, a compass and a barometer, a phone-watch pair, a wet screen - so the report states plainly which of
those the current equipment can show and which only hardware can.

## 2. What to do

1. Read `TEST-LAB` sections 1 to 4 in the catalog. They are the whole instruction for the form of the report.
2. Inventory **this repository only**. Search its docs, scripts, harness, CI definitions (none exist: there is no
   `.github/`), agent-rules file (`CLAUDE.md`; `AGENTS.md` only delegates to it), the canon repository's
   `rules/contrib/tourist_mob.md` (read only; a search on 2026-10-23 found no device or emulator line in it),
   tickets and the ticket archive for devices, emulators and AVDs, virtual machines, browsers, hosts, peripherals,
   test services, test tiers and commands, "verified on hardware" language, recurring traps and the contracts whose
   conformance asks for a device. Read what you find instead of judging by file name. Points this product must
   answer, each with its source or `unknown`:
   - A2 and B2: the emulator images behind the S0002 installs (API level, ABI, phone and Wear OS), and how a
     synthetic location fix, a sensor value and the phone-watch pairing are fed to them - the sources found so far
     record none of these.
   - A1 and B2: whether any physical phone, watch, TV box or gamepad exists and what it proves; S0013 decision 8.2
     (emulator-only or one physical pair) is open, and S0004 asks for "a real fix on a phone".
   - A4: peripherals the signature behaviours need - speaker and torch for SOS, volume and media keys for the water
     flashlight, the watch hardware key and rotary - and where location, network and permission conditions are
     handled (location is foreground-only through the platform `LocationManager`; no network permission is declared;
     S0013 plans no network-denial tests).
   - C2: for `INPUT-PARITY` (touch, hardware keys, gamepad, TV remote) and `PACKAGE-VERSIONING` (version read-back
     on both emulators), whether the equipment in A can prove the conformance and what is missing.
3. Ask the owner in one batch for what is not written anywhere (the devices that exist only in their head: a model,
   an OS version, which product uses which). Questions the first read already raises: which physical phones or
   watches exist, if any; which emulator images were used in S0002 and whether they are shared with FMS_A or
   StreamsPlayer_mob; how a location fix was or will be injected; whether the phone-watch pairing follows a recorded
   recipe. Put the unanswered questions in the report as `unknown`.
4. Write the report as `PROPOSAL-YYYY-MM-DD-tourist-android-inventory.md` in the catalog's `test-lab/` folder (the
   filing date in the name), in the form of section 3 (parts A to E). Status line: `proposed`. Every item names its
   source (a repository file, or "owner's statement") or says `unknown`. Say what you did not verify.
5. No secrets in the report: no serial numbers, tokens, passwords, private addresses or account names - an alias and
   a role (`TEST-LAB` rule 2). Host paths are written without the Windows account name.
6. Add a pointer file `docs/contracts/TEST-LAB.md` (a few lines: id, version, home in the catalog, this repository's
   role) with its row in `docs/contracts/README.md`, and one adoption row for `TEST-LAB` in the catalog registry
   (`Implements -` while the report is missing, `0.1` and the date once it is filed). `CLAUDE.md` names the
   catalog once, already; it is not edited.
7. Do not change any code, test or harness. A finding that needs a change becomes its own ticket.

## 3. Known sources (found when the ticket was written, 2026-10-23)

A start for the search, not its result.

- `PLAN/S0002_gradle-skeleton-and-port-ledger.md` section 4 - install and version read-back on a phone and a wear emulator; image names not recorded
- `gradle/libs.versions.toml` - platform levels: compileSdk 37, targetSdk 36, minSdk 26 phone and 28 wear, watch face 36
- `CLAUDE.md` sections 3 and 5 - location is foreground-only through `LocationManager`; the Android SDK path on this machine
- `PLAN/S0013_deep-dynamic-audit.md` sections 5 to 7 and 3.3 (8.2) - host-side adb stand, disposable AVD pair, backup gate, invalid-run rule; Draft, nothing built
- `PLAN/S0013_deep-dynamic-audit.md` sections 1.1 and 12 - no network-denial tests; emulator has no real GNSS and no phone barometer; pairing instability
- `PLAN/S0004_phone-shell-and-dashboard.md` - verification level "a real fix on a phone"; the location and activity-recognition grant flow
- `PLAN/S0003_core-port.md`, `docs/BOOTSTRAP_HANDOFF.md` sections 3 and 5 - GNSS and sensor sources; the permission list (no network permission)
- `PLAN/S0005_phone-lights.md`, `S0006_phone-sos.md`, `S0007_phone-stopwatch.md` - "device" verification level: wet-touch lock, SOS in three modes, stopwatch with several participants
- `PLAN/S0008_wear-app.md`, `S0009_watch-face.md` - "Wear OS emulator or watch"; Data Layer pair; hardware-key touch lock; barometer
- `PLAN/S0014_in-app-help.md` sections 4 and 5.4 - walk matrix by locale and theme; F1 on a hardware keyboard; synthetic fix and step count for screenshots
- `docs/contracts/INPUT-PARITY.md` - conformance asks for touch, hardware keys, gamepad and TV remote
- `docs/contracts/PACKAGE-VERSIONING.md` - version bands read back on the two emulators; band proposal filed 2026-10-23
- `tools/port-ledger/test-port-ledger.ps1`, `build-logic/convention/src/test/` - the host-only tests that run today (port-ledger self-test, build-logic unit tests)
- Searched and not found: mock-location or `adb emu` geo-fix practice, any network-condition test, CI definitions, a device line in the canon contrib record

## 4. Done when

- [x] 1. The report exists in the catalog, in the form of section 3, with sources or `unknown` on every item.
      Filed 2026-10-23 as `PROPOSAL-2026-10-23-tourist-android-inventory.md` in the catalog's `test-lab/` folder,
      status `proposed`, parts A to E plus a closing list of what was not verified.
- [x] 2. The pointer file `docs/contracts/TEST-LAB.md`, its row in `docs/contracts/README.md` and the registry row exist.
      The registry row (`TEST-LAB` / Tourist Android, role P, Implements 0.1, Verified 2026-10-23) sits beside the
      other Android products' `TEST-LAB` rows.
- [x] 3. No file in this repository restates the catalog text; nothing was changed in code, tests or harness.
      The pointer is a few lines and cites section 3 by number only; `CLAUDE.md` is not edited.
- [x] 4. The repository's own ticket validator runs clean: `pwsh -NoProfile -File tools/harness.ps1 spec_catalog/validate`,
      exit 0 (8 OK, 1 WARN, 0 FAIL; the warning is the older S0001 hygiene note about its archived file, not
      touched here).
- [x] 5. The owner is told which questions are still open: the seven of the report's section E2 (below).
- [x] 6. The journal row was inserted through the harness, never by hand (the row exists from the ticket's
      creation; the status change below went through `spec_catalog/update`).

## 5. Progress

Done 2026-10-23.

- **Owner batch (one round, four questions):** O1 the physical phones and watches are FastMediaSorter Android's;
  O2 the emulator images are the existing shared stands, described in the Android profile, no alias named;
  O3 the synthetic fix and sensor values are fed through the emulator console or adb; O4 the pairing follows the
  FastMediaSorter Android recipe. None names a model, version or alias, so those stay `unknown` in the report.
- **Read or searched:** `CLAUDE.md`, `README.md`, `docs/`, the tickets S0001 to S0016 (S0002, S0004, S0008, S0013,
  S0014 in full; the others by search for equipment and test terms), `gradle/`, `build-logic/`, `tools/`,
  `.sza-canon.json`, `.sza-profile.json`; in the catalog `test-lab/` (core, Android profile, the FastMediaSorter
  report) and the registry; the canon contribution record (no device line, as found when this ticket was written).
- **Not run:** no device, emulator, Gradle or test command (the closing section of the report lists what was not
  verified).
- **What the report found that no ticket says:** S0002 records no emulator image, API level or ABI (the Wear
  emulator is at API 36 or higher by inference: it took the watch face, minSdk 36); S0013's "images at the S0002
  pins" therefore names no image; "device" in the tickets does not separate emulator from hardware; no recorded stand
  covers phone API 26 to 29 or Wear API 28, though S0008 asks for the oldest supported OS; S0004's "FastMediaSorter
  not installed" collides with the shared phones; nothing records how a wet touch would be produced.
- **Open questions (report section E2):** which alias carried the S0002 installs; may the S0013 stand create its
  own disposable AVDs beside the shared ones; which fleet devices Tourist may install on and how S0004's state is
  met; are stands below the FastMediaSorter floor wanted; one injection and pairing recipe in the Android profile;
  are a TV, head unit, gamepad or remote Tourist targets; the equipment and procedure for the wet-touch proof.
  Every finding that needs a change becomes its own ticket when the owner asks; none was started here.
