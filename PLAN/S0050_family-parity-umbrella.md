# S0050 - family-parity-umbrella

**Ticket:** S0050
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
Agent finding (not the owner's words) - written by the agent session of 2026-11-13; rewritten the same day after the owner's second message above. This is the tracking ticket of the set; it implements nothing itself and closes when every clause below has its evidence.

**Reading of the ordered state (agent's; the owner corrects it if wrong).** "This application" is one of a family of standalone Android apps, each extracted from one sub-program of FastMediaSorter; this repository is the app of the hike kit (the Tourist dashboard, SOS, the lights, the stopwatch, on phone and watch). The plural in the owner's text is the family. The question the first version of this ticket asked - one kit or several apps - is withdrawn on that reading. If the owner meant splitting the kit itself into several apps, that is a new ticket and it changes the frozen anchors of CLAUDE.md section 3.

The ordered state, clause by clause, with the tickets that carry it. Statuses as read on 2026-11-13.

1. **The whole functionality of the corresponding FastMediaSorter sub-program, on phone and watch, as a standalone app that works without the rest of FastMediaSorter.**
   - Completeness: S0051 `fms-kit-functional-parity-inventory` (feature-by-feature map against FastMediaSorter's feature inventory; includes the one known exclusion, the store watch without the Tourist dashboard, as an owner decision) and S0052 `new-fms-kit-source-detection` (new FastMediaSorter files and seam files, which the drift check cannot see today).
   - Standalone: the device half of S0053 `family-parity-release-gate` (installs and works with FastMediaSorter absent, coexists with it, updates independently).
   - Reaching users: S0010 (release pipeline dry run), S0025 (GitHub publication), S0027 (Google Play publication), S0012 (promotion), all `Draft`. Nothing is published yet, so this clause is unproven until one of them ships.
2. **The apps look more or less the same.** S0015 (unified UI contracts, `In Progress`, its 2026-11-08 deadline has passed) and S0048 `family-look-parity-record`.
3. **They develop together; new and fixed functionality is carried in parallel.**
   - S0043 `drift-backlog-clearance` - the 17 changed FastMediaSorter files with no decision.
   - S0044 `drift-check-between-releases` - the cadence of the drift check.
   - S0045 `local-divergence-ledger-and-carry-back` - Tourist-side changes, the way back to FastMediaSorter, and the build-order rule (FastMediaSorter first, Tourist ports; owner decision of 2026-11-13).
   - S0046 `kit-behaviour-contract-proposal` - whether any shared behaviour becomes a catalog contract.
   - S0051 and S0052 - new functionality and new files.
   - Outside this repository: FastMediaSorter S4467 (the kit's fate there, waits for Tourist's first public release) is that repository's decision.
4. **They overlap by documentation.** S0049 `drift-carry-updates-help-and-docs` (a carried behaviour change updates its help and strings); the cross-links and the site are S0026 and S0028 (`Draft`).
5. **One family, bound by canons and contracts.** S0047 `contract-pointers-reread-and-conformance` (eleven pointers are behind the catalog; the canon stamp is behind the installed plugin); S0028 for the family map.
6. **All of the above stays true.** S0053 `family-parity-release-gate`: the checks of clauses 1, 3, 4 and 5 and the independence proof become release-blocking gates with one PASS or FAIL line naming the version.

**What the set guarantees and what it cannot.** It guarantees that, at every release, this product has no unseen difference from FastMediaSorter's kit: every change, every new file and every new feature record is carried or excluded with a written reason, contract versions are current, and independence is proven on a device. It cannot make the two products change at the same moment: FastMediaSorter is read-only from here, carrying is done by a session, and the lag is bounded by the release gate, not by zero. Parallel development in the strict sense would need shared code or a behaviour contract; CLAUDE.md section 2 rules the first out, and S0046 puts the second to the owner.

Suggested order, not binding (the owner orders `PLAN/RELEASE_QUEUE.md`): S0047 and S0043 first, because both are stale today and need no design; S0051 and S0052 early, because their findings become new port tickets; S0044 and S0045 next, they change the tools S0043 uses; S0049 after S0044; S0053 after the tools it wires exist; S0046 after the owner answers its input round; S0048 after S0015's evidence exists.

Acceptance: each of the six clauses has a closed ticket or a recorded owner decision; the parity map of S0051 has no unexplained row; the gate of S0053 prints PASS at the release candidate.

Not in this ticket: any code, any edit of FastMediaSorter or StreamsPlayer (read-only from here), any tag, upload or publish.
