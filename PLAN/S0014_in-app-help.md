# S0014 - In-app help: F1 and a `?` button on every window, and a help page or layer for each

**Status:** BlockNeedUserTest
**Status note:** Implemented 2026-10-25 (phone app): F1 and the ? button through one resolver, 21-surface help map, bundled pages in EN RU UK, gate PASS WITH ADVISORIES, 550 tests green, walk of 66 rows passed. Waiting for the owner: review of the page wording, a walk by a person on a physical phone, and criterion 9 repeated on a device (water flashlight and stopwatch with help present). Carried elsewhere: support link (S0010 4.6 and 4.9), forbidden-terms test (S0012).
**Phase:** outside the handoff phase plan (handoff section 8) - a cross-cutting user-experience feature; its slices follow the ports of S0004 to S0007
**Verification level:** device (emulator, API 33) - the button and F1 on every surface of the phone app in every shipped locale and both themes, results in the walk file (section 4, criterion 12); not yet by a person on a physical phone.

## 0. Source

- **Origin.** Raised by the portfolio owner on 2026-10-16 through the canon session, for every product with a user
  interface. In the owner's words (paraphrased): F1 means Help. Many of the product's own windows and dialogs get a
  `?` button in the top-right corner, added beside whatever is already there and never in place of it. Pressing it
  either explains every significant element of that window right away, or opens an HTML page with a screenshot and a
  detailed description in the user's language - what each part is for and in what order to use it - cross-linked to
  the help of the other windows, to the site's documentation and portals, and to sibling products where that is
  appropriate. The pages are simple but attractive. **Each project decides what and where is more appropriate.**
- **Contract.** `CONTEXT-HELP` 0.1 (domain `in-app-help/`, status draft, owner shared; this product is a named
  consumer). Rules by number: the key (1), the button (2), same answer for both (3), which surfaces (4), the two
  forms layer and page (5 to 7), the page anatomy (8 to 11), offline and private (12), language (13), generated
  screenshots (14), permanent address (15), voice (16), one resolver and the help map (17), ship together (18), the
  gate (19); conformance rungs in its section 4. Reference artifacts shipped with the contract:
  `reference/help-page-template.html`, `reference/help.css`, `reference/help-map.example.json`.
- **Canon.** `UI_UX.md` section 7; `DOCUMENTATION_CONCEPT.md` sections 4 and 5 (the help pages are a ship-together
  surface); the `feature-to-site` skill (window help step); the `contract-sync` skill (pointer file, registry row).
- **Other contracts the one above uses:** `ICON-SET` `app.help` (the glyph; a proposal widens its meaning and names),
  `ICON-RENDER` rule 5 (hit target), `APP-BEHAVIOUR` rules 4, 8, 9, 11, `PAGE-STYLE` and its `sza-kit.css` (page
  look), `DOC-INTERNAL-QUALITY` and `DOC-EXTERNAL-QUALITY` (link integrity), `CHECK-VERDICT` and `CHECK-BASELINE`
  (the gate), `LOCALIZATION` (locale parity), `SITE-STRUCTURE` (the address promise). `WINDOWS-UI` section 2 is a
  Windows matter and does not apply here.
- **This repository.** `CLAUDE.md` sections 1 to 3 (product, standing law, anchors), `docs/BOOTSTRAP_HANDOFF.md`
  sections 2, 3, 5 (read-only record), `docs/contracts/README.md`, `PLAN/S0004` to `S0007`, `S0010`, `S0012`,
  `S0013`. The extraction source `P:\ANDROID\FastMediaSorter_mob_v2` was read for the recon of section 5 only
  (read-only; FastMediaSorter Android is cited with its repository name).

## 1. Goal and why

**Goal.** A person using Tourist on a phone who does not know what a screen's elements are, or in what order to use
them, presses F1 (hardware keyboard) or the `?` action in the top app bar and gets the help of that screen - a read-only
layer on the live screen, or a bundled page with a screenshot, in the language of the app - without leaving the app
to find out, without a network, and without any state of the screen changing.

**Why.** The owner's request above, applied to a product whose screens are used under stress: a wet hand on the water
flashlight, a group member in trouble on SOS, a stopwatch started by several people. Those are the screens where
"what does this do and what happens if I touch it" must be answerable in advance. The contract makes the answer the
same shape in every SZA product, so a user who asked one product for help asks Tourist the same way.

**Baseline - read 2026-10-16** (nothing estimated):

| Item | Value | How it was read |
| --- | --- | --- |
| Product code in this repository | none; Gradle skeleton is S0002 | `README.md` Status; `PLAN/S0002` Draft |
| F1 handler, help action, help assets, About screen | none | no code exists |
| `docs/contracts/CONTEXT-HELP.md` pointer, registry row | none | `docs/contracts/README.md` lists seven pointers, none for this contract |
| Site, portal, hub card | none | `.sza-canon.json` `site.kind` "none"; `PLAN/S0012` section 1 |
| Shipped locales | none yet; EN, RU and UK from the first release, others optional | handoff section 3 |

## 2. Scope

### 2.1 Non-goals

- **No wording written here and no page built here.** Page text is drafted in the product's voice in the slices and
  reviewed by the owner (`CONTEXT-HELP` rule 16).
- **The watch is out of scope.** The Wear OS app (S0008) and the watch face (S0009) get no `?` button, no layer and no
  page from this ticket: no help is owed on a surface without a pointer or keyboard, and the canon session's platform
  note for this repository (2026-10-16) puts the watch outside. Phone help may still explain, in words, that SOS
  started on one device reaches the other (D6 asks the owner to confirm this boundary).
- No first-run tour or onboarding (`APP-BEHAVIOUR` rule 11 owns the first run; a tour may link into the pages), no
  single-control tooltips, no site structure (`SITE-STRUCTURE`, S0010 decision 4.9).
- No tracking, analytics or network request in the app or in a page (`CONTEXT-HELP` rule 12; `PLAN/S0012` section 2.1
  states the same line for the whole product).
- No change to a ported behaviour (section 2.2). The help feature adds a way to ask; it does not reinterpret what a
  screen does.

### 2.2 Standing law and hard constraints for this product

- **The handoff lists "new tourist capabilities that exist nowhere today" as a non-goal** (handoff section 2; this is
  an extraction). In-app help is new code and new content. The owner's request of 2026-10-16 is the explicit
  exception, recorded here, in the same way watch-face owner decision 4.4 records its exception. It adds a way to ask
  and an answer; it adds no tourist capability.
- **Ported files and the drift check** (`CLAUDE.md` section 2 items 1 and 2). The help resolver, the help map, the
  viewer, the page set and the gate are new files with no FastMediaSorter source. Where the `?` action or the F1 hook
  has to touch a ported screen, the touch is the smallest possible, it is made after that screen's port commit and
  recorded as a decision next to its ledger row, so the drift check keeps listing real upstream changes and not this
  edit.
- **The water flashlight's signature rule** (`CLAUDE.md` section 3: no wet touch can switch it off; ported exactly). A
  help control on that screen must not become a way out of the lock or a way to change the lit state. Opening a page
  in another window would take the screen away from the light, so for this screen the form is decided in P1 against
  the rule - layer, or an action that is inert while the lock is on - with the reason written in the map.
- **SOS and every other running state.** Opening help while a siren, a flashlight or a stopwatch runs must stop and
  change nothing; Back returns to the surface (`CONTEXT-HELP` rule 7).
- **No network permission is added.** The permission list in handoff section 5 has none. Pages are bundled assets that
  need none; a link that leaves the app goes out as a system-browser intent on the user's tap (`CONTEXT-HELP` rule 11
  and `APP-BEHAVIOUR` rule 4).
- **Over-claims.** Help text obeys `PLAN/S0012` section 2.1: SOS sounds, blinks and reaches the paired device, and
  sends no SMS and makes no call; the water flashlight is a touch lock, not waterproofing; speed, altitude, heading
  and coordinates are readings, not a certified instrument. The forbidden-terms test of S0012 (step P3) covers page
  sources the build owns, so it covers these pages.
- **Location truth.** Location is foreground-only; a page must not say otherwise. A screenshot of the dashboard shows
  a position, so it is demo content only (criterion 6).

### 2.3 Steps

Work in slices; a slice is done when its surfaces are in the help map and the gate counts them. Start with the
screens a user meets first (the start screen and the dashboard), then the rest. Steps P0 to P2 do not need the ports
to be finished; P3 onward works slice by slice as S0004 to S0007 land.

- **P0 Align.** Read `CONTEXT-HELP`. Run the `contract-sync` skill for this one contract: the pointer file
  `docs/contracts/CONTEXT-HELP.md`, and this product's own adoption row in the registry (today's date, honest
  `partial` while it is partial). Pointers the contract makes live and the index lacks today: `ICONOGRAPHY.md`
  moves from "conditional" to active (the `app.help` glyph); `APP-BEHAVIOUR`, `LOCALIZATION`, `CHECK-VERDICT` and
  `CHECK-BASELINE` have no pointer file here, add the ones this ticket really uses. Do not widen into a general
  sync (S0011 is that round).
- **P1 Inventory and decisions.** Enumerate every surface (screen, dialog, bottom sheet, settings page if one exists,
  widget configuration, permission rationale) from the source of truth, never from memory: the manifest's activities,
  every `DialogFragment` and bottom-sheet class, the layout files, the widget providers - recomputed from the
  Tourist tree after the ports. For each choose `layer`, `page`, `both`, `inherit` (reason) or `none` (reason) by what
  is more appropriate for this product; trivial one-question confirmations and system dialogs are `none`. Also decide
  here, and write down with reasons: the viewer (D3), how the screens that are one control (front flashlight closes on
  any touch) and the lock-bearing screens ask for help (section 2.2), whether the F1 hook of the ported base activity
  is kept as the one F1 path (section 5.3), the order of the slices, and one page per settings page or one section
  each if a settings screen exists. Write the decisions into the help map (rule 17; shape in
  `reference/help-map.example.json`), named in `CLAUDE.md` once it exists.
- **P2 The ask.** One resolver (surface id, optional element, target). F1 is handled at activity level through key
  dispatch, honoured with a hardware keyboard attached, and never registered system-wide. The `?` button is an action
  of the top app bar, before the overflow: the `app.help` glyph, the vocabulary name as accessible name and tooltip
  ("Help (F1)" in the UI language), added beside what is there, existing controls keep their place - proved by a
  before and after diff of the control set. Hit target per `ICON-RENDER` rule 5. Where a screen has no app bar, P1
  decides how the corner is reached without displacing a control. TV remote reaches it through the product's menu
  (`CONTEXT-HELP` rule 1), gamepad and keyboard per the product's INPUT-PARITY declaration.
- **P3 The answer.** For `layer` surfaces: the read-only numbered layer of rule 6, drawn on the live screen, activating
  nothing, dismissed by Back, the action again or a tap outside, working at the narrowest width, at font scale 2.0 and
  in a right-to-left locale if one ships. For `page` surfaces: pages built from the reference template (anatomy of
  rule 8, entries of rule 9), one per surface, in every shipped locale (rule 13), bundled in the package as app assets
  and opening with no network (rule 12), published from the same files on the product's site only if S0010 decision 4.9
  opens one. Screenshots generated from the build per locale and, where the app has both, per theme, with demo content,
  marker bounds from the UI's own element tree (rule 14). Cross-links (rule 11): the help of neighbouring screens, the
  site guides and portal when 4.9 gives them, the hub's family map, and sibling products that share the function -
  chosen from the real ones (section 5.5); none is invented.
- **P4 The gate.** Surfaces against the map, pages against the locale set, links resolved, reasons present, a
  baseline that only falls (rule 19); screenshot freshness (rung 2); the walk matrix - every surface by locale by
  theme, F1 and the button (rung 3) - before the release that first ships the feature. The gate runs in the
  repository's normal check and prints a `CHECK-VERDICT` line; where the check lives is a `CHECK-PLACEMENT` record
  made when the first check exists (S0002 and S0013 are the places that may create it).
- **P5 Docs-sync.** Run `feature-to-site` for the surfaces that exist: the how-to and the README link to the help
  index; the About page links to it if the product has one; the ledger carries the user-visible entry once the
  repository has a ledger (`ledgerShape` is "none" today); the help set is a row of the surfaces manifest.
- **P6 Close.** The registry row verified and dated; the product's own record updated; findings about the contract
  go to the catalog as a proposal, never as an edit.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002 (module layout, locale set, asset directory, the first normal check); S0004 (start
  screen, app bar, the base-activity seam and its F1 hook, the communication policy and termbase, the `app.help`
  glyph question); S0005, S0006, S0007 (the screens and dialogs that get help); S0010 (decisions 4.6 name and 4.9
  site; what the first release contains); S0011 (contract-sync round, Draft; P0 here is one contract only); S0012
  (forbidden-terms test and over-claim lines apply to the pages); S0013 (its M7 measures the labels and targets this
  feature adds). S0008 and S0009 are out of scope (section 2.1).
- **D1 Sequencing (decided by the facts, 2026-10-25):** S0004 to S0007 had landed before this ticket ran, so option (a) was gone and the work is the sweep (b): the framework and the pages were added over the finished screens, each touch on a ported file kept to one call and recorded as a decision next to its ledger row.
- **D2 Does help ship in the first release (owner, 2026-10-25): yes.** The walk matrix (criterion 12) and the page set are on S0010's critical path; the walk file is `docs/help/walk-<date>.md`, produced by `tools/help/walk-help.ps1`.
- **D3 Viewer (owner, 2026-10-25): in-app.** A full-screen dialog fragment over the live screen holding a WebView: script off, network loads blocked, anything outside `assets/help` answered empty, external links to the system browser on a tap. The screen below keeps its lifecycle, so a running siren, torch or stopwatch is never stopped by asking, and Back returns to it.
- **D4 Locale set of the pages (decided by the tree):** EN, RU and UK - the three value sets the UI ships (`values`, `values-ru`, `values-uk`). The generator and the gate take the set from the resource folders, so a locale added later fails the gate until its pages exist.
- **D5 Screenshot tooling (project, 2026-10-25): the host-side stand over adb**, as section 5.4 proposed: `tools/help/capture-help.ps1` on a debuggable emulator (root, so the screens that are not exported can be started), the inflated view tree of `dumpsys activity top` for marker bounds (it needs no idle screen, so a ticking stopwatch and a strobing SOS screen work) and the accessibility dump for dialog windows, `screencap` scaled to JPEG, the System UI demo mode for a pinned status bar, a clean `pm clear` state with seeded preferences per capture. No cooperation from the app, and no screenshot is edited by hand.
- **D6 Watch boundary (assumed, to confirm):** S0008 and S0009 stay without help, as section 2.1 and the canon session's platform note say. Not asked in the 2026-10-25 round; nothing here adds help to the watch.
- **D7 Cross-links to FastMediaSorter (still blocked by S0010 decision 4.6):** no page links to it or names it.
- **D8 Publication (still blocked by S0010 decision 4.9):** the pages ship in the package; the footer's support link stays out until an address exists (`docs/help/help.config.json`, `support.url` is null).
- **D10 Lock-bearing screens (owner, 2026-10-25): no element on the water and the front flashlight.** Neither shows a help button and F1 opens nothing there (`ask: off` in the map, with the reason); their help is the Lights page (`e-water`, `e-front`), read before the light is opened.
- **D9 Name (blocked by S0010 decision 4.6):** every page takes the product name from the `app_name` string, one place.

## 4. Acceptance

Static predicates, per slice. A criterion voided by the shape is kept and marked.

- [x] 1. Every surface in the slice has a map row; no map row without a surface; every `none` and `inherit` row has a
      reason. The surface list is enumerated from the manifest, the dialog and sheet classes and the layouts.
      *Evidence:* the gate enumerates the manifest's activities and widget receivers, the `DialogFragment` classes and the dialog call sites and diffs them against `help-map.json`; 21 rows, every `none` and `inherit` row has a reason (`check-help.ps1`).
- [x] 2. F1 on each help-bearing surface (hardware keyboard) and the `?` action open the same target; the `?` action
      is the only control added to its corner (diff of the control set); F1 is not a global hotkey; both go through the
      one resolver and no screen carries help code of its own.
      *Evidence:* walk file, 66 rows: the button and F1 open the same page (same help id, anchor and theme); one resolver (`HelpResolver`), one F1 installer (`HelpInstaller`), no screen carries help code; F1 is read only at the product's own windows. The control-set diff is by construction (one `HelpButton` added in two layouts, one menu action appended by `StandardToolbar`, one dialog title), not a recorded before-and-after dump.
- [x] 3. Each page has the eight parts of rule 8 in order, an `alt` text, `lang` and `dir`, no request to any origin
      (no `http` outside an `<a href>`), no script needed to read it; every link resolves.
      *Evidence:* gate: anatomy, `lang`/`dir`, referrer meta, alt text, no http outside an `<a href>`, no script file, 550 links and fragments resolved.
- [x] 4. Each page opens at the help id of its surface, or at an element anchor; opening help leaves the surface alive
      and Back returns to it.
      *Evidence:* walk file: each row checks the viewer's log line (page and anchor), that the screen below is still the one in front and that Back closes the help; on the SOS screen the signal kept running while the help was open and after.
- [x] 5. Pages and screenshots exist in every shipped locale; the screenshot's language equals the page's; the page is
      chosen by the app's language and falls back to the source language, never to a missing page.
      *Evidence:* gate: pages, screenshots and capture records in en, ru and uk; the capture sets the per-app language after every `pm clear`, so a screenshot is in the page's language (checked on the images); `HelpResolver.chooseLocale` falls back to the source language (unit tests).
- [x] 6. Screenshots come from the generator; none is hand-edited; demo content only - no real position, path, name
      or account on any image or page.
      *Evidence:* every image is written by `capture-help.ps1` from the build on an emulator; the dashboard fix is the synthetic null point of the coordinate grid; each capture starts from cleared app data with seeded demo preferences.
- [ ] 7. The `?` hit target meets `ICON-RENDER` rule 5, the accessible name is the vocabulary name from the termbase
      in each locale, and the control is reachable by keyboard, gamepad and the TV-remote menu route.
      *Evidence:* Partly: the button is 48 dp wide (toolbar action and corner button), named "Help (F1)" in every locale from the strings and reachable by D-pad and keyboard focus. The "TV-remote menu route" has no counterpart: the product has no menu, so only focus reaches the button.
- [x] 8. The gate runs in the repository's normal check; its verdict line follows `CHECK-VERDICT`; accepted debt lives
      in a baseline that only falls.
      *Evidence:* the gate prints one `CHECK-VERDICT` line and exits 0, 1 or 2; it runs inside the repository's normal check, `./gradlew :phone:test` (`HelpGateTest`, without the freshness check so another ticket's layout edit cannot turn the build red; the release pre-flight runs it with the check); accepted debt in `docs/help/baseline.json`, which fails when an entry is stale. No `CHECK-PLACEMENT` record: no runner registry exists yet.
- [ ] 9. Signature rules intact on a device with help present: the water flashlight survives wet touches and leaves
      only by its exit rule (S0005 acceptance repeated); a running SOS keeps sounding and blinking while help is open;
      a running stopwatch keeps running.
      *Evidence:* Partly: SOS verified on the emulator (the signal kept running while help was open, walk file). The water flashlight is untouched by construction (no control, F1 opens nothing: checked in the walk) and the stopwatch keeps counting because the help is a dialog over a resumed activity; neither was re-run through the S0005/S0007 acceptance on a device.
- [x] 10. No network permission, no analytics, tracker or redirector added (source search finds none new); the
      permission list of handoff section 5 is unchanged by this ticket.
      *Evidence:* no manifest change by this ticket; the viewer sets `javaScriptEnabled = false` and `blockNetworkLoads = true`, answers every URL outside `assets/help` empty, and the gate fails if help code opens a connection or exposes a script bridge.
- [ ] 11. Page and layer text passes the forbidden-terms test of S0012 and states nothing beyond the over-claim lines
      of section 2.2.
      *Evidence:* Partly: the S0012 forbidden-terms test does not exist yet, so the gate carries a short list of over-claim words as advisories for the owner's wording review (3 advisories, the water-flashlight line "not waterproofing" among them) and fails on promises of unbuilt features. The prose was written against the section 2.2 lines and its claims checked against the code.
- [x] 12. Before the release that first ships the feature: the walk matrix (every surface by shipped locale by theme,
      F1 and the button, one row each) exists as a dated file. Void while D2 keeps help out of the first release.
      *Evidence:* `docs/help/walk-2026-10-25.md`, written by `tools/help/walk-help.ps1`: 66 rows (10 surfaces x 3 locales x 2 themes, plus the two silent flashlights once per locale), all passed. Automated through adb key and tap injection on an emulator, not a person's hands.
- [x] 13. The registry row for `CONTEXT-HELP` is verified and dated, the pointer file exists, and the journal row was
      inserted through the harness, never by hand.
      *Evidence:* `docs/contracts/CONTEXT-HELP.md` and its index row, the registry row `CONTEXT-HELP` / Tourist Android (partial 0.1), the findings filed as `PROPOSAL-2026-10-25-tourist-android-findings.md`; the journal row was updated through the harness.

## 5. Facts specific to this repository

Read 2026-10-16. The Tourist tree has no product code, so the surface counts come from the extraction source (a
hint, not the source of truth) and are recomputed in P1.

### 5.1 Platform and toolkit

- Android, phone app and Wear OS app under one application id, plus a watch face; variants `store` and `noLegal`
  (`CLAUDE.md` section 3). Phone UI in the source is Android Views (XML layouts, activities on a shared base
  activity, `DialogFragment`s); the watch is Compose. Only the phone is in scope here.
- Android has no F1 without a hardware keyboard (or a TV remote or gamepad that maps to it); the `?` action in the
  top app bar is the way in for everyone else (`CONTEXT-HELP` rules 1 and 2).
- Source of truth for the surface list, once the ports exist: the manifest's activities, the dialog and sheet
  classes, the layouts, the widget providers.

### 5.2 Provisional surface inventory (from the extraction source; counts to be recomputed)

| Kind | Surfaces | Notes |
| --- | --- | --- |
| Main / start | 1 | the start screen is new in this product (S0004, placement open) |
| Screens | 6 | Tourist dashboard (portrait and landscape layouts, one activity), SOS, water flashlight, front flashlight, stopwatch, and the Tourist counter |
| Dialogs | 4 | stopwatch settings, stopwatch result, music picker, counter start |
| Settings pages | 0 to n | the handoff names no settings screen; the stored preferences (unit system, SOS mode, flashlight options) get their home in S0004 |
| Wizards, viewers | 0 | none in the kit |
| No-UI trampoline | 1 | the flashlight toggle activity is transparent and excluded from recents; proposed `none` |
| Widgets | 4 | Tourist, front flashlight, water flashlight, stopwatch; home-screen widgets are not windows with an app bar; proposed `none` with a reason, decided in P1 |
| Permission prompts | system dialogs | `none`; any rationale screen the product draws itself is a surface |

About 7 screens and 4 dialogs, plus the unknowns. The compass is not a screen: it is the hero tile of the dashboard,
so it is an element entry on the dashboard's page. The Tourist counter is in the source's manifest but is not named
in the handoff's source map; whether it is ported is decided in S0004, and the inventory follows.
The watch side (about 8 Compose surfaces) is out of scope.

### 5.3 Help that already exists, and what it means for this ticket

- **In this repository:** none.
- **In the extraction source (FastMediaSorter Android, read-only):**
  - A global F1 hook in its base activity opens an input-help dialog per screen identity, with a registry of
    shortcuts, a link resolver to portal pages keyed by permanent page ids, and a first-run hint. It is a shortcut
    overview, not the whole-surface explanation of `CONTEXT-HELP`. None of the kit screens uses it (six other
    screens do). The handoff's source map names the base activity as a seam but not these classes, so whether S0004's
    port keeps the F1 hook, and whether the shortcut overview survives, is open; P1 decides it together with S0004 so
    that F1 has exactly one path through the resolver (rule 17).
  - The kit screens use no function key: the stopwatch uses Space, S, R, the digits and Enter; the water flashlight
    its volume, media and camera keys and Back; the front flashlight Enter and D-pad centre. F1 is free.
  - The sorter's main menu has a Help item with a help glyph. It belongs to the sorter, not the kit.
- **Reuse:** the shortcut-overview content, if ported, is the seed of the keyboard part of a screen's page (rule 9
  asks for the shortcut of each element). The sorter's portal links are not reused (D7, D8).

### 5.4 Locales, strings, screenshots

- **Locales.** EN, RU, UK from the first release; the source ships thirteen value sets on both phone and watch (EN plus
  Arabic, Simplified Chinese, Bengali, German, Spanish, French, Hindi, Italian, Portuguese, Russian, Ukrainian, Urdu).
  Tourist's own set is fixed in S0002 and S0004. Arabic and Urdu are right-to-left; if they ship, the corner mirrors.
- **String tooling.** None in Tourist yet. The source has a string-localization check script, a termbase and a
  communication policy in three languages; S0004 adapts the policy (adapted, not copied) and creates the termbase,
  which the help names come from (rule 13).
- **Screenshot tooling.** None in Tourist. The source uses Maestro flows and store-listing assets, not a generator.
  The natural base here is the host-side stand that S0013 specifies (adb over emulator images; per-app locale set
  from outside; UI dumps for element bounds; screenshots), because it needs no cooperation from the app and so gives
  marker bounds from the element tree (rule 14). Demo content needs a synthetic fix and a synthetic step count.
  Choice and reasons recorded in P1 (D5).

### 5.5 Site, portal and siblings

- **Site and portal:** none. `site.kind` is "none"; no hub card; the host-root indexing work belongs to the hub.
  Publication and the link back down from a site wait for S0010 decision 4.9 (D8). The pages are complete without it.
- **Siblings:** FastMediaSorter Android shares the kit functions (the source of the port) - linking is blocked by 4.6
  (D7). StreamsPlayer for Android shares no function with Tourist, so rule 11 owes it no link. A family map link is
  possible only through the hub after 4.9.

### 5.6 Open tickets this touches

- **S0004** is the largest dependency: the start screen and app bar decide where the `?` stands; the base-activity
  seam decides the F1 path; the termbase decides the names; the `app.help` glyph is the first use of `ICON-SET` in
  this product. S0004 is not edited from here.
- **S0005, S0006, S0007** own the screens; under D1 option (a) each gains its slice (map rows, page, layer) when
  scheduled. Their acceptance lists are not changed by this ticket.
- **S0010** owns the name, the site, the package contents and the first-release dry run (D2, D7, D8, D9).
- **S0012** (promotion): generated screenshots can also feed the demo capture and the listing, which is a saving and
  not a dependency; the pages must pass its forbidden-terms test.
- **S0013**: its M7 measures labels, 48 dp targets, font scale 2.0 and locale parity on the same UI the `?` action
  joins; no conflict. The stand's hardware-key injection can drive the F1 half of the walk.
- **S0011**: a contract-sync round, still Draft; if it runs before P0 the pointer set may already have grown, so P0
  re-reads the index.

## 6. Risks

- **Drift from FastMediaSorter.** A help hook inside a ported screen is a divergence the drift check would list as
  upstream work. Mitigation: new files for everything new, smallest touch on ported files, a recorded decision per
  touch (section 2.2).
- **A help control on a lock-bearing screen** could weaken the water flashlight's rule or end a running signal. The
  form for those screens is chosen against the rule, and criterion 9 proves it on a device.
- **Screens that are one large control** (the front flashlight closes on any touch) have no free corner; the corner
  decision may need an owner view.
- **Page text over-claims** about SOS, the water flashlight or the instruments. Mitigation: section 2.2 and the S0012
  test.
- **Rename churn.** The working name "Tourist" may change (4.6); page text takes it from one place.
- **Locale growth.** Each added locale multiplies pages and screenshots; the gate counts the locale set, so a locale
  added later fails until its pages exist.
- **Scope creep into a tour or tooltips.** Both are outside the contract (section 2.1).

## 7. Open questions

- D1 to D9 of section 3.3.
- Whether the Tourist counter screen is ported at all (S0004).
- Whether the product gets a settings screen and an About screen (S0004 and S0010 shape them), and so whether they
  carry the help index link of P5.

## 8. Progress

Created 2026-10-16 as a Draft by the canon session's request. Implemented 2026-10-25 in one run, all steps P0 to P6 for the
phone app, after the owner's answers of the same day (D2, D3, D10, scope).

**Delivered**

- **P0:** `docs/contracts/CONTEXT-HELP.md` and its index row, `ICONOGRAPHY.md` made active, the registry row (partial 0.1),
  the findings filed as a catalog proposal. **P1:** `phone/src/main/assets/help/help-map.json`, 21 surfaces from the
  manifest, the dialog classes and the dialog call sites; named in `CLAUDE.md` section 6.
- **P2:** package `com.sza.tourist.help` - `HelpMap`, `HelpResolver` (one resolver), `HelpInstaller` (F1 for screens and
  dialog fragments through a window-callback wrapper, installed once in `TouristApplication`), `HelpButton` (self-wiring `?`
  for the start and SOS screens and for dialog titles), the toolbar action added by `StandardToolbar`, `HelpDialogFragment`
  (the viewer). Touches on ported files, each recorded as a decision beside its ledger row: `StandardToolbar` and the four
  dialog classes. `LightsActivity` also gained one line, the system-bar inset padding the other toolbar screens already
  had: its bar sat under the status bar, where the back arrow and the help button could not be tapped.
- **P3:** prose in `docs/help/src` (EN, RU, UK in one file per page, UI names as `{ui:string}` tokens), the generated pages
  and screenshots under `assets/help/<locale>/`, the kit CSS copied from the catalog. **P4:** `tools/help/check-help.ps1`
  (the gate), `HelpGateTest` (it inside `:phone:test`), `HelpResolverTest`, `baseline.json`, and the walk
  (`walk-help.ps1`, `docs/help/walk-2026-10-25.md`). **P5:** README and the tools README. **P6:** this record, the registry
  row, the proposal.

**For the owner**

1. The wording of every page is a draft in the product's voice, written against the code and the over-claim lines; it needs
   the owner's review (rule 16). Every claim was checked against the source, and the writers listed what looked wrong:
2. **Found in the app, not fixed here** (out of this ticket; read from the code unless a measurement is named): on the
   emulator the Back key does not close the water flashlight (measured), which matches the signature rule; a gesture-navigation
   swipe was not tried. The power button turns the screen and the flash off without closing the light, and the flash comes
   back when the screen wakes. The stopwatch starts counting by itself when its screen opens, and its bottom row
   overflows a 411 dp phone in Russian and Ukrainian so the settings gear has zero width (measured; the two accepted
   baseline lines). A repeated key (Space, Enter, volume up) writes laps in a row and R clears a running participant's
   time without a confirmation. The Russian and Ukrainian text of the empty-result message says "unload" (a leftover of
   the sorter). Saving a result on Android 8 and 9 needs a permission the manifest does not declare. The counter's starting
   number replaces the count without saving it to history. The Lights toolbar sat under the status bar (fixed here, one line).
3. The capture needs a debuggable emulator and `adb root`; the pages are regenerated after any layout or string change (the
   gate fails a stale capture, and the unit-test run skips only that one check).
4. Open and unchanged: the footer's support link (no address), cross-links and publication (S0010 4.6 and 4.9), the watch (D6,
   assumed out), `CHECK-PLACEMENT` (no runner registry yet), and a person's walk on a physical phone.

## Last Audit

2026-10-27, mode pre-handoff: sections 2, 4, 7 and 8 re-read from this file on disk. No code was changed and no
build was run for this block; the evidence is the cited records of 2026-10-25 (gate, unit tests, walk file), not a
fresh run.

Coverage per goal of section 1 (phone app; the watch is out of scope, D6):

- **F1 and the `?` button through one resolver, on every surface of the help map** - covered: 21 surfaces, the walk
  file `docs/help/walk-2026-10-25.md` (66 rows, all passed, driven by adb key and tap injection on an emulator).
- **A page or layer per surface, in EN, RU and UK** - covered: pages, screenshots and capture records in every
  shipped locale; the gate reports `PASS WITH ADVISORIES` (3 advisories, 550 links, 2 baselined) and runs inside
  `./gradlew :phone:test` (550 tests, 0 failures).
- **Help leaves the surface alive** - covered for a running SOS signal (it kept sounding and blinking while the
  help was open); covered by construction for the water flashlight and the stopwatch.

Verdict: the work covers the task except for the observations the owner has to make. Unobserved, each as it
stands:

- [ ] The wording of every page, read by the owner (rule 16): the pages are a draft in the product's voice, written
      against the code and the over-claim lines of section 2.2; the gate lists 3 over-claim advisories for that
      review, among them the water-flashlight line "not waterproofing".
- [ ] The `?` button and F1 walked by a person on a physical phone, in the three locales (the strict form of
      criterion 12).
- [ ] Criterion 9 on a device: the water flashlight with help present survives wet touches and leaves only by its
      exit rule (the S0005 acceptance repeated), and a running stopwatch keeps running while the help is open.

Carried elsewhere, not left for development here: the footer's support link waits for S0010 4.6 and 4.9; the S0012
forbidden-terms test (criterion 11) is S0012's to build; `CHECK-PLACEMENT` waits for a runner registry; the app
defects found during the walk (section 8, "For the owner", item 2) are findings for their own tickets, not help work.
