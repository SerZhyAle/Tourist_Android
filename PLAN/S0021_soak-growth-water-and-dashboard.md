# S0021 - Growth over a held screen: water flashlight file descriptors, dashboard memory

**Status:** BlockByOtherTask
**Status note:** Missing: the 30-minute soak evidence for acceptance items 1 and 2; no such soak exists yet. Owner decision 5.1: the re-measure runs at the next release beside the S0013 matrix, not before, and it is above the 5-second ceiling. Blocker: S0013. The stand is ready (slope.ps1 -SkipMinutes and -FdCeilingCsv, fd listing); the exact commands are section 6.4. Not built, not tested until the release.
**Origin:** parked finding of the S0013 dynamic audit (2026-10-25); proposed severity P1 (a leak whose CI sits
above the control band), proof rank L (three independent 10-minute soaks).
**Phase:** finding of S0013 (deep dynamic audit), Phase 2 soak
**Complexity:** hard - assessed at the spec pass 2026-10-29: the water descriptor series reads as bounded
warm-up with no code change indicated (section 4.1), but the dashboard's native growth is a research question
only a 30-minute soak answers (section 4.2). Confirmed at the plan pass 2026-11-08 (section 6).
**Depends on:** S0013
**Verification level:** device - measured on the S0013 disposable phone and watch profiles

## 0. Source

- S0013 scene G1 (each active screen held, 10 minutes per decision 8.3), budget 8.4a (CI upper bound <= control
  band + 1 %/h), the M4 Theil-Sen slope with a 400-600-resample bootstrap CI.

## 1. Problem

| Screen held 10 min | Series | Slope %/h of the median (95% CI) | Runs |
| --- | --- | --- | --- |
| phone water flashlight | open file descriptors | 96.6 (90.1..102.6), 111.9 (104.7..118.1), 113.2 (105.8..119.4) | 3 of 3 |
| phone water flashlight | PSS | 2.1 (1.9..2.2), 2.0 (1.9..2.2) | 2 of 3 above the band |
| phone dashboard | PSS | 8.3 (7.2..9.9), 18.4 (17.6..19.2), 22.7 (21.8..23.5) | 3 of 3 |
| watch dashboard | PSS | 6.5 (6.1..6.8) | 1 run |
| watch stopwatch | PSS | 17.9 (13.8..21.2) | 1 run |

Control band (M8, Settings held 10 min on the same recipes): PSS -3.7..0.0 %/h phone, -0.1..0.0 %/h watch;
threads 0..0; fd 0 by construction (INDEX.md deviation 2). The water flashlight's descriptor count grows
roughly one per minute while the lamp is simply on - the clearest leak signal of the audit.

Caveats: 10-minute soaks (decision 8.3) let warm-up (caches, JIT) weigh on the PSS slopes; the fd series has
no such warm-up. The measured build is the debug build (run-as needs it), which also logs 31 (water) and 81
(dashboard) lines a minute at idle (budget 8.3g, 1/min) - debug logging may be part of the growth.

## 2. Evidence

- `temp/S0013/runs/20261025-base-phone/scenes/G1-*`, `20261025-verify-phone-soak/scenes/G1-*`,
  `20261025-base-watch/scenes/G1-*` (`samples.csv`, `evidence.json`); controls in `temp/S0013/control/`.
- Re-run: the exact commands are section 6.4.

## 3. Acceptance

- [ ] The water flashlight's descriptor count is bounded: over a 30-minute G1-water soak (3 runs) the slope
      taken after the first 10 minutes has its CI upper bound within the band, and the plateau is no higher than
      the ceiling of a continuously drawing screen (the dashboard). Optionally the stand captures one
      `/proc/<pid>/fd` listing at the plateau to name the descriptor kind. (Re-cut 2026-11-08, question 5.2.)
- [ ] The dashboard PSS slope re-measured on a 30-minute soak (S0013 decision 8.3 is re-cut to 30 minutes for
      this ticket only, question 5.1) before it is called a leak or warm-up.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0013 (the audit this finding came from; its decision 8.4 sets the release-candidate
  cadence the re-measure follows), S0004 (the phone dashboard), S0005 (the phone water flashlight).
- **When the re-measure runs (5.1):** decided 2026-11-08 (owner, quiz): at the next release, beside the S0013
  matrix run; not before.
- **Acceptance item 1 re-cut (5.2):** decided 2026-11-08 (owner, quiz): bounded growth over a 30-minute
  G1-water soak instead of "gone" (wording in section 3).

## 4. Analysis of the recorded soaks (2026-10-29, queue pass, no new run)

Read from the S0013 evidence named in section 2; no device run was made in this pass (device matrix runs are
release-tier, S0013 status note).

### 4.1 Water flashlight descriptors - bounded warm-up, not a leak (inferred)

- In all three soaks the lamp arms at 106-108 descriptors, then every step lands 2-4 s after a wall-clock
  minute boundary: +3 at each of the first five boundaries, +1 at the next three, then the count stays at
  **126** for the remaining 64, 72 and 70 samples (2 s apart) and falls to 93 when the screen is left. The
  same ceiling in three independent runs is a fixed capacity, not an accumulation.
- The only per-minute work on that screen is the clock: `WaterFlashlightActivity` re-posts its tick at each
  minute boundary, and the render thread reports one frame a minute (`app_time_stats ... count=1`, 106 frames
  for the whole scene). The lockdown poll is removed once the lamp is armed.
- The dashboard and SOS screens, which draw continuously (1 252 and 1 207 frames), sit at 123-127 descriptors
  from their first samples - the same ceiling the lamp reaches after eight frames.
- Reading: each early frame adds the next member of the window's graphics buffer set (buffer handles and sync
  fences on the emulator's gralloc) until the set is full; a screen drawing one frame a minute takes eight
  minutes to do what others do in a second. This is inferred from timing and the shared ceiling; no
  `/proc/<pid>/fd` listing was captured, so the descriptor kind is not proven.
- Consequence for the budget: the fd control band is 0 by construction, and a 10-minute Theil-Sen slope over a
  fill phase that lasts 8 of those minutes measures the fill, whatever the ceiling. As written, acceptance item
  1 ("gone") cannot be met by any code change short of removing the clock's redraw; it needs re-cutting
  (question 5.2).
- No code change is indicated. The water PSS slope (about 2 %/h, 2 of 3 runs) is of the same order and is
  likely part of the same fill; it is judged by the same re-measure.

### 4.2 Dashboard PSS - open

- All three phone runs: PSS jumps from about 50 MB to 66 MB in the first minute (warm-up), then still grows by
  1.7-2.2 MB over the next nine minutes. Java heap is flat at 11.8-12.0 MB, threads flat at 25-27; native heap
  grows 0.6-0.7 MB over eight minutes, private dirty about 0.2-0.6 MB.
- A flat Java heap rules out a growing object graph on the Kotlin side; whether the native and graphics growth
  levels off cannot be told from ten minutes. This is the research item the 30-minute soak of acceptance item 2
  answers. The watch rows of section 1 (single runs) are outside this ticket's acceptance.

### 4.3 The debug-logging caveat

Of the 385 app-process log lines of the water soak, 308 are `Explicit concurrent copying GC freed` - the
collections the stand's own `dumpsys meminfo` sampling forces. The app's own idle logging is a few lines, so
debug logging is not a plausible cause of either growth.

### 4.4 The recorded soaks through the tail (2026-11-08, queue pass, no new run)

Direct calls of `slope.ps1` on the samples named in section 2. These are 10-minute soaks, so this tries the tool and is
not acceptance evidence, which needs 30 minutes.

| Series | Whole CSV | Tail |
| --- | --- | --- |
| water fd, verify run 1 | 109.68 %/h (CI 101.07..116.62), exit 1 | past 8.5 min: 0.00 %/h (CI 0.00..0.00, n=46), plateau 126, ceiling 127, exit 0 |
| water fd, verify run 2 | 110.95 %/h (CI 104.22..117.20), exit 1 | past 8.5 min: 0.00 %/h (CI 0.00..0.00, n=46), plateau 126, ceiling 127, exit 0 |
| water fd, base run | 93.60 %/h (CI 85.73..100.06), exit 1 | no verdict, exit 2: its `evidence.json` carries no `idleWindow` |
| dashboard PSS, verify run 1 | not called | past 5 min: 12.49 %/h (CI 10.50..14.92), limit 0.96, exit 1 |

The reading of 4.1 holds over the last 1.5 minutes of two runs. The dashboard's PSS is still growing after minute 5 of
its run, so the growth is more than the first-minute jump; whether it levels off stays the question of 4.2.

## 5. Owner questions (decided 2026-11-08, quiz)

### 5.1 When does the re-measure run?

**Decided (owner, 2026-11-08): a.**

The remaining evidence is device time on the S0013 stand: G1-water 3 soaks and G1-dashboard at 30 minutes
(about 1.5 h of emulator time plus the debug build `run-as` needs), above the 5-second ceiling between releases.

- **a (recommended)** - at the next release, beside the S0013 matrix run; the ticket waits until then.
- b - the owner names this run now, out of release.
- c - accept section 4.1 for the water flashlight on the recorded evidence and run only the dashboard soak.

### 5.2 Re-cut acceptance item 1?

**Decided (owner, 2026-11-08): a.** The wording is acceptance item 1 in section 3.

- **a (recommended)** - "The water flashlight's descriptor count is bounded: over a 30-minute G1-water soak
  (3 runs) the slope taken after the first 10 minutes has its CI upper bound within the band, and the plateau
  is no higher than the ceiling of a continuously drawing screen (dashboard)." Optionally the stand captures one
  `/proc/<pid>/fd` listing at the plateau to name the descriptor kind.
- b - keep "gone": the lamp's clock must stop causing frame-driven growth, which means a product change to the
  clock (for example no clock on the lamp) - a separate ticket.

### Quiz decisions (2026-11-08)

- When does the re-measure run? -> At the next release, beside the S0013 matrix run (5.1 a; matches S0013
  decision 8.4, release candidate only, and CLAUDE.md section 7; the owner took the recommended option).
- How is acceptance item 1 re-cut? -> Bounded growth over a 30-minute G1-water soak (3 runs), slope after the
  first 10 minutes within the band, plateau no higher than the dashboard's ceiling (5.2 a; the recorded
  evidence reads as bounded warm-up, section 4.1; the owner took the recommended option).
- Resolved without asking:
  - The dashboard soak is one 30-minute run: S0013 section 9 defines proof rank L as holding "across a soak or
    across at least three independent profiles", so a single long soak is rank L; acceptance item 2 already
    names "a 30-minute soak". The 30-minute length re-cuts S0013 decision 8.3 (10 minutes) for this ticket only,
    and the owner chose 5.1 with that length in the question.
  - The re-measure is release tier: CLAUDE.md section 7 defers the full build and slower or unmeasured tests to
    the release, and the soak needs the debug build and about 2 hours of emulator time (3 water soaks of 30
    minutes plus one dashboard soak, before setup; the 5.1 estimate of about 1.5 h is below that sum).

## 6. Plan (2026-11-08, queue pass)

No shared-contract boundary and no user-facing surface: the app is not changed, only the S0013 stand that measures
it. The complexity stays `hard`: section 4.2 remains an open research question until phase 2 answers it.

### 6.1 Coverage inventory

| Line | Where |
| --- | --- |
| Acceptance 1, slope after the first 10 minutes within the band, 3 runs | 1.1 (tool), 2.3 (run) |
| Acceptance 1, plateau no higher than the dashboard's ceiling | 1.2 (tool), 2.3 (run) |
| Acceptance 1, optional `/proc/<pid>/fd` listing at the plateau | 1.3 (tool), 2.1 (run) |
| Acceptance 2, dashboard PSS on a 30-minute soak, leak or warm-up | 1.1 (tool), 2.2 and 2.4 (run, reading) |
| Decision 5.1, at the next release beside the S0013 matrix, not before | phase 2 is release tier; 1.4 is the release flow's pointer |
| Decision 5.2, the re-cut wording | section 3 item 1; 2.3 judges that wording |
| Resolved: one 30-minute dashboard run, three water runs | 2.1 and 2.2 are separate invocations (a single `-Repeat 3` over both screens would run six soaks) |
| Research item 4.2 | 2.4 |
| Caveat 4.3, debug logging | out-of-scope: settled by section 4.3, no step |
| Watch rows of section 1 | out-of-scope: section 4.2, outside this ticket's acceptance |

### 6.2 Produces and consumes

- Phase 1 consumes `Read-Samples`, `Get-SpanSamples`, `Read-JsonFile`, `Get-SlopePctPerHour`, `Sx`, `Get-AppPid`, all
  present in the stand. It produces `Get-SoakTail`, `Get-ColumnMax`, `Save-FdListing` and the `slope.ps1` parameters
  `-SkipMinutes` and `-FdCeilingCsv`.
- Phase 2 consumes phase 1, the variant's debug APK and the M8 control of the `phone` recipe. It produces the four
  `samples.csv` and the verdict lines. Phase 3 consumes those.

### 6.3 Phase 1 - the stand can judge the acceptance (between releases, no device)

`slope.ps1` judged a whole CSV: it could not take a slope after the first 10 minutes, its series ran past the soak
window into the fall after the screen is left, and nothing compared a plateau with a ceiling. The soak length itself
needs no change: `run-matrix.ps1 -SoakMinutes` exists and the scene sampler runs for up to 24 hours.

- [x] 1.1 `_judges.ps1` declares `Get-SoakTail`; `slope.ps1` takes `-SkipMinutes` and reads its series through it.
      Check: `slope.ps1 -SkipMinutes 8.5 -Columns fd` on a recorded water soak exits 0, without it exits 1.
- [x] 1.2 `_judges.ps1` declares `Get-ColumnMax`; `slope.ps1 -FdCeilingCsv` judges the plateau against the ceiling.
      Check: the same call against a recorded dashboard soak prints both numbers.
- [x] 1.3 `scenes.ps1` declares `Save-FdListing`; the G1 phone water driver calls it between the soak and leaving the
      lamp. Check: the readers self-test's driver shape line.
- [x] 1.4 `tools/release/README.md` names this re-measure beside the sweep, outside its verdict (S0013 decision 8.4).
      Check: `tools/fast-check.ps1 -Area docs` exits 0.
- [x] 1.5 `readers-selftest.ps1` holds fixtures for 1.1 to 1.3, a leaking series among them, so the guard can fail.
      Check: exit 0, recorded time at or under 5 s.

### 6.4 Phase 2 - the re-measure (release tier, decision 5.1)

The S0013 stand order applies first (`INDEX.md`, "Order of a full pass"): the probe APK, `:phone:assembleNoLegalDebug`,
`selftest.ps1` green, the phone control present for the current recipe hash. Stand path below: `S=PLAN/S0013_deep-dynamic-audit/stand`.

- [ ] 2.1 `pwsh -NoProfile -File $S/run-matrix.ps1 -Scenes G1 -SoakScreens water -Devices phone -SoakMinutes 30 -Repeat 3 -RunId S0021-water`
- [ ] 2.2 `pwsh -NoProfile -File $S/run-matrix.ps1 -Scenes G1 -SoakScreens dashboard -Devices phone -SoakMinutes 30 -RunId S0021-dashboard`
- [ ] 2.3 Acceptance 1, for r in 1, 2, 3, each must exit 0:
      `pwsh -NoProfile -File $S/slope.ps1 -Csv temp/S0013/runs/S0021-water/scenes/G1-water-phone-r$r/samples.csv -Columns fd -SkipMinutes 10 -FdCeilingCsv temp/S0013/runs/S0021-dashboard/scenes/G1-dashboard-phone-r1/samples.csv`
- [ ] 2.4 Acceptance 2, the verdict is the reading:
      `pwsh -NoProfile -File $S/slope.ps1 -Csv temp/S0013/runs/S0021-dashboard/scenes/G1-dashboard-phone-r1/samples.csv -Columns pssKb -SkipMinutes 10`
      Exit 0: warm-up, the growth levels off. Exit 1: a leak, parked as its own ticket with these numbers. Exit 2: the
      CI straddles the limit and tells neither, which is written as such.

### 6.5 Phase 3 - verdict

- [ ] 3.1 The numbers of 2.3 and 2.4 and the descriptor kinds of `fd-listing.txt` go into section 4; the audit below
      is rewritten from them. A breach of acceptance 1 is a finding of this ticket, not a release gate (S0013 decision 8.4).

## Last Audit

**Date:** 26-11-08 17:33 | **Mode:** static | **Outcome:** Partial
**Counts:** PASS 5 | WARN 1 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 2 | RELEASE 3

Queue pass (run-spec-queue pq2). Phase 1 of section 6 is done: only the stand and its documents changed, the app did
not; nothing built, no device run. The ticket's deliverable is the measurement, which does not exist yet, so it is not
Implemented and waits at BlockByOtherTask for the release run.

| Outcome | Check | Command, exit | Expected, actual |
| --- | --- | --- | --- |
| PASS | 1.1 tail | `slope.ps1 -Csv <water> -Columns fd`, then with `-SkipMinutes 8.5` | 1 then 0; 1 then 0 on two runs, 1 then 2 on the run with no window (section 4.4) |
| PASS | 1.2 ceiling | the same call with `-FdCeilingCsv <dashboard>`, exit 0 | plateau within ceiling; 126 and 127 |
| PASS | 1.3 listing placement | `readers-selftest.ps1`, driver shape line | ok; ok |
| PASS | 1.4 release pointer | `tools/fast-check.ps1 -Area docs`, exit 0 | 5 checks; 5 PASS |
| PASS | 1.5 fixtures | `readers-selftest.ps1`, exit 0 | at or under 5 s; 35 fixtures, 0.95 to 3.39 s |
| WARN | acceptance 1 | section 4.4 | a 30-minute soak in 3 runs; 10-minute tails in 2 runs, flat at 126, read as bounded and unproven |
| RELEASE | acceptance 1 | section 6.4 steps 2.1 and 2.3 | not run, device run above the 5-second ceiling |
| RELEASE | acceptance 2 | section 6.4 steps 2.2 and 2.4 | not run, same reason |
| RELEASE | `Save-FdListing` on a device | section 6.4 step 2.1 | not run, same reason; only its placement is checked |
| EXEMPT | watch rows of section 1 | none | outside this ticket's acceptance (section 4.2) |
| EXEMPT | debug-logging caveat | none | settled by section 4.3 |

No probe lines: the ticket is outside the needs-manual-test status. No shared-contract boundary, no user-facing surface.

### Action items

1. Run section 6.4 at the next release, beside the S0013 matrix (owner decision 5.1), then phase 3.

### Manual / on-target

None for a human. Phase 2 is a stand run on the disposable emulator profiles.
