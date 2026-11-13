# S0022 - Notification hygiene: two SOS notifications while switching mode; a dead watch stopwatch's notification

**Status:** BlockByOtherTask
**Status note:** Diagnosed 2026-10-29: no app defect, no code change. Item 1 is the stand counting the SOS notification's 200 ms enqueue copy (API 33 NMS dump); item 2 is the designed time-origin stopwatch, which resumes after a kill. Blocker: S0031 (stand notification reader and F8-watch judge). Blocker: S0013 (decision 8.7, add stopwatch-resumed). Then release-tier D3 phone and F8 watch re-runs, 3 of 3.
**Complexity:** low - diagnosis pass (2026-10-29), confirmed: both items are stand misreads of ported behaviour, no product code changes; what remains is S0031's reader fixes, S0013 decision 8.7 and a release-tier re-run
**Phase:** finding of S0013 (deep dynamic audit)
**Verification level:** device - reproduced by script on the S0013 disposable phone and watch profiles

## 0. Source

- S0013 scenes D3 (SOS mode-flip storm, budget 8.3f: at most one notification per active feature) and F8 (hard
  kill mid-feature, the known-outcome set of decision 8.7, draft).

## 1. Problem

1. **Phone SOS.** Switching the SOS mode 60 times (sound and light, sound only, light only, about 0.2 s apart)
   while the signal runs shows two notifications of the app at once for the one active feature, 3 of 3 runs
   (`20261025-base-phone`, `20261025-verify-phone`).
2. **Watch stopwatch.** With the watch stopwatch running, the app process was killed with SIGKILL. Its ongoing
   notification stayed in the shade with no process behind it (`notifs=1`, no pid), 3 of 3 runs
   (`20261025-verify-watch` F8 r1-r3): the watch shows a running stopwatch that no longer runs.

## 2. Evidence

- `temp/S0013/runs/20261025-verify-phone/scenes/D3-phone-r{1,2,3}/samples.csv` (`notifs` column),
  `temp/S0013/runs/20261025-verify-watch/scenes/F8-watch-r{1,2,3}/evidence.json`.
- Re-run: `run-matrix.ps1 -Scenes D3 -Devices phone -Repeat 3` and `-Scenes F8 -Devices watch -Repeat 3`.

## 3. Acceptance

- [ ] One SOS notification at any moment of the D3 storm (3 of 3).
- [ ] After the watch app's process dies, no ongoing stopwatch notification claims a running stopwatch, or the
      stopwatch truly resumes (owner's choice in decision 8.7), re-measured by F8 watch (3 of 3).

## 4. Spec pass and diagnosis (2026-10-29, unattended queue pass)

Rung: complex by the ticket's own acceptance (device re-measure on the S0013 stand), worked first as a
diagnosis. Shared-contract boundary: none - neither item touches a format, a wire shape or a Data Layer
message. Both items are behaviour of files ported from FastMediaSorter (port-ledger rows for
`ui/sos/SosService.kt`, `wear/ui/apps/stopwatch/WearStopwatchOngoingNotificationManager.kt` and
`wear/data/repository/WearStopwatchSessionRepositoryImpl.kt`, ported 2026-10-25), so a change here is a
divergence under CLAUDE.md section 2 and needs a defect of the app to justify it. None was found.

### 4.1 Item 1 - the second SOS notification is the same notification counted twice

- `SosService` posts exactly one notification id (`NotificationIds.SOS_SIGNAL`), and every mode change re-runs
  `startSignal`, which re-posts it through `startForeground` with the new mode in its text. There is no second
  notification of the app in that flow; the flashlight shortcut entry is off in D3 (`notifs=0` before and after
  the SOS span).
- The stand counts `NotificationRecord(.*pkg=com.sza.tourist ` lines between `Notification List:` and
  `Snoozed notifications:` of `dumpsys notification --noredact`. On API 33, the S0013 phone image
  (`system-images;android-33;google_apis;x86_64`), AOSP `android13-release`
  `NotificationManagerService.dumpImpl` prints, inside that range, the posted `Notification List:` and then the
  `Enqueued Notification List:` before `mSnoozeHelper.dump` writes `Snoozed notifications:`. With a notification
  assistant bound, every enqueue waits `DELAY_FOR_ASSISTANT_TIME = 200` ms before `PostNotificationRunnable`
  moves it to the posted list (Android System Intelligence runs on the image: `AiAiEcho` lines in the D3
  logcat). An update of the one key therefore appears in both lists for 200 ms.
- The samples fit that and nothing else: D3-phone r1 has `notifs=2` in 4 of 14 samples of the SOS span, only
  inside the flip storm, never before or after it, while `fgServices` stays 1 throughout.
- Verdict: not a defect of the app. The fix is the stand's reader (count only the posted list, or distinct
  keys), parked as S0031 item 1.

### 4.2 Item 2 - the ongoing notification after a kill describes a stopwatch that is still running

- The watch stopwatch is a stored time origin, not a counting process: the notification's chronometer and the
  ongoing-activity status are counted by the system from `timeZero`
  (`WearStopwatchOngoingNotificationManager.post`), the measurement is written on every change
  (`WearStopwatchSessionRepositoryImpl.update`) and restored on the next process start, rebased through the wall
  clock when the boot count moved (`WearStopwatchSnapshot.decode`; unit tests `a mixed measurement survives the
  round trip on the same boot` and `after a reboot a running participant keeps counting from its real start`).
  Opening the stopwatch (from the notification's touch intent or the program list) restores the same running
  measurement, and `SyncWearStopwatchOngoingUseCase` puts the indicator back in line with it.
- So the notification left after SIGKILL is true: the stopwatch still runs, as acceptance item 2's second
  branch requires. The first branch is not reachable without a rewrite: no code runs after SIGKILL, so only a
  foreground service whose death takes the notification with it could remove it, and that would also end the
  measurement - a behaviour FastMediaSorter does not have.
- The stand reports `stale-notification-left` for any notification with no pid and never re-opens the
  stopwatch: the cold start after the kill lands on the program list (r1 checkpoint dump; the r2 and r3 dumps are
  empty). The judge and its allowed set are parked as S0031 item 2; the allowed set itself is S0013 decision 8.7.

### 4.3 Decisions

| Question | Answer | Settled by |
| --- | --- | --- |
| Acceptance item 2, branch (the ticket's "owner's choice in decision 8.7") | the stopwatch truly resumes - already the ported behaviour | fact: code and tests of 4.2; the other branch needs a rewrite (CLAUDE.md section 2). Proposed to S0013 decision 8.7: add `stopwatch-resumed` to the `F8-watch` allowed set. Approving that set stays the owner's, in S0013 |
| Change `SosService` to post fewer updates during a flip storm | no | item 1 is a measurement artifact; the shade holds one notification throughout |

### 4.4 Left open

1. S0031 (parked 2026-10-29): fix the notification reader and the F8-watch judge (with the S0019 torch reader).
2. S0013 decision 8.7: approve the known-outcome set, including `stopwatch-resumed` for `F8-watch`.
3. RELEASE: after both, `pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/run-matrix.ps1 -Scenes D3
   -Devices phone -Repeat 3` and `... -Scenes F8 -Devices watch -Repeat 3` - the literal form of both acceptance
   items. Device runs are release-tier and were not run.

## Last Audit

**Date:** 26-10-29 03:45 | **Mode:** static | **Outcome:** Partial (two UNCHECKABLE items keep it below Verified)
**Counts:** PASS 4 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 2 | EXEMPT 0 | RELEASE 3

- PASS - one SOS notification id: `NOTIFICATION_ID = NotificationIds.SOS_SIGNAL` is the only id `SosService`
  posts; `startForegroundCompat` is called from `startSignal` (every mode change) and `stopSignal` (grep).
- PASS - the stand's range spans both NMS lists: AOSP `android13-release` `NotificationManagerService.java`,
  `Notification List:` (line 6099), `Enqueued Notification List:` (6172), `mSnoozeHelper.dump` (6181),
  `DELAY_FOR_ASSISTANT_TIME = 200` (438) and its `postDelayed` (7359), fetched in this pass; the stand's sed
  range read in `_instruments.ps1` line 144.
- PASS - the watch measurement is persisted and restored: `update` writes `WearStopwatchSnapshot.encode` on
  every change, `restore` decodes it at process start, `decode` rebases across a boot (grep).
- PASS - the F8-watch judge reads only `notifs` and `pid` (`scenes.ps1` line 1565) and never re-opens the
  stopwatch; the r1 checkpoint after the kill shows the program list.
- UNCHECKABLE - the notification assistant is bound on the S0013 phone image: inferred from `AiAiEcho` lines
  and the 4-of-14 sample rate, not read from `dumpsys notification` (no device run).
- UNCHECKABLE - acceptance items 1 and 2 in their literal form (3 of 3 on the stand) cannot pass until S0031's
  reader fixes and S0013 decision 8.7 land.
- RELEASE - `./gradlew :wear:test` (the two `WearStopwatchSnapshotTest` cases cited in 4.2) - not run, Gradle
  is release-tier.
- RELEASE - `run-matrix.ps1 -Scenes D3 -Devices phone -Repeat 3` after S0031.
- RELEASE - `run-matrix.ps1 -Scenes F8 -Devices watch -Repeat 3` after S0031 and S0013 decision 8.7.

No product code, resource or test changed; no probe exists or is needed (the ticket changes no code).
Not built, not tested until the release.
