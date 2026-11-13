# Rule adoption family - pointer

| Field | Value |
| --- | --- |
| **Ids** | `REPO-STAMP` 0.13, `HARNESS-PROFILE` 0.13, `REPO-LAYOUT` 0.11, `RULE-DELIVERY` 0.12 (registry, 2026-10-25) |
| **Home** | shared contracts catalog, `rule-adoption/` |
| **Owner** | sza-unified-rules |
| **Role here** | adopter |

What this repository owes:

- `REPO-STAMP`: `.sza-canon.json` at the root, values looked up from the live tree, digest recomputed, never
  moved by hand. Both `canon.adoptedOn` and `canon.reconciledOn` stay present and parseable, so the age of the
  stamp is never unknown (section 2 rule 4). The optional `schemaVersion` (0.13, absent means wire 1.0) arrives
  with the next adoption run, never by a hand write.
- `HARNESS-PROFILE`: `.sza-profile.json` at the root, `version` `"1"` (wire 1.0); harness scripts run from the
  plugin through `tools/harness.ps1`, never copied or edited. Ledger shape `none` keeps no dev-log (section 3
  rule 2), so `paths.changelog` is not declared here.
- `REPO-LAYOUT`: the names tools address - `PLAN/`, `temp/`, `docs/contracts/`; the `Sxxxx` ticket scheme and the
  three release package files are named in `CLAUDE.md`, which is what rule 3's exceptions need.
- `RULE-DELIVERY`: the canon arrives as the `sza` plugin; staleness is judged by the compliance gate from the
  digest, aged from `canon.reconciledOn`.
