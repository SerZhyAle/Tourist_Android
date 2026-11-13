# S0039 - tourist-magnetometer-realtime-mbar

**Ticket:** S0039
**Status:** BlockExternal
**Status note:** Phone target with barometer and target without sensor are missing for on-device live mbar updates, interference sentence retention, and unit setting independence observation.
**Date:** 2026-11-10

## Captured request and migration scope

for Tourist sub app the panels Magnetometer showing the realtime mbar. After its realisation - add the ticke to Tourist idependet mobile android application

## Existing implementation and follow-up

The phone dashboard contains a magnetic-field panel. Display the realtime ambient pressure in millibars (mbar) alongside the interference sentence in the detail line of the magnetic-field panel when the device provides barometer readings, keeping the Pressure tile's ADR-4 unit intact. Port the formatting and localized strings from FastMediaSorter S4525.

Source request: FastMediaSorter S4525.

## Specification

**Complexity:** Complex - confirmed at specification and plan: changes span source, localized resources, tests and documentation.

The magnetic-field tile displays the current ambient pressure in millibars beside its existing sensor status when a barometer reading is available. With no pressure reading, the existing status remains alone. Magnetic readings and interference messages remain unchanged. The separate Pressure tile retains its metric and imperial units. The reading updates with the existing dashboard sensor state; no new permissions or sensor lifecycle are introduced. This is a local UI presentation and does not change a shared data contract.

Required research: resolved - the existing dashboard state already contains pressure in hectopascals, equivalent to millibars; FastMediaSorter implements the requested formatting and EN/RU/UK strings. The existing Pressure tile owns its own unit conversion. No owner decision remains open.

Acceptance: the detail line contains a rounded, localized mbar value whenever pressure is present, including while magnetic data is unavailable; without pressure it shows only its prior sensor status. Metric and imperial settings produce the same mbar detail, while the Pressure tile retains its existing conversion. The UI strings and dashboard help cover EN/RU/UK.

## Owner inputs (Approval gate)

- **Related tickets:** S0038 (magnetic-field panel); S0014 (dashboard help).
- **UI placement:** The existing magnetic-field tile detail line, beside its sensor status; omit pressure when unavailable.

## Plan

Coverage: the source port and strings cover the display, fallback, live state and units; formatter and tile tests cover the cases; the docs phase covers user surfaces. Shared contracts and permissions are unchanged. The sensor lifecycle remains the existing one.

1. [x] Port the formatter and magnetic-field detail from FastMediaSorter, including EN/RU/UK resources and relevant tests; refresh ledger rows only after carrying all source changes. Check declarations, resource parity and the fallback directly by source inspection. Files: phone tourist helpers, their tests, phone strings and docs/port-ledger.jsonl.
2. [x] Update README and dashboard help in EN/RU/UK; regenerate the dashboard help pages with the repository tool. Check the sources and generated page content. Files: README.md, docs/help/src/dashboard.json and generated dashboard help pages.
3. [x] Audit the acceptance criteria and record release-tier build, tests, device observation and help capture. Static checks must name their verdicts.

All pre-implementation research and placement decisions are resolved. Plan review: phase 1 consumes existing dashboard state and produces the UI and strings consumed by phase 2; phase 3 consumes both. No forward dependency remains.


## Last Audit

**Date:** 2026-11-11 | **Mode:** static | **Outcome:** Partial
**Counts:** PASS 5 | WARN 0 | FAIL 0 | MANUAL 1 | UNCHECKABLE 0 | EXEMPT 1 | RELEASE 3

### Action items

1. PASS - help gate unchanged by this ticket: `pwsh -NoProfile -File tools/fast-check.ps1 -Area help`, exit 1: expected 8 tracked known-red findings of S0014 (six uk/stopwatch-result bounds, two stale baseline entries); actual `help-check: FAIL (8 finding(s))`, no net-new findings from dashboard help edits. Tracked known-red in CLAUDE.md section 7, not counted as green; scoped gate `pwsh -NoProfile -File tools/help/build-help.ps1 -Pages dashboard` exits 0. The prior BlockByOtherTask misread S0014 as a blocker; S0014 is at BlockNeedUserTest (released).
2. PASS - `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui`, exit 0: expected and actual `unified-ui-source: PASS`.
3. PASS - `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs`, exit 0: expected and actual `docs-check: PASS (5 checks)`.
4. PASS - `pwsh -NoProfile -File tools/help/build-help.ps1 -Pages dashboard`, exit 0: expected and actual `help-build: PASS (1 page(s), 3 locale(s))`.
5. PASS - static source, resource, rendered-help and ledger inspection in this pass, exit 0: expected and actual `static-audit: PASS`. The sensor state feeds the tile; null returns the original sentence; mbar formatting is independent of the separate Pressure tile; three locales and source hashes match. Tests are present, not run.
6. EXEMPT - shared contracts, permissions and sensor lifecycle: no change to their boundaries.
7. RELEASE - Build, unit tests and full gates: `pwsh -NoProfile -File tools/release/preflight.ps1` on the release candidate.
8. RELEASE - Refresh dashboard help screenshots: `pwsh -NoProfile -File tools/help/capture-help.ps1 -Serial <emulator>`, then `pwsh -NoProfile -File tools/help/build-help.ps1 -Pages dashboard` and `pwsh -NoProfile -File tools/help/check-help.ps1`.
9. RELEASE - Device UI matrix: `pwsh -NoProfile -File tools/ui/run-ui-contracts.ps1` as documented in `tools/ui/README.md`.

### Manual / on-target

- [ ] On a built phone app with a barometer, observe that the magnetic-field detail updates in mbar, retains the interference sentence and is independent of the unit setting; also check the no-barometer fallback and screen-reader output. No device observation or build was run in this pass.

Documentation registry: `ticket-specs`, `spec-journal`, `release-plan`, `readme`, `help-sources`, `help-captures` and `port-ledger` are affected; help captures remain for the release generator. Other registry records are unchanged. The app has no remote, site, listing source or separate changelog; authored app and help locales are EN/RU/UK. Blocker tokens are selection exclusions, not enforced closure gates; unanswered-question carry is checked at closure.

### Block recheck - 2026-11-11

- `pwsh -NoProfile -File tools/fast-check.ps1 -Area help`: exit 1; expected 8 findings under S0014 tracked known-red; actual `help-check: FAIL (8 finding(s))`.
- `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui`: exit 0; expected and actual `unified-ui-source: PASS`.
- `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs`: exit 0; expected and actual `docs-check: PASS (5 checks)`.
- `pwsh -NoProfile -File tools/help/build-help.ps1 -Pages dashboard`: exit 0; expected and actual `help-build: PASS (1 page(s), 3 locale(s))`.
- `rg -n 'S0039' phone/src wear/src watchface/src`: exit 1; expected and actual no ticket-id probes outside `BlockNeedUserTest`.
- `adb devices -l`: listed two Wear OS emulators and no phone. Phone target with barometer and target without sensor are missing for on-device live observation.
