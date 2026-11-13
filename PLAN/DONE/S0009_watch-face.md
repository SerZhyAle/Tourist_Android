# S0009 - Watch face

**Status:** Implemented
**Phase:** 9 of 10 (handoff section 8)
**Verification level:** device - the watch face installs and shows the time with the product branding.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 9, section 4.4, section 3 (extraction-only non-goal), section 7
(PACKAGE-VERSIONING).

## 1. Goal

The watch face package under `com.sza.tourist.watchface` exists, installs and shows the time with the product's
branding. Owner decision 4.4 settled that the module exists; this phase settles what is in it.

## 2. Scope

- The module created empty by S0002 gets its content. FastMediaSorter's watch face is a Watch Face Format
  (WFF) face with no tourist content: no altitude, compass, sunrise or steps complication; its only tourist link
  is a shortcut slot that opens the program. The handoff's source map does not name that module's path: locate
  it in `P:\ANDROID\FastMediaSorter_mob_v2` (read-only) at ticket start and record it in the port ledger.
- If the face is a clone, it is ported in full and rebranded (full port, never a rewrite); if tourist
  complications are added, that is new code - an explicit exception to the extraction-only non-goal, written
  into this ticket before any line is written.
- The face's version family is the one registered for it in PACKAGE-VERSIONING by S0002's amendment.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002 (module, frozen id, version family); S0008 (the watch app the face accompanies).
- **4.4 Watch face composition (owner 2026-10-25):** Rebranded clone of FastMediaSorter's WFF watch face (digital clock, battery indicators, tap target launching Tourist Wear app, 6 customizable complication slots, brand accents, EN/RU/UK strings, minSdk 36).

## 4. Acceptance

- [x] The watch face installs on a Wear OS device or emulator and shows the time with the product branding.
- [x] Its versionName and versionCode come from its registered family; the id is `com.sza.tourist.watchface`.
- [x] Every ported file has a ledger row; drift check clean or decided.

## 5. Implementation and evidence (2026-10-25)

- **Watch Face Format v4 & Platform level:**
  Ported declarative resource-only WFF v4 (`com.google.wear.watchface.format.version` = 4) watch face from FastMediaSorter's `watchface` module.
  `min-sdk-watchface` is 36 (Wear OS 6 / API 36 required by WFF v4) in `gradle/libs.versions.toml`.
- **Identity & Packaging:**
  Package ID `com.sza.tourist.watchface`, resource-only declarative module (`enableKotlin = false`, `hasCode = "false"`, R8 minification stripping empty R class with 0 DEX files in debug and release APKs).
  Debug face name `Tourist (Debug)` (`watchface/src/debug/res/values/strings.xml`, `values-ru`, `values-uk`); release name `Tourist`.
- **Visual Composition & Complications:**
  Large digital clock (`SYNC_TO_DEVICE`), date display, ambient twin mode with sparse outlines (`#FFA5D6A7`, `#FF2E7D32`).
  Watch battery level indicator (`[BATTERY_PERCENT]`) at top center; paired phone battery bar at bottom center.
  Tourist brand green wave lanes (`#FF1B5E20`, `#FF2E7D32`, `#FF388E3C`, `#FF4CAF50`, `#FF81C784`) and mint tilt parallax particles (`#FFE8F5E9`) drawn with vector primitives and `<Transform>`/`<Gyro>`.
  Launch tap target on dial center opens `com.sza.tourist/com.sza.tourist.wear.MainActivity`.
  Six customizable complication slots (outer left/right, inner left/right, top left/right) and editable metadata in `watch_face_info.xml` (`<Editable value="true" />`).
  Bundled digits font families (`tourist_clock_casual.ttf`, `tourist_clock_condensed.ttf`, `tourist_clock_mono.ttf`, `tourist_clock_serif.ttf`) with font notices in `third_party_notices.txt`.
  Localization: English, Russian, Ukrainian strings for complication slot labels and screen reader text; `locales_config.xml`.
- **Port Ledger & Drift Check:**
  17 ported files registered in `docs/port-ledger.jsonl`.
  `tools/port-ledger/check-drift.ps1` clean: `193 row(s) in step, 0 decided, 0 uncarried`.
  `tools/port-ledger/test-port-ledger.ps1` self-test passed: 20 passed, 0 failed.
- **Builds & Automated Tests:**
  `./gradlew :watchface:assembleDebug :watchface:assembleRelease` passed: outputs `watchface-debug.apk` (80,522 bytes) and `watchface-release-unsigned.apk` (75,995 bytes), both with 0 DEX files.
  `./gradlew :watchface:test` and `./gradlew :phone:test` passed with 0 failures.
- **Emulator Verification:**
  Installed onto `Wear_OS_Small_Round` (API 37) emulator via `adb install -r`.
  Verified package registration via `dumpsys package com.sza.tourist.watchface`:
  `versionCode=261025125`, `versionName=2.61.0251.258`, `minSdk=36`, `targetSdk=36`, `installed=true`.
