# Release pipeline

The build side of a release (S0010): keys, the pre-release sweep and its written verdict, the update proof.
Nothing here tags, pushes, uploads or publishes; that is the `release` skill's one-way step, run only when
the owner asks for that exact release.

| Script | Who, when | What it does |
| --- | --- | --- |
| `new-release-keys.ps1` | the owner, once | creates the app signing key and the upload key and the `keystore.properties` the build reads, in `P:\ANDROID\Tourist_credentials` |
| `new-release-keys.ps1 -Rehearsal -Destination temp/<ticket>/keys` | a dry run | the same with throwaway keys and random passwords, under `temp/` only |
| `preflight.ps1 [-Ref <commit>] [-Keys <file>]` | start of every release | builds and judges every package, writes the verdict (exit 0 PASS, 1 FAIL, 2 could not run) |
| `update-proof.ps1 -Serial <emulator> -Prior <apk> -Candidate <apk> [-NoLaunch]` | after a PASS | installs the candidate over a real prior install and checks version, install identity and data |

Run every script as `pwsh -NoProfile -File tools/release/<script>.ps1 ..` from the repository root.

## Keys and signatures

Owner decision 4.2: Play App Signing with the product's own key imported as the app signing key, plus a
separate upload key. The build (build-logic `ReleaseSigning`) reads one properties file, named by
`TOURIST_KEYSTORE_PROPERTIES` or found as `keystore.properties` in the repository root (ignored by git):
`storeFile`, `storePassword`, `keyAlias`, `keyPassword` for the app signing key and the same four with the
prefix `upload.` for the upload key.

| Package | Signed with | Why |
| --- | --- | --- |
| phone and wear, `noLegal` APKs | app signing key | sideload; the same certificate Play puts on its downloads |
| phone and wear, `store` APKs and AABs | upload key | uploaded to Play, which re-signs them with the app signing key |
| watch face APK and AAB | upload key | Play-bound; it has no variants |

A locally built `store` APK therefore does not update a `noLegal` install (two certificates); a store build
installed from Play does. The phone and the watch of one variant always share a certificate, which the Data
Layer needs. Without the properties file the release packages are built unsigned, with a warning; without
the `upload.` keys the Play-bound ones are.

Importing the app signing key into Play (the PEPK step) and keeping a backup of both keys are the owner's,
in Play Console (S0027).

## The verdict

`preflight.ps1` judges a commit extracted with `git archive` into `temp/rc/src` (never the working tree, which
carries other tickets' work) and writes `temp/release/<versionName>/`: `preflight.json` and `preflight.md`
(the verdict naming the version), `build.log`, `build-logic.log`, `drift.log`, one log per gate, `packages/`,
`reach-candidate.json`, `permissions.md`.

The release is where the full build and every check above 5 seconds run (TESTING_AND_QA section 2; the fast
tier between releases is `tools/fast-check.ps1`, `CLAUDE.md` section 7). The pre-flight runs all of it on the
judged source:

- the port-ledger drift check;
- the script gates: the help gate with freshness (`check-help.ps1`), the UI source gate
  (`check-unified-ui.ps1`), the documentation gate (`check-docs.ps1 -Check all`), and the self-tests
  `test-docs.ps1` and `test-port-ledger.ps1`;
- the build-logic unit tests (`gradlew -p build-logic :convention:test`);
- one Gradle invocation over `:phone:test`, `:wear:test`, `:watchface:test` (every variant's unit suite,
  `HelpGateTest` and `UnifiedUiComponentsTest` included) and every release APK and AAB.

Each check is PASS, WARN, OPEN or FAIL:

- **FAIL** is a defect of the candidate (drift uncarried, a red gate or self-test, a leak, a failed build or
  test, a missing or unsigned package, a debug or mixed signature, a wrong id or version, a reach shrink,
  native code without notices).
  Any FAIL makes the verdict FAIL.
- **OPEN** is a release precondition outside the build: rehearsal keys, a working-tree snapshot, families not
  registered in PACKAGE-VERSIONING, a contract row not verified, no privacy page, no channel declared. The
  verdict can be PASS with OPEN checks; it is **shippable** only when it is PASS and nothing is OPEN.
- **WARN** is the release plan not validating; reconcile it and carry on.

`-WorkingTree` judges a snapshot of the tree for a dry run; its verdict is never shippable.

The help pages are refreshed before the candidate commit is cut, so the pre-flight's help gate judges them:
the debug build on an emulator, `tools/help/capture-help.ps1`, `build-help.ps1`, `check-help.ps1`
(`tools/help/README.md`). The pre-release sweep is then the verdict plus its device half, each PASS written
into the same folder:

- one `update-proof.ps1` PASS per package that ships;
- the help walk, `tools/help/walk-help.ps1`, on the candidate's debug build;
- the UI device matrix, `tools/ui/run-ui-contracts.ps1` on its `S0015_` AVD (`tools/ui/README.md`).

A FAIL in any of them blocks the release.

Beside the sweep and outside its verdict (S0013 decision 8.4: release candidate only, not a gate), the stand's
measurements run on the candidate's debug build: the S0013 matrix (`PLAN/S0013_deep-dynamic-audit/INDEX.md`) and the
soak re-measure of S0021 (section 6.4 of that ticket, about 2 hours of emulator time).

## Reach

The first release defines the reach baseline: commit its `reach-candidate.json` as
`tools/release/reach-baseline.json` in the same change that records the release. From then on every candidate
is compared per package (phone and wear per variant, the watch face): a higher minSdk, a dropped ABI or a newly
required feature is a FAIL that needs an owner decision before the release. Countries and form factors are
console settings and are checked in the console (S0027).

## Update proof

Emulator only: the script uninstalls the package first. The prior package is installed fresh and launched, the
candidate goes over it with `adb install -r`, and the device must report the candidate's versionCode with the
same `firstInstallTime`; on a userdebug image (`adb root`) every data file of the prior install must still be
there, on a Wear image that refuses root the data step is SKIP. For a dry run the prior is the package of an
earlier pre-flight; for a release it is the package last shipped on that channel.
