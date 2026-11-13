# S0046 - kit-behaviour-contract-proposal

**Ticket:** S0046
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

Ordered-state clause served: "bound by canons and contracts" - the kit's behaviour is the one thing the family shares that no contract writes down.

Finding: handoff section 6a item 3 and CLAUDE.md section 2 item 3 say the kit's behaviour is written in no catalog contract, so the port ledger is the whole guarantee; `docs/contracts/README.md` adds that a behaviour that must be shared with a third product is written in the catalog first. The ledger only carries file content by hand and, with two products changing the same behaviour (see `drift-backlog-clearance`, `local-divergence-ledger-and-carry-back`), is the weakest binding in the family. Candidates for a catalog contract, none judged here:

- the SOS signal and its Data Layer messages (paths, `SosMode` payload names); the paths' `/fms/` prefix was replaced by this product's prefix at port time, so a Tourist watch and a FastMediaSorter phone do not talk to each other - whether that is intended is an owner question;
- the water flashlight's rule (no wet touch can switch it off), the product's signature;
- the persisted dashboard tile names (a tile name is stored as the focused tile and must never move);
- the unit-system hand-over to the watch.

Goal:
1. An owner-input round, asked once in question-with-options form (section 3.3 of the ticket): which behaviours, if any, become a shared contract; whether SOS interoperability between the two products is wanted.
2. If yes: file a catalog proposal through the `contract-sync` skill (invariant 10: the catalog first, never a copy of the text here), add the pointer under `docs/contracts/`, name the conformance evidence (the ported unit tests, the ledger rows), and tell FastMediaSorter's session through the owner (that repository is read-only from here).
3. If no: write the decision, with its reason, into CLAUDE.md section 2 item 3, and close.

Never edit the catalog outside its proposal mechanism. No code change is part of this ticket.

