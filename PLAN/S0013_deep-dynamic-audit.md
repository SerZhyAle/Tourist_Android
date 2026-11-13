# S0013 - Deep dynamic audit: five properties

## Remaining development (as of 2026-10-27)

What is still to be built or fixed before this ticket can leave In Progress. Counts are read from the matrix of
section 3.2 on this date: 65 cells PASS, 55 FAIL, 50 COULD NOT VERIFY, 21 open, the rest `n/a`.

1. **Stand: the 50 COULD NOT VERIFY cells.** Each is an invalid or unreachable run, not a verdict. The paired scenes
   (B3, C6, F6, G3) need a working phone-watch pair on the emulators (M9's pairing health check, section 12); for
   every other cell the stand cause is found and fixed, or the cell is left open with its reason (sections 5 and 7).
2. **Stand: the 21 open P5 cells.** The `locale-a11y.ps1` sweep (M7) has not run for them; run it, or mark the cell
   `n/a` with a reason.
3. **Findings: the 55 FAIL cells.** A P0 or P1 is admitted only with rank T or L (section 9): re-run each FAIL cell
   with `-Repeat 3`, dedupe by symptom, and file what is real. Six findings are already parked as S0018-S0023
   (Draft); whether they cover every FAIL cell is not recorded yet.
4. **This document.** Section 4 still carries the design-only acceptance criteria (all unchecked) and section 13 still
   says "None"; rewrite the acceptance for the implementing phases and fill section 13 with the findings and the run
   journal.
5. **Phase 4 again, then Phase 5.** The understandability sweep repeats once the S0015 migration has landed (the
   strings and the UI must be stable); Phase 5, the full matrix on the release candidate with the store variant
   (decision 8.6), is the last run and feeds S0010.

Owner inputs that are not development, listed so they are not lost (section 3.3): 8.1 re-issues the `[h]` budgets
after the first baseline (decided 2026-11-10); 8.7 approves the known-outcome set, `stand/outcomes.json` being a draft until then (decided 2026-11-10, approved stamped).

**Status:** BlockByOtherTask
**Status note:** Missing: every open acceptance item (section 4, items 3-8) is an emulator-pair run - unmeasured, above the 5-second ceiling, release tier: COULD NOT VERIFY re-runs incl. the pair scenes, the open P5 M7 sweep, FAIL cells with -Repeat 3 and their finding map. Blocker: S0015 (Phase 4 repeats after the UI migration lands). Blocker: S0010 (Phase 5 runs on the release candidate). Stand fixes done (S0031); decisions 8.1 and 8.7 approved. Not built, not tested until the release; commands in section 13.
Every section below is a contract for the implementing phases. Nothing is measurable until the port lands
(S0002 onward); Phase 0 stand work can start once S0002 pins the API levels and variants.
**Phase:** quality work beside the handoff section 8 order - starts measuring at the first feature phase and
feeds the S0010 release dry-run
**Complexity:** hard - reassessed 2026-10-29: the cross-device measurement stand, variants, fault matrix and release-candidate audit span many scripts and require owner decisions.
**Verification level:** design - the scene matrix, instrument registry, runner contracts and budgets are
complete and mutually consistent on paper; the implementing phase proves each script by running it

## 0. Source

- `CLAUDE.md` sections 1 (capability list - M0's ground truth), 3 (anchors, the foreground-only location
  rule, variants), 4 (ticket discipline).
- `docs/BOOTSTRAP_HANDOFF.md` sections 2 (goals), 3 (EN/RU/UK, accessibility, imperial units, input parity),
  5 (permissions, stored state, Data Layer paths, the carried source defects S4068 and S4069), 9, 10.
- Canon `TESTING_AND_QA.md` §3 (no retry-until-green; flakes get ledger rows), §4 (device drive, the
  backup / refuse-without-backup / restore-in-finally discipline, emulator false-FAIL traps, confirm the
  device runs the new build); canon skill `spec-to-audit` (P0-P3 tags, park-don't-fix, phase-boundary
  audits).
- `CHECK-VERDICT` §2 (exit codes and the one verdict line), `CHECK-BASELINE` (accepted-debt file),
  `CHECK-PLACEMENT` (runner registry) - catalog, automated-checks.
- `INPUT-PARITY` 0.3 (adopter - property 5's input modes), `DIAGNOSTIC-REPORT` 0.12 (producer, conditional -
  no diagnostic bundle in the first release, so out of scope here).
- Tickets this audit measures: S0002-S0009 (the port); S0010 consumes its final pass.

## 1. Problem

The kit's promises are promises about time. SOS must keep sounding through screen-off and Doze; the water
flashlight must stay responsive under a wet-touch storm and keep the screen awake while it is on, and leave
by its exit rule and nothing else; a stopwatch runs for hours; the phone-watch pair must survive link loss;
the torch must not hold the camera of another app. None of that is visible to a static audit: review, unit
tests, the port-ledger drift check and the S0001 validators cannot see a leak slope, an unreleased wakelock,
a janky frame, a Russian string that falls back to English, or an SOS that dies at screen-off. The
repository has no dynamic evidence channel at all, and the release plan (S0010) currently takes no measured
input about time-dependent behaviour.

This ticket is the dynamic complement to the static audits: numbers and reproductions, not impressions. It
specifies what is measured, from outside the app, against which budgets, with which severity - and nothing
else.

**Documented decisions are not findings.** Anything already recorded - CLAUDE.md, the handoff (including its
carried source defects: S4068 FastMediaSorter, the never-populated weather and dew-point tiles, and S4069
FastMediaSorter, the phone dashboard that never asks for its grants), the S3178 FastMediaSorter variant
split, port-ledger decisions - is context, not a finding. A finding here is only the measured consequence of
such a record in this product, and it cites the record it measures.

### 1.1 Non-goals

- **No static re-audit.** Review, unit tests, the drift check and the S0001 validators own statics; nothing
  here re-reports a code smell or a design opinion.
- **No new product code for measurement** - no test receiver, no debug endpoint, no added log line (the port
  rule; section 5 states the outside-only stand). A quantity that cannot be read from outside is an open
  cell with its reason, or an owner decision - never a code change smuggled in as tooling.
- **No fixes here.** A finding becomes a fresh ticket, parked with its evidence; the active ticket never
  switches (section 9).
- **No benchmark against FastMediaSorter.** Behaviour parity is the port ledger's guarantee; performance
  parity between the two products is not claimed or measured.
- **No fuzzing, no random monkey runs, no root, no stress beyond the defined storms**, and no network
  denial tests - the kit has no network feature beyond the Data Layer.
- **No CI placement in this ticket**; a `CHECK-PLACEMENT` record lands with the implementing phase only if
  decision 8.4 makes the audit a gate.
- **Nothing published or uploaded; no release triggered; the source repositories stay read-only.**

## 2. The five properties

Operational definitions; each names its instrument (section 3.1):

1. **Responsiveness** - how long the user and the UI thread wait: start (cold, warm, hot), input-to-frame,
   frame pacing, ANR. M1, M2, M3.
2. **Wrapped errors** - every failure at a boundary (permission revoked, location or satellites off, sensor
   absent, camera busy, audio focus lost, Data Layer down, storage full, process death, boot-time failure)
   is caught where it can be understood, lands in the closed known-outcome set, is logged without
   secret-class content, is said to the user in a human sentence, and leaves the system consistent
   (services, state, preferences). M5, M3, M7.
3. **No system blocking** - the app never makes other programs or the platform wait: no wakelock, foreground
   service, exclusive resource (camera and torch, audio focus), location request, sensor registration,
   notification, or log and disk churn outlives the feature that needs it; nothing pins the device awake in
   idle. M3.
4. **Leaks** - memory and kernel-side resources and registrations: PSS, private and heap growth, file
   descriptors, threads, binder, sensor/location/broadcast registrations, notifications, temporary files,
   log growth, preferences growth - each as a slope with a confidence band, never a snapshot. M3, M4.
5. **Understandability and accessibility** - a person understands what a control does before and after
   flipping it, can undo it, in EN, RU and UK, and through assistive semantics (label, state, target size,
   font scale) and the input modes INPUT-PARITY declares. M7.

Properties 2 and 5 meet at the error sentence: property 2 owns the outcome set and log hygiene, property 5
owns whether the sentence is understandable in every locale and readable at font scale 2.0.

## 3. Scene catalog, script registry, matrix

### 3.1 Script registry

Every script speaks `CHECK-VERDICT` §2: exit `0` passed (or `NOT APPLICABLE` by a recorded reason), `1`
failed - something was measured and judged defective (a finding), `2` could not verify - nothing was
inspected or the run is invalid under section 7. Codes `3` (advisory) and `4` (queue) are never emitted:
these runners attribute everything they find to the run that produced it and wait on no shared lock. Each
prints one verdict line. Scripts live in the tactical folder only after approval; none exists at Draft.

| Id | Script | Contract | Verdict basis |
| --- | --- | --- | --- |
| M0 | `selftest` | coverage self-test: reads CLAUDE.md section 1 capability tokens and the handoff section 3 locale/mode list, walks the matrix - every capability maps to at least one open or measured cell (or an `n/a` naming its owner decision); every open cell names an existing script id; every script row declares exactly 0/1/2 | 0 complete; 1 gap list; 2 sources unreadable |
| M1 | `ping-ui` | main-looper ping from outside: `am start -W` round-trip (cold, warm, hot) and `dumpsys gfxinfo` frame percentiles over the scene window | budgets 8.1 |
| M2 | `key-latency` | injected-input latency: `input keyevent` / `tap` (and the watch hardware key and rotary source) timestamp vs the next presented frame | budgets 8.1 |
| M3 | `sampler` | periodic outside harvest: `dumpsys meminfo` (PSS, private, heap), `/proc/<pid>/fd` and `/proc/<pid>/task` counts, `dumpsys sensorservice` registrations, `dumpsys power` wakelocks, `dumpsys alarm`, `dumpsys activity` services and ANR counters, notifications, data-dir size (run-as), logcat rate plus the secret-class scan | budgets 8.2-8.4 |
| M4 | `slope` | growth slope with confidence band: Theil-Sen slope and bootstrap CI over the M3 series, judged against the M8 control band | budgets 8.4 |
| M5 | `fault-inject` | the F-group boundary failures (and the B4/F9 boot case), with a state-consistency readback after each | budgets 8.2 |
| M6 | `storm` | the D-group drivers, incl. emulator sensor and rotation injection and the locale flip-flop | budgets 8.1 |
| M7 | `locale-a11y` | per-locale UI dumps (label present, state text, English-fallback detection), touch-target sizes, font-scale 1.3 and 2.0 re-dumps, toggle reversibility by state hash | budgets 8.5 |
| M8 | `control` | the no-app run: the full instrument set on the same profile with the app not installed; writes the per-instrument noise floor (its own 95% CI) as `control.json` | consumed by M4 and section 7 |
| M9 | `profile` | the disposable profile lifecycle and the backup gate of section 6 | refuses with 2 without a verified backup |
| M10 | `run-matrix` | walks the scene x property cells, calls the scripts, aggregates (any 1 -> 1; else any 2 -> 2; else 0), one verdict line per cell | `CHECK-VERDICT` §2 |

### 3.2 Matrix

Cell states: `measured` (a run exists, verdict and run id recorded), `open` (no run yet), `n/a` (with
reason). At Draft every cell is open or n/a - nothing builds yet. Scene tags: `[p]` phone emulator, `[w]`
watch emulator, `[pair]` both. Every scene runs in both variants where the section 4.3 capability split
allows; a capability hidden in the store variant makes its cells `n/a: 4.3 split` on store runs. Rows carry
their instruments; `M3` alone in a P2 cell means the logcat exception scan, in a P3/P4 cell the system
columns of the sampler.

**A. Lifecycle**

| Scene | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- |
| A1 cold start `[p,w]` | measured M1, M2: FAIL (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3, M4: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | measured M7: PASS (watch; p5-watch) |
| A2 warm and hot return `[p,w]` | measured M1, M2: FAIL (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | measured M3, M4: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | open M7 |
| A3 exit (back from root, recents swipe) `[p,w]` | n/a: nobody waits after exit | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3, M4: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | open M7 |
| A4 process death and restore (`am kill`, reclaim) `[p,w]` | measured M1, M2: FAIL (phone+watch; base-phone, verify-watch) | measured M5: FAIL (phone+watch; base-phone, verify-watch) | measured M3: COULD NOT VERIFY (phone+watch; base-phone, verify-watch) | measured M3: FAIL (phone+watch; base-phone, verify-watch) | open M7 |

**B. Power and session**

| Scene | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- |
| B1 screen off and on mid-feature `[p,w]` | measured M1, M2: FAIL (phone+watch; base-phone, verify-watch) | measured M3: PASS (phone+watch; base-phone, verify-watch) | measured M3: FAIL (phone+watch; base-phone, verify-watch) | measured M3, M4: FAIL (phone+watch; base-phone, verify-watch) | n/a: no UI while off; resume wording is A4 and B2's cell |
| B2 Doze and app standby (forced idle, standby buckets) `[p,w]` | measured M1: FAIL (phone+watch; verify-phone, verify-watch) | measured M5: PASS (phone+watch; verify-phone, verify-watch) | measured M3: FAIL (phone+watch; verify-phone, verify-watch) | measured M3, M4: FAIL (phone+watch; verify-phone, verify-watch) | n/a: no UI during idle |
| B3 watch link loss and regain (the session-mobility case) `[pair]` | measured M1, M2: COULD NOT VERIFY (pair; base-watch) | measured M5: COULD NOT VERIFY (pair; base-watch) | measured M3: COULD NOT VERIFY (pair; base-watch) | measured M3, M4: COULD NOT VERIFY (pair; base-watch) | open M7 |
| B4 phone reboot with the torch shortcut enabled `[p]` | measured M1: FAIL (phone; base-phone) | measured M5: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | open M7 |
| B5 watch charger dock and undock (ambient <-> active) `[w]` | measured M2: FAIL (watch; base-watch) | measured M3: PASS (watch; base-watch) | measured M3: FAIL (watch; base-watch) | measured M3: PASS (watch; base-watch) | open M7 |

**C. Main actions**

| Scene | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- |
| C1 dashboard live (rotation, steps, simulated fix, sun) `[p]` | measured M1, M2: FAIL (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3, M4: COULD NOT VERIFY (phone; base-phone) | measured M7: COULD NOT VERIFY (phone; p5-phone) |
| C2 compass/heading storm held on the hero tile `[p,w]` | measured M1, M2: FAIL (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3, M4: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | measured M7: FAIL (watch+phone; p5-watch, p5-phone) |
| C3 torch on/off (camera torch, screen light) `[p]` | measured M2: FAIL (phone; base-phone) | measured M5: COULD NOT VERIFY (phone; base-phone) | measured M3: COULD NOT VERIFY (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M7: FAIL (phone; p5-phone) |
| C4 water flashlight full cycle incl. the exit rule `[p,w]` | measured M2: FAIL (phone+watch; verify-phone, base-watch) | measured M3: FAIL (phone+watch; verify-phone, base-watch) | measured M3: FAIL (phone+watch; verify-phone, base-watch) | measured M3, M4: COULD NOT VERIFY (phone+watch; verify-phone, base-watch) | measured M7: COULD NOT VERIFY (watch+phone; p5-watch, p5-phone) |
| C5 SOS phone-only full cycle, all modes `[p]` | measured M2: FAIL (phone; base-phone) | measured M5: COULD NOT VERIFY (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M7: PASS (phone; p5-phone) |
| C6 SOS Data Layer pair, start and stop from both sides `[pair]` | measured M1, M2: COULD NOT VERIFY (pair; base-watch) | measured M5: COULD NOT VERIFY (pair; base-watch) | measured M3: COULD NOT VERIFY (pair; base-watch) | measured M3, M4: COULD NOT VERIFY (pair; base-watch) | open M7 |
| C7 stopwatch multi-participant, laps, music, ongoing activity `[p,w]` | measured M2: FAIL (phone+watch; base-phone, base-watch) | measured M5: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3, M4: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | measured M7: FAIL (watch+phone; p5-watch, p5-phone) |
| C8 home-screen widgets (tourist, front light, water, stopwatch) `[p]` | measured M2: COULD NOT VERIFY (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M7: COULD NOT VERIFY (phone; p5-phone) |
| C9 notification-shade torch shortcut `[p]` | measured M2: FAIL (phone; verify-phone) | measured M5: FAIL (phone; verify-phone) | measured M3: PASS (phone; verify-phone) | measured M3: PASS (phone; verify-phone) | measured M7: FAIL (phone; p5-phone) |
| C10 open in external map, share the location `[p]` | measured M2: FAIL (phone; verify-phone) | measured M5: FAIL (phone; verify-phone) | measured M3: PASS (phone; verify-phone) | measured M3: PASS (phone; verify-phone) | measured M7: COULD NOT VERIFY (phone; p5-phone) |

**D. Storms**

| Scene | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- |
| D1 torch and water toggle storm (100x) `[p]` | measured M1, M2: FAIL (phone; verify-phone) | measured M3: PASS (phone; verify-phone) | measured M3: FAIL (phone; verify-phone) | measured M3, M4: COULD NOT VERIFY (phone; verify-phone) | n/a: stress, no new UI semantics |
| D2 key/tap storm with the touch lock on `[w]` | measured M2: FAIL (watch; base-watch) | measured M3: FAIL (watch; base-watch) | measured M3: FAIL (watch; base-watch) | measured M3, M4: COULD NOT VERIFY (watch; base-watch) | n/a: stress; the lock's wording is C1 and C4's cell |
| D3 SOS mode-flip storm (sound/light) with notifications `[p,w]` | measured M2: FAIL (phone+watch; verify-phone, base-watch) | measured M3: PASS (phone+watch; verify-phone, base-watch) | measured M3: FAIL (phone+watch; verify-phone, base-watch) | measured M3: PASS (phone+watch; verify-phone, base-watch) | n/a: stress |
| D4 sensor storm at max injection rate `[p]` | measured M1: FAIL (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3, M4: COULD NOT VERIFY (phone; base-phone) | n/a: stress |
| D5 locale and unit flip-flop (EN, RU, UK; metric/imperial; 12/24h) `[p]` | measured M2: FAIL (phone; base-phone) | measured M3: PASS (phone; base-phone) | n/a: no shared resource held | measured M3: PASS (phone; base-phone) | open M7 |
| D6 rapid screen ping-pong `[p,w]` | measured M1, M2: FAIL (phone+watch; base-phone, verify-watch) | measured M3: COULD NOT VERIFY (phone+watch; base-phone, verify-watch) | measured M3: COULD NOT VERIFY (phone+watch; base-phone, verify-watch) | measured M3, M4: FAIL (phone+watch; base-phone, verify-watch) | n/a: stress |

**E. Large inputs**

| Scene | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- |
| E1 stopwatch scale (max participants, thousands of laps, days-long restore) `[p,w]` | measured M2: PASS (phone+watch; base-phone, base-watch) | measured M5: PASS (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3, M4: FAIL (phone+watch; base-phone, base-watch) | open M7 |
| E2 sensor and clock extremes (poles, equator, 0 satellites, polar day) `[p]` | measured M2: FAIL (phone; base-phone) | measured M5: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | open M7 |
| E3 max-length participant names, large share payload `[w]` | measured M2: COULD NOT VERIFY (watch; base-watch) | measured M3: PASS (watch; base-watch) | measured M3: PASS (watch; base-watch) | measured M3: FAIL (watch; base-watch) | open M7 |

**F. Boundary failures (all injected by M5 unless tagged)**

| Scene | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- |
| F1 permission revoked at runtime (location, notifications, and activity-recognition where the variant has it) `[p,w]` | measured M2: FAIL (phone+watch; base-phone, base-watch) | measured M5: FAIL (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | open M7 |
| F2 location and satellites off, cold GNSS `[p]` | measured M2: COULD NOT VERIFY (phone; base-phone) | measured M5: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | open M7 |
| F3 sensor absent (barometer on phone-class hardware, no step counter) `[p,w]` | measured M2: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | measured M5: FAIL (phone+watch; base-phone, base-watch) | measured M3: FAIL (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | open M7 |
| F4 camera busy when the torch is requested `[p]` | measured M2: PASS (phone; base-phone) | measured M5: PASS (phone; base-phone) | measured M3: COULD NOT VERIFY (phone; base-phone) | measured M3: PASS (phone; base-phone) | open M7 |
| F5 audio failure and focus loss mid-siren (call, DND) `[p]` | measured M2: COULD NOT VERIFY (phone; base-phone) | measured M5: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | measured M3: PASS (phone; base-phone) | open M7 |
| F6 Data Layer down at SOS start `[pair]` | measured M2: COULD NOT VERIFY (pair; base-watch) | measured M5: COULD NOT VERIFY (pair; base-watch) | measured M3: COULD NOT VERIFY (pair; base-watch) | measured M3: COULD NOT VERIFY (pair; base-watch) | open M7 |
| F7 storage full, preferences write fails `[p,w]` | measured M2: PASS (phone+watch; base-phone, base-watch) | measured M5: PASS (phone+watch; base-phone, base-watch) | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3, M4: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | open M7 |
| F8 hard kill mid-feature, then restore `[p,w]` | measured M1, M2: COULD NOT VERIFY (phone+watch; verify-phone, verify-watch) | measured M5: FAIL (phone+watch; verify-phone, verify-watch) | measured M3: PASS (phone+watch; verify-phone, verify-watch) | measured M3: FAIL (phone+watch; verify-phone, verify-watch) | open M7 |
| F9 boot with every shortcut enabled and one failing (B4's fault twin) `[p]` | measured M1: FAIL (phone; base-phone) | measured M5: PASS (phone; base-phone) | measured M3: COULD NOT VERIFY (phone; base-phone) | measured M3: PASS (phone; base-phone) | open M7 |

**G. Idle soak (M3 samples throughout)**

| Scene | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- |
| G1 each active screen held (dashboard live, water on, SOS on, stopwatch running) `[p,w]` | measured M1: FAIL (phone+watch; base-phone, base-watch) | measured M3: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | measured M3: FAIL (phone+watch; verify-phone-soak, base-phone, base-watch) | measured M3, M4: FAIL (phone+watch; verify-phone-soak, base-phone, base-watch) | n/a: static UI; wording measured in C |
| G2 backgrounded and in standby all day `[p,w]` | n/a: user absent; wake latency is B2's cell | measured M3: PASS (phone+watch; base-phone, base-watch) | measured M3: FAIL (phone+watch; base-phone, base-watch) | measured M3, M4: COULD NOT VERIFY (phone+watch; base-phone, base-watch) | n/a: no UI shown |
| G3 paired idle overnight, SOS off `[pair]` | n/a: as G2 | measured M3: COULD NOT VERIFY (pair; base-watch) | measured M3: COULD NOT VERIFY (pair; base-watch) | measured M3, M4: COULD NOT VERIFY (pair; base-watch) | n/a: no UI shown |

**H. Watch face - conditional**

| Scene | P1 | P2 | P3 | P4 | P5 |
| --- | --- | --- | --- | --- | --- |
| H1 watch face selection and live render `[w]` | measured M1: COULD NOT VERIFY (watch; base-watch) | measured M3: PASS (watch; base-watch) | measured M3: PASS (watch; base-watch) | measured M3, M4: COULD NOT VERIFY (watch; base-watch) | open M7 |

The row is kept so M0 sees the capability; its cells turned open on 2026-10-25, when S0009 shipped the face
(decision 8.5).

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002-S0009 (provide the measurable app; Phase 0 needs only S0002), S0010 (consumes
  the Phase 5 pass), S0004's strings (Phase 4's input).
- **8.1 Budget values.** Approve or re-cut every `[h]` row of section 8 after the first baseline run,
  before Phase 1 verdicts count.
  **Decided (owner, 2026-11-10, quiz): approved as written in section 8 and budgets.json.** The hypothesis budgets `[h]` become operational benchmarks (with 8.3c tolerance <= 7.5 s as measured per S0020).
- **8.2 Reference hardware.** Emulator-only, or add one physical phone-watch pair for GNSS and barometer
  realism. Hardware brings section 6's physical-device rules and never the owner's daily watch without a
  per-run approval.
  **Decided (owner, 2026-10-25): emulator-only.** F2, F3 and the GNSS/barometer parts of C1 stay weaker than
  hardware (section 12); no physical device is touched by this ticket.
- **8.3 Soak durations.** G1 and G3 lengths. Proposed reference: 30 minutes per active screen per device,
  one overnight pair run; cost scales linearly with the choice.
  **Decided (owner, 2026-10-25): short - G1 10 minutes per active screen per device, G2 10 minutes, G3 one
  hour.** The slope CI is wider than the reference would give; a P4 finding still needs T or L (section 9).
- **8.4 Cadence.** Per phase boundary and release candidate, or release candidate only. If it becomes a
  gate, a `CHECK-PLACEMENT` record lands with the implementing ticket.
  **Decided (owner, 2026-10-25): release candidate only; not a gate.** No `CHECK-PLACEMENT` record is written.
- **8.5 Watch face scope.** Group H activates only if decision 4.4 keeps a face (S0009).
  **Resolved 2026-10-25 by fact: S0009 is Implemented, the face exists, group H is open.**
- **8.6 Variant order.** Both variants in every phase, or noLegal first with store measured at Phase 5.
  **Decided (owner, 2026-10-25): noLegal first; store is measured at Phase 5.**
- **8.7 The known-outcome set.** The owner approves the closed list of failure outcomes for property 2
  (drafted by the Phase 1 session from the ported behaviour) before F-group verdicts count.
  **Decided (owner, 2026-11-10, quiz): approved.** C4-exit: left-by-back is a breach of the water rule (not in outcomes; only left-by-hardware-key allowed); F2: silent-stale-data allowed alongside location-off-explained; F8-sos: sos-ended-cleanly allowed (START_NOT_STICKY); F8-watch: stopwatch-resumed and ended-cleanly both allowed (S0022/S0031). `stand/outcomes.json` stamped approved: "2026-11-10".
- **8.8 The secret-class list.** The owner approves what the logcat scan treats as secret. Draft: coordinate
  pairs, absolute paths, account and token patterns.
  **Decided (owner, 2026-10-25): coordinate pairs, and account and token patterns.** Absolute paths and
  participant names are not secret-class.

### Quiz decisions (2026-11-10)

- Approve hypothesis budgets [h] after baseline (decision 8.1)? -> "(Рекомендуется) Утвердить текущие бюджеты [h] из budgets.json (допуск 8.3c <= 7.5с по S0020; гипотезы становятся утвержденными нормативами)" (hypothesis budgets confirmed as operational standards).
- Water flashlight exit by Back key/gesture (decision 8.7, C4-exit)? -> "(Рекомендуется) Нарушение правила: выход по Back запрещен (в outcomes остается только left-by-hardware-key; исключает случайный выход от капель)" (enforces product signature rule: leave by exit rule and nothing else).
- Location turned off on live dashboard (decision 8.7, F2)? -> "(Рекомендуется) Допустить silent-stale-data наряду с location-off-explained (соответствует поведению портированного кода без рефакторинга)" (preserves ported behaviour without rewrite).
- Hard kill outcomes for SOS and watch stopwatch (decision 8.7, F8-sos, F8-watch)? -> "(Рекомендуется) Утвердить оба исхода (SOS не перезапускается самопроизвольно, секундомер восстанавливает время по S0022/S0031)" (matches START_NOT_STICKY and time-origin persistence).

## 4. Acceptance

Rewritten 2026-11-10 for the implementing phases (the design-only list it replaces was satisfied when the
stand was built: matrix, registry, budgets, invalid-run rule, journal row through the harness). Each item is
a static predicate of this document or of a named command's verdict line.

- [x] 1. The stand exists under `PLAN/S0013_deep-dynamic-audit/stand/`: one script per registry row M0-M10
      of section 3.1, the approved budget set (`budgets.json`, decision 8.1) and the approved known-outcome
      set (`outcomes.json` stamped `approved`, decision 8.7). No product code was written for measurement.
- [x] 2. The stand readers named by S0019 section 4.4 and S0022 (dead-client torch entry, D1 quiet window,
      notification enqueue copy, F8-watch resume) are corrected - S0031, Implemented.
- [ ] 3. No cell of section 3.2 is `COULD NOT VERIFY` without a recorded stand cause: each is re-run valid,
      or left `open` with its reason (sections 5 and 7). The `[pair]` scenes B3, C6, F6, G3 pass M9's
      pairing health check first.
- [ ] 4. No P5 cell is `open`: each holds an M7 verdict with its run id, or `n/a` with a reason.
- [ ] 5. Every FAIL cell is re-run with `-Repeat 3`; every cell still failing 3 of 3 maps to a finding
      ticket by symptom (S0018-S0023, S0031-S0035, or a fresh Draft), and a P0/P1 carries rank T or L.
- [ ] 6. Phase 4 is repeated after S0015 lands (strings and UI stable).
- [ ] 7. Phase 5: `run-matrix.ps1 -Phase all -Variant store -WithM7` on the release candidate exits with
      its verdict line recorded in section 13; S0010 consumes it, and a P0/P1 with T or L blocks it.
- [ ] 8. Scratch and run folders stay under `temp/S0013/`; nothing of a run is committed (section 11).

## 5. The measurement stand - outside the app

The stand is host-side (adb over the emulator pair) and never asks the app for cooperation: no test
receiver, no debug endpoint, no added log line (the port rule; a quantity that cannot be read from outside
is an open cell with its reason, or an owner decision - never a code change smuggled in as tooling). The
commission named the instruments in Windows-desktop terms; their measurement intent lands here as:

| Commissioned intent | Carrier here |
| --- | --- |
| UI-thread ping (the `SendMessageTimeout(WM_NULL)` class of probe) | M1: `am start -W` round-trip and `dumpsys gfxinfo` frame percentiles - the main looper answered from outside |
| Injected-key latency probe | M2: `input keyevent` / `tap`, plus the watch hardware key and rotary source, against the next presented frame |
| GDI/USER/kernel handle, thread, private-bytes, heap sampler | M3: fd and task counts from `/proc`, `dumpsys meminfo` PSS/private/heap, binder and services from `dumpsys activity` |
| Growth slope with a confidence band | M4: Theil-Sen slope, bootstrap CI, judged against the M8 control band |
| Watchdog (ANR, hang, crash) | M3: `dumpsys activity` ANR counters and the logcat FATAL/ANR harvest |

Stand rules:

- **Version readback before every run** (canon TESTING_AND_QA §4): `dumpsys package` names the build under
  test, or the run is invalid (section 7) - a same-version debug build can silently keep old code.
- **API pins follow S0002**; the dumpsys surfaces differ by level, so the implementing phase records an
  instrument-availability matrix per pinned level and marks unavailable cells open-with-reason.
- **Emulator quirks are invalid-run candidates, not findings** (canon trap list: unindexed media store,
  untappable bottom-sheet items, touch wedges, pairing instability).
- **Run-as reads**: the debuggable build allows `run-as` for data-dir size and file-set reads - reading the
  app's own files through the platform's debug channel, still no app cooperation.
- **Localization of probes**: the stand sets the per-app locale (`cmd locale set-app-locales` or the system
  locale) and font scale, and re-reads the UI dump per setting.

## 6. The isolated disposable profile

- **Reference profile:** a disposable AVD pair (phone and wear, images at the S0002 pins), created per run
  from a committed recipe - never a committed image - and deleted in the runner's `finally`. Its data
  folder, its settings and its preference set are the profile's own: nothing of the owner's environment is
  in play. The only host artifact is the run folder under `temp/S0013/runs/<run-id>/` (gitignored).
- **Uniform backup gate** (canon TESTING_AND_QA §4 discipline, carried to the device profile; it applies to
  every run, including the disposable profile - it is cheap, and it makes "what the run touched" an
  artifact): before any scene, M9 captures the app's `/data/data` tree (run-as tar), external-storage
  additions, the `dumpsys package` grant set and the profile settings the app can reach; writes the
  recovery manifest (paths, sizes, SHA-256, restore commands, run id); **refuses to start - exit 2 - if the
  manifest is missing or a checksum disagrees**; restores in `finally` even after a crash of the runner.
- The manifest is the evidence source for property 4's temporary-file and preferences-growth cells: the
  file-set diff before and after a scene is the measured fact.
- **Physical devices** (only if 8.2 approves): the same gate applies unchanged; no factory reset, ever; the
  owner's paired watch is touched only with a per-run approval; a failed backup parks the run as invalid.

## 7. The control run and the invalid-run rule

- **M8 control run:** the full instrument set on the same profile with the app not installed, once per
  profile recipe and per host-load profile. Its output `control.json` is each instrument's noise floor - its
  own 95% CI over the control window. The probe's own jitter is the ruler's zero mark.
- **A run is invalid (exit 2, never a finding) when:** the measured signal lies within the control jitter
  band; the probe self-check failed (adb drop, `dumpsys` timeout, emulator wedge, pairing failure on
  `[pair]` scenes); the version readback does not name the build under test; the backup gate failed; M0
  reports a matrix inconsistency.
- **No retry-until-green** (canon TESTING_AND_QA §3): an instrument that invalidates twice for the same
  cause files a stand defect ticket; the flake stays visible as a ledger row, and the cell stays open.
- **Re-take the control** whenever the emulator image, the host-load profile or an API pin changes.

## 8. Budgets

Marked `[h]` = hypothesis until the M8 control plus the first baseline pass; the owner re-issues them
(decision 8.1) before Phase 1 verdicts count. Marked `[hard]` = encodes a CLAUDE.md or handoff rule;
re-issued only together with the rule it encodes. Reference device: the emulator pair; hardware re-baselines
if 8.2 approves it.

| Id | Property | Metric | Budget |
| --- | --- | --- | --- |
| 8.1a | P1 | cold start, `am start -W` TotalTime | <= 2000 ms `[h]` |
| 8.1b | P1 | warm start TotalTime | <= 700 ms `[h]` |
| 8.1c | P1 | injected input -> presented frame, p95 per scene | <= 150 ms `[h]` |
| 8.1d | P1 | janky-frame share per scene window | <= 5% `[h]` |
| 8.1e | P1 | ANR count, any scene | 0 `[hard: an ANR is a hang the platform had to break]` |
| 8.1f | P1 | crash count, any scene | 0 `[hard: handoff section 9 acceptance]` |
| 8.2a | P2 | injected faults landing in the known-outcome set | 100% `[hard: decision 8.7 defines the set]` |
| 8.2b | P2 | raw exception or stack text visible in the UI | 0 `[hard]` |
| 8.2c | P2 | secret-class tokens in logcat (list: decision 8.8) | 0 `[hard]` |
| 8.2d | P2 | state consistent after each fault (hash unchanged unless the outcome declares a change) | 100% `[hard]` |
| 8.3a | P3 | wakelock held > 5 s after its feature's screen exits | 0 `[h]` |
| 8.3b | P3 | foreground service outside its feature's active span | 0 `[hard: handoff permissions and services]` |
| 8.3c | P3 | sensor/location registrations cleared after the reading screen exits | <= 5 s `[h; CLAUDE.md section 3: foreground-only location]` |
| 8.3d | P3 | torch/camera released after off | <= 500 ms `[h]` |
| 8.3e | P3 | audio-focus release misses on stop | 0 `[hard]` |
| 8.3f | P3 | concurrent notifications per active feature | <= 1 `[hard]` |
| 8.3g | P3 | idle logcat lines by the app | <= 1/min `[h]` |
| 8.3h | P3 | idle battery/thermal delta vs the control run | within control band `[h]` |
| 8.4a | P4 | PSS, fd and thread slopes over each soak | CI upper bound <= control band + 1%/h `[h]` |
| 8.4b | P4 | registrations back to baseline after every scene | 100% `[hard]` |
| 8.4c | P4 | preferences key set after the first full pass | stable `[hard: handoff stored state is closed]` |
| 8.4d | P4 | data-dir growth per soak hour (declared caches excluded) | <= 5 MB `[h]` |
| 8.5a | P5 | untranslated or English-fallback strings across EN, RU, UK | 0 `[hard: handoff section 3]` |
| 8.5b | P5 | toggle reversibility: state hash after on -> off equals before | 100% `[hard]` |
| 8.5c | P5 | touch-target size | >= 48 dp phone, >= 40 dp watch `[h]` |
| 8.5d | P5 | clipped or unlabelled controls at font scale 2.0 | 0 `[hard]` |
| 8.5e | P5 | interactive nodes with label and state description | 100% `[hard: handoff section 3 accessibility]` |

## 9. Severity and proof rank

Severity follows the canon `spec-to-audit` scale, specialised to dynamic findings:

- **P0** - user harm, a signature-rule breach, data loss, or platform-wide blocking: SOS fails to sound on
  demand; the water flashlight yields to a wet touch; preferences destroyed; a resource held so the system
  or other apps cannot work.
- **P1** - resource or responsiveness failure: a leak whose CI sits above the control band; an unreleased
  camera, wakelock or service; an ANR; a budget breached by 2x or more.
- **P2** - degraded experience: a budget breached under 2x; a fallback-to-English string; a label clipped at
  font scale 2.0.
- **P3** - polish: wording, layout, minor inconsistency.

Proof rank of a finding (this ticket's dynamic ladder):

- **R** - reproduced once by a scripted scene.
- **V** - visual artifact attached (screenshot or recording) showing the effect to a person.
- **T** - stand-quantified: numbers from M1-M4, at least three runs, run ids cited.
- **L** - long-run: holds across a soak or across at least three independent profiles.

**P0 and P1 are admitted only with T or L.** R and V alone ground at most a P2/P3 and open a follow-up
measurement. A false finding is as expensive as a miss (canon): the control band, the three-run
confirmation and dedupe by symptom exist so that a P0/P1 verdict is never a single noisy frame.
Out-of-scope findings are parked as fresh draft tickets with symptom and evidence - never fixed here, never
switching the active ticket.

## 10. Phases, ordered by risk

Each phase ends with the mandatory boundary audit of what it measured; findings become tickets; cells flip
to `measured` with run ids.

- **Phase 0 - stand and control** (after S0002): M0, M8, M9 and the M3 skeleton, proven on the skeleton app
  and on the empty profile. Delivers the noise floor, the backup gate, a green self-test.
- **Phase 1 - signatures** (after S0005-S0006 phone, S0008 pair): C4, C5, C6 over B1-B3 and F4-F6. The
  harm-adjacent features first: the water rule, SOS through Doze, link loss mid-signal.
- **Phase 2 - lifecycle and soak** (after S0004): groups A and G - the leak core; G1's slopes are the
  ticket's centre of mass.
- **Phase 3 - storms and large inputs** (after the target features' Phase 1-2 green): groups D and E.
- **Phase 4 - understandability sweep** (strings stable): M7 across group C, both variants, three locales,
  font scales 1.3 and 2.0; re-run per feature phase.
- **Phase 5 - full matrix on the release candidate**: every cell re-run, F-group full sweep including boot;
  the S0010 dry run consumes this verdict; a P0/P1 with T or L evidence blocks it.

## 11. Artifact privacy

- All artifacts live under `temp/S0013/runs/<run-id>/` (gitignored): `control.json`, sample CSVs, UI dumps,
  screenshots, logcat harvests, the recovery manifest. Nothing is committed (the invariant: no secrets, and
  commits only when asked); nothing leaves this machine; no remote exists.
- Sensitive classes in artifacts: screenshots and dumps can carry location-derived content (coordinates) -
  on the emulator profile these are synthetic, and they are still treated as sensitive; logcat can carry
  identifiers. Ticket evidence quotes numbers, ids and verdict lines only; coordinates are redacted to a
  precision class before any quote; no account data exists on the profile.
- Retention: a run folder is kept until the findings citing it close, then deleted; the numbers survive in
  the matrix and the findings, the content does not.
- `DIAGNOSTIC-REPORT` is not applicable: the first release exports no diagnostic bundle (the pointer's
  condition is unmet).

## 12. Risks

- **Emulator realism**: no real GNSS constellation, no barometer on the phone image - F2, F3 and parts of
  C1 stay weaker than hardware; decision 8.2 is the exit.
- **Instrument drift by API level**: dumpsys surfaces change between releases; the availability matrix of
  Phase 0 is the guard, unavailable cells stay open with reasons.
- **Emulator quirks as false FAILs** (canon trap list): the invalid-run rule is the guard; a quirk seen
  twice becomes a stand defect ticket.
- **Pair-mechanism instability** on emulators can starve `[pair]` scenes of valid runs - the pairing health
  check is part of M9's pre-flight.
- **Matrix size vs session cost**: phases bound the matrix; no session runs groups it was not assigned.
- **Carried source defects** (S4068, S4069 FastMediaSorter) may show their measured consequence here; the
  finding cites the upstream ticket - it is not re-filed as a new port defect.

## 13. Findings and run journal

The section 3.2 matrix records the historical 2026-10-25 baseline, verification and P5 run ids; its verdicts
are historical, not a fresh pass. The remaining-development inventory above records the 2026-10-27 counts.
Findings from those runs were recorded as S0018-S0023; their coverage of all FAIL cells remains to be mapped.
S0019 section 4.4 identifies the stand's dead-client torch reader and D1 quiet window as needing correction,
followed by an F8/D1 three-repeat run. The owner decisions in section 3.3, items 8.1 and 8.7, were decided on 2026-11-10 (budgets and outcomes set approved).

2026-10-29 unattended spec gate: complex work; required owner decisions 8.1 (re-issue every hypothesis
budget after the baseline) and 8.7 (approve the known-outcome set, including its listed questions) are
missing. F-group verdicts and Phase 1 budget verdicts cannot count before those decisions. Phase 4 must be
repeated after S0015 lands; Phase 5 consumes a release candidate. The document registry is not yet available
(S0029); this ticket and its tactical index are the relevant records, and product documentation is unchanged.

2026-11-10 quiz pass: owner decisions 8.1 (hypothesis budgets approved) and 8.7 (known-outcome set approved) are decided. `stand/outcomes.json` stamped approved: "2026-11-10". F-group verdicts now count.

RELEASE: after the decisions and the required stand fixes, run
`pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Scenes F8,D1 -Devices phone -Repeat 3`,
`pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1`, and
`pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Phase all -Variant store -WithM7`
as appropriate to the phase and release candidate. These slow or unmeasured device runs are not run between
releases. The matrix, its findings and the stand have not been newly verified in this pass.

2026-11-10 unattended queue pass (run-spec-queue pq2): section 4 rewritten for the implementing phases
(items 1-2 met by the stand and S0031; items 3-8 open). Every open item is a device run on the emulator
pair - unmeasured, above the 5-second ceiling, release tier (CLAUDE.md section 7) - or waits on another
ticket: Phase 4 on S0015 (In Progress), Phase 5 on the release candidate that S0010 (Draft) dry-runs.
The FAIL-to-finding map of item 5 is drawn from the `-Repeat 3` re-run, not from the single 2026-10-25
runs. No device was driven and nothing was built or tested in this pass.

Blocked by: S0015
Blocked by: S0010
