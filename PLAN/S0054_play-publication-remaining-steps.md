# S0054 - play-publication-remaining-steps

**Ticket:** S0054
**Status:** Approved
**Complexity:** medium - no compiled source; repo-side listing and declaration texts, then ordered owner console steps and one-way release steps (confirmed at the 2026-11-14 pass)
**Date:** 2026-11-13

## Captured request and migration scope

Owner's rule (deviceinfo-mob session, 2026-11-13): "\"Что осталось до публикации\" это должен быть тикет в репозитори проекта, а не мне сюда"
Owner's order for this product (2026-11-13, relayed by the deviceinfo-mob session): "Выполни всё это теперь для Tourist_mob"

What remains before Tourist is published on Google Play, as of 2026-11-13. Already done the same day on the owner's order (owner decisions of the same day: the recommendations table - repository Tourist_Android, PLAN public, history rewritten to drop a private address, privacy pages in site/, store name "Tourist: Hiking Kit", Free, Sign in details "No", contact sza@ukr.net, graphics by script, categories Travel & Local and Personalization, IARC by the facts; both Play apps created; target audience main 18+, watch face 13+; Data safety strictly by the facts):
- Public remote https://github.com/SerZhyAle/Tourist_Android (MIT, issues on, wiki and projects off; commit 5db4d6e holds the product tree over the five rewritten foundation commits) and its Pages site (workflow .github/workflows/pages.yml, run 43456039842 green): privacy.html, privacy-ru.html, privacy-uk.html, sitemap.xml, robots.txt and the assets answer 200; the site root answers 404 (no landing yet, S0026), so the repository has no homepage and the listings no website.
- Play Console app com.sza.tourist ("Tourist: Hiking Kit", personal account 8446656778368889827, app 4975092571968022444, App, Free): every App content item saved, overview "You're all caught up" (privacy URL, Sign in details No, no ads, no advertising ID, not a government app, no financial features, health Activity and fitness, target audience 18 and over, Data safety precise location and in-app search history collected ephemerally and not shared, fresh IARC rating ESRB Everyone / PEGI 3 / IARC 3+); store settings Travel & Local and sza@ukr.net; default listing en-US with ru-RU and uk translations, icon and feature graphic; Wear OS form factor added. Record: docs/play/PLAY_DATA_SAFETY.md "Console record" and docs/play/PLAY_LISTING.md "Console fields".
- Play Console app com.sza.tourist.watchface ("Tourist Watch Face", app 4973503680609248842, App, Free): every App content item saved (no data collected, IARC Everyone, target audience 13 and over), store settings Personalization and sza@ukr.net, Wear OS form factor added; listing empty.

Remaining, in order:
1. Commit and push the records written on 2026-11-13 after the first push (CLAUDE.md "Public presence", docs/play/PLAY_DATA_SAFETY.md, docs/play/PLAY_LISTING.md, this ticket and the tickets filed the same day) - on the owner's word.
2. Done 2026-11-13: phone (6, 1080x1920) and Wear OS (4, 454x454) screenshots of the store variant for en-US, ru-RU and uk, captured on emulators with a London geo fix and no account (`docs/play/screenshots/`), uploaded to the main app's listing; the two watch face captures wait for its listing (item 3). Seen in the captures and not fixed: low-contrast status bar icons on the start screen in the light theme, the "SOS" button wrapping in ru and uk, the watch stopwatch "Lap" button pinned left.
3. Watch face listing: write its source in docs/play (title, short and full description in en, ru, uk; icon and feature graphic as render targets of tools/store/render-store-assets.ps1), then fill the watch face's default listing and its Wear OS screenshots. **Source written 2026-11-14** (`docs/play/PLAY_LISTING_WATCHFACE.md`, `-ru`, `-uk`, registered as `play-listing-watchface`; limits measured: title 18 of 30 in all three, short description 78, 73 and 77 of 80, full description 1341, 1313 and 1311 of 4000; graphics: the two existing render targets, see Assumed below; screenshots: `docs/play/screenshots/watchface/01_watch_face.png` and `02_watch_face_ambient.png`, 454 x 454 each). **Open: entering it in the console (DO 1 of 3.3).**
4. Landing (S0026): once https://serzhyale.github.io/Tourist_Android/ answers 200, set it as the website in Store settings of both apps and as the repository homepage (gh repo edit --homepage).
5. Done 2026-11-13: GitHub social preview uploaded from docs/github/social-preview-1280x640.png.
6. The ACCESS_FINE_LOCATION permission declaration of the phone store package (S0036 A1, deadline 2027-01-27): its text must say that the fix's latitude and longitude go to Open-Meteo for the weather, as the privacy pages and the Data safety answers now do. **Text written 2026-11-14** in `docs/play/PLAY_DATA_SAFETY.md`, section "Fine location declaration" (514 characters, names Open-Meteo and the precise latitude and longitude; agrees with the Data safety row and the privacy pages). **Open: entering it in the console (DO 2 of 3.3).**
7. Release side (S0010 and S0027, owner's order only): the owner's release keys (S0010 Q1) in P:\ANDROID\Tourist_credentials, the PACKAGE-VERSIONING fold (S0010 Q2), store bundles of phone, wear and watchface, pre-flight verdict, first upload to internal testing with the app signing key import (one-way), the update proof, closed testing if the account requires it, countries (all, never narrowed) and rollout.
8. Wear OS opt-in and the Wear OS review policy for both apps (console Wear OS step 3; allowed only after the Wear screenshots and a Wear bundle on a testing track; one-way, owner's yes).
9. Production and review (one-way, owner's order only).
10. After the landing and the Play listing answer 200: hub:S0011 (SITE-FAMILY-MAP row and hub card) and the footer-grid tickets filed in each map consumer become workable.
11. Account-level, not this app: "Declare your trader status" (due February 2027 for this account; the decision is the owner's).

## Pass record (unattended, 2026-11-14)

- **Rung:** complex (an umbrella of owner and one-way steps, three new documents). No source file and no compiled file edited.
- **Done in the repository:** item 3, the source of the watch face listing (three files, one registry row `play-listing-watchface` in `docs/DOCUMENT_REGISTRY.jsonl`); item 6, the declaration text in `docs/play/PLAY_DATA_SAFETY.md`; the closing sentence of `docs/play/PLAY_LISTING.md` now names the new source instead of "no source yet".
- **Facts the watch face text rests on** (read, not assumed): `watchface/src/main/res/raw/watchface.xml` (digital clock, date, four wave lanes, three particle layers moved by the accelerometer angles, the hourly drift direction, the ambient outline, the watch battery bar with its 25 and 10 percent thresholds and the charging colour, six customizable slots - two top, four on the bottom arc -, the tap `Launch` of `com.sza.tourist/com.sza.tourist.wear.MainActivity`), `watchface/src/main/AndroidManifest.xml` (`android:hasCode="false"`, no permission), `gradle/libs.versions.toml` (`min-sdk-watchface = "36"`, Wear OS 6), `wear/src/main/java/com/sza/tourist/wear/MainActivity.kt` (the launch target exists).
- **Over-claim line kept (S0012 section 2.1):** the face text says nothing about a phone battery bar, a clock style or backdrop photo set from the phone, or what a slot shows. The kit's watch app has no complication or style service (no `ComplicationDataSourceService`, `WearFaceSlot` or `WearClockStyle` anywhere in `wear/src`; `docs/kit-parity.jsonl` rows `wear.complication_data_sources` S2047 and `wear-os.phone-battery-report-heartbeat` S3990 are `absent-by-design`), so the slots start empty and the bottom bar is a bare track; the face's own XML still reads those feeds and is a port. Nothing parked: the parity map already decides it.
- **Evidence** (all run in this pass):
  - `tools/fast-check.ps1 -Area drift`, at the start, before the first edit: exit 0, `WITHIN BUDGET: 1 uncarried item(s) (1 of 379 row(s)), the oldest 0 day(s) of 7` (SettingsDropdownRow.kt, changed 2026-11-14), 0 NEW SOURCE. This ticket touches no ported file and no seam file, so there is no end-of-ticket drift run owed. PASS.
  - `tools/fast-check.ps1 -Area parity`: exit 0, `OK: 339 row(s) for 339 in-scope record(s) of 1448 active: 58 present, 269 absent-by-design, 12 gap (open); 0 new, 0 invalid, 0 stale`. PASS.
  - Field limits measured by a script over the three files: expected title <= 30, short <= 80, full <= 4000 | actual 18 / 78 / 1341 (en), 18 / 73 / 1313 (ru), 18 / 77 / 1311 (uk); no `...` and no long dash in any file. Screenshots expected 454 x 454 | actual 454 x 454 and 454 x 454. Declaration length 514.
  - `tools/fast-check.ps1 -Area docs` after the edits: exit 0, `docs-check: PASS (5 checks)` (registry 28 records, 300 files declared; links, style, assets, harness registry), 1.8 s. PASS.
  - Compile gate: not applicable - no compiled source edited (and `.sza-profile.json` declares no `commands.compileCheck`). Not built, not tested: nothing in this pass is built.
- **Not attempted, and why:** item 1 (commit and push: outward, the owner's word, Q below); item 4 (waits for the landing, S0026, itself `BlockQuestions`); items 7 to 9 (release, upload, Wear OS opt-in, production: one-way, the owner's order only, and S0010 owner inputs Q1 and Q2 stand open); item 10 (waits for the landing and the Play listing answering 200); item 11 (account level, the owner's decision). Items 2 and 5 were done on 2026-11-13.
- **Doc sync:** registry record stated and added (`play-listing-watchface`); no help page, string table, README sentence or site page changes (the listing text is a console source, not a site surface; the README and `docs/play/PLAY_LISTING.md` claims about the face stay "a separate package for the watch" and agree with the new text); no locale left behind (en, ru, uk written together).

### Assumed (unattended, 2026-11-14)

- **Graphics of the watch face listing are the main app's two render targets** (`docs/play/icon-512.png`, `docs/play/feature-graphic-1024x500.png`). Alternative: a face-specific pair, an extension of `tools/store/render-store-assets.ps1` that draws the face. Reason: the pictures carry the product name only and the face's launcher glyph is the same compass star; the script has no recorded time, so it was not run (section 7 of CLAUDE.md). Reversible in the console. Owner may overrule.
- **The face text describes the shipped package only** (no phone battery bar, no clock style, no slot content). Alternative: describe the FastMediaSorter face in full. Reason: the kit's watch app lacks those services by the parity map, so the claims would be false on a store build. A later port of the services would change the text in the same edit (`feature-to-site`). Owner may overrule, with a port decision first (CLAUDE.md section 2 rule 4).
- **Russian and Ukrainian titles "Tourist: циферблат"**, the Latin product name kept and the word for the object after it, as the main listing does ("Tourist: набор для похода"). Alternative: "Циферблат Tourist". Reason: one pattern across both apps; 18 of 30 characters. Owner may overrule.
- **"Wear OS 6 and newer" in the first sentence** of the full description, from `min-sdk-watchface = "36"`. Alternative: leave the platform out and let Play filter devices. Reason: a visitor on an older watch learns it before the install button. Owner may overrule.
- **The location declaration sentence is 514 characters**, written as one block. Alternative: a shorter text if the console form's own limit is lower; the limit is read when the form opens (DO 2). Owner may overrule.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0010 (release keys, PACKAGE-VERSIONING fold, first upload), S0012 (over-claim lines for the listing text), S0026 (landing, item 4), S0027 (release), S0036 (ACCESS_FINE_LOCATION declaration, A1); blocks hub:S0011 and fms-lite:L0312.
- **DO:** In the Play Console of `com.sza.tourist.watchface` (app 4973503680609248842), enter the default listing en-US and the translations ru-RU and uk from `docs/play/PLAY_LISTING_WATCHFACE.md`, `-ru` and `-uk`; upload `docs/play/icon-512.png`, `docs/play/feature-graphic-1024x500.png` and the two Wear OS screenshots `docs/play/screenshots/watchface/01_watch_face.png` and `02_watch_face_ambient.png`; save, send nothing for review. Report the result so the console record is written in `docs/play/PLAY_LISTING_WATCHFACE.md`.
  - Owner answer (2026-11-14): deferred to the release ("Перенести на релиз"). The step stays an owner console action that waits for the Wear OS opt-in (item 8), which comes after a bundle is on a testing track; it no longer blocks this ticket. The console record is written when the result is reported.
- **DO:** In the Play Console of `com.sza.tourist`, enter the fine location declaration with the text of `docs/play/PLAY_DATA_SAFETY.md`, section "Fine location declaration" (deadline 2027-01-27), reading the form's character limit first (the text is 514 characters). Report the result so the console record is written in `docs/play/PLAY_DATA_SAFETY.md`.
  - Owner answer (2026-11-14), verbatim: "выполнить всё необходимое в рамках этого тикета". Read as: this step is delegated to the pass that works this ticket - the agent enters the declaration in the Play Console of `com.sza.tourist` (read the form's character limit first, shorten the text only if the form requires it) and writes the console record in `docs/play/PLAY_DATA_SAFETY.md`. The deadline 2027-01-27 stands. Enter and save only: nothing is sent for review or published (items 7 to 9 stay the owner's order only).

### Questions (v1, 2026-11-14) - answered 2026-11-14

- Q: Commit and push the Play records of 2026-11-13 and 2026-11-14 to the public remote now?
  - Commit and push the Play and ticket records only (CLAUDE.md "Public presence", `docs/play/`, the registry row, this ticket) on main (recommended) - the public repository carries the records of the console state and the new listing source
  - Hold until the release - nothing is published earlier; the public remote keeps the older "Public presence" text and a console state the repository does not show
- DO: Enter the watch face listing, its graphics and its two Wear OS screenshots in the Play Console of `com.sza.tourist.watchface` from `docs/play/PLAY_LISTING_WATCHFACE.md` and its translations, and report the result.
- DO: Enter the fine location declaration of `docs/play/PLAY_DATA_SAFETY.md` in the Play Console of `com.sza.tourist` before 2027-01-27, and report the result.

### Quiz decisions (2026-11-14)

- Commit and push the Play records of 2026-11-13 and 2026-11-14 to the public remote now? -> "Запушить только записи": commit and push to `main` the Play and ticket records only (CLAUDE.md "Public presence", `docs/play/`, the registry row, this ticket) (the owner's click on the recommended option; the quiz itself committed and pushed nothing, the pass that works this ticket does it, item 1).
- DO 1, the watch face listing, graphics and Wear OS screenshots in the Play Console of `com.sza.tourist.watchface` -> deferred to the release (the step is needed only by the Wear OS opt-in, item 8; the ticket is not blocked by it).
- DO 2, the fine location declaration in the Play Console of `com.sza.tourist` -> "выполнить всё необходимое в рамках этого тикета" (the owner's own text, verbatim): delegated to the pass that works this ticket; enter and save only, deadline 2027-01-27 stands (see section 3.3 for the reading).

## Blocked tickets

Tickets waiting on this one (TASK-FLOW rule 69). The status tools write a line when a ticket enters
`BlockByOtherTask` and close it when that ticket is released; a moved status of this ticket triggers the check.

- Blocks: hub:S0011 | since: 2026-11-13
- Blocks: fms-lite:L0312 | since: 2026-11-13
- Blocks: S0028 | was: Draft | since: 2026-11-14
- Blocks: S0050 | was: In Progress | since: 2026-11-14
