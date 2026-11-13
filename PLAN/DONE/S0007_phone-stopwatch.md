# S0007 - Phone stopwatch: multi-participant screen, music, widget

**Status:** Implemented
**Status note:** Phone stopwatch multi-participant, music, widget fully implemented and verified on Pixel 6 (API 33)
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

- [x] The stopwatch runs with several participants on a device; the widget reflects its state. Verified on Pixel 6 (API 33): 1 and 4 participants, start/lap/stop, hardware volume key control, result file generation in `/sdcard/Download/Tourist_stopwatch_*.txt`, and widget state toggle verified via broadcast and SharedPreferences.
- [x] The music picker works or is cut with the reason recorded. Verified: Music picker kept and working with runtime `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` (maxSdkVersion=32) permission request. Test audio track `Tourist_Pace_Track.wav` pushed, scanned into MediaStore, picked and played in StopwatchActivity.
- [x] Ported unit tests pass; every ported file has a ledger row; drift check clean or decided. Verified: all unit tests passed in `:phone:testStoreDebugUnitTest` and `:phone:testNoLegalDebugUnitTest`, R8 release assemblies clean in `:phone:assembleStoreRelease` and `:phone:assembleNoLegalRelease`, 163 ledger rows in `docs/port-ledger.jsonl` with 0 drift.

## 5. Record of the phase

### 5.1 What was ported, and where
- **Domain & Data:**
  - `domain/model/stopwatch/StopwatchModels.kt`, `StopwatchSettings.kt`, `MusicTrackOption.kt`
  - `domain/repository/MusicTrackRepository.kt`, `data/repository/MediaStoreMusicTrackRepository.kt`
  - `data/settings/StopwatchSettingsStore.kt`
  - `domain/usecase/stopwatch/LoadMusicTracksUseCase.kt`, `ObserveStopwatchSettingsUseCase.kt`
  - `core/util/ElapsedClock.kt`, `core/di/ElapsedClockModule.kt`, `core/di/MusicTrackModule.kt`
- **UI & Helpers:**
  - `ui/stopwatch/StopwatchViewModel.kt`, `StopwatchActivity.kt`
  - `ui/stopwatch/StopwatchSettingsDialogFragment.kt`, `StopwatchResultDialogFragment.kt`, `MusicPickerDialogFragment.kt`
  - `ui/stopwatch/helpers/StopwatchEngine.kt`, `StopwatchAudioManager.kt`, `StopwatchInputManager.kt`, `StopwatchRegionBinder.kt`, `StopwatchResultRenderer.kt`, `StopwatchResultFileWriter.kt`, `StopwatchTimeFormatter.kt`, `MusicTrackAdapter.kt`
  - `ui/common/widget/SettingsDropdownRow.kt`, `SettingsSelectionRow.kt`, `PopupMenuWidthMeasurer.kt`
  - `core/share/SharePayload.kt`, `SystemShareInvoker.kt`, `utils/MediaStoreNotifier.kt`, `ui/dialog/DialogKeyboardDelegate.kt`
- **Widget:**
  - `widget/StopwatchWidgetProvider.kt`, `widget/helpers/StopwatchWidgetStateStore.kt`
- **Resources:**
  - Layouts: `activity_stopwatch.xml` (portrait & land), `view_stopwatch_region.xml`, `dialog_stopwatch_settings.xml`, `dialog_stopwatch_result.xml`, `dialog_music_picker.xml`, `item_music_track.xml`, `widget_stopwatch.xml`, `xml/widget_stopwatch_info.xml`, custom row layouts.
  - Drawables: `ic_settings.xml`, `ic_widget_stopwatch_accent.xml`, `ic_arrow_drop_down.xml`, `ic_chevron_right.xml`, `ic_arrow_forward.xml`, `item_focus_selector.xml`, `widget_background.xml`, `ic_pause.xml`.
  - Values: strings (EN, RU, UK), colors, dimens, attrs, themes.
- **Entry Points:**
  - Added `row_stopwatch` in `activity_main.xml` and `MainActivity.kt`.
  - Wired `btnStopwatchOpen` in `TouristHeroTileManager.kt`, `TouristActionsManager.kt`, and `activity_tourist_info.xml`.
  - Declared `StopwatchActivity` and `StopwatchWidgetProvider` in `AndroidManifest.xml`.
- **Tests:**
  - `StopwatchViewModelTest`, `StopwatchEngineTest`, `StopwatchAudioManagerTest`, `StopwatchInputManagerTest`, `StopwatchResultRendererTest`, `StopwatchTimeFormatterTest`, `StopwatchWidgetStateStoreTest`, `SettingsRepositoryImplTest`.

### 5.2 Left behind
- `StopWatchListeningUseCase.kt` and speech recognition / voice control: cut per kit scope (Tourist uses hardware keys, gamepad/remote, and touch without speech recognition engine dependency).
- `enable_stopwatch` setting: dropped because Stopwatch is a first-class feature always available in Tourist.

### 5.3 Tactical decisions of this ticket
1. **Music picker permission:** Declared `READ_MEDIA_AUDIO` (API 33+) and `READ_EXTERNAL_STORAGE` (`android:maxSdkVersion="32"`) in `AndroidManifest.xml`. Requested at runtime when opening `MusicPickerDialogFragment`.
2. **Exported file format:** Output file naming pattern `Tourist_stopwatch_<yyyyMMdd_HHmmss>.txt` saved to `Downloads` directory via MediaStore / file system.
3. **Widget action:** Explicit broadcast action `com.sza.tourist.action.STOPWATCH_WIDGET_TOGGLE` registered in manifest filter.
