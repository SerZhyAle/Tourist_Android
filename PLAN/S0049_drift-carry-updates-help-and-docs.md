# S0049 - drift-carry-updates-help-and-docs

**Ticket:** S0049
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

Ordered-state clause served: "they overlap by documentation".

Finding: the documentation of the two products overlaps only by format (the same contracts), not by content. Of the 375 ledger rows only 2 map resource files; strings and help prose are authored here (`docs/help/src/*.json`, mapped to screens by `phone/src/main/assets/help/help-map.json`), and nothing links a ported behaviour (a ledger row) to the help page, string or README sentence that describes it. When a behaviour change is carried from FastMediaSorter, the help of its surface can go stale without any gate noticing: the help gate checks the map and the freshness of captures, not behaviour (invariant 17 asks for the user-facing text to land in the same edit, in every shipped locale: en, ru, uk). Cross-links between the products' documentation are the business of S0026 and S0028 and are not repeated here.

Goal:
1. A mapping from ledger rows (or groups of rows) to the help pages, string names and README or listing sentences they affect, kept next to the help map or the ledger and read by the tools that already read them.
2. The drift report prints `affected help: ...` for every uncarried row, so the carry list is also the documentation to-do list.
3. The carry procedure (CLAUDE.md section 2 and `tools/port-ledger/README.md`) states that updating the affected help and strings, in every shipped locale, is part of carrying a behaviour change, and a carry without it is not complete.
4. Decide in the plan whether FastMediaSorter's own help wording for the same behaviour is used as a read-only reference when a text has to change (no copying across; reuse of decisions, not of text).
5. The help gate (`tools/help/check-help.ps1`) or the docs check learns the new mapping: a mapped row with no page, or a page named by no row where one is expected, fails.

Prose of user-facing text stays present-tense shipped fact. Between releases only the fast gates (`fast-check.ps1 -Area help`, `-Area docs`, `-Area strings`) run.

