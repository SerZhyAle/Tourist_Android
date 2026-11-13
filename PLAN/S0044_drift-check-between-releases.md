# S0044 - drift-check-between-releases

**Ticket:** S0044
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

Ordered-state clause served: "developed together, updated in parallel" - the cadence of the carry-in direction.

Finding: the standing law (CLAUDE.md section 2, item 2) runs the drift check only at the start of every release. Between releases a change in FastMediaSorter is invisible here: on 2026-11-13, 17 changed sources had no decision, some for two to three weeks, and nothing had reported them. CLAUDE.md section 7 files the drift check under the release tier because its run time is unmeasured. An ad-hoc re-hash of all 375 ledger sources in one PowerShell call finished within a few seconds, so a between-releases form is probably feasible; that was not measured with the process-inclusive Stopwatch the section requires.

Goal:
1. Measure `tools/port-ledger/check-drift.ps1` process-inclusive (several runs) and record the figure where CLAUDE.md section 7 keeps its measurements.
2. If it is 5 seconds or less, expose it as a fast check (`tools/fast-check.ps1 -Area drift`, read-only, same exit-code contract as the script), name it in CLAUDE.md section 7 as a between-releases rung, and say what a ticket audit records when it reports uncarried rows. If it is over 5 seconds, design a cheaper form first (for example compare only sources whose FastMediaSorter commit is newer than the last recorded check) and measure that.
3. Decide when it runs between releases (the end of each ticket that touches ported files, the start of a working session, or both) and write the rule in CLAUDE.md section 2.
4. The release-time run stays as it is (release preflight).

Constraints: read-only on FastMediaSorter; the scripts under `tools/port-ledger/` are this repository's own; the port-ledger self-test is release tier. No full build in this ticket.

Related: `drift-backlog-clearance` (clears the rows that exist today), `drift-carry-updates-help-and-docs` (what the report should also print).

