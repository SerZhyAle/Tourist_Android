# S0001 - Canon adoption, contract pointers, ticket store and release plan

**Status:** Verified
**Status note:** Foundation verified 2026-10-02: compliance gate exit 0, validators exit 0 (see Last Audit).
**Phase:** 1 of 10 (handoff section 8)
**Verification level:** gate - the canon compliance gate and the harness validators, exit codes cited.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 1, and the owner's session brief of 2026-10-02: adopt the canon in
new-repo mode (overlay Android, reference model), sync the contracts, stand up the ticket store and the release
package plan from the canon harness, turn section 8 into phase tickets and section 4 into owner-decision items,
ask the phase 2 decisions, verify, commit locally. No product code, no remote, no push.

## 1. Goal

The repository has a real agent contract, a canon stamp, contract pointers, a working ticket store with its
release plan, and one ticket per remaining phase - so every later session starts from the store, not from the
handoff.

## 2. Scope

- `CLAUDE.md` rewritten from the bootstrap stub: canon pointer, overlay B, reference model, contrib record, the
  catalog named once, handoff section 6a as standing law, frozen anchors, the release package plan.
- `AGENTS.md` kept as the delegating stub.
- `.sza-canon.json` with a freshly computed digest; `.sza-profile.json` for the harness; `tools/harness.ps1`
  forwarder (no harness script is copied).
- `docs/contracts/` - one pointer per contract of handoff section 7, plus its index.
- This product's rows in the catalog registry.
- Contrib record in the canon repository (`rules/contrib/tourist_mob.md`).
- `PLAN/` ticket store: journal, phase tickets S0001-S0010, `RELEASE_QUEUE.md`, `RELEASE_READY.md`,
  `RELEASE_QUEUE_DONE.md`.
- `LICENSE` (MIT, owner decision 4.8), `README.md` status and license lines, `.gitattributes` cleanup.

## 3. Decisions taken in this phase

- **Ticket id scheme:** `S0001`.. (`^S\d{4}$`), the harness default. Another prefix was considered and
  rejected: the harness allocator (`New-CatalogId`) and `insert.ps1 -Id` hard-code `^S\d{4}$` although
  `grammar.ticketIdPattern` is a profile key (57 occurrences in 42 harness files, counted on plugin
  2026.1002.3), so any other scheme would validate ids that allocation can never issue. Our ids overlap
  FastMediaSorter's and StreamsPlayer's S spaces, so a ticket of another repository is always cited with the
  repository name next to its id.
- **Release package plan:** the harness files under `PLAN/`, reconciled on the journal's single write path
  (`Write-Catalog` -> `Sync-ReleaseQueue`); package number from the `current-next-release:` marker, starting at
  1; `PLAN/` is **tracked** in git. The six decisions are recorded in `CLAUDE.md` section 4.
- **No product channel declared:** `.sza-canon.json` keeps `channels: []`, `site.kind: none` until owner
  decisions 4.5, 4.7 and 4.9 are taken (S0010).
- **Handoff and the catalog path:** `docs/BOOTSTRAP_HANDOFF.md` names the catalog by path. It is an origin
  record written before this repository's first commit and not edited from here, so it carries a dated
  exemption (SZA-CTR02) in `.sza-canon.json`; every living file cites contracts by id.
- **`.gitattributes`:** the bootstrap file carried one FastMediaSorter-only line (a `letterbox-fill` vector path
  that does not exist in this tree); removed here, path patterns for ported byte-hashed vectors are S0002's.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** none - first phase; S0002 depends on it.
- **4.1 Frozen anchors:** answered 2026-10-02, recorded in S0002.
- **4.2 Signing:** answered 2026-10-02, recorded in S0002.
- **4.4 Watch face module:** existence answered 2026-10-02 (yes), recorded in S0002; composition in S0009.
- **4.8 License:** answered 2026-10-02 - MIT, `Copyright (c) 2026 Serhii Zhyhunenko (SerZhyAle)`; `LICENSE`
  written in this phase because the compliance gate fails without it.
- **Open, attached to their phases:** 4.3 (S0008), 4.5, 4.6, 4.7, 4.9 (S0010), start-screen placement (S0004).

## 4. Acceptance

- [x] Canon compliance gate from the plugin root: exit 0, 0 errors.
- [x] `spec_catalog/validate`: exit 0.
- [x] `spec_catalog/release-queue -Validate`: exit 0; one status change moves a ticket's line between
      `RELEASE_QUEUE.md` and `RELEASE_READY.md`.
- The foundation commit carries this ticket in its closed state; no remote, no push.

## Last Audit

2026-10-02, run in this session against plugin `2026.1002.3` (canon `2026.10.02.3`, core digest
`sha256:410463a9..`, printed identically by the plugin and the canon repository checkout):

- Baseline before this phase: `check-compliance.ps1` from the plugin root, exit 1,
  `3 error(s), 0 warning(s) (overlay ?)` - SZA-CANON01 (no stamp), SZA-LAY01 (no LICENSE), SZA-RULES01 (no agent
  file, the stub was untracked).
- After: plugin root, exit 0, `0 error(s), 1 warning(s) (overlay B, canon 2026.10.02.3)`. The warning is
  SZA-CANON06: the contrib record exists in the canon repository checkout and reaches the plugin cache only with
  the canon session's deploy. The same gate from the canon checkout with `-RepoRoot` on this repository: exit 0,
  `0 error(s), 0 warning(s)`.
- `tools/harness.ps1 spec_catalog/validate`: exit 0, `9 OK, 0 WARN, 0 FAIL` over 10 records, ids dense
  S0001 to S0010.
- `tools/harness.ps1 spec_catalog/release-queue -Validate`: exit 0, `10 ticket(s), no drift`.
- Catalog: three adoption rows written to the registry section 2 (`PACKAGE-VERSIONING` and `INPUT-PARITY`
  `pending`, the rule-adoption family verified), byte-checked against a backup: 1342 bytes inserted after the
  StreamsPlayer Android rows, every other byte identical, CRLF preserved.
- Driven, not read: this ticket moved In Progress -> Verified through `spec_catalog/update` (exit 0, header
  synced); its line left `RELEASE_QUEUE.md` (10 -> 9 tickets) and appeared in `RELEASE_READY.md` under package 1,
  and `release-queue -List -Ready` exits 0 from that moment (it exited 1 while the ready file was empty).
- Found and reported to the canon session, not patched: `release-queue -List -Ready` exits 1 with "The property
  'Count' cannot be found on this object" while `RELEASE_READY.md` holds no ticket (`release-queue.ps1` line 256
  assigns an unwrapped result, line 302 reads `.Count` under StrictMode; the same class as the validator defect
  recorded for streamsplayer_mob); the release-plan seed files and the harness forwarder are shipped by no
  template (contrib record, candidate core edits).
