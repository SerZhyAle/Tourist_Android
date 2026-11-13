# S0053 - family-parity-release-gate

**Ticket:** S0053
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

Ordered-state clause served: all of them, as a standing condition rather than a one-time cleanup. The other tickets of the set bring the product to the ordered state once; without a gate the state decays the first time FastMediaSorter or the catalog moves.

Finding: the release preflight (CLAUDE.md section 7) runs the drift check, the script gates and the build, but nothing that checks the other clauses: feature completeness, contract versions, or independence. The handoff acceptance items that make the product "standalone" - section 9 item 1 (installed with FastMediaSorter not installed, the whole kit works), item 4 (installs and updates independently of FastMediaSorter) and item 5 (building and releasing needs no FastMediaSorter build) - have no recorded step: `tools/release/README.md` and S0010 contain no mention of FastMediaSorter being absent or present on the device (searched 2026-11-13).

Goal: wire the following into `tools/release/preflight.ps1` as release-tier gates that fail red, with one written PASS or FAIL line naming the version judged (invariant 5):
1. Drift: zero uncovered rows (tools of `drift-backlog-clearance` and `drift-check-between-releases`).
2. New sources: zero `NEW SOURCE` without a decision (`new-fms-kit-source-detection`).
3. Feature parity: no active FastMediaSorter kit record without a row in the parity map, and no open `gap` row without an owner-accepted exception (`fms-kit-functional-parity-inventory`).
4. Contract pointers: each pointer's version equals the catalog registry's, or a dated exception exists (`contract-pointers-reread-and-conformance`); read-only on the catalog.
5. Help and docs mapping of `drift-carry-updates-help-and-docs`: every carried row with an affected page has that page current in every shipped locale.
6. Independence, static: the release build and the unit suites read no FastMediaSorter file (only the drift scripts do, read-only).
7. Independence, device half, written into `tools/release/README.md`: (a) install the release packages on a device or emulator with FastMediaSorter absent and walk every kit surface; (b) install them beside FastMediaSorter (the application ids differ) and check there is no clash in notifications, widgets, the Data Layer or the torch; (c) update a prior Tourist install, not only a fresh one (invariant 6).

Gates 1 to 6 are fast static checks measured one by one; whichever is over 5 seconds stays release tier. Nothing here runs between releases except the checks the other tickets already put there. Related: S0010 (release pipeline dry run) owns the preflight; this ticket adds gates to it and does not replace it.

