# S0052 - new-fms-kit-source-detection

**Ticket:** S0052
**Status:** Draft
**Complexity:** medium - not assessed at capture
**Date:** 2026-11-13

## Captured request and migration scope

Так.
Идея этого приложения была чтобы весь функционал по суб-программы Tourist телефоне (любом андроид устройстве), часах , который реализован в FMS_A был представлен в виде самостоятельных приложений android,  которые могут заинтересовать пользователя, которому не интересен весь остальной функционал FMS_A
Но при этом эти приложения не только выглядят более-менее одинаково, но и развиваются вместеи новый/исправленный функционал обновляется/исправляется в них паралельно, они пересекаются по документации, принадлежат общему семейству программ и связаны канонами и контрактами.
Исследуй всё ли верно в моих мположениях?

Создай такие спецификации в рамках этого продукта, которые приведут его к вышеописанному заказанному состоянию

правильно ли я понял, что по производству всех заказаных задач
это приложение будет иметь весь функционал по соответствующей суб-программы телефона (любом андроид устройстве), часах , который реализован в FMS_A был представлен в виде самостоятельных приложений android,  которые могут заинтересовать пользователя, которому не интересен весь остальной функционал FMS_A
Но при этом эти приложения будут выглядеть более-менее одинаково, развиваться вместе и новый/исправленный функционал обновляется/исправляется в них паралельно, они пересекаются по документации, принадлежат общему семейству программ и связаны канонами и контрактами.
Попровб спецификации чтобы было именно так

---
Agent finding (not the owner's words) - written by the agent session of 2026-11-13 after the owner's second message above. Part of the set tracked by `family-parity-umbrella`.

Ordered-state clause served: "new functionality is updated in them in parallel" - the part of it that the first set of tickets could not see.

Finding: `tools/port-ledger/check-drift.ps1` reports only `DRIFT`, `SOURCE GONE` and `NO DEST`, all for rows that already exist in the ledger (read 2026-11-13: the script holds no code that enumerates FastMediaSorter files). A class that FastMediaSorter adds to a kit package after the port - a new file, a new test, a class split out of an existing one - has no row, so the drift check cannot see it. New FastMediaSorter functionality therefore stays invisible until someone notices it; the gyroscope panel (FastMediaSorter S4560 -> S0037) reached this repository only because the owner filed the pair by hand. Tickets `drift-backlog-clearance` and `drift-check-between-releases` cover changed files only.

A second blind spot: seam files. Part of a FastMediaSorter file was ported into a new class here (the camera torch was extracted from `DeviceActionHandler` into `TorchController`), so a later fix in the part that was ported is a change to a file the ledger may not watch at all.

Goal:
1. A committed kit-scope manifest of FastMediaSorter source paths and globs the kit draws from (derived from handoff section 5 and from the directories of the sources the ledger already holds), with explicit exclusions, each with a reason.
2. The drift tooling reports `NEW SOURCE <path>` for every file inside the scope that has no ledger row and no exclusion, with the same exit-code contract as today (1 when any uncovered row exists). Resolution of a `NEW SOURCE` is one of: port it (`record-port.ps1`) or exclude it with a decision pinned to the file's hash.
3. Seam files: a way to watch a source that was ported only in part (a ledger row of kind `partial` naming the destination class and the ported region, or an equivalent), so a later change to it is reported as drift with a note that only part of it is ours.
4. The first run will be noisy; the manifest is tuned until every hit is a decision, and the result is written down.
5. The self-test (`tools/port-ledger/test-port-ledger.ps1`) covers the new status; it is release tier. Time the new run and, if it is within 5 seconds, include it in the between-releases rung of `drift-check-between-releases`.

Related: `fms-kit-functional-parity-inventory` (the same question at the level of feature records), `family-parity-release-gate`. FastMediaSorter is read-only from this repository.

