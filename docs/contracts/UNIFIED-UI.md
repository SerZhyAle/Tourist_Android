# Unified UI - pointers

| Id | Version read | Home | Role |
| --- | --- | --- | --- |
| UI-SIZING | 1.1 | `ui-sizing/` | consumer |
| BUTTONS | 2.0 | `buttons/` | consumer |
| BOOLEAN-OPTION | 2.0 | `boolean-option/` | consumer |
| COLLAPSIBLE-GROUP | 1.1 | `collapsible-group/` | consumer |
| SETTINGS-NAVIGATION | 2.0 | `settings-navigation/` | consumer |

Read 2026-10-27. ICON-RENDER 0.24 delegates covered geometry to these contracts; its pointer remains
[ICONOGRAPHY.md](ICONOGRAPHY.md). APP-SETTINGS 0.7 applies only to the reversible settings lifecycle.
WINDOWS-UI and PAGE-STYLE have no applicable product surface.

Implementation and declared variants: [design record](../ui/UNIFIED_UI.md). Migration inventory and deadlines:
S0015, `PLAN/S0015_unified-ui-contracts/`. Source gate: `tools/ui/check-unified-ui.ps1`. Real-control fixture
and frame-commit runner ship only in debug APKs; `tools/ui/run-ui-contracts.ps1` records the device matrix.

Full conformance remains open: SETTINGS-NAVIGATION section 3.3 official site/download, support and repository
resources await S0010 (owner confirmed 2026-10-27). Release-build timing and minimum-platform evidence must
be recorded before the measured adoption gate closes. A debug measurement is not release conformance.
