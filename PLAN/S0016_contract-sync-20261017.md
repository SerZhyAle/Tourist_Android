# S0016 - Synchronize accepted contract proposals of 2026-10-17

**Ticket:** S0016
**Status:** Draft
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

- [ ] Every applicable target has a clause-to-surface audit and an implemented, excepted or justified
      not-applicable outcome; no silent optional treatment of a mandatory requirement.
- [ ] Every declared exception contains the rationale/evidence/review fields above, and temporary debt
      has its expiry and remediation ticket. Recommended corrections/additions are recorded and handed off.
- [ ] Commands, exit codes, artifact versions/hashes and observed results are recorded. Missing generated
      artifacts, device/browser checks and joint replay remain explicitly unverified until performed.
- [ ] Own implementation rows, pointers and docs reflect actual verified behavior. Other products' claims
      and credentials/anchors remain untouched. No release, tag, upload or deployment is implied.
- [ ] Open questions are resolved or explicitly carried to a named successor; the final audit distinguishes
      proven conformance, exceptions, recommendations and remaining unverified work.

## 7. Last Audit

Task created only. No product code, build, runtime, device or conformance test was changed or run by
the dispatch operation. A future implementation session records its fresh evidence here.
