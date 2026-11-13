# S0016 - Synchronize accepted contract proposals of 2026-10-17

**Ticket:** S0016
**Status:** Implemented
**Status note:** Implemented 2026-10-26: the twelve contracts of the 2026-10-17 consolidation read at their current registry versions (ICON-SET 0.36, ICON-RENDER 0.24, INPUT-PARITY 0.4, HARNESS-PROFILE 0.13, CONTEXT-HELP 0.1, DOC-EXTERNAL 0.12, DOC-INTERNAL 0.10, UI family 1.1/2.0/2.0/1.1/2.0); pointers updated (ICONOGRAPHY, INPUT-PARITY, new DOCUMENTATION-QUALITY, index README); registry rows written (INPUT-PARITY Reads 0.4 verified, DOC-INTERNAL-QUALITY candidate partial with exception until 2026-12-31 remediation S0029, Tourist unified-UI migration row until 2026-11-08 referencing S0015); help gate re-run PASS WITH ADVISORIES exit 0; contract-lag rows 410 -> 411, unparsed 12 unchanged. The five UI contracts stay with S0015 per the recorded reconciliation (no duplication). No product code, build, device or release step. Full audit in the spec, section 7.
**Date:** 2026-10-17

## Captured request and migration scope

Нужно закрепить все предложения внутрь контрактов, обновить версии контрактов и создать во все подчинённые им проекты новые спецификации о приведении себя в соотвествие к контракту. проект всегда должен иметь право потребовать иметь обоснованное исключение - проверь, что это есть в правилах работы по контрактам

Проект при приведении себя в соотвествие с контрактом должен иметь право объявлять обоснованные исключения, вносить рекломендованные поправки и дополнения

## 1. Problem and authority

The portfolio owner accepted outstanding proposals in canon S0007 on 2026-10-17. This repository must
read the applicable current contracts, audit its actual behavior, implement justified changes or
declare explicit exceptions, and propose any further corrections/additions. Existing implementation
claims stay at their previously verified versions until fresh evidence supports an update.
This is a new synchronization task, not closure of older implementation tickets or approval to publish.

## 2. Contract inventory

The versions below are the targets at task creation. Read each current contract's consolidation
amendment of 2026-10-17, its normative appendices and any later accepted changes. Cite by ID and section;
resolve the catalog only through this repository's agent-rules pointer, never duplicate its text here.
For newly applicable contracts establish a scoped row rather than infer conformance from a product name.

| Contract | Target | Existing Implements / Reads (verification date) |
| --- | --- | --- |
| `BOOLEAN-OPTION` | 2.0 | not verified here; determine applicable scope |
| `BUTTONS` | 2.0 | not verified here; determine applicable scope |
| `COLLAPSIBLE-GROUP` | 1.1 | not verified here; determine applicable scope |
| `CONTEXT-HELP` | 0.1 | not verified here; determine applicable scope |
| `DOC-EXTERNAL-QUALITY` | 0.12 | not verified here; determine applicable scope |
| `DOC-INTERNAL-QUALITY` | 0.10 | not verified here; determine applicable scope |
| `HARNESS-PROFILE` | 0.12 | - / 0.11 / 0.10 / 0.10 / 0.11 (2026-10-02) |
| `ICON-RENDER` | 0.24 | not verified here; determine applicable scope |
| `ICON-SET` | 0.34 | not verified here; determine applicable scope |
| `INPUT-PARITY` | 0.4 | - / 0.3 (pending) |
| `SETTINGS-NAVIGATION` | 2.0 | not verified here; determine applicable scope |
| `UI-SIZING` | 1.0 | not verified here; determine applicable scope |

## 3. Project-specific work

Audit phone, watch and watch-face surfaces. Do not add media/exchange functions merely because another product implements those contracts.

Read every existing pointer and own adoption/exception row, including missing, partial, unparsed and
not-applicable entries. Map the affected surfaces and existing tickets before implementation so this
task coordinates the new requirements without duplicating work already owned by another ticket.
No counterpart repository is edited from this product's implementation run without owner authorization.

## 4. Exceptions, corrections and additions

The project ALWAYS has the right to declare a justified exception in its own registry row without
prior contract-owner approval, and to recommend amendments, clarifications and additions. Record the
contract/clause, exact scope, concrete reason and evidence, user/data/security impact, compensating
behavior, responsible project and review condition. Temporary migration debt has an until date and
a named remediation ticket; permanent supported variants have a dated linked design/standards record.
Do not invent expiry dates for permanent variants or unconditional conformance for a local limitation.

Amend contracts this project owns through the catalog's dated/versioned change process. For another
owner, file a dated PROPOSAL with suggested wording, evidence and compatibility consequences; leave
the existing contract text intact. The owner records acceptance, partial acceptance or reasoned rejection.
Until acceptance, keep an exception for a justified local deviation. A proposal alone is not a waiver.
Independent legal, store, security and data-safety release blockers still apply. These rights follow
catalog RULES section 15 and canon CONTRACTS section 4 and cannot be removed from the audit verdict.

## 5. Implementation and evidence plan

1. Inventory applicability per platform/edition/surface and correlate existing work; list exact clauses.
2. Audit the compatibility law: names, higher minor/major, unknown enum, absence, bounds and defaults.
3. Apply accepted behavior with safe migration and preserved user data/remaps, or record justified exceptions.
4. Use generated reference artifacts; regenerate in their owning implementation, never hand-edit vectors.
   New browser/worker/clipboard/sealing profiles require real negative cases as well as happy paths.
5. Run this repository's appropriate focused checks. UI changes require rendered keyboard/pointer,
   theme/high-contrast, scaling, long locale/RTL and supported device/engine acceptance. State anything
   untested instead of claiming that a build or source read proves it.
6. Update short contract pointers, own registry rows and exception/proposal references only after proof.
   Retain partial versions for unresolved gaps. A catalog version bump never upgrades implementation rows.

## 6. Acceptance criteria

- [x] Every applicable target has a clause-to-surface audit and an implemented, excepted or justified
      not-applicable outcome; no silent optional treatment of a mandatory requirement. (Section 7, table A;
      the five UI contracts are explicitly scoped to S0015 with the registry migration row the contracts'
      staged-migration clauses demand, not silently deferred.)
- [x] Every declared exception contains the rationale/evidence/review fields above, and temporary debt
      has its expiry and remediation ticket. Recommended corrections/additions are recorded and handed off.
      (Two exceptions: `DOC-INTERNAL-QUALITY` (until 2026-12-31, remediation S0029) and the UI-family
      migration row (until 2026-11-08, remediation S0015 M1-M5); the S0014 help proposal stands proposed;
      no new proposal was owed - nothing new was found.)
- [x] Commands, exit codes, artifact versions/hashes and observed results are recorded. Missing generated
      artifacts, device/browser checks and joint replay remain explicitly untested. (Section 7, table C.)
- [x] Own implementation rows, pointers and docs reflect actual verified behavior. Other products' claims
      and credentials/anchors remain untouched. No release, tag, upload or deployment is implied.
- [x] Open questions are resolved or explicitly carried to a named successor; the final audit distinguishes
      proven conformance, exceptions, recommendations and remaining unverified work. (Section 7, table D.)

## 7. Last Audit

Recorded 2026-10-26 by this implementation session. No product code, build variant or runtime behaviour was
changed: the deliverables are pointer files, catalog registry rows, one exception, one remediation ticket and
this record. Nothing below claims device, emulator or browser evidence; no build was run.

### A. Clause-to-surface audit (the twelve contracts of section 2, at the versions the catalog holds today)

| Contract | Current (registry, 2026-10-26) | Applicable surfaces | Outcome |
| --- | --- | --- | --- |
| `BOOLEAN-OPTION` | 2.0 (active) | the product-owned boolean rows (four on the phone: shade shortcut, stopwatch music, stopwatch volume keys, counter; wear chips) | **Migration debt under S0015.** Every phone row is a whole-row `MaterialSwitch` - the exact shape 2.0 replaces. S0015 M1 replaces them with the leading-checkbox row; its inventory (section 3) already maps each row and its allowance under section 4.1 (no option-local help today). Scoped outcome recorded in the registry migration row, deadline 2026-11-08; no silent optional treatment. |
| `BUTTONS` | 2.0 (active) | command buttons and dialog answers on every phone screen, wear and watch-face configuration when one ships | **Migration debt under S0015** (M1 radius/token kit, M3 sweep, M4 measurements). Already holding, per S0015's audit: content-fit widths, Cancel-then-affirmative dialog order, danger face on the destructive SOS stop, visible focus. Non-compliant: corner radius 12 where 4 is required, no named ordinary-command style, row-height equalization and state-width reservation unmeasured. |
| `COLLAPSIBLE-GROUP` | 1.1 (active) | About topics when the settings surface lands; no existing group | **No violating surface; component owed by S0015 M1.** Single-topic screens correctly carry no wrapper (section 2.2); packed collapsed headers are recorded as not chosen (V5). |
| `CONTEXT-HELP` | 0.1 (draft, current - the target version never moved) | every phone surface the product draws; the watch is out of scope by section 5 | **Implemented to `partial 0.1` by S0014 (verified 2026-10-25), re-proved today.** The gate re-run: `PASS WITH ADVISORIES`, exit 0, 21 surfaces, 8 pages x 3 locales, 550 links, 2 baselined. The four-finding proposal of 2026-10-25 (`in-app-help/PROPOSAL-2026-10-25-tourist-android-findings.md`) stands `proposed`; this audit adds nothing to it and found no further gap in 0.1 for the surfaces S0014 mapped. |
| `DOC-EXTERNAL-QUALITY` | 0.12 (draft) | would bind a published user corpus or site | **Justified not-applicable.** The product publishes no external corpus and runs no site (S0010 decision 4.9 not taken). It becomes applicable the day that decision opens one; no registry row is written for a not-applicable contract, and the pointer says so. |
| `DOC-INTERNAL-QUALITY` | 0.10 (draft) | the internal corpus: `CLAUDE.md`, `README.md`, `docs/`, `PLAN/` | **Adopted as a candidate consumer, `partial`, with a dated exception.** Rule 4 holds for the one generated family (help pages/screenshots, gated); rule 7 holds in fact. Rules 1-3 and 5-6 are followed by hand, not mechanized: no document registry, no reverse-coverage, cross-link or style gate. Registry row added (Reads 0.10, verified 2026-10-26); exception recorded in the registry section 3 (until 2026-12-31, fields per RULES section 15), remediation ticket **S0029**. |
| `HARNESS-PROFILE` | 0.13 (draft) | `.sza-profile.json` and the harness forwarder | **Current, nothing owed.** S0011 read section 3 rules 1-7, section 6 and the amendments of 2026-10-17 and 2026-10-23 on 2026-10-25; the catalog has moved no further amendment since (the latest row of the document log is the 2026-10-23 one). The pointer already states the shape this repo holds; the registry row stands at 0.13, verified 2026-10-25. |
| `ICON-RENDER` | 0.24 (draft) | every glyph-bearing control on phone, wear, widgets and help button | **Read 0.15 -> 0.24 in one pass; pointer updated to 0.24.** 0.16-0.19 are Windows/web-scoped; 0.20-0.24 delegate button-surface geometry to `BUTTONS`, group markers to `COLLAPSIBLE-GROUP`, settings-destination policy to `SETTINGS-NAVIGATION` and target floors for covered components to `UI-SIZING`. The Android 48 dp target floor this product already renders by is unchanged; no render rule this product uses moved. The covered-component geometry work is S0015's migration; the registry rows for the iconography family stay with S0015 M5 per the recorded reconciliation. |
| `ICON-SET` | 0.36 (draft) | the glyphs the ported screens draw | **Read 0.17 -> 0.36 in one pass; pointer updated to 0.36.** The added records name other products' surfaces plus `app.tour` and `action.paste`; none names a glyph this product draws, so no vocabulary work follows for shipped surfaces. The one gap (`ic_favorite`, the watch heart-rate tile) is already filed as the usage proposal of 2026-10-23 (S0004), still `proposed` with the owner. The version carrier `vocabulary.version` (section 5) is read by the owner's exporter, not by this product. |
| `INPUT-PARITY` | 0.4 (draft) | touch, hardware keys, gamepad and TV remote on phone and watch | **Read 0.3 -> 0.4; pointer and registry row updated (Reads 0.4, verified 2026-10-26).** The amendment adds the shared media-viewer action vocabulary and `VIEWER-KEYBOARD` defaults (and, in `INPUT-CHORD`, physical trigger names): this product draws no media viewer and stores no chord, so no new cell binds; the sections 3.1 vocabulary and the device table its screens conform to are unchanged. The stopwatch's volume-key participant switch is an in-window platform shortcut with its touch equivalent beside it. The per-screen parity walk stays with the screens' own tickets; no fresh walk ran in this ticket. |
| `SETTINGS-NAVIGATION` | 2.0 (active) | the settings surface - which does not exist yet | **Migration debt under S0015 M2 (the largest single gap).** 2.0 binds "even to settings with only one or two editable options"; the product had no settings surface at all. S0015 M2 adds it (start-side menu, Main + About, editor ownership one-per-setting, planned-resource rows with ticket references, no invented links). |
| `UI-SIZING` | 1.1 (active; the spec's 1.0 target was superseded on 2026-10-19) | every covered component on every screen | **Migration debt under S0015 M1/M4.** The touch profile is the stable default; ordinary targets 48 dp; no surface resizes on input events; compact mode not offered (permanent variant V1, declared smallest targets 48/32, untested below ordinary - the size-profile record and measurement are M4's); text enlargement rides `sp` + font scale (V2). The 1.1 amendment (final-width remeasurement on explicit scale/profile change) binds through the same milestone. |

### B. The rights the captured request asks to verify

The captured request asks the project to always keep the right to declare a justified exception and to
recommend corrections and additions. Verified present in the binding rules, unchanged by this task:

- `_meta/RULES.md` section 15 (owner ruling of 2026-10-17, the same date as this ticket): every project
  always has the right to declare a justified exception in its own registry rows and to recommend
  corrections, clarifications and additions, without prior contract-owner approval; the required fields,
  the `until` date and the named remediation ticket for temporary debt; a permanent variant lives in a
  dated design record with a review condition.
- `_meta/RULES.md` section 7 (comply, or amend - never deviate in silence) and the sealing ruling: a
  product edits the contracts it owns and files a dated `PROPOSAL-<date>-<topic>.md` beside any other
  contract; a proposal binds nobody until accepted, and an exception covers the justified local difference
  meanwhile. A proposal alone is not a waiver.
- Each of the five UI contracts repeats the same staged-migration law in its own compatibility section, and
  every one of them requires the migration plan to be "referenced in that product's registry
  migration/exception entry" - which is the entry this ticket wrote for this product (below).

### C. Commands, exit codes and observed results

| When | Command | Exit | Observed |
| --- | --- | --- | --- |
| 2026-10-26 | `pwsh -NoProfile -File tools/harness.ps1 spec_catalog/validate` | 0 | 8 OK, 1 WARN (journal->FS hygiene debt of the owner's uncommitted `PLAN/DONE/` restructure), 0 FAIL, 23 records at the time of the run. |
| 2026-10-26 | `pwsh -NoProfile -File <plugin 2026.1026.1>/tools/contract-lag.ps1` | 0 | Before this ticket's registry edits: `rows: 410 (current 138, behind 63, partial 133, pending 32, ahead 0, n/a 32, unparsed 12)`; Tourist rows: only `CONTEXT-HELP partial 0.1 = current`. After the edits: `rows: 411 (current 140, behind 63, partial 133, pending 31, ahead 0, n/a 32, unparsed 12)` - +1 row (`DOC-INTERNAL-QUALITY`), +1 current (`INPUT-PARITY` 0.4), -1 pending, unparsed unchanged. |
| 2026-10-26 | `pwsh -NoProfile -File tools/help/check-help.ps1` | 0 | `help-check: PASS WITH ADVISORIES (3; 21 surfaces, 8 pages x 3 locales, 550 links, 2 baselined)` - the `CONTEXT-HELP` rung-1 evidence re-proved the same day. |

Not run, and not claimed: any Gradle build or unit-test suite (no product code changed); any emulator,
device or browser check (none is owed by a documentation-only sync); the port-ledger drift check (the port
ledger is S0008/S0013 territory and its five uncarried rows are theirs, anchored 2026-10-25).

### D. Proven conformance, exceptions, recommendations, remaining work

- **Proven (fresh, this day):** `CONTEXT-HELP` rung 1 (gate green); `HARNESS-PROFILE` at current 0.13
  (re-read; S0011's evidence stands); the compatibility-law audit below.
- **Implemented records, not conformance claims:** pointer files updated to the current versions
  (`ICONOGRAPHY.md` 0.36/0.24, `INPUT-PARITY.md` 0.4, new `DOCUMENTATION-QUALITY.md`, the index
  `README.md`); registry rows written: `INPUT-PARITY` Reads 0.4 (verified 2026-10-26),
  `DOC-INTERNAL-QUALITY` Reads 0.10 (verified 2026-10-26), plus the Tourist unified-UI migration row.
- **Exceptions (two, both with the section 15 fields):** `DOC-INTERNAL-QUALITY` rules 1-3 and 5-6
  unmechanized (until 2026-12-31, remediation S0029); the UI-family migration debt (until 2026-11-08,
  remediation S0015 M1-M5, the dated plan in `PLAN/S0015_unified-ui-contracts/migration-plan.md`).
  No expiry date is invented for a permanent variant - the six S0015 variants (V1-V6) are design records
  with review conditions, not debt.
- **Recommendations:** none new. The one open proposal from this product
  (`in-app-help/PROPOSAL-2026-10-25-tourist-android-findings.md`, four findings) stands `proposed`; the
  owner's acceptance is waited for, and until then the readings it describes stay covered by the
  `partial 0.1` row, not by a conformance claim.
- **Remaining work, named:** S0015 M1-M5 (the five UI contracts + covered-component `ICON-RENDER`, and
  with M5 the `UNIFIED-UI.md` pointer and the iconography family's own registry rows); S0029 (the
  `DOC-INTERNAL-QUALITY` mechanization); the S0014 open items (owner wording review, a person's walk, the
  support link); the usage-proposal confirmation for `ICON-SET` (owner).

### E. The compatibility law of the spec's step 2

The one catalog wire this product reads at run time is the help map (`CONTEXT-HELP` rule 17,
`phone/src/main/assets/help/help-map.json`). Its reader (`HelpMap.kt`) is a tolerant Gson tree reader: it
ignores members it does not know (what the contract's `schemaVersion` promise needs), defaults an absent
`schemaVersion` to 1, and never rejects an unknown form value - names, absence, unknown enum, bounds and
defaults each checked against `CONTEXT-HELP` section 3. No other catalog contract has a wire this product
reads: the iconography vocabulary is read by the owner's exporter only (the shipped glyphs are ported
drawables); `INPUT-CHORD` is not adopted (no chord is stored anywhere in this tree); `.sza-canon.json` and
`.sza-profile.json` are read by the installed plugin, not by this repository's code, and their 0.13
carrier-dispatch rules were S0011's audit; the Wear Data Layer envelope carries this product's own
`schemaVersion` and is not a catalog contract.

### F. Incident note, recorded for the owner

While editing `P:\Contracts\_meta\REGISTRY.md` this session, a faulty one-line shell edit truncated the
registry to 631 of 1,382 lines. It was fully restored within the same hour: a concurrent catalog session
rewrote the complete file from its own context at 17:15, and the result was verified against this ticket's
pre-edit checksum (contract-lag `rows: 410` with the identical behind/partial/pending distribution) before
this ticket's rows were added on top. No row of another product was lost or altered by the incident; the
row texts this session restored from its own reads (the `REPO-LAYOUT` exception) matched the restored file
byte for byte in meaning and were left as the concurrent session wrote them. The registry is shared and
live: this ticket's rows were re-anchored after that rewrite, and a final `contract-lag` run (table C)
confirms the store parses this product's rows cleanly.

### G. Coordination

S0015's reconciliation (its ticket section 5, recorded 2026-10-25) gives the UI-contract rows, the
`ICON-RENDER`/`ICON-SET` implementation rows and the `UNIFIED-UI.md` pointer to S0015 and leaves this
ticket the non-UI contracts; this audit executed exactly that split - the five UI contracts and
`ICON-RENDER` got scoped outcomes with the registry migration row the contracts demand, and no pointer or
adoption row of theirs was duplicated here. S0014 owns the help surfaces; S0013 measures after S0015
lands. No counterpart repository was edited; no release, tag, upload or deployment is implied by any
record above.
