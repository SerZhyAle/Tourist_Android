# S0002 - Gradle skeleton, version stamping, port ledger and drift check

**Status:** Draft
**Phase:** 2 of 10 (handoff section 8)
**Verification level:** build + install - every module and variant builds, each package installs on a device or
emulator, versionName and versionCode read back from the installed package.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 2, sections 3, 4.1, 4.2, 4.4, 5 (tech stack reference), 6a, 7
(PACKAGE-VERSIONING).

## 1. Goal

Empty phone, wear and watch-face apps that build, install and carry their frozen ids, their signing
configuration and mechanically derived versions - plus the port ledger and the drift-check script, which must
exist before the first ported file lands in S0003.

## 2. Scope

- Modules: phone, wear, watch face (owner decision 4.4: the module exists). Directory names fixed here
  (`phone`, `wear`, `watchface` is the proposal; StreamsPlayer for Android, the sibling, chose the same - reuse
  the decision, never its code); the lock path rules and `probes` of `.sza-profile.json` change in the same
  commit.
- Variants: `store` and `noLegal`; interface in `src/main`, implementation in the variant source set, one DI
  module per variant (handoff section 3). The FastMediaSorter flavor matrix is not inherited. What each variant
  hides is owner decision 4.3 (S0008); the skeleton variants stay empty.
- Toolchain: re-read FastMediaSorter's `gradle/libs.versions.toml`, `docs/TECH_STACK.md` and its pins for the
  current values; minSdk is a tactical decision with 26 (phone) and 28 (watch) as the reference, targetSdk is
  whatever Play requires at the first release. Android SDK: `C:\Users\serzh\AppData\Local\Android\Sdk`.
- Signing configuration that reads the key path from a gitignored file or an environment variable; no key
  material in the repository. The key itself is the owner's to create (section 3.3).
- Version stamping per PACKAGE-VERSIONING (Android profile), derived mechanically, never hand-bumped.
- **Port ledger and drift check** (handoff section 6a item 2), committed before the first ported file: ledger
  rows carry the source path in FastMediaSorter, the source file's SHA-256 at port time, the destination path
  and the port date; the script re-hashes the sources and lists every file changed in FastMediaSorter since its
  port; a change left uncarried is a decision written next to its ledger row, never silence. Written for this
  repository from the handoff text; the sibling's script is read for its decisions only.
- `.sza-canon.json` keeps `channels: []`; the first channel declaration moves to S0010 with decisions 4.5, 4.7
  and 4.9 (the sibling met SZA-SURF01, a hosted privacy page required once a store channel is declared -
  re-check the gate before declaring anything).
- `.gitattributes`: path patterns for ported test vectors, so the ledger's SHA-256 stays valid across line
  endings.
- `docs/BOOTSTRAP_HANDOFF.md` section 5 line counts are re-measured when a port ticket starts, not here.

## 3. Contract work owned by this phase

- PACKAGE-VERSIONING: the package families in the band table - phone and watch share `com.sza.tourist` (and the
  noLegal variant shares it too), so they need disjoint bands; the watch face has its own id and family. A
  MINOR amendment, written in the catalog first: another product's contract is amended by proposal, never by
  edit, so the exact diff goes to the canon session as a `PROPOSAL-<date>-<topic>.md` beside the contract. The
  contract stays at its registry version until its owner folds the proposal in; this repository's registry row
  is not touched beyond its own `pending` line.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0001 (foundation) must be closed first.
- **4.1 Frozen anchors (answered 2026-10-02):** `com.sza.tourist` for the phone app and the watch app (one
  shared id, required by the Data Layer) and for the noLegal variant, which never meets the store variant on
  one store; `com.sza.tourist.watchface` for the watch face. Frozen forever after the first shipped package.
- **4.2 Signing key location (answered 2026-10-02):** `P:\ANDROID\Tourist_credentials`, outside the repository,
  after the `FastMediaSorter_credentials` and `StreamsPlayer_credentials` precedents.
- **4.2 Play App Signing (answered 2026-10-02):** Play App Signing with the product's own key imported as the
  app signing key at the first Play upload, and a separate upload key for the AAB. One signature on Play builds
  and sideload APKs, so the variants update each other and the Data Layer pairs phone and watch across them.
- **4.4 Watch face module (answered 2026-10-02):** the module exists. Its composition (clone, or clone with
  tourist complications - new code, an explicit exception to the extraction-only non-goal) is asked right
  before S0009.
- **4.8 License (answered 2026-10-02):** MIT - recorded in S0001, `LICENSE` written there.
- **Signing key creation:** the key is the owner's and `P:\ANDROID\Tourist_credentials` does not exist yet.
  Nothing is generated under that path by an agent; builds use the debug signature, and release builds stay
  unsigned until the owner supplies the key.

## 4. Acceptance

- [ ] Every module and both variants build from a clean checkout; exit codes cited.
- [ ] Each package installs; `applicationId`, versionName and versionCode read back from the device.
- [ ] Release builds signed from the out-of-repo key; no signing material tracked (`git ls-files` clean) -
      waits for the owner's key.
- [ ] Port ledger and drift-check script written, self-tested and run green on the empty ledger.
- [ ] PACKAGE-VERSIONING amendment diff sent to the canon session; the contract stays at its registry version
      until the owner folds the proposal in.
