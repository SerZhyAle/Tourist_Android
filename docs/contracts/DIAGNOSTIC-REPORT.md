# DIAGNOSTIC-REPORT - pointer

| Field | Value |
| --- | --- |
| Id | DIAGNOSTIC-REPORT |
| Version read | 0.14, draft, 2026-10-27 |
| Home | Shared catalog, `diagnostic-report/` |
| Role here | Producer |

About exports a user-requested `Tourist-logs-yyyyMMdd-HHmmss.zip` through the system destination picker.
`environment.txt` carries `schema_version=1`, build/API/heap values and explicit unavailable volume/log
families. `report.txt` explains those limits. No clipboard carrier exists, so section 8.A requires no JSON
envelope. There are no session log files to rotate or truncate.

The writer accepts no user-authored strings, locations, paths, settings dumps or account data. Its allowlist
is the privacy boundary; no arbitrary source enters the archive. Export performs no upload. The bounded
archive and its entries are checked by `DiagnosticBundleTest`; the destination-picker flow needs on-target
coverage before a release conformance claim.
