# DIAGNOSTIC-REPORT - pointer

| Field | Value |
| --- | --- |
| **Id** | `DIAGNOSTIC-REPORT` |
| **Version** | 0.12, draft (registry, 2026-10-02) |
| **Home** | shared contracts catalog, `diagnostic-report/` |
| **Owner** | StreamsPlayer (Windows) |
| **Role here** | producer, conditional - only if this product exports a diagnostic log bundle |

What this repository owes:

- Nothing until a diagnostic export is scheduled. When it is: write the bundle in the contract's shape,
  sanitized, and add this product's registry row in the same ticket. Handoff section 2 lists no such export in
  the first release.
