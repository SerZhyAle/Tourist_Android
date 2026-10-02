# Rule adoption family - pointer

| Field | Value |
| --- | --- |
| **Ids** | `REPO-STAMP` 0.11, `HARNESS-PROFILE` 0.10, `REPO-LAYOUT` 0.10, `RULE-DELIVERY` 0.11 (registry, 2026-10-02) |
| **Home** | shared contracts catalog, `rule-adoption/` |
| **Owner** | sza-unified-rules |
| **Role here** | adopter |

What this repository owes:

- `REPO-STAMP`: `.sza-canon.json` at the root, values looked up from the live tree, digest recomputed, never
  moved by hand.
- `HARNESS-PROFILE`: `.sza-profile.json` at the root; harness scripts run from the plugin through
  `tools/harness.ps1`, never copied or edited.
- `REPO-LAYOUT`: the names tools address - `PLAN/`, `temp/`, `docs/contracts/`.
- `RULE-DELIVERY`: the canon arrives as the `sza` plugin; staleness is judged by the compliance gate.
