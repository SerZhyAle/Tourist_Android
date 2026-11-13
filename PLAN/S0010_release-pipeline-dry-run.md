# S0010 - Release pipeline and first-release dry run

**Status:** Draft
**Status note:** 2026-10-28: pipeline built and dry-run with rehearsal keys (section 6). Open: the owner's keys,
the PACKAGE-VERSIONING fold, the channel declaration (section 3.3).
**Phase:** 10 of 10 (handoff section 8)
**Verification level:** dry run - build, pre-release sweep with a written verdict, drift check, update over a
prior install; nothing tagged, uploaded or published.
**Depends on:** S0025

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 10, section 9 (acceptance), sections 4.5 to 4.9, 6a, 7; the canon's
release runbook, driven by the `release` and `store-publish` skills. The owner's order of 2026-10-26 (four
publication tickets S0025 to S0028) settles the direction of decisions 4.5, 4.7 and 4.9 (section 3.3).

## 1. Goal

The release pipeline exists and has been proven end to end without any one-way step: a release candidate built
from a clean tree, the pre-release sweep, the drift check and an update over a real prior install all pass.

## 2. Scope

- Build every package of the release candidate; the sweep's written PASS or FAIL names the version it judged.
- Run the port-ledger drift check at the start of the run: zero uncarried FastMediaSorter changes without a
  written decision next to the ledger row (handoff acceptance 6).
- Update a real prior install (phone, watch, and the face if it ships), not only a fresh install; reach does not
  shrink (countries, minimum platform, device set), judged per variant.
- Declare this product's channels in `.sza-canon.json` as decisions 4.5, 4.7 and 4.9 settle them (a store
  channel may require a hosted privacy page; re-check the compliance gate); fill the Play and sideload rows of
  the canon's channel reference only for channels that ship.
- Register the PACKAGE-VERSIONING families' registry rows once the owner has folded S0002's proposal in; verify
  this product's other registry rows and add the conditional ones (INSTALL-TRUST, product web pages, icons,
  diagnostic report) only if their condition holds.
- Privacy page, permission list and the store data-safety declaration say the same thing; a fresh IARC
  questionnaire if the content-exposure profile calls for one, never a copied rating id.
- No tag, no upload, no publish: a release happens only when the owner asks for that exact release.

### 2.1 Triage

Complex rung: new files (`tools/release/`), a build-logic change (signing per variant), more than three files.

### 2.2 Decisions taken here (2026-10-28)

- **Two keys, signed per variant.** Owner decision 4.2 (Play App Signing with the product's own key imported,
  plus a separate upload key) maps onto the build as: `noLegal` packages signed with the app signing key (the
  certificate Play also puts on its downloads, so a Play install and a sideload install update each other),
  `store` packages and the watch face (Play-bound, no variants) signed with the upload key. One properties file
  carries both (`upload.` prefix for the upload key). Without the upload key the Play-bound packages build
  unsigned, never with the app key in its place.
- **The sweep judges a commit, never the working tree.** The tree carries other tickets' work; `preflight.ps1`
  extracts the commit with `git archive`. A working-tree snapshot is allowed for a dry run and is never shippable.
- **PASS versus shippable.** A defect of the candidate is FAIL; a precondition outside the build (rehearsal keys,
  unregistered families, an unverified contract row, no privacy page, no channel) is OPEN. Shippable = PASS with
  nothing OPEN. The device half (`update-proof.ps1`) writes beside the verdict.
- **The first release defines the reach baseline** (`reach-candidate.json`, committed as
  `tools/release/reach-baseline.json` with that release); every later candidate is compared per package.
- **Dry run with rehearsal keys.** The owner's keys do not exist yet, and no agent generates anything under
  `P:\ANDROID\Tourist_credentials`. Throwaway keys under `temp/S0010/keys` prove every step except "signed with the
  owner's key"; the re-run with the owner's keys is acceptance 1.
- **The prior install of a dry run** is the package of an earlier pre-flight run (a later stamp gives a higher
  versionCode in every family); for a real release it is the package last shipped on that channel.

**Contract boundary:** consumer of `PACKAGE-VERSIONING` (versions; the Tourist families are proposed, not yet
folded in). No shared format, wire shape or algorithm constant changes here.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002 to S0009 (everything the candidate carries); S0025, S0026, S0027 (the channels).
- **4.5 Distribution of the first release - resolved without asking (2026-10-28):** Play for the `store` variant
  (phone + Wear, S0027) and a GitHub sideload channel for the `noLegal` variant (S0025 section 2.4, S0027 section
  2.1), in the order S0025 -> S0026 -> S0027 -> S0028. Settled by the owner's order of 2026-10-26. An INSTALL-TRUST
  page is therefore needed; it is hosted by S0026.
- **4.6 Name and branding - not blocking here:** the dry run carries the working name and stand-in artwork; the
  question is asked once, where it blocks (S0026 W2, S0027 P3).
- **4.7 GitHub remote - direction resolved by the order of 2026-10-26 (a remote exists); the repository name,
  visibility and history policy are S0025's owner inputs.** The channel declaration of this ticket takes that
  name as input, hence the Depends-on line.
- **4.9 Site - resolved by the order of 2026-10-26:** the product gets a site and joins the family (S0026).
- **Watch face in the first release:** S0027's owner input. The pipeline builds, signs and judges the face either
  way; this ticket does not decide whether it ships.

## 5. Open questions

- **Q1 - the owner's release keys.** Status: Open. The key is the owner's to create (S0002 section 3.3). Options:
  (a) run `pwsh -NoProfile -File tools/release/new-release-keys.ps1` in a console - recommended: it creates both
  keys (RSA 4096, PKCS12, 10000 days) and the `keystore.properties` in `P:\ANDROID\Tourist_credentials`, asks
  each password twice and never puts it on a command line; (b) create both keys in Android Studio (Generate
  Signed Bundle) and write `keystore.properties` by hand in the format of `tools/release/README.md`. Either way
  the owner keeps an offline backup: a lost app signing key is unrecoverable for the sideload channel.
- **Q2 - the PACKAGE-VERSIONING fold.** Status: Open. The Tourist bands (phone 0..5, watch 6..9 on the shared id,
  watch face own namespace 0..5) are filed as a proposal beside the contract (S0002, 2026-10-23) and the
  contract's owner has not folded them in. Options: (a) accept the proposal as filed and have the catalog's
  owning session fold it - recommended: the build already derives exactly these codes and the dry run proved
  them; (b) amend the bands first; the build and its tests change with the amendment.
- **Q3 - channel declaration.** Status: Open. Blocker: S0025. `.sza-canon.json` `channels`,
  `versionShape.tagRegex` and `ledgerShape` are written once S0025 has the repository name; the Play row once
  S0027 has created the console app.

## 6. Plan and progress

- [x] P1 Drift check at the start of the run. Five rows had drifted, all from FastMediaSorter S4248 (commit
      44c85a65a, 2026-10-25: scheduled phone-to-watch messages of the sorter's scheduled operations); decided
      as not carried with `decide-drift.ps1`. `check-drift.ps1` exit 0: "OK: nothing uncarried (362 in step,
      6 decided)".
- [x] P2 Signing per variant in build-logic (`ReleaseSigning`, `AndroidApplicationConventionPlugin`,
      `DistributionVariantsConventionPlugin`). `gradlew -p build-logic :convention:test` exit 0;
      ReleaseSigningTest 9 of 9, VersionStampTest 9 of 9.
- [x] P3 `tools/release/`: `new-release-keys.ps1`, `preflight.ps1`, `update-proof.ps1`, `_release.ps1`, README.
- [ ] P4 Dry run: rehearsal keys, pre-flight A, pre-flight B at least 15 minutes later, update proof B over A
      for every package on a phone and a Wear emulator, a reach-shrink negative test, the release-plan
      `-Ship -DryRun`.
- [ ] P5 With the owner's keys (Q1) and the families folded (Q2): pre-flight on a commit, update proof, channel
      declaration (Q3), privacy and data-safety agreement with S0026 and S0027.

## 4. Acceptance

- [ ] Release candidate builds, signed from the out-of-repo key, with versions from the registered families.
- [ ] Pre-release sweep: written verdict naming the version, PASS.
- [ ] Drift check: nothing uncarried without a written decision.
- [ ] Update over a prior install proven; reach not shrunk.
- [ ] Dry run of the release operation completed with no push, tag or upload.
