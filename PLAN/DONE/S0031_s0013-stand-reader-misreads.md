# S0031 - S0013 stand readers misjudge notifications and torch

**Ticket:** S0031
**Status:** Implemented
**Status note:** Implemented 2026-10-29, not built, not tested until the release: notification count de-duplicated by identity, torch reader ignores a dead client's on entry, F8-watch judge re-opens the stopwatch (stopwatch-resumed, draft outcome set), D1 quiet window freed of the water flashlight; 25 fixtures green in 2 s (stand/readers-selftest.ps1), pipelines run on a device shell. RELEASE: run-matrix.ps1 -Scenes F8,D1,D3 -Devices phone -Repeat 3 and -Scenes F8 -Devices watch -Repeat 3; S0013 decision 8.7 for the verdicts to count.
**Complexity:** medium - rewritten at the spec pass (2026-10-29), confirmed at the plan pass: three readers in two stand libraries, one scene driver in `scenes.ps1`, the draft outcome set and one new fixture check; no product code; the acceptance runs are release-tier
**Date:** 2026-10-29

## Captured request and migration scope

Parked by S0022 (2026-10-29): two more S0013 stand readers misjudge the app, after the torch reader of S0019
section 4.4 - the second sighting of a stand misread, which S0013 section 12 turns into a stand defect ticket.

1. Notification count includes the enqueue queue. `_instruments.ps1` counts `NotificationRecord(.*pkg=<pkg>`
   lines between `Notification List:` and `Snoozed notifications:` of `dumpsys notification --noredact`. On
   API 33 (AOSP android13-release `NotificationManagerService.dumpImpl`) that range holds both the posted
   `Notification List:` and the `Enqueued Notification List:`, and with a notification assistant bound every
   post waits `DELAY_FOR_ASSISTANT_TIME = 200` ms in the enqueued list. The D3 storm re-posts the one SOS
   notification (same id) on every mode flip, so a sample taken inside that window counts the same key twice:
   D3-phone r1 shows `notifs=2` in 4 of 14 samples of the SOS span while `fgServices` stays 1. Fix: count only
   the `Notification List:` section, or count distinct keys.
2. F8-watch judge treats an ongoing notification without a process as stale. `scenes.ps1` (F8 watch driver)
   reports `stale-notification-left` whenever `notifs > 0` and no pid. The watch stopwatch is a stored time
   origin: the notification's chronometer is counted by the system, the measurement is persisted and restored
   after process death (and rebased after a reboot), so a live notification with no process is the designed
   state. The judge never re-opens the stopwatch to read whether it resumed; the cold start after the kill
   lands on the program list (`ui-01-after-hard-kill.xml` r1; r2 and r3 dumps are empty). Fix: re-open the
   stopwatch after the kill and judge `stopwatch-resumed` (running time >= the time at the kill) against
   `ended-cleanly`; the allowed set is S0013 decision 8.7.
3. Carried from S0019 section 4.4: the torch reader counts a `turned on for client PID <p>` entry of a dead
   PID, and D1's `torch-storm` quiet window starts before the water flashlight is closed.

Done when the three readers are fixed and F8 (phone, watch), D1 and D3 each give the app's real outcome
3 of 3 on the S0013 profiles.

## 1. Spec pass (2026-10-29, unattended queue pass)

**Rung:** complex. The primitive test fails on: more than 3 files change (`_instruments.ps1`, `scenes.ps1`,
`outcomes.json`, `INDEX.md`), a new script is added, and one item touches an owner-gated set (decision 8.7).

**Shared-contract boundary:** none. The stand is a measurement tool of the S0013 tactical folder
(`PLAN/S0013_deep-dynamic-audit/stand/`); no format, wire shape or algorithm constant that another product reads
changes, and no product code, port-ledger row or catalog contract is touched.

**Goals.** The three readers report what the app really does: (1) `notifs` counts a notification once while it
sits in both NMS lists, (2) the F8-watch judge reads whether the stopwatch resumed instead of inferring from a
notification, (3) `torchOn` and `Get-TorchOn` ignore an `on` entry whose client PID is gone, and D1's `torch-storm`
quiet window holds only the app's own state.

**Constraints.** Nothing runs above 5 s between releases (CLAUDE.md section 7): the stand's own runs (`run-matrix.ps1`)
destroy and recreate profiles, take minutes, and stay release-tier. Fixes are written so a pure function carries the
parsing and a fixture check proves it without a device. The draft outcome set is extended only as a proposal;
approval stays the owner's (S0013 decision 8.7, S0022 section 4.3).

**Research items (all closed):**

| Item | Answer | Settled by |
| --- | --- | --- |
| Does the API 33 dump put the enqueue copy inside the stand's sed range | yes: `Notification List:`, `Enqueued Notification List:`, then `Snoozed notifications:`; one key appears in both for 200 ms | S0022 section 4.1 (AOSP `android13-release`); record line layout re-read on a live dump 2026-10-29 (`NotificationRecord(0x..: pkg=.. user=.. id=.. tag=.. importance=.. key=..: Notification(`) |
| How to count the app's notifications once | distinct `pkg user id tag` identity of the record lines inside the existing range; a line the substitution does not recognise stays counted, never dropped | the record layout above; the key is `user\|pkg\|id\|tag\|uid` |
| Why a lit torch is read after the process died | the camera service switches a dead client's torch off without a log entry | S0019 section 4.1 |
| How the stand tells the client is dead | `/proc/<pid>` is gone; an entry with no readable PID stays "alive" (the old behaviour), so the reader can only lose false positives | S0019 section 4.4 (a); the sampler already reads `/proc/<pid>` as the shell user |
| What the watch stopwatch screen shows | `M:SS.hh`, `H:MM:SS.hh` in one node (`text="0:16.66"`) | `WearStopwatchTimeFormatter`; F7 dump `20261025-base-watch` |
| Is `stopwatch-resumed` an allowed F8-watch outcome | proposed in the draft set; approval is the owner's | S0022 section 4.3, S0013 decision 8.7 |

**Owner inputs:** none are needed to implement. Decision 8.7 (approving the set) is needed before F-group verdicts
count; it is S0013's and stays open.

## 2. Plan

Coverage inventory: item 1 -> P1; item 2 -> P2 (+ the draft set); item 3 first half (dead PID) -> P3; item 3 second
half (D1 quiet window) -> P4; the "3 of 3 on the S0013 profiles" acceptance -> `out-of-scope: release-tier device run`,
recorded as RELEASE items; documentation -> P5.

| Phase | Produces | Consumes | Static check |
| --- | --- | --- | --- |
| P1 notification reader | `@@notif` counts distinct identities (`_instruments.ps1`) | - | the sample script contains the dedupe pipeline; the pipeline gives 1 on an enqueue-copy fixture and 2 on two real ids |
| P2 F8-watch judge | `Get-StopwatchMs`, a re-open + judge in the F8 watch driver, `stopwatch-resumed` in the draft set | - | `outcomes.json` parses and lists it; the judge function gives resumed / ended-cleanly / stale on fixtures |
| P3 torch reader | `Measure-TorchLit` + `$script:TorchProbeCmd` (`_instruments.ps1`), `ConvertFrom-Sample` and `Get-TorchOn` use them | - | fixture: a dead-PID `on` entry reads 0, a live one reads 1, an untagged one reads 1 |
| P4 D1 quiet window | the water flashlight opens inside `water-storm`, with a quiet gap after `torch-storm` (`scenes.ps1`) | - | driver text order checked by a fixture over the script source |
| P5 check and documents | `readers-selftest.ps1` (fixtures of P1-P4), `INDEX.md` row | P1-P4 | `readers-selftest.ps1` exits 0; `fast-check.ps1 -Area scripts` on each edited script; `-Area docs` |

No phase consumes what a later phase produces.


## 3. Record of the work (2026-10-29)

Files touched (all in the S0013 tactical folder, `PLAN/S0013_deep-dynamic-audit/`): `stand/_instruments.ps1`,
`stand/scenes.ps1`, `stand/outcomes.json`, `stand/readers-selftest.ps1` (new), `INDEX.md`. No product code, resource,
port-ledger row or catalog contract changed.

1. **Notification count** (`_instruments.ps1`, `@@notif`). The record lines of the existing range are cut to their
   identity (`pkg user id tag`, the hash and `importance=..` removed) and de-duplicated with `sort -u`; the posted
   record and its enqueue copy are one key. A line the cut does not recognise stays whole, so a layout change can
   only over-count.
2. **F8-watch judge** (`scenes.ps1`). The driver waits 6 s (a measurement started afresh by the next open would read
   less than this one), reads the stopwatch (`Get-StopwatchMs`, `M:SS.hh` / `H:MM:SS.hh`), kills the process,
   samples the notification from outside, measures the cold start (it must stay before the re-open: it force-stops
   the app), opens the stopwatch again, reads it, and `Get-StopwatchKillOutcome` answers `stopwatch-resumed`
   (after >= before), else `stale-notification-left` (a notification is left) or `ended-cleanly`. An unreadable
   reading is `not-run` (invalid, never a finding). The scene stops the stopwatch at its end, so the P4 leak read
   is not fed by the scene's own notification. `outcomes.json` lists `stopwatch-resumed` in the DRAFT set and carries
   the question for decision 8.7; the set stays unapproved (`approved: null`).
3. **Torch reader** (`_instruments.ps1`, `scenes.ps1`). `$script:TorchProbeCmd` tags each `Torch for camera` entry
   `alive` or `dead` on the device (`/proc/<client pid>`); `Measure-TorchLit` reads the newest entry per camera and
   calls it lit only for an `on` not tagged `dead`; `ConvertFrom-Sample` (`torchOn`) and `Get-TorchOn` share both. An
   entry whose PID cannot be read is tagged `alive`, the old behaviour.
4. **D1 quiet window** (`scenes.ps1`). After `torch-storm` ends, 6 s of dark torch (the 8.3d check needs a sample in
   exit + 2 s .. next span - 0.3 s; the sampler runs every 2 s), then `water-storm` begins, then the water
   flashlight opens - its lit torch now belongs to its own span.
5. **Check** (`stand/readers-selftest.ps1`): 25 fixtures, listed in its header.

### 3.1 Evidence

| Claim | Command | Exit | Expected | Actual |
| --- | --- | --- | --- | --- |
| the five edited or new scripts parse | `pwsh -NoProfile -File tools/fast-check.ps1 -Area scripts -Path <script.ps1>` for `_instruments.ps1`, `scenes.ps1`, `readers-selftest.ps1` | 0, 0, 0 | PASS | `fast-check: PASS (1 PowerShell script(s) parsed)` x3, about 0.27 s each |
| `outcomes.json` is valid JSON | `ConvertFrom-Json` over the file | ok | parses | parses |
| the readers give the right answer on the fixtures | `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/readers-selftest.ps1` | 0 | 25 of 25 | `S0031 readers: PASS (25 fixtures read as expected)`, 1.97 s process-inclusive (fast tier) |
| the fixture reproduces the S0022 misread | same run, case 2 | 0 | pre-S0031 line counts the enqueue copy twice, new line once | 2 and 1 |
| the new pipelines run on a device shell (toybox) | the real `@@notif` line and `Get-Sample` against `com.sza.fastmediasorter` on `emulator-5558` (Wear OS API 37, read-only; that package holds one stopwatch notification) | 0 | `@@notif 1`, full sample parses | `@@notif 1`, `notifs=1 torchOn=0`, 1.4 s |
| the liveness tag works on a device shell | `$script:TorchProbeCmd` with its source replaced by a `printf` of the system_server PID, 999999 and no PID, same device | 0 | alive, dead, alive | alive, dead, alive; `/proc/<pid>` of another uid is visible to the shell |

### 3.2 Decisions

| Question | Answer | Settled by |
| --- | --- | --- |
| Count by section or by distinct key (item 1) | distinct identity inside the existing range | covers both layouts (with and without the enqueued list) and a future one; the range end keeps the snoozed section out |
| What counts as "resumed" (item 2) | reading after the re-open >= reading before the kill | the ticket's own rule; the 6 s wait makes a fresh measurement read lower |
| Is `stopwatch-resumed` added to the set | yes, in the DRAFT only, with a question for the owner | S0022 section 4.3; approval stays S0013 decision 8.7 |
| Where the D1 fix goes (item 3b) | open the water flashlight inside its span, leave a dark gap before it | S0019 section 4.4 (b), second form; the first form (judge after the open) would stop judging the torch-storm exit |

## 4. Left open

1. RELEASE: the literal acceptance, 3 of 3 on the S0013 profiles. `run-matrix.ps1` destroys and recreates the profile
   it uses and runs for minutes, so it is release-tier and was not run:
   `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Scenes F8,D1,D3 -Devices phone -Repeat 3`
   and `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Scenes F8 -Devices watch -Repeat 3`.
   Run it only when no other session holds the S0013 profiles.
2. S0013 decision 8.7: approving the outcome set (with the F8-watch question) is what lets an F-group verdict count.
3. The phone F8 and D3 drivers are unchanged; they only read through the fixed `notifs` and `torchOn`.

## Last Audit

**Date:** 26-10-29 21:18 | **Mode:** static | **Outcome:** Verified (static and fast rungs; the stand's device runs are RELEASE)
**Counts:** PASS 9 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 1 | RELEASE 2

- PASS - item 1: `_instruments.ps1` `@@notif` line holds `sort -u | wc -l` over cut identities; fixture 2 (copy) gives 1, fixture 3 (control) gives 2 (readers-selftest, exit 0).
- PASS - item 1 on a device shell: `@@notif 1` through `Get-Sample` on `emulator-5558`.
- PASS - item 2: `Get-StopwatchKillOutcome` answers resumed, stale and ended-cleanly on the fixtures; `outcomes.json` lists `stopwatch-resumed` and `ended-cleanly`, not the stale finding.
- PASS - item 2 order: the cold start is measured before the second `Open-Watch` (driver shape check).
- PASS - item 3a: dead, live, unreadable and newest-wins cases; the shell tag produced by the real `$script:TorchProbeCmd` (host bash and device shell).
- PASS - item 3b: D1 driver text has `End-Feature torch-storm`, `Pause-Sec 6`, `Begin-Feature water-storm`, then the water flashlight tap.
- PASS - scope: edits are confined to the five files above; `git`-independent, read from disk.
- PASS - probe invariant: no `S0031` string in `phone/`, `wear/`, `watchface/` (grep); the ticket is not in the needs-manual-test status.
- PASS - hygiene: no long dash or three dots in the edited code, no `torch_<cam>` residue (the keys the old reader created are gone with it).
- EXEMPT - shared-contract boundary: the stand reads and judges only; nothing another product reads changes.
- RELEASE - `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Scenes F8,D1,D3 -Devices phone -Repeat 3` - not run (profile lifecycle, minutes).
- RELEASE - `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Scenes F8 -Devices watch -Repeat 3` - not run.

Not built, not tested until the release. The stand's device runs, and the notification assistant's 200 ms window on
the API 33 image, are observed only there; today the window is reproduced by a fixture built from the AOSP layout
recorded in S0022 section 4.1.
