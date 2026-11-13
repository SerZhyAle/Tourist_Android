# S0011 - Contract synchronization - Tourist Android, round 2026-10-07

**Status:** Implemented
**Status note:** Implemented 2026-10-25: rule-adoption family read at the catalog current versions (REPO-STAMP 0.13, HARNESS-PROFILE 0.13, REPO-LAYOUT 0.11, RULE-DELIVERY 0.12); the combined registry row split into four, pointer docs/contracts/RULE-ADOPTION.md updated, one dated exception (REPO-LAYOUT rule 3, BOOTSTRAP_HANDOFF.md, until 2026-12-31). Compliance gate exit 0 (0 errors, 1 warning: digest lag, adopt-canon re-sync owed), spec_catalog/validate exit 0, contract-lag 7 rows with 0 behind and 0 unparsed. Catalog vectors are reader cases and were not run; no code, profile or stamp changed. INPUT-PARITY and PACKAGE-VERSIONING stay pending.
**Phase:** outside the handoff phase plan (handoff section 8) - contract synchronization ordered by the owner on 2026-10-07
**Owner:** the Tourist_mob session files and executes this ticket; the canon session only wrote the content below
**Verification level:** the "Definition of done" at the end of this ticket - contracts read by id and section, conformance-vector command and exit code cited where a contract has vectors, registry rows verified.

## 0. Source

Written by the canon session on 2026-10-07, on the owner's order, from the shared contracts catalog's registry and
the contracts' document logs. Hand it to the session that owns this product: that session files it as its own
ticket, edits its own registry rows and its own code, and nobody does either on its behalf. Contracts are cited
by id and section, never linked; the catalog moves before the code (CONTRACTS.md).

**What happened.** On 2026-10-07 the owner sealed every contract in the catalog (only the contract's owner edits it;
every other product files a `PROPOSAL-<date>-<topic>.md`) and had all open proposals accepted. Each accepted
proposal raised the version of the contract it changed and left a dated document-log row. This spec lists
what that asks of this product. A proposal that still needs the owner's choice is **not accepted yet** - where a
log row below says so, the question is open and the text does not bind on that point.

Registry surfaces covered: Tourist Android.

## In short - what this round asks of this product

No proposal in this round came from or targets Tourist Android directly. Read the changed contracts in the table below
against the product (they are the ones its rows name), move the rows that match, and keep `pending` - with a note
naming what is missing - the ones that cannot be verified honestly.

## Contracts changed on 2026-10-07

| Contract | Surface | Synchronized with | Current | Verified |
| --- | --- | --- | --- | --- |
| `HARNESS-PROFILE` | Tourist Android | 0.10 | 0.11 | 2026-10-02 |
| `REPO-LAYOUT` | Tourist Android | 0.10 | 0.11 | 2026-10-02 |
| `REPO-STAMP` | Tourist Android | 0.11 | 0.12 | 2026-10-02 |
| `RULE-DELIVERY` | Tourist Android | 0.11 | 0.12 | 2026-10-02 |

### `HARNESS-PROFILE` - 0.10 to 0.11

Home: domain `rule-adoption/`, status draft, owner sza-unified-rules. Read the domain README at the current version, then the documents it names.

| Date | Version | Kind | What changed | Owed |
| --- | --- | --- | --- | --- |
| 2026-10-07 (this round) | 0.11 (`HARNESS-PROFILE`) | additive | Rule 2: `paths.changelog` is derived from the stamp rather than fixed against it - the stamp's `ledgerFile`, else `dev/CHANGELOG.md`, under `ledgerShape` `2`; empty under `4` and `none`; an explicit profile value wins; no stamp means the shipped default. An empty value means the repository keeps no dev-log and the tools that need one say so and create nothing. The derivation under `ledgerShape` `1` and `3` is **not decided** and the default stays as it was. Corrects a premise of fact in the 2026-10-02 decision: the harness does read `paths.changelog` (`devlog/add_to_dev_log.ps1` writes it, `spec_catalog/complete.ps1` checks it), so the contradiction is not merely latent in a repository that .. | Review - adopt it, or record a dated exception saying why not |

### `REPO-LAYOUT` - 0.10 to 0.11

Home: domain `rule-adoption/`, status draft, owner sza-unified-rules. Read the domain README at the current version, then the documents it names.

| Date | Version | Kind | What changed | Owed |
| --- | --- | --- | --- | --- |
| 2026-10-07 (this round) | 0.11 (`REPO-LAYOUT`) | additive | Rule 3 gains a third exception to the type-prefix requirement: the release package files that section 8 of the canon's `RELEASE_AND_DISTRIBUTION` names, under the default names `PLAN/RELEASE_QUEUE.md`, `PLAN/RELEASE_READY.md`, `PLAN/RELEASE_QUEUE_DONE.md` or under whatever names the repository writes into its agent-rules file. Closes the item the spec-scheme proposal left open and the third bullet of CyrFlip's seconding (`PROPOSAL-2026-09-23-own-spec-scheme.md`, `PROPOSAL-2026-09-25-cyrflip-rule-adoption.md` section 1). Documents what the canon prose already allows; no gate checks the prefix, so no reader changes. Not covered: a free-form backlog file such as CyrFlip's `Contract_Conformance_ .. | Review - adopt it, or record a dated exception saying why not |

### `REPO-STAMP` - 0.11 to 0.12

Home: domain `rule-adoption/`, status draft, owner sza-unified-rules. Read the domain README at the current version, then the documents it names.

| Date | Version | Kind | What changed | Owed |
| --- | --- | --- | --- | --- |
| 2026-10-07 (this round) | 0.12 (`REPO-STAMP`) | breaking | Rule 4 writes the meaning of an absent or unparseable `canon.adoptedOn`, which 0.10 and 0.11 left open: with `canon.reconciledOn` also absent or unparseable the age is unknown, and an unknown age is read as past the 180-day window, so a differing digest is an error - never an age of zero. An absent date draws no finding of its own. Oldest-consumer test: the reader that shipped before reads such a stamp as age zero, so it silently judges a warning where this says error - hence `breaking`, although no key changes and no stamp has to change (the adoption run always writes `adoptedOn`); only a reader moves, and section 6 says so. Closes the first of the three items the stamp-defaults proposal le .. | MUST adapt - an implementation on the older shape does something wrong against this one |

### `RULE-DELIVERY` - 0.11 to 0.12

Home: domain `rule-adoption/`, status draft, owner sza-unified-rules. Read the domain README at the current version, then the documents it names.

| Date | Version | Kind | What changed | Owed |
| --- | --- | --- | --- | --- |
| 2026-10-07 (this round) | 0.12 (`RULE-DELIVERY`) | breaking | Rule 5 carries the same reading at the rung where it takes effect: where neither `canon.reconciledOn` nor `canon.adoptedOn` is present and parseable, the age is unknown and read as past the window, so a differing digest is an error. The ladder was silent on that case and the canon's reader filled the silence with age zero (`PROPOSAL-2026-09-23-stamp-defaults.md`). Kind and effect as in the `REPO-STAMP` row above: a reader changes, a stamp does not. | MUST adapt - an implementation on the older shape does something wrong against this one |

## Never verified (`pending`)

- `INPUT-PARITY` (Tourist Android), current 0.3
- `PACKAGE-VERSIONING` (Tourist Android), current 0.4

## Definition of done

1. Each contract above is read at its current version and cited by id and section, never by catalog path.
2. The implementation matches it, or a dated exception (reason, `until` date) is in REGISTRY section 3. A
   deviation that nothing records is a violation (CONTRACTS.md section 4).
3. The product's own suite runs the catalog's conformance vectors at the current version where the contract has
   any; the command and its exit code are cited. Where it has none, the row says so.
4. The product's own rows in REGISTRY section 2 carry `Implements`, `Reads`, `Verified` (today) and a one-line
   note, in the cell format of that section, one contract id per row. A row that cannot be verified honestly
   stays `pending` with a note naming what is missing.
5. Each `docs/contracts/<ID>.md` pointer names the current version.
6. **No contract document is edited by this product, drafts included - every contract is sealed.** A needed
   change, or a disagreement with an accepted text, is a `PROPOSAL-<date>-<topic>.md` beside the contract in
   its domain folder; the owner decides. Another product's registry rows are not edited either.

## Acceptance

- [x] Every numbered item of "Definition of done" above holds; the commands and exit codes that prove it are cited in this ticket.

## Progress

Done 2026-10-25.

- **The target versions are the catalog's of the day, not the table's.** The table above was cut on 2026-10-07; the
  rule-adoption domain moved again by the amendments of 2026-10-17 and 2026-10-23, so the four contracts were read at
  their current versions: `REPO-STAMP` 0.13, `HARNESS-PROFILE` 0.13, `REPO-LAYOUT` 0.11, `RULE-DELIVERY` 0.12.
  The 0.13 wire-carrier dispatch and the ledger-shape text of 0.12 are read with the four rows above.
- **Definition of done 1 (read and cite).** The domain README at the current versions, sections 2 to 6 and the three
  document logs, read in full; the registry rows and the pointer cite them by id and section only.
- **Definition of done 2 (match or exception).** No code, profile or stamp had to change.
  `.sza-canon.json` carries `adoptedOn` 2026-10-02 and `reconciledOn` 2026-10-17, both parseable, so the 0.12
  unknown-age rule never applies; no `schemaVersion` is a valid legacy stamp and the key waits for the next adoption
  run. `.sza-profile.json` declares `version` `"1"` (wire 1.0) and no `paths`. One exception is recorded in REGISTRY
  section 3: `REPO-LAYOUT` rule 3, `docs/BOOTSTRAP_HANDOFF.md` carries no type prefix (a frozen origin record, also
  addressed by the stamp exemption `SZA-CTR02`), until 2026-12-31.
- **Definition of done 3 (vectors).** The catalog now holds generated vectors for these three contracts
  (`rule-adoption/vectors/`, `REPO-STAMP` and `HARNESS-PROFILE` 0.13, `RULE-DELIVERY` 0.12), but they are cases for the
  canon's own stamp and profile readers and this repository has no reader, so they were not run here. `REPO-LAYOUT`
  has none. The product's own files were judged by the installed readers instead:
  - `pwsh -NoProfile -File <plugin 2026.1021.2>/tools/check-compliance.ps1 -RepoRoot .` - exit 0, 0 errors,
    1 warning (SZA-CANON03, the digest lag below).
  - `pwsh -NoProfile -File tools/harness.ps1 spec_catalog/validate` - exit 0, 0 FAIL (the one WARN is the older
    hygiene note about tickets filed under `PLAN/DONE/`, not touched here).
  - `pwsh -NoProfile -File <plugin 2026.1021.2>/tools/contract-lag.ps1 -Product Tourist` - exit 0, 7 rows: current 5,
    behind 0, partial 0, pending 2, ahead 0, unparsed 0.
- **Definition of done 4 (registry rows).** The combined row of 2026-10-02 is split into four, one contract id per
  row, each `Implements` `-`, `Reads` the current version, `Verified` 2026-10-25 and a note with the evidence. The
  catalog registry is outside git and live the moment it was written; a copy of the file as it was is kept in the
  session scratch only.
- **Definition of done 5 (pointers).** `docs/contracts/RULE-ADOPTION.md` and its row in `docs/contracts/README.md`
  name 0.13 / 0.13 / 0.11 / 0.12.
- **Definition of done 6 (sealed).** No contract document and no other product's row was edited, and no proposal was
  needed: the accepted text was followed, and the one gap met (the installed harness does not derive
  `paths.changelog` from the stamp yet) is a reader lag that section 6 of the contract itself records.

**Left open, none of it started here.**

- `INPUT-PARITY` (current 0.4, row at 0.3) and `PACKAGE-VERSIONING` (current 0.5, row at 0.2) stay `pending`: nobody
  has read them against the code since the shell shipped. S0016 owns the `INPUT-PARITY` read.
- The stamp is one digest behind the installed canon (2026.10.17.1 against 2026.10.21.2): a warning, with the error rung
  from 2027-04-16. The `adopt-canon` re-sync clears it and also writes `schemaVersion`.
- `paths.changelog` is not declared on purpose: ledger shape `none` means the contract's value is empty, but the
  installed `Get-SzaPath` throws on an empty value. Declare it empty once the harness derives or accepts empty.
- Nine finished tickets sit in `PLAN/DONE/` while the journal names their old paths; `REPO-LAYOUT` rule 6 and
  `HARNESS-PROFILE` rule 2 ask a repository that spells its archive that way to declare it in the profile. An owner
  decision, not taken here.
