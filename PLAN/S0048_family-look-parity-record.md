# S0048 - family-look-parity-record

**Ticket:** S0048
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

Ordered-state clause served: "the apps look more or less the same".

Finding: today the family look is a goal carried by shared UI contracts (UI-SIZING, BUTTONS, BOOLEAN-OPTION, COLLAPSIBLE-GROUP, SETTINGS-NAVIGATION, ICON-SET and ICON-RENDER), not by shared code, and the catalog registry lists no consumer conformance for the five unified UI contracts. In this repository S0015 (adopt the unified UI contracts) is `In Progress`, its dated deadline of 2026-11-08 has passed, and it waits on S0010 and on release-build evidence; `docs/contracts/UNIFIED-UI.md` itself says full conformance is open. Tourist already differs from its FastMediaSorter source in one visible control: a leading checkbox instead of the switch (ledger decision of 2026-10-27, `SettingsToggleRow` -> `SettingsCheckboxRow`). A search of FastMediaSorter's tree on 2026-11-13 found BOOLEAN-OPTION only in `themes.xml` and in its contract documents, no migration of its switch rows. StreamsPlayer's state was not examined. Nothing records which differences between the two products are intended.

Goal:
1. Decide what "the family look" includes beyond the contracts (theme and colour set, app icon language, About screen with the related-products block, navigation shape) - an owner input if the answer is not already in a contract.
2. A committed record under `docs/ui/` listing every surface of the kit as it looks in Tourist against FastMediaSorter (and StreamsPlayer where it has the same surface): `same`, `deliberate divergence` (contract id and decision) or `unintended`. Captures come from the existing UI matrix tooling (`tools/ui`), run at release tier.
3. Every `unintended` row becomes a ticket; every contract-driven divergence that FastMediaSorter has not adopted is raised as an outbound request through the path made by `local-divergence-ledger-and-carry-back`, never edited there.
4. S0015's remaining acceptance stays in S0015; this ticket does not duplicate it and does not close it.

Static inspection and records only between releases; the capture runs belong to the release.

