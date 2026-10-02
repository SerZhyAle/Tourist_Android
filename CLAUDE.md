# CLAUDE.md - Tourist for Android

Shared contracts live outside this repository, in the catalog at `P:\Contracts` - the one file here that names it. Cite a contract by id and section, never by path or link; pointers in `docs/contracts/`.

> **Canon:** the SZA Unified Rules, shipped as the `sza` Claude Code plugin (`/plugin marketplace add SerZhyAle/sza-unified-rules`, `/plugin install sza@sza-unified-rules`; outside Claude Code read `rules/INVARIANTS.md` at https://github.com/SerZhyAle/sza-unified-rules). Consumption model **reference**: the canon is not restated; this file keeps only this repository's deltas. Overlay **B** (Android). Adoption stamp: `.sza-canon.json`. Per-project record: `rules/contrib/tourist_mob.md` in the canon repository. A universal rule that looks wrong for this repo is reported to the canon session, never patched here.

`AGENTS.md` delegates to this file.

## 1. The product

- A hike kit for the phone and the watch: the Tourist dashboard (with a compass hero tile), SOS, the water
  flashlight that no wet touch can switch off, the screen and camera flashlights, a multi-participant stopwatch.
  Phone app and Wear OS app under one application id, plus a watch face.
- "Tourist" is the working name; the store name is an open owner decision (S0010).
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
  background location, no Google Play services dependency for it.
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
  6. `PLAN/` is **tracked**: there is no remote yet, so an ignored ticket store would exist in exactly one place,
     and the tickets carry the owner's recorded decisions.

## 5. Repository layout

- `PLAN/` - tickets and the release plan. `docs/` - the handoff record and `docs/contracts/` pointers.
  `tools/` - the harness forwarder. Gradle modules (phone, wear, watchface) arrive with S0002.
- Scratch and evidence that is not committed: `temp/<ticket>/` (gitignored). `.gitignore` carries the leak
  globs for signing material.
- Android SDK on this machine: `C:\Users\serzh\AppData\Local\Android\Sdk`.
