# S0036 - google-play-policy-oct-2026

**Ticket:** S0036
**Status:** Verified
**Status note:** Audit Verified 2026-11-08: no tree change needed; owner actions A1-A3 carried by S0027 section 6; merged-manifest rung is the release's.
**Complexity:** light - assessed at the spec pass: the research found no manifest, code or dependency change to make; the work is one recorded audit plus two owner actions handed to S0027
**Date:** 2026-10-30

## Captured request and migration scope

[https://support.google.com/googleplay/android-developer/answer/18100766](https://support.google.com/googleplay/android-developer/answer/18100766)

раздай задания андроид-проектам

## Assignment context

Review Google's October 29, 2026 Play policy announcement linked above for this Android product and its Google Play distribution plans. Verify the current policy text and exact deadlines against Google's linked policy and deadline pages. Inventory applicable app modules, variants, permissions, SDKs and Play Console declarations; distinguish actual requirements from clarifications and from changes that do not apply.

Assess local network access, location, all-files access, contacts, accessibility, call logs, AppFunctions, subscriptions and payments where relevant, and the announced app quality and developer account requirements. Implement and validate necessary project changes within the ticket's normal workflow. Record evidence, non-applicability and any account or Play Console actions and deadlines that need the owner. Do not assume a permission or policy violation without evidence.

## 1. Spec

**Shared-contract boundary:** not touched. No format, wire shape, algorithm constant or behaviour that another product reads changes; the ticket records a policy audit and edits no code.

**Goal:** every item of Google's 2026-10-29 Play policy announcement is classified for this product as a requirement, a clarification or not applicable, with evidence from the tree; every account or Play Console act it creates for the owner is written down with its deadline and handed to the ticket that performs the console work.

**Sources read 2026-11-08** (through a page reader that summarizes, so each date was cross-read on a second Google page):

- The announcement, `support.google.com/googleplay/android-developer/answer/18100766` - states "at least 30 days" from 2026-10-29 for the new policies.
- The policy preview, `answer/16909972` - gives the dated rules for location, local network and manage contacts.
- The Policy Deadlines table, `table/12921780` - eight rows dated 2027-01-27 to 2027-08-25.
- The app quality page, `answer/17492799` - memory and code optimization, February 2027.

Known source inconsistency, not blocking: the announcement body reads as if the local network and manage contacts rules fall under the general 30-day window, while the preview page and the deadlines table both date them 2027-08-25. Neither rule applies to this product (2.1), so the dates used below are the table's.

## 2. Findings

### 2.1 Classification of every announced item

| Item | Kind | Date | Applies here | Evidence |
| --- | --- | --- | --- | --- |
| Location permissions: minimum scope, the Android location button on API 37+, a declaration for fine location | requirement (announced April 2026) | 2027-01-27 | **yes, phone `store`**: the declaration of 3.1 A1. The button rule is not yet in force: `target-sdk` is 36 | 2.2 |
| Local network permission (`ACCESS_LOCAL_NETWORK`, declaration) | new policy | 2027-08-25 | no: no module declares the permission; the app reads one public host, Open-Meteo | P1 |
| Manage Contacts (`MANAGE_CONTACTS`) and the contacts row of the table | new policy and clarification | 2027-01-27 and 2027-08-25 | no: no contacts permission, no `ContactsContract` call | P1, P2 |
| AppFunctions API | new policy | 2027-04-14 | no: no AppFunctions use | P2 |
| Subscriptions with eligibility discounts; Payments and agentic purchases | updated policy and clarification | 2027-01-27 | no: free, no ads, no purchases, no billing library (S0027 section 2.1) | P2 |
| All Files Access (`MANAGE_EXTERNAL_STORAGE`) | clarification | none | no: audio tracks are listed through `READ_MEDIA_AUDIO` and, up to API 32, `READ_EXTERNAL_STORAGE` | P1 |
| Accessibility API (`isAccessibilityTool`) | clarification | none | no: no `AccessibilityService`; the `probe-a11y` manifest under `PLAN/S0013_deep-dynamic-audit/stand/` is a test stand, not a shipped module | P2 |
| SMS and call log groups, `READ_CALL_LOG` for phone-call verification | clarification and dated change | 2027-01-27 | no: no SMS or call log permission | P1 |
| Foreground service row: geofencing removed as a reason | table row | 2027-01-27 | no: the only foreground service is `SosService`, type `mediaPlayback`; no geofencing | P1, P2 |
| Deceptive Behavior, Families ads SDK program, Cryptocurrency, Play Age Signals (optional), Android XR, car games | clarification or optional | none | no: no ads SDK, no crypto, no XR, no car surface; Age Signals is optional and not adopted | P2 |
| Japan authorization | account reminder | 2026-12-01 | no: it binds a developer who sells into or out of Japan; the product is free with no purchases | S0027 section 2.1 |
| Trader status declaration | account requirement | from November 2026 | **account level, owner only**: A2 | 3.1 |
| App quality, code optimization (25 percent each of obfuscation, optimization and shrinking, only where DEX exceeds 10 MB, all form factors) | requirement | February 2027 | applies in principle; R8 code and resource shrinking is on in the phone and Wear release builds and the watch face has `hasCode="false"`; the percentages are read in the console after the first upload: A3 | P3, P4 |
| App quality, memory thresholds (90th percentile of field data, phones and tablets only) | requirement | February 2027 | phone only, measured from field data after publication; the Wear package is out of scope by the page's own wording: A3 | A3 |

### 2.2 Location, the one live item

- The phone `store` package declares `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` (`phone/src/main/AndroidManifest.xml`) and no background location. `phone/src` has no `store` or `noLegal` manifest, so the `store` package carries the main manifest as it is.
- Use is foreground only: `GnssStatusDataSource` and `MotionReadingSource` call `LocationManager.requestLocationUpdates` at 1000 ms and 500 ms with a 0 m distance while the dashboard collects, and release it in `awaitClose` (CLAUDE.md section 3, the foreground-only decision).
- Why the two cheaper scopes do not fit: the dashboard shows a live fix (speed, altitude, bearing) and the satellite status of the GNSS receiver. That is a continuous stream, not "one-time or infrequent", so the Android location button is the wrong tool, and the satellite status needs the precise permission. The declaration therefore has a true answer to give.
- **The coordinates leave the device.** `OpenMeteoWeatherProvider` adds the fix's latitude and longitude, at full precision, to the Open-Meteo forecast request (`phone/src/main/java/com/sza/tourist/data/weather/OpenMeteoWeatherProvider.kt` lines 35 and 36). So the declaration text, the Data safety answer and the privacy page must say that precise location is sent to the weather provider; a line saying location "is not sent anywhere" would be false. S0027 section 2.3 P2 expected "nothing leaves the device" only if its audit proved it; this finding is the audit's first answer, and it disproves it for location.
- The Wear `store` package removes both permissions explicitly (`wear/src/store/AndroidManifest.xml` lines 25 and 26); the Wear `noLegal` package declares them and never meets Play (CLAUDE.md section 3, variants).
- When `target-sdk` moves to 37 (`gradle/libs.versions.toml`, re-checked by S0010) the location button rule starts to bind an app's one-time reads. The dashboard has none, so no code change is expected then either; the S0010 re-check records it.

## 3. Decisions and owner actions

### 3.1 Owner actions, none performed here

- **A1 - fine location declaration, before 2027-01-27.** In Play Console, app content: state that `ACCESS_FINE_LOCATION` serves the dashboard's live position and the GNSS satellite status while the screen is open, foreground only, and that neither coarse location nor the location button can show them. Draft sentence for the owner: "Tourist shows the hiker's live position, speed, altitude and the satellites the phone receives while the Tourist screen is open. A continuous precise fix is the feature; approximate location and the one-time location button cannot provide it. Location is read only while the screen is open and never in the background. The position is sent to the weather service to fetch the forecast for the place." S0027 P1 and P2 settle whether anything else leaves the device before the sentence is filed.
- **A2 - trader status, from November 2026.** An account-level declaration in Play Console. Whether it applies depends on the account type, which the owner has not given (S0027 section 3.3, Play developer account). Read the console's own wording at S0027 P4.
- **A3 - app quality, February 2027.** After the first upload, read the code optimization percentages in the app bundle explorer of Play Console, and the memory section of Android vitals once field data exists. Nothing can be read before there is a bundle.

### 3.2 Decisions

- No manifest, code, dependency or build change is made: the audit found nothing that the announcement requires of the tree. Adding or removing a permission "to be safe" would break the rule that a manifest names only what the runtime uses (SECURITY_AND_PRIVACY section 3).
- The owner actions are carried by S0027, which creates the app in the console (P4) and drafts the declarations (P2). They are not performed from here: the app does not exist in the console, and every console act is the owner's.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0027 (carries the owner actions A1 to A3 and the console work), S0010 (re-checks `target-sdk`, which decides when the location button rule binds), S0026 (the privacy page that must agree with the location declaration).
- **Owner decisions needed to close this ticket:** none. The audit is closed on the tree; the account facts and the console acts are the owner's and live in S0027 section 3.3.

## 4. Acceptance

- [x] 1. P1: no source manifest of the phone, Wear (`main`, `store`, `noLegal`) or watch face declares `ACCESS_LOCAL_NETWORK`, `MANAGE_CONTACTS`, `READ_CONTACTS`, `WRITE_CONTACTS`, `READ_CALL_LOG`, `WRITE_CALL_LOG`, `MANAGE_EXTERNAL_STORAGE`, `BIND_ACCESSIBILITY_SERVICE`, `ACCESS_BACKGROUND_LOCATION`, `READ_SMS`, `RECEIVE_SMS` or `SEND_SMS`. Command: a `grep -nE` of those names over the five manifests. expected: no declaration | actual: one hit, `wear/src/store/AndroidManifest.xml:17`, which is `USE_FULL_SCREEN_INTENT` with `tools:node="remove"` - a removal, not a declaration (exit 0 only because of that line).
- [x] 2. P2: no shipped source or build file uses `AccessibilityService`, `AppFunction`, `BillingClient`, `NsdManager`, `ContactsContract`, `CallLog` or geofencing. Command: a `grep -rnE` over `phone/src/main`, `phone/src/store`, `phone/src/noLegal`, the same three of `wear/src`, `watchface/src`, the three module build files and `gradle/libs.versions.toml`. expected: no hit | actual: no hit, exit 1.
- [x] 3. P3: release shrinking is on in both code modules. Command: `grep -nE "isMinifyEnabled|isShrinkResources"` over `phone/build.gradle.kts` and `wear/build.gradle.kts`. expected: four `true` lines | actual: `phone/build.gradle.kts` lines 31 and 32, `wear/build.gradle.kts` lines 29 and 30, all `true`.
- [x] 4. P4: the watch face has no code. Command: `grep -n hasCode watchface/src/main/AndroidManifest.xml`. expected: `false` | actual: line 12, `android:hasCode="false"`.
- [x] 5. The location registrations are foreground-scoped and released. Command: a `grep -rn` of `requestLocationUpdates` over `phone/src/main` and `wear/src/main`, and a read of the release lines of the two phone sources. expected: each registration has a release | actual: `GnssStatusDataSource.kt:118` is released at line 143, `MotionReadingSource.kt:91` at line 104; the Wear call sites (`AndroidWearTouristRepository.kt` lines 339 and 347) sit in a package whose `store` manifest removes the permission.
- [x] 6. Every owner action of 3.1 names its deadline and a carrier, and the carrier's own record holds it. Command: `grep -n "S0036" PLAN/S0027_google-play-publication.md`. expected: one line per owner action A1, A2 and A3 | actual: lines 94, 95 and 96 of that file, in its section 6, one each.
- [x] 7. The merged-manifest rung (`aapt dump permissions` of the built `store` packages) is the release's and is already S0027 acceptance 1; it is not run between releases.

## 5. Open questions

- **Fine location declaration (A1).** Status: Open. Carrier: S0027. The wording of 3.1 A1 is entered in the console at S0027 P4, after P1 and P2 prove what leaves the device; deadline 2027-01-27.
- **Trader status (A2).** Status: Open. Carrier: S0027. Whether it applies is an account fact only the owner has (S0027 section 3.3); deadline "from November 2026" by Google's wording.

## Last Audit

**Date:** 26-11-08 18:01 | **Mode:** static | **Outcome:** Verified
**Counts:** PASS 9 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 1 | EXEMPT 3 | RELEASE 1

### Checks
- PASS - acceptance 1 to 6 of section 4, each run in this pass (grep exits and hit lines are quoted there).
- PASS - every row of 2.1 maps to a predicate of section 4 or to S0027 section 2.1; no announced item is unmapped.
- PASS - probe sweep: `grep -rn S0036` over `phone/src`, `wear/src`, `watchface/src`, `build-logic` and `tools` printed nothing; the ticket never held a probe and sits in no needs-manual-test status.
- PASS - `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs` after the edit of this record and of S0027 section 6: docs-registry, docs-links, docs-style, docs-assets, docs-harness-registry all PASS, `docs-check: PASS (5 checks)`, 1.8 s. expected: PASS | actual: PASS.
- UNCHECKABLE - the verbatim policy wording. The four Google pages were read through a summarizing page reader, so the quoted dates are cross-read between the preview page and the deadlines table, not compared against a character-exact copy. The one inconsistency found (the 30-day window of the announcement against 2027-08-25 of the table) concerns two items that do not apply here.
- EXEMPT - A1, fine location declaration (deadline 2027-01-27): a console act of the owner; the app is not in the console yet. Carried: S0027 section 6, line 94.
- EXEMPT - A2, trader status (from November 2026): an account fact and a console act of the owner. Carried: S0027 section 6, line 95.
- EXEMPT - A3, app quality read-out (February 2027): needs an uploaded bundle and field data. Carried: S0027 section 6, line 96.
- RELEASE - the merged-manifest permission list of the built `store` packages. Release command: `pwsh -NoProfile -File tools/release/preflight.ps1`, then `aapt dump permissions` on the `store` phone and Wear packages as S0027 acceptance 1 states. Not built, not tested until the release.

### Action items
1. Before S0027 P2 files the Data safety answers: state that precise location is sent to the weather provider (`OpenMeteoWeatherProvider.kt` lines 35 and 36) - S0027 section 6, line 94.

### Manual / on-target
None open. The owner's console acts are carried by S0027 and are not checks of this ticket's record.
