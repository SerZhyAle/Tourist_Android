# Unified UI checks

`pwsh -NoProfile -File tools/fast-check.ps1 -Area ui` checks command widths, retired switch rows,
new-string parity, help registration and watch-face applicability, after `tools/strings/check-string-tables.ps1`
(double-encoded text in any string table, S0042). It proves source properties only.

At release, build and install the phone noLegal debug APK, then run:

```powershell
pwsh -NoProfile -File tools/ui/run-ui-contracts.ps1 -Serial emulator-5592
```

Use a dedicated AVD whose name starts with `S0015_`. The runner refuses shared devices. The scenarios in
`scenarios.json` cover three authored locales, light/dark, font scales 0.85/1.0/1.3/2.0, a 320 dp phone,
ordinary phone and a wide layout. Device overrides are restored on exit. The runner writes screenshots,
interaction/geometry/contrast results and raw activation-to-frame samples under `temp/S0015/ui`.
`-Scenario <id>` narrows a run; `-OutputRoot <path>` selects retained evidence.

The debug-only `UiShowcaseActivity` draws the actual shared controls. `UiContractInstrumentation` uses
real pointer/key injection and frame-commit callbacks, never screenshots as a timing estimate. Each timing
scenario contains 20 independent first visits and 20 warm visits per destination. Report nearest-rank p95
(sample 19 of 20), maximum and all outliers. A debug run is explicitly preliminary: the contract also owes
the named release build and minimum supported configuration before adoption can advance.

At release, help screenshots are generated separately with `tools/help/capture-help.ps1`, then `build-help.ps1` and
`check-help.ps1`. Unit checks live in `UnifiedUiComponentsTest` and `DiagnosticBundleTest`.
