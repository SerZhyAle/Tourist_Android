# S0025 - Publish the repository on GitHub

**Status:** Draft
**Status note:** Written 2026-10-26 from the owner's order (section 0). Nothing is created, pushed or tagged; every outward step waits for the owner's go (section 3.3). The order settles the direction of S0010 decision 4.7 (a GitHub remote exists); the repository name, the visibility and what the public history contains stay open.
**Phase:** outside the handoff phase plan (handoff section 8); first of the four publication tickets S0025 to S0028 (GitHub, site, Google Play, family links)
**Verification level:** static predicates on the public repository (`gh api`, `git ls-remote`, a fresh clone that builds) plus a written leak scan of the tree and of every commit. The first GitHub Release is not part of this ticket (section 2.4).

## 0. Source

- **Owner's order, 2026-10-26** (Russian source, quoted): "create specifications ... 1 about publish it in github". The order lists four items; this is item 1. Item 0 (document the project in sza.od.ua) is S0026, item 2 (Google Play) is S0027, item 3 (update sza.od.ua and the other products) is S0028.
- S0010 section 3.3: decisions 4.5 (distribution), 4.6 (name and branding), 4.7 (GitHub remote), 4.9 (site). `CLAUDE.md` sections 3 (frozen anchors, MIT), 4 (item 6: PLAN tracked "because there is no remote yet"), 5 (`temp/<ticket>/` for uncommitted evidence).
- Canon: GITHUB_INTERACTION sections 2 to 5, SECURITY_AND_PRIVACY sections 1 and 6, CHANNEL_MATRIX (GitHub row), invariants 3, 4, 17, 18; the `release` skill. Contract pointer `INSTALL-TRUST` (adopter once a sideload page ships; S0026 hosts that page).
- **Measured 2026-10-26** (read by the session that wrote this ticket):

| Item | Value | How it was read |
| --- | --- | --- |
| Git remote | none | `git remote get-url origin` fails |
| GitHub repository | none named `Tourist*` among the owner's 31 public repositories | `gh repo list SerZhyAle --limit 100`; S0012 read HTTP 404 for `Tourist_mob` and `Tourist` on 2026-10-09 |
| `gh` authentication | account SerZhyAle, scopes `gist`, `read:org`, `repo` | `gh auth status` |
| History | 5 commits; `PLAN/` is in all 5 (32 tracked files at HEAD) | `git log --oneline -- PLAN`, `git ls-tree -r HEAD -- PLAN` |
| Images in HEAD | 12 (`.png`, `.jpg`), among them the S0004 evidence captures with a location fix | `git ls-tree -r HEAD` |
| PLAN in the sibling repositories | FastMediaSorter Android and STREAMS Player (Windows): `PLAN/` ignored, 0 files tracked | `git ls-files PLAN`, `git check-ignore` |
| License | `LICENSE` present, MIT (owner, 2026-10-02) | file |

## 1. Goal

The source of Tourist is public at `https://github.com/SerZhyAle/<name>`. The repository holds exactly what is meant to be public (no secret, no signing material, no private capture, and the ticket store only if the owner decides so), tells a stranger in its first README paragraph what the product is, builds from a fresh clone, and carries the metadata that the site and the family map can link. The public repository is the prerequisite of the product site (S0026, GitHub Pages) and of the sideload channel.

## 2. Scope

### 2.1 Non-goals

- No tag, no GitHub Release, no APK upload (section 2.4). No Google Play work (S0027).
- No topics, social-preview image or directory submission: S0012 steps P4, P5 and P9 own them. This ticket sets the description and the homepage only.
- No CI workflow. The release pre-flight runs locally (S0010); a workflow is a separate ticket if the owner asks.
- No competitor's product or brand name in the description or the README (S0012 section 2.1 binds every published text).
- The repository is not renamed, deleted or force-pushed after the first push without a new owner decision: its name becomes part of a permanent Pages address (section 7).

### 2.2 Decisions taken by the order

- A GitHub remote exists and the sideload channel (the `noLegal` variant, CLAUDE.md section 3) has a home. Contact on every surface: sza@ukr.net, GitHub SerZhyAle (SITE-FAMILY-MAP rule 3; S0012 section 2.2).
- Issues are on; Discussions, Wiki and Projects are off (a bug-report path is a mandatory support surface, DOCUMENTATION_CONCEPT section 4). Taken as the default; the owner may override.
- README is English only; Russian and Ukrainian live on the site (S0026), which the canon permits (feature-to-site step 5). The README links the site once it answers.

### 2.3 Steps

**Phase A - pre-publication audit (A, read-only; evidence in `temp/S0025/`)**

- **A1 Leak scan of the tree and of every commit**: signing material and its globs, `local.properties`, keystores, tokens (`gho_`, `ghp_`, `-----BEGIN`), `google-services.json`, service-account files, any string naming the credentials folder's contents; e-mail addresses other than sza@ukr.net in files and in commit author fields; absolute local paths in files other than the exempted record (`.sza-canon.json`, id `SZA-CTR02`). Command shape: `git grep` over `git rev-list --all`, plus `git log -p` for added lines. Zero hits, or each hit dispositioned in writing.
- **A2 Private content**: look at every image in history. The S0004 evidence captures show a location fix; a real coordinate or a personal detail in any capture blocks publication of that commit (SECURITY_AND_PRIVACY section 4: no public capture goes out unchecked).
- **A3 History policy**: apply the owner's answer to the PLAN question (section 3.3). Whatever is chosen, make a backup bundle of the local history first (`git bundle create`, kept outside the repository), and rewrite nothing before the owner's go.
- **A4 Notices**: inventory the bundled third-party code, fonts and icons from the version catalog and the resources; `THIRD-PARTY-NOTICES.txt` is required inside every distributed package when a binary is bundled (invariant 12), so the inventory is also an input of the first release.
- **A5 README for a stranger**: rewrite as shipped fact (invariant 17): what Tourist is, the kit's parts, how to build from source, license, contact, the hub link. The current "Status: Foundation only" section is dropped. No "soon", "planned", "beta".
- **A6 CLAUDE.md section 4 item 6** states its own reason ("there is no remote yet"). It expires with the remote, so it is rewritten to the owner's PLAN decision in the same edit.

**Phase B - create and push (A·GO: the owner's go quoted in the go ledger)**

- **B1** `gh repo create SerZhyAle/<name>` with the chosen visibility and no push; set `origin`. Set the description (the README's first paragraph, one sentence) and `has_issues`/`has_wiki` per 2.2. The homepage stays empty until the site answers (S0026 sets it).
- **B2** Push `main` with the history policy of A3. `git push` without `--force`; the first push is the only push of this ticket.
- **B3** Prove it: `git ls-remote origin refs/heads/main` equals the local `main`; a clone into a new empty directory builds `:phone:assembleStoreDebug` and `:phone:assembleNoLegalDebug` with only `ANDROID_HOME` set (no file outside the clone), exit code and output cited.

### 2.4 Handed to the release, not done here

- The first GitHub Release (noLegal APKs, `.sha256`, notices) is the release package's `-Ship` step: S0010 proves the pipeline, the `release` skill runs it, and the owner asks for that exact release. The channel declaration in `.sza-canon.json` (`channels`, `versionShape.tagRegex`, `ledgerShape`) is S0010 scope and takes this ticket's repository name as input.
- On approval of this ticket, S0010 decision 4.7 is marked resolved by reference to this ticket's 3.3 answers.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0010 (decisions 4.5 to 4.9; the release that fills the GitHub row), S0012 (P4 topics and P5 social card follow this ticket), S0026 (the site that is served from this repository; sets the homepage), S0027, S0028.
- **Repository name (4.7), open.** Options: (a) `Tourist_mob` - recommended: it is this working directory's name, follows the family's `<Product>_mob` pattern, was found free on 2026-10-09 and again on 2026-10-26, and a rename of the store name (4.6) later does not touch a technical address; (b) `Tourist` - shorter, but ties a permanent address to a working name; (c) wait for the store name (4.6) - delays S0026, S0027 and S0028.
- **Visibility, open.** Options: (a) public - recommended: MIT was chosen on 2026-10-02, GitHub Pages for a public product site, the family map and the sideload channel all need it; (b) private - none of the downstream tickets can proceed.
- **History and the ticket store, open.** The five commits carry `PLAN/` (tickets, owner decisions, local paths, evidence captures). FastMediaSorter Android and STREAMS Player keep `PLAN/` untracked. Options: (a) publish the history as it is, PLAN included - transparent, but publishes every local path, session note and capture; (b) rewrite local history to drop `PLAN/` after the backup bundle, keep `PLAN/` ignored and local - recommended: it matches both siblings, and the consequence (the ticket store exists in one place, the reason of CLAUDE.md section 4 item 6) is accepted knowingly and backed up another way; (c) publish a fresh root commit of the current tree without `PLAN/`, keeping the old history local only - loses the five commits' messages in public, leaves the local branch unconnected to `origin`.
- **Go for phase B** (create, set metadata, push): asked once, after A1 to A3 pass, quoting the repository name, visibility and history policy.

## 4. Acceptance

Each a static predicate; the command, its exit code and its output are cited when the criterion is ticked.

- [ ] 1. `gh api repos/SerZhyAle/<name> --jq '[.visibility,.license.spdx_id,.default_branch,.has_issues,.has_wiki]'` prints the chosen visibility, `MIT`, `main`, `true`, `false`.
- [ ] 2. `git ls-remote origin refs/heads/main` equals `git rev-parse main`.
- [ ] 3. The leak scan report in `temp/S0025/` shows zero undispositioned hits over the tree and over `git rev-list --all`; the image review is recorded per image.
- [ ] 4. The PLAN decision holds: with (b) or (c), `git ls-tree -r --name-only origin/main | grep -c '^PLAN/'` prints `0` and no commit reachable from `origin/main` contains a `PLAN/` path; CLAUDE.md section 4 item 6 states the decision.
- [ ] 5. A fresh clone in an empty directory builds both debug variants (phone `store` and `noLegal`) with only `ANDROID_HOME` set.
- [ ] 6. README: first paragraph is a present-tense statement of the product; a search for `soon|planned|upcoming|not yet|beta|experimental|скоро|планируется|ещё не|бета` finds nothing; the only address is sza@ukr.net (S0012 criterion 14); no competitor name.
- [ ] 7. `.gitignore` leak globs still cover signing material, and `git ls-files` lists none of `*.jks *.keystore *.p12 *.pem *.pfx keystore.properties local.properties`.
- [ ] 8. The canon compliance check, run the way the plugin documents (never a guessed path), reports no error for the repository.
- [ ] 9. Every outward act (create, metadata, push) has a go-ledger row with the owner's quoted go.

## 5. Risks

- **Public history is permanent.** A secret found after the push needs rotation, not only a rewrite (SECURITY_AND_PRIVACY section 1). The scan therefore runs before B1.
- **Location captures.** An S0004 capture with the owner's real position would be published with the commit that holds it.
- **History rewrite.** Option (b) changes every commit hash; the bundle is the way back, and the rewrite happens only on the owner's go.
- **One place.** With `PLAN/` out of git, the ticket store has no remote copy.
- **Name permanence.** The repository name becomes the Pages path `serzhyale.github.io/<name>/`, and a row in SITE-FAMILY-MAP makes that address permanent (SITE-FAMILY-MAP section 4).

## 6. Open questions

- Does "publish in GitHub" also mean the first sideload release with downloadable APKs? Taken as: the release is the package ship (section 2.4), not this ticket.
- Whether the watch face APK (S0009) goes to GitHub Releases with the phone and watch packages - decided at the release.
