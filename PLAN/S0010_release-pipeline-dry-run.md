# S0010 - Release pipeline and first-release dry run

**Status:** Draft
**Phase:** 10 of 10 (handoff section 8)
**Verification level:** dry run - build, pre-release sweep with a written verdict, drift check, update over a
prior install; nothing tagged, uploaded or published.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 10, section 9 (acceptance), sections 4.5 to 4.9, 6a, 7; the canon's
release runbook, driven by the `release` and `store-publish` skills.

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

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002 to S0009 (everything the candidate carries).
- **4.5 Distribution of the first release (open):** Play (phone + wear) plus a sideload variant at once, or
  GitHub/sideload first. Decides the signing flow, the version family registration and whether an
  INSTALL-TRUST page is needed. Note from the FastMediaSorter ticket S4024: a personal developer account created
  after November 2023 puts every new app through a closed test (12 testers, 14 days) before production.
- **4.6 Name and branding (open):** "Tourist" is the working name. The store name, icon and listing text, and
  whether the product names FastMediaSorter ("all this and more in FastMediaSorter", the S4024 Spam-policy
  advice), are the owner's. Until then the product carries the working name and stand-in artwork.
- **4.7 GitHub remote (open):** repository name and visibility. Nothing is pushed until the owner names it.
- **4.9 Site (open):** whether the product gets a page and joins the family map (SITE-FAMILY-MAP) - an owner
  decision, not a consequence of adopting the canon.
- **Signing key (follow-up of 4.2):** the owner creates the key at `P:\ANDROID\Tourist_credentials` (location
  answered in S0002) before the first signed build.

## 4. Acceptance

- [ ] Release candidate builds, signed from the out-of-repo key, with versions from the registered families.
- [ ] Pre-release sweep: written verdict naming the version, PASS.
- [ ] Drift check: nothing uncarried without a written decision.
- [ ] Update over a prior install proven; reach not shrunk.
- [ ] Dry run of the release operation completed with no push, tag or upload.
