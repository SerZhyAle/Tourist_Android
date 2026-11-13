# S0027 - Publish Tourist in Google Play

**Status:** Draft
**Status note:** Written 2026-10-26 from the owner's order (section 0). Nothing is created in Play Console, uploaded or released; every console act is the owner's (A to O) and every upload needs the owner's go for that exact release (invariant 4). Blocked by S0010 (release pipeline, signing key) and S0026 (the hosted privacy page).
**Phase:** outside the handoff phase plan (handoff section 8); third of the four publication tickets S0025 to S0028
**Verification level:** static predicates on the built packages and the listing sources; the console state is the owner's and is recorded as dated evidence; after publication, public predicates (HTTP status of the listing, an install from Play on a device).

**Depends on:** S0010, S0026

## 0. Source

- **Owner's order, 2026-10-26** (Russian source, quoted): "create specifications ... 2 publish in Google Play".
- S0010 section 3.3: decisions 4.5 (distribution), 4.6 (name and branding), the signing key follow-up of 4.2. `CLAUDE.md` section 3: application ids (`com.sza.tourist` for phone and watch, `com.sza.tourist.watchface` for the face), signing (Play App Signing with this product's own key imported, plus a separate upload key; the key does not exist yet), variants (`store` goes to Play; `noLegal` never meets it on one store).
- Canon: RELEASE_AND_DISTRIBUTION sections 2, 3, 5, 6; SECURITY_AND_PRIVACY sections 3 to 6; CHANNEL_MATRIX (Play row); invariants 1, 2, 4, 5, 6, 13, 14, 15; the `release` skill.
- S0012 section 2.4 and section 7: the Play listing row, the closed-test requirement for a personal account created after November 13, 2023 (read at source 2026-10-09; re-read before acting), the Spam and minimal-functionality risk of a listing close to FastMediaSorter's, the health and sensitive-capability risk, and the variant truth line.
- Sibling precedent, read only: StreamsPlayer for Android went straight to Play (its decision B39 of 2026-10-14), generated a hosted privacy page from `docs/PRIVACY_POLICY*.md` and filed its store declarations in a dedicated ticket (its S0033). Reuse the decisions where they fit; never the code.
- **Measured 2026-10-26:** the listing `https://play.google.com/store/apps/details?id=com.sza.tourist` was HTTP 404 on 2026-10-09 (S0012) and is re-read in A1; `.sza-canon.json` `channels` is empty; the signing key folder does not exist yet; `PACKAGE-VERSIONING` families are not registered (S0010).

## 1. Goal

Tourist (store variant: the phone app and the Wear OS app under `com.sza.tourist`) is live on Google Play in English, Russian and Ukrainian, signed by Play App Signing with the product's own key, with a privacy page, a data-safety form, a content rating and a permission list that say the same true thing, and the first update has been proven over a real prior install.

## 2. Scope

### 2.1 Non-goals

- No tag, upload or rollout by this ticket's drafting; those are the one-way steps of 2.3 P5 to P8 and need the owner's go each.
- The `noLegal` variant, the GitHub sideload channel and its trust page (S0010, S0026) - not on Play.
- Positioning research, topics, communities, launch moments (S0012). Promotion follows production, not the upload.
- Paid features, ads, in-app purchases, accounts: none exist; the Data safety and Content rating answers say so because it is true.
- The watch face (`com.sza.tourist.watchface`, S0009) is a separate Play app with its own listing; whether it is in this publication is owner input 3.3.
- No competitor name and no reference to another app in any Play field (S0012 section 2.1: Play forbids it in metadata).

### 2.2 Decisions

- **Order of work:** S0025 -> S0026 -> this ticket -> S0028. The privacy URL (S0026) must answer before the console form is filled.
- **Variant truth:** the listing, the declarations and the screenshots describe the `store` variant only. Capabilities that are `noLegal`-only today (S0012 section 2.1) never appear.
- **Tracks:** internal -> closed (when the account requires it) -> production, staged rollout at production.
- **Frozen anchors** (invariant 1): the application id and the signing key are frozen from the first upload. Play App Signing is configured once, before the first upload, with the imported product key as the app signing key and a separate upload key.
- **Reach baseline** (invariant 2): the first release defines the baseline - minimum platform, ABIs, form factors (phone, Wear OS), countries - and records it so that no later release shrinks it silently. Hardware features that are not essential (camera flash, compass, GNSS, step counter) are declared not required, so the device filter does not exclude a phone that can still run the rest of the kit.
- **Listing sources** live in the repository (`docs/store/play/<locale>/`: title, short description, full description, release notes), generated into nothing and entered into the console by the owner. Wording derives from `docs/POSITIONING.md` when S0012 step P1 has produced it, otherwise from the README paragraph of S0025; S0012 section 2.1 binds both; every text is a present-tense shipped fact. Character caps are read from the console at submit time, never hard-coded.

### 2.3 Steps

- **P1 (A, read-only)** Permission and capability truth audit of the merged manifests of the `store` phone package and the Wear package, read from the built packages: every permission, foreground-service type, hardware feature and exported component maps to a one-sentence user-visible job (SECURITY_AND_PRIVACY section 3, invariant 13). A line that cannot be mapped is removed. Shared with S0026 W3: one audit, two consumers.
- **P2 (A)** Declarations draft from the privacy source of S0026, in `docs/store/play/declarations.md`: the Data safety answers (what is collected, what is only processed on the device, what is shared: expected "nothing leaves the device" only if the audit proves it), foreground-service declarations for any type the store variant uses, a health-apps declaration only if the store variant carries a health feature, the app-content answers (target audience not children, no ads, no login, no financial or government function), and the **fresh IARC questionnaire** answers (invariant 15: never a copied portfolio rating id).
- **P3 (A)** Listing sources for `en`, `ru`, `uk`, the 512 px icon (the product icon, unaltered), the feature graphic, phone screenshots and Wear OS screenshots - real captures of the store variant, each looked at before use (SECURITY_AND_PRIVACY section 4). Dimensions are read from the console.
- **P4 (A to O)** Console setup in the owner's signed-in browser: create the app, set Play App Signing as in 2.2, enable the Wear OS form factor, enter the declarations of P2, the listing of P3 and the privacy URL. The owner's account facts (3.3) decide whether a closed test is required.
- **P5 (A·GO)** Release candidate from S0010's pipeline signed with the upload key: the store variant bundles (`.aab`) for phone and Wear, versions from the registered families (invariant 8), `THIRD-PARTY-NOTICES.txt` inside every package when a binary is bundled (invariant 12). Upload to the internal track.
- **P6 (A)** Update proof (invariant 6): a second build with a higher version code installed from the internal track over the first, on a device with Play; the version and the retained data are read with `dumpsys package` and cited.
- **P7 (A·GO / O)** Closed test (if required: testers recruited by the owner, 14 days continuous, console-confirmed) and the production release at a staged percentage, each on the owner's go quoting the version.
- **P8 (A)** Post-release verification (RELEASE_AND_DISTRIBUTION section 6): listing URL status, each locale renders, the data-safety section shows, an install from Play on a device whose installer is the Play Store. Then the site (S0026) gets the stage 3 get-it link, `.sza-canon.json` `channels` gains `play`, the README gains the install line, and the durable listing URL is handed to S0028.

**Contract boundary:** consumer of `PACKAGE-VERSIONING` (families registered by S0010) and `INSTALL-TRUST` (not applicable to Play); no shared format is changed.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0010 (blocker: pipeline, signing key, versioning families), S0026 (blocker: privacy URL and source), S0012 (positioning inputs; its step P8 listing text is produced here, the console entry is P4), S0009 (the watch face), S0025, S0028 (consumes the listing URL).
- **Play developer account, open, no recommendation (a fact only the owner has).** Which account publishes, its type and creation date decide the testing path. Options: (a) an existing personal account created before 2023-11-13 - no closed-test gate to expect; (b) a personal account created after that date - a closed test with at least 12 testers opted in for 14 days before production access (S0012, re-read before P4), testers supplied by the owner; (c) an organization account - state the rule found in the console at P4. Also: whether the same account already carries the FastMediaSorter listing (the Spam and repetitive-content risk of S0012 section 7).
- **Name and branding (4.6), open, blocks P3.** Store title (a working-name stand-in until decided), icon, whether FastMediaSorter is named (never inside Play metadata, S0012 section 3.3). Same answer as S0026.
- **Watch face in this publication, open.** Options: (a) phone and Wear OS app first, the watch face as a second publication after the first is live - recommended: two reviews are not stacked on the first release and the face's data safety is trivial to add later; (b) all three at once; (c) phone and Wear OS only, the face dropped.
- **Countries and regions, default taken.** All countries the console allows for a free app; narrowing later is forbidden by invariant 2, so a restriction is a first-release decision. Options: (a) default; (b) a list the owner names.
- **Price and monetisation: free, no ads, no purchases** (S0012 section 2.1). Resolved.
- **Go for each outward act** (P5 upload, P7 closed test start, P7 production): asked at that moment, quoting the version and the track.

## 4. Acceptance

- [ ] 1. The audit table of P1 exists in `docs/store/play/`; the permission list printed by `aapt dump permissions` of the built `store` phone and Wear packages equals the table's declared set, and every row has a sentence.
- [ ] 2. The privacy page, the permission list and the Data safety answers agree: a line-by-line comparison file `temp/S0027/triad.md` shows no difference (invariant 14); the privacy URL returns `200`.
- [ ] 3. The IARC certificate is a fresh one dated in the console, recorded with its date; the answers file shows no copied id.
- [ ] 4. Listing sources exist for `en`, `ru`, `uk`; the forbidden-terms test of S0012 step P3 (when it exists) and the shipped-fact grep find nothing; caps are confirmed against the console at submit.
- [ ] 5. `apksigner verify --print-certs` on the upload artifacts shows the upload key; the console's app signing certificate fingerprint equals the product key's; no keystore or credential file is tracked (`git ls-files` finds none).
- [ ] 6. The reach baseline (minimum platform, ABIs, form factors, countries) is written in the release record; non-essential hardware features are `required="false"` in the merged manifest (`aapt dump xmltree` cited).
- [ ] 7. Update proof: version A then version B installed from the internal track on a Play device; `adb shell dumpsys package com.sza.tourist` shows B and the data of A kept.
- [ ] 8. If required, the closed test ran for the 14 days with the number of testers the console reports; the console's production-access state is recorded.
- [ ] 9. Production: `curl -s -o /dev/null -w "%{http_code}" "https://play.google.com/store/apps/details?id=com.sza.tourist"` prints `200`; the listing renders in each locale; `dumpsys package` of an installed copy reports the Play installer.
- [ ] 10. After 9 only: the site shows the Play link (S0026 stage 3), `.sza-canon.json` `channels` includes `play`, the README names the listing; before 9 none of them does.
- [ ] 11. `THIRD-PARTY-NOTICES.txt` is inside every distributed package when a binary is bundled.
- [ ] 12. Every outward act has a go-ledger row with the owner's quoted go, and every console act is marked as the owner's.

## 5. Risks

- **Closed-test delay** (account type): up to two weeks before production; launch moments wait for production.
- **Policy review of the signature features.** Foreground location, the SOS signal and a flashlight lock sit near the sensitive-capability and misleading-claims rules; the copy keeps to S0012 section 2.1 and the declarations to the audit.
- **Health policy:** if decision 4.3 keeps heart rate and steps `noLegal`-only, the store watch build loses them (S0012 section 7); the listing is cut per variant, not the other way round.
- **Spam and minimal functionality:** a listing that resembles FastMediaSorter's; the listing leads with the kit, not the compass.
- **Frozen anchors.** The application id and the signing key cannot change after the first upload; a lost upload or app-signing key is recoverable only through Play's reset path.
- **Name.** "Tourist" is a crowded search word.

## 6. Open questions

- Which Play app category fits (Tools, Travel & Local, Health & Fitness) - picked from the console's definitions at P4, never from a competitor's listing.
- Whether the Wear OS app needs its own screenshots set of every size at the first submission - read from the console.
- Inherited from S0036 (A1), deadline 2027-01-27: the Play Console declaration for `ACCESS_FINE_LOCATION` of the phone `store` package (live fix and GNSS satellite status, foreground only; coarse location and the location button cannot serve a continuous precise fix). The draft sentence is in S0036 section 3.1. The Data safety answer, the privacy page and that sentence must say that the fix's latitude and longitude go to the weather provider (`OpenMeteoWeatherProvider.kt`); P2's "nothing leaves the device" expectation is already disproved for location.
- Inherited from S0036 (A2), from November 2026: the trader status declaration of the developer account; read the console's wording at P4, since it depends on the account type of 3.3.
- Inherited from S0036 (A3), February 2027: after the first upload, read the code optimization percentages in the app bundle explorer, and the memory section of Android vitals once field data exists.
