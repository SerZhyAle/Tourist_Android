# S0051 - fms-kit-functional-parity-inventory

**Ticket:** S0051
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

Ordered-state clause served: "this app will have the whole functionality of the corresponding sub-program of FastMediaSorter".

Finding: nothing in the first set of tickets checks completeness. The handoff section 5 source map was a hand-made class list written on 2026-10-02, and the port ledger records only what was ported. FastMediaSorter's feature inventory `docs/ALL_FEATURES.jsonl` held 1456 records on 2026-11-13; a crude keyword filter (tourist, sos, flashlight, torch, stopwatch, compass, altitude, sunrise, satellite, trip distance, touch lock) over its active, non-Launcher records returned 45, 18 of them in the area `Programs & Tools` and 15 in `Wear OS`. The filter is crude and the list needs a human pass. Some of those records are not named in the handoff map, for example "Tourist weather tiles", "Tourist barometer, trip, navigation and extended weather tiles", "Tourist permission card", "Tourist tally counter", "Tourist inline stopwatch tile", "Tourist compass on phones without a fused orientation sensor", "SOS from the watch reaches a backgrounded phone". Whether each exists in this product is recorded nowhere. Known exclusions already on record: the handoff non-goals (launcher gadgets, a weather service, embedded maps, route tracking) and the open FastMediaSorter defect S4068 (weather and dew-point tiles never populated). One exclusion that contradicts the ordered state as worded: on the watch, the store variant carries no Tourist dashboard (S0008 decision 4.3, owner 2026-10-25, the FastMediaSorter S3178 split kept for it), so the store watch app does not carry the whole kit; only the noLegal watch does.

Goal:
1. A committed parity map (`docs/kit-parity.jsonl`): one row per FastMediaSorter feature record in the kit scope - its id and name, and the Tourist state: `present` (with the surface and the test that proves it), `absent-by-design` (with the handoff non-goal or the owner decision) or `gap` (with the ticket that closes it). Built from the keyword hits plus a human pass over the areas Programs & Tools, Wear OS, Widgets, Permissions, Localization and the kit rows of Settings & Navigation, and cross-checked against the handoff source map and the FastMediaSorter class catalog (`*Tourist*`, `*Sos*`, `*Flashlight*`, `*Stopwatch*`).
2. Every `gap` becomes a ticket - a port, never a rewrite (CLAUDE.md section 2). Every `absent-by-design` row is shown to the owner as one list, so the product's exclusions are explicit and each is either confirmed or turned into a gap.
3. Owner input (section 3.3), asked once in question-with-options form: the store watch variant without the Tourist dashboard. Options: keep it as a recorded exception; lift the split after a Play policy check of the health permissions; ship the dashboard in the store watch without the health tiles.
4. A read-only fast check (`tools/fast-check.ps1 -Area parity`, same exit-code convention as the port-ledger scripts) that reads FastMediaSorter's `ALL_FEATURES.jsonl` and lists active records inside a committed scope manifest (areas, id patterns, spec ids, with reasons for exclusions) that have no row in the map - that is new FastMediaSorter functionality - and exits 1 if any exists. Time it; if it is over 5 seconds it stays release tier.

Related: `new-fms-kit-source-detection` (the same question at the level of source files), `family-parity-release-gate` (wires this check into the release). FastMediaSorter is read-only from this repository.

