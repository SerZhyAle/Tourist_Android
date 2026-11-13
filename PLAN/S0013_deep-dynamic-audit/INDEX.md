# S0013 tactical folder - the measurement stand

The stand that measures the five properties of `PLAN/S0013_deep-dynamic-audit.md` from outside the app. Every
script speaks `CHECK-VERDICT` section 2: exit `0`/`1`/`2`, one verdict line, the line last. Run them with
`pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/<script>.ps1 ...` (never as a Bash command).
Artifacts go to `temp/S0013/` only (gitignored); nothing here is ever shipped.

## Files

| File | Registry id | What it is |
| --- | --- | --- |
| `stand/selftest.ps1` | M0 | coverage self-test: CLAUDE.md section 1 and handoff section 3 against the matrix, the registry, `budgets.json` and the drivers |
| `stand/ping-ui.ps1` | M1 | `am start -W` cold/warm/hot and `dumpsys gfxinfo` frame pacing on a live profile |
| `stand/key-latency.ps1` | M2 | injected tap/key/rotary to the next frame (logcat mark + framestats, one clock) |
| `stand/sampler.ps1` | M3 | the periodic outside harvest plus the logcat crash/ANR/secret scan |
| `stand/slope.ps1` | M4 | Theil-Sen slope and bootstrap CI of a sampler CSV against the control band; `-SkipMinutes` judges the soak window past its fill, `-FdCeilingCsv` a descriptor plateau against a drawing screen (S0021) |
| `stand/fault-inject.ps1` | M5 | the F group and B4 through the engine, P2 by default |
| `stand/storm.ps1` | M6 | the D group through the engine |
| `stand/locale-a11y.ps1` | M7 | the EN/RU/UK x font 1.0/1.3/2.0 sweep of every checkpoint of group C |
| `stand/control.ps1` | M8 | the no-app control run; writes `temp/S0013/control/<recipe>-<hash>.json` |
| `stand/profile.ps1` | M9 | disposable AVD lifecycle from `profile.recipe.json`, install + version readback, the backup gate |
| `stand/run-matrix.ps1` | M10 | walks the scene x property cells, one verdict line per cell, the aggregate last |
| `stand/readers-selftest.ps1` | - | fixture check of the readers and judges that misjudged the app (S0031): the notification count, the torch reader, the F8-watch judge, the D1 and F8 driver shape, the soak tail and descriptor ceiling (S0021); no device, 0.95 to 3.39 s (three runs, 2026-11-08), run it after any edit to a reader |
| `stand/_stand.ps1`, `_instruments.ps1`, `_judges.ps1`, `_engine.ps1`, `scenes.ps1` | - | the shared library: process and adb helpers, instruments, budget judges, the scene engine, the 41 scene drivers |
| `stand/budgets.json` | - | machine form of ticket section 8; M0 fails if a `text` differs from the ticket row |
| `stand/outcomes.json` | - | the known-outcome set of property 2 - approved 2026-11-10 (decision 8.7) |
| `stand/profile.recipe.json` | - | the committed AVD recipes: `phone` (API 33 google_apis), `phone-nobaro`, `wear` (API 37 Wear OS), `wear-nobaro` |
| `stand/probe-a11y/` | - | the stand's accessibility probe APK (source + `build.ps1`) - see deviation 1 |

## Order of a full pass

1. `probe-a11y/build.ps1` (once per machine), then build the variant's debug APKs (`:phone:assemble<Variant>Debug`,
   `:wear:assemble<Variant>Debug`, `:watchface:assembleDebug`).
2. `selftest.ps1` must be green: a red M0 makes every run invalid (ticket section 7).
3. `control.ps1 -Recipe phone` and `-Recipe wear` - once per recipe hash and host-load profile.
4. `run-matrix.ps1 -Phase all` (fresh profiles, created and destroyed by the run), then
   `locale-a11y.ps1` for the P5 cells (the sweep is slow, so it is a run of its own).
5. Findings: each FAIL cell is re-run with `-Repeat 3` before a P0/P1 is claimed (rank T, ticket section 9).

## Instrument availability (pins of S0002: phone API 33 google_apis x86_64, watch API 37 Wear OS x86_64)

| Instrument surface | Phone API 33 | Watch API 37 |
| --- | --- | --- |
| `am start -W` TotalTime | yes | yes (screen must be awake; `LaunchState: UNKNOWN` with the screen off) |
| `dumpsys gfxinfo` / `framestats` | yes | yes |
| `dumpsys meminfo` TOTAL / heaps | yes | yes |
| `/proc/<pid>/fd` via run-as, `/proc/<pid>/task` | yes | yes |
| `dumpsys sensorservice` active connections (`\| uid N \|`) | yes | yes |
| `dumpsys location` registrations (`uid/pkg/id Request[`) | yes | yes |
| `dumpsys power` wake locks (own `uid=`, attributed `ws=WorkSource{uid}`) | yes | yes |
| `dumpsys media.camera` torch event log (an `on` entry counts only while its client PID is alive: `/proc/<pid>`, S0031) | yes | no torch on the watch |
| `dumpsys notification --noredact` list (counted by distinct `pkg user id tag`: API 33 lists an update of one key in the posted and the enqueued list for 200 ms, S0031) | yes | yes |
| `dumpsys audio` focus stack | yes | yes |
| `dumpsys package` uid field | `userId=` | `appId=` |
| uiautomator dump of a live screen | fails ("could not get idle state") | fails likewise |
| accessibility probe dump | yes | yes |
| battery / thermal | synthetic on the emulator - 8.3h uses the CPU ms/min proxy | same |

## Deviations from the design, recorded

1. **An on-device helper for the dumps.** Section 5 describes a host-side stand. uiautomator cannot dump a screen
   that never idles (the dashboard's live compass), so M7 and the drivers read the window tree through the
   stand's own accessibility service APK (`probe-a11y/`, package `com.sza.s0013probe`). It is not product code,
   asks the app for nothing, and reads exactly what TalkBack reads; it is bound only for the duration of a dump
   (or a whole M7 sweep), so the measured app runs with accessibility off. It also yields the
   `stateDescription` that uiautomator omits (budget 8.5e).
2. **The fd control band is 0.** The reference process's fd table is not readable without root, and the fd count
   is an exact counter read without jitter; the 8.4a allowance (+1 %/h) is the whole margin for fd.
3. **8.3h is a CPU proxy.** The emulator's battery and thermal readings are synthetic; the budget is judged on
   background-idle CPU ms/min of the app against the control band of an idle reference process, for the
   background idle scenes (G2, G3) only.
4. **Unit-system flip (D5) writes the stored setting.** The phone app has no control for the measurement system;
   the scene writes `unit_system` into the app's own `settings` preferences between runs of the app.
5. **The launcher is reset after C8.** Widgets live in the launcher's layout, not the app's data; the scene clears
   the launcher so no widget outlives it.
