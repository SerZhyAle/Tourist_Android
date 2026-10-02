# S0007 - Phone stopwatch: multi-participant screen, music, widget

**Status:** Draft
**Phase:** 7 of 10 (handoff section 8)
**Verification level:** device - the stopwatch runs with several participants; unit tests of the ported classes.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 7, section 5 (phone module: stopwatch), section 6a.

## 1. Goal

The multi-participant stopwatch of the kit runs on the phone, with its music picker and its widget.

## 2. Scope

Source paths relative to `P:\ANDROID\FastMediaSorter_mob_v2` (read-only), `A` =
`app_v2/src/main/java/com/sza/fastmediasorter`.

- `A/ui/stopwatch/` (13 files at hand-off, with its helpers); `A/domain/model/stopwatch/`,
  `A/domain/usecase/stopwatch/`, `StopWatchListeningUseCase.kt`,
  `A/data/repository/settings/StopwatchSettingsStore.kt`.
- Widget: `A/widget/StopwatchWidgetProvider.kt` and `A/widget/helpers/StopwatchWidgetStateStore.kt`.
- Music picker: `MediaStoreMusicTrackRepository`, `MusicTrackRepository`, `A/di/MusicTrackModule.kt`.
- Stored state keep-or-drop, written down: `enable_stopwatch`, the keys of `StopwatchSettingsStore` and
  `StopwatchWidgetStateStore`.
- Unit tests ported: `StopwatchViewModelTest` and the five `ui/stopwatch/helpers` tests.
- A check at ticket start: the phone permission list of handoff section 5 names no media-read permission, yet
  the music picker reads audio from `MediaStore`. Read the source's merged manifest and decide, with evidence,
  whether the permission ports or the picker's source is cut; the declaration must match runtime use.
- INPUT-PARITY and accessibility as in S0004; EN/RU/UK strings.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0003 (core), S0004 (shell).
- **Owner questions:** n/a - no open owner decision of handoff section 4 is needed by this phase.

## 4. Acceptance

- [ ] The stopwatch runs with several participants on a device; the widget reflects its state.
- [ ] The music picker works or is cut with the reason recorded.
- [ ] Ported unit tests pass; every ported file has a ledger row; drift check clean or decided.
