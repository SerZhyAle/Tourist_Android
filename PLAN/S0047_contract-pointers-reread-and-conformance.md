# S0047 - contract-pointers-reread-and-conformance

**Ticket:** S0047
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

Ordered-state clause served: "belong to one family of programs and are bound by canons and contracts" - the binding is only as current as the last time the pointers were read.

Finding, read from `P:\Contracts\_meta\REGISTRY.md` on 2026-11-13 against the pointers in `docs/contracts/` (read 2026-10-25 to 2026-10-27; the catalog was consolidated again on 2026-10-29 and 2026-11-12):

| Contract | Pointer says | Registry says |
| --- | --- | --- |
| PACKAGE-VERSIONING | 0.2 | 0.6 |
| INPUT-PARITY | 0.4 | 0.6 |
| CONTEXT-HELP | 0.1 | 0.3 |
| ICON-SET | 0.36 | 0.39 |
| DIAGNOSTIC-REPORT | 0.14 | 0.15 |
| BUTTONS | 2.0 | 2.1 |
| SETTINGS-NAVIGATION | 2.0 | 2.1 |
| INSTALL-TRUST | 1.1 | 1.2 |
| HARNESS-PROFILE | 0.13 | 0.16 |
| DOC-INTERNAL-QUALITY | 0.11 | 0.12 |
| DOC-EXTERNAL-QUALITY | 0.13 | 0.14 |
| PAGE-CONTENT / PAGE-STYLE / SITE-FAMILY-MAP | 1.2 / 1.2 / 1.2 | 1.5 / 3.10 / 1.12 |

Unchanged since the pointer: UI-SIZING 1.1, BOOLEAN-OPTION 2.0, COLLAPSIBLE-GROUP 1.1, ICON-RENDER 0.24, REPO-STAMP 0.13, REPO-LAYOUT 0.11, RULE-DELIVERY 0.12, TEST-LAB 0.1. The pointers themselves say to re-read the registry before work at a boundary, so the gap is expected; what is missing is a read of what each bump asks of this product. Also: `.sza-canon.json` carries canon version 2026.10.17.1 (reconciled 2026-10-17) while the installed `sza` plugin is 2026.1113.2.

Goal (through the `contract-sync` and `adopt-canon` skills):
1. For every bumped contract, read the change rows between the pointer's version and the registry's, list what the product now owes, and update the pointer file with the new version, the date read and the state.
2. Where the product no longer conforms, either fix it (a ticket) or write a dated exception; nothing is silently stale.
3. PACKAGE-VERSIONING: confirm whether this product's families are registered in the catalog before the first package ships (the pointer says "not registered yet"), and file the amendment catalog-first if not.
4. Re-sync the canon stamp (`adopt-canon` re-sync path) and record it in the canon repository's per-project record only through the canon session's own route.
5. Add the read date to each pointer so the next gap is visible.

Static inspection and registry reads only; no build.

