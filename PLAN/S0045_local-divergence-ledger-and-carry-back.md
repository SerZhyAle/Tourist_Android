# S0045 - local-divergence-ledger-and-carry-back

**Ticket:** S0045
**Status:** Draft
**Complexity:** medium - not assessed at capture
**Date:** 2026-11-13

## Captured request and migration scope

Так.
Идея этого приложения была чтобы весь функционал по суб-программы Tourist телефоне (любом андроид устройстве), часах , который реализован в FMS_A был представлен в виде самостоятельных приложений android,  которые могут заинтересовать пользователя, которому не интересен весь остальной функционал FMS_A
Но при этом эти приложения не только выглядят более-менее одинаково, но и развиваются вместеи новый/исправленный функционал обновляется/исправляется в них паралельно, они пересекаются по документации, принадлежат общему семейству программ и связаны канонами и контрактами.
Исследуй всё ли верно в моих мположениях?

Создай такие спецификации в рамках этого продукта, которые приведут его к вышеописанному заказанному состоянию

---
Agent finding (not the owner's words) - written by the agent session of 2026-11-13 that answered the owner's question above. Part of a set of tickets that bring the product to the ordered state; the tracking ticket is `family-parity-umbrella`.

Ordered-state clause served: "new/fixed functionality is updated/fixed in them in parallel" - the other direction (Tourist -> FastMediaSorter) and the build-order rule.

Finding: the port ledger and drift check see one direction only. A ledger row stores the source SHA-256 at port time and nothing about the destination, so an edit made to a ported file on the Tourist side is invisible, and there is no mechanism to hand a Tourist-originated fix or change back to FastMediaSorter, which keeps its own copy of the hike kit until FastMediaSorter S4467 is decided (it waits for Tourist's first public release). FastMediaSorter is read-only from this repository (CLAUDE.md section 1). Tourist-side changes found on 2026-11-13 that have no path back:

- S0019: the camera torch lookup is remembered and the Lights row toggles in place (`TorchController`, which was extracted from FastMediaSorter's `DeviceActionHandler`, so no ledger row maps it one to one);
- S0015: `SettingsToggleRow` became `SettingsCheckboxRow` by BOOLEAN-OPTION 2.0 (recorded only as a ledger decision of 2026-10-27);
- S0037: a local activity collector in the ported gyroscope code (recorded as a ledger decision);
- S0038: the corrected English microtesla unit string.

Owner decision of 2026-11-13, quoted in FastMediaSorter S4560 and in S0037: a new kit capability is realised in FastMediaSorter first and ported here afterwards through the ledger (pairs S4560/S0037, S4524/S0038, S4525/S0039). It is written in no rule of this repository yet.

Goal:
1. Record the destination SHA-256 in the ledger at port and at refresh (a backward-compatible field; the reader keeps every existing row valid) and baseline the existing rows from the git history of each destination, with the limit of that baseline written down.
2. A report of ported files edited on this side since their port or refresh, in the same tool family as `check-drift.ps1`.
3. Every local edit is classified once: `local by design` (a decision with a reason) or `carry back`.
4. `carry back` produces an outbound request pack in the S0028 request-pack form (`PLAN/<ticket>/requests/fms-<topic>.md`: exact change, files, tests, the reason), delivered to FastMediaSorter's own session by the owner and never applied from here; the outbound row stays open until FastMediaSorter applies or declines it, and the report shows open rows.
5. Seed the four examples above.
6. Write the build-order rule (FastMediaSorter first, Tourist ports; paired tickets named with each other) and the carry-back rule into CLAUDE.md section 2.

Related: `drift-backlog-clearance`, `drift-check-between-releases`, `kit-behaviour-contract-proposal` (whether a shared behaviour is better written as a catalog contract than carried by hand). Keep the exit-code contract of `check-drift.ps1`. The self-test is release tier.

