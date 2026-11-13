# CLAUDE.md - Tourist for Android

Shared contracts live outside this repository, in the catalog at `P:\Contracts` - the one file here that names it. Cite a contract by id and section, never by path or link; pointers in `docs/contracts/`.

> **Canon:** the SZA Unified Rules, shipped as the `sza` Claude Code plugin (`/plugin marketplace add SerZhyAle/sza-unified-rules`, `/plugin install sza@sza-unified-rules`; outside Claude Code read `rules/INVARIANTS.md` at https://github.com/SerZhyAle/sza-unified-rules). Consumption model **reference**: the canon is not restated; this file keeps only this repository's deltas. Overlay **B** (Android). Adoption stamp: `.sza-canon.json`. Per-project record: `rules/contrib/tourist_mob.md` in the canon repository. A universal rule that looks wrong for this repo is reported to the canon session, never patched here.

`AGENTS.md` delegates to this file.

## 1. The product

- A hike kit for the phone and the watch: the Tourist dashboard (with a compass hero tile), SOS, the water
  flashlight that no wet touch can switch off, the screen and camera flashlights, a multi-participant stopwatch.
  Phone app and Wear OS app under one application id, plus a watch face.
- "Tourist" is the product name; the store name is "Tourist: Hiking Kit" and the watch face's "Tourist Watch Face"
  (owner, 2026-11-13, closing decision 4.6 for the store; the icon is still the compass stand-in).
- An independent product, extracted from FastMediaSorter Android. The only coupling to any other product is the
  shared contracts catalog: no shared code, no shared Gradle module, no shared release.
- Origin, owner decisions and the extraction source map: `docs/BOOTSTRAP_HANDOFF.md` - written 2026-10-02 by the
  FastMediaSorter session of its ticket S4070, before this repository's first commit. Not edited from this
  repository's sessions; its open items are tickets now, and work is driven from the ticket store, not from the
  handoff.
- Read-only from sessions in this repository: `P:\ANDROID\FastMediaSorter_mob_v2` (the extraction source) and
  `P:\ANDROID\StreamsPlayer_mob` (a sibling extracted the same way: reuse its decisions where they fit, never its
  code).

## 2. Standing law - staying in step with FastMediaSorter (handoff section 6a)

The kit's behaviour must never quietly diverge from FastMediaSorter's. With no shared code allowed, three
mechanisms carry that guarantee, and each binds every ticket:

1. **Full port, never a rewrite.** Every class of the handoff source map is copied into this tree and cleaned
   (package `com.sza.fastmediasorter` -> `com.sza.tourist`, sorter references cut), together with its unit
   tests. Reimplementing a behaviour that can be ported is refused in review.
2. **Port ledger and drift check.** Every ported file has a row in the committed port ledger: source path in
   FastMediaSorter, the source file's SHA-256 at port time, destination path, port date. The drift-check script
   re-hashes the sources and lists every file changed in FastMediaSorter since its port - the to-do list for
   carrying a fix across. It runs at the start of every release; a change left uncarried is a decision written
   next to its ledger row, never silence. Both exist before the first ported file (S0002).
3. **No behaviour contract yet.** The kit's behaviour is written in no catalog contract, so the ledger is the
   whole guarantee. A behaviour that must be shared with a third product (the SOS signal, its Data Layer
   messages) is written in the catalog first, never as a document here.

## 3. Frozen anchors and owner decisions

- **Application ids** (owner, 2026-10-02): `com.sza.tourist` for phone and watch (one shared id - the Data Layer
  requires it) and for the noLegal variant, which never meets the store variant on one store;
  `com.sza.tourist.watchface` for the watch face. Frozen forever once the first package ships.
- **Signing** (owner, 2026-10-02): the key lives in `P:\ANDROID\Tourist_credentials`, outside the repository;
  Play App Signing with this product's own key imported as the app signing key, plus a separate upload key.
  Store builds and sideload APKs carry one signature. The key does not exist yet and is the owner's to create.
- **License:** MIT (owner, 2026-10-02).
- **Watch face:** the module exists (owner, 2026-10-02); its composition is decided before S0009.
- **Variants:** two - `store` and `noLegal`. Interface in `src/main`, implementation in the variant source set,
  one DI module per variant. The FastMediaSorter flavor matrix is not inherited.
- **Location** is foreground-only, read through the platform `LocationManager` while a screen is open: no
  background location, no Google Play services dependency for it. A watch in ambient is not an open screen
  (owner, 2026-11-08): the dashboard's sensor and location registrations stop there and restart on interactive
  wake.
- **The water flashlight's rule** - no wet touch can switch it off - is the product's signature and is ported
  exactly.
- Open owner decisions are owner-input items inside the ticket of the phase that needs them (section 3.3 of each
  ticket); each is asked once, in question-with-options form, right before that phase.

## 4. Ticket store and the release package plan

- **The harness is adopted from the plugin, not copied.** Run any harness script through the forwarder:
  `pwsh -NoProfile -File tools/harness.ps1 <cluster>/<script> [args]` (for example `spec_catalog/validate`,
  `spec_catalog/select -Id S0002 -Format json`). Configuration is `.sza-profile.json` and nothing else; never
  edit or copy a harness script.
- **Ticket ids:** `S0001` onward (`^S\d{4}$`, the harness grammar, which its allocator hard-codes). These ids
  overlap FastMediaSorter's and StreamsPlayer's S spaces, so a ticket of another repository is always cited with
  the repository name next to its id.
- Specs: `PLAN/Sxxxx_<slug>.md`. Journal: `PLAN/spec-catalog.jsonl`, written only through `spec_catalog/insert`,
  `update`, `close`, `archive` - never by hand.
- **Release package plan** - six decisions, recorded once:
  1. Files: `PLAN/RELEASE_QUEUE.md` (work remaining), `PLAN/RELEASE_READY.md` (ready),
     `PLAN/RELEASE_QUEUE_DONE.md` (shipped history, newest first).
  2. Single write path: every journal mutation goes through the harness's `Write-Catalog`, which calls
     `Sync-ReleaseQueue`; no skill or script maintains the files by itself.
  3. Ready set: `Implemented`, `Verified`, `BlockNeedUserTest`. Work remaining: `Draft`, `Approved`, `Tactical`,
     `In Progress`, `Partial`, `Broken`, `BlockByOtherTask`, `BlockQuestions`, `BlockExternal`. `Archived` leaves
     the plan.
  4. Package number: the `current-next-release:` marker in `PLAN/RELEASE_QUEUE.md`, starting at 1, advanced by
     `-Ship`. Not tied to a branch - this repository works on `main`. `--` is the not-scheduled bucket.
  5. Operator commands (all through `tools/harness.ps1 spec_catalog/release-queue`): `-List`, `-List -Ready`,
     `-Validate`, `-Reconcile`, `-SetCurrent N`, `-Ship -Release N [-Version X] -DryRun`. Package headings and
     line order are the owner's; no script reorders them.
  6. `PLAN/` is **tracked and public** (owner, 2026-11-13, the DeviceInfo and StreamsPlayer shape): the tickets carry
     the owner's recorded decisions, and the public remote keeps the second copy of the ticket store. The owner's
     private e-mail address is never written into a ticket, an owner quote included.

## 5. Repository layout

- `PLAN/` - tickets and the release plan. `docs/` - the handoff record, `docs/contracts/` pointers and `docs/play/`
  (the Play listing texts, the App content and IARC answers, the listing graphics). `site/` - the hand-written privacy
  pages (en, ru, uk) and the vendored family kit, checked by `tools/site/check-site.ps1` and published to GitHub Pages
  by `.github/workflows/pages.yml`. `tools/` - the harness forwarder and the product's gates; `tools/store/` renders the
  listing graphics. Gradle modules (phone, wear, watchface) arrive with S0002.
- Scratch and evidence that is not committed: `temp/<ticket>/` (gitignored). `.gitignore` carries the leak
  globs for signing material.
- Android SDK on this machine: `C:\Users\serzh\AppData\Local\Android\Sdk`.

## 6. In-app help (S0014, `CONTEXT-HELP`)

- **The help map** is `phone/src/main/assets/help/help-map.json`: one row per surface the phone app draws, with its
  form (`page`, `inherit` or `none`; `layer` and `both` are not built) and the elements of its page in marker
  order. The resolver and the gate read the same file. A new screen, dialog or widget needs a row in the same edit
  (the gate fails without it); a screen whose touch must never be spent keeps `ask: off` and says why.
- **Where things live.** Prose: `docs/help/src/<help>.json`, all shipped locales in one file, UI names written as
  `{ui:string_name}` so a renamed button is renamed in its help. Generated, never edited by hand: the pages and
  screenshots under `phone/src/main/assets/help/<locale>/` and the capture records in `docs/help/shots/`.
  Tools: `tools/help/` (README there): `capture-help.ps1` (emulator), `build-help.ps1`, `check-help.ps1` (the gate).
- **The watch is out of scope** (no pointer, no keyboard). Location truth and the over-claim lines of S0012 section
  2.1 bind help text; the water flashlight is a touch lock, not waterproofing, and SOS sends no SMS and makes no call.

## 7. Verification between releases

Between releases run only checks measured at 5 seconds or less and a direct inspection of the change; defer the
full build and slower or unmeasured tests to the release (TESTING_AND_QA section 2). Record each skipped rung in
the ticket audit as `RELEASE` with the exact release command; report "not built, not tested until the release".

- Fast command: `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui`, or `-Area help`, or `-Area docs` after an
  edit to the documentation corpus; for a PowerShell edit use `-Area scripts -Path <script.ps1>`. The scripts
  check parses the named file; it does not prove its behaviour.
- String tables (S0042): any edit of a `values*/*.xml` file runs `-Area strings` (`-Area ui` includes it): it
  fails on double-encoded text, invalid UTF-8, a BOM, U+FFFD, C1 or NUL characters in every table of phone, wear
  and watchface, 0.6 s. A table is edited only through UTF-8-explicit tools; Windows PowerShell 5.1 `Get-Content`
  without `-Encoding` re-encodes it (`tools/strings/README.md`). After an edit of `tools/strings/`, run its
  self-test: `pwsh -NoProfile -File tools/strings/test-string-tables.ps1` (1.8 s).
- Documents: a new file under `docs/` or `PLAN/` (or a Markdown file under `tools/`) needs a record in
  `docs/DOCUMENT_REGISTRY.jsonl` or `PLAN/DOCUMENT_REGISTRY.jsonl` in the same edit; `-Area docs` fails without
  it (S0029, `tools/docs/README.md`).
- Measured 2026-10-29 with a process-inclusive Stopwatch: UI source gate 0.380 s (exit 0); help gate with
  `-SkipFreshness` 1.369 s (exit 1, existing capture/baseline findings); full help gate 1.527 s (exit 1);
  port-ledger self-test 5.685 s (exit 0, release tier). Through the wrapper, same method: `-Area docs` 1.906 s
  (exit 0); the full timing list is in S0030. The docs self-test, the build-logic tests and the device scripts
  have no recorded time: release tier.
- Known red: the help gate has stale/missing captures and stale baseline entries, tracked with S0014; it remains
  a gate, never a green result. Gradle suites have no fast form or recorded time and belong to release.
- Release: `pwsh -NoProfile -File tools/release/preflight.ps1` runs the drift check, every script gate (help with
  freshness, UI source, string tables, docs) and the three self-tests, the build-logic tests, and the full Gradle build with every
  module's unit suite; complete the device half in `tools/release/README.md` (help capture and walk, UI matrix,
  update proof).
