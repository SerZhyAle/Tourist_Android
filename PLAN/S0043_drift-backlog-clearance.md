# S0043 - drift-backlog-clearance

**Ticket:** S0043
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

Ordered-state clause served: "new/fixed functionality is updated/fixed in them in parallel" - the carry-in direction (FastMediaSorter -> Tourist).

Measured 2026-11-13 by an ad-hoc re-hash of the sources of every row of `docs/port-ledger.jsonl` against `P:\ANDROID\FastMediaSorter_mob_v2` (the official `tools/port-ledger/check-drift.ps1` was NOT run: release tier, CLAUDE.md section 7): 375 rows, 353 in step, 22 changed in FastMediaSorter after the port. 5 of the 22 are covered by a decision pinned to the current source version. 17 are not covered:

- phone: `core/ui/BaseActivity.kt`, `domain/tourist/TouristTripTracker.kt`, `domain/usecase/sos/SaveSosModeUseCase.kt`, `domain/usecase/tourist/ObserveTouristDashboardUseCase.kt`, `domain/usecase/tourist/ObserveWatchTouristHealthUseCase.kt`, `ui/flashlight/FrontFlashlightViewModel.kt`, `ui/settings/helpers/OperationsFlashlightShortcutPermissionManager.kt`, `ui/stopwatch/StopwatchViewModel.kt`, `util/PackageManagerCompat.kt`, `service/PhoneWearListenerService.kt` (the last is ported from the `wearGms` source set);
- watch: `wear/domain/model/WearGeometryMode.kt`, `wear/ui/common/WearCaptionText.kt`, `wear/ui/common/WearListColumn.kt`, `wear/ui/common/WearScreenScaffold.kt`, `wear/util/GridColumnFit.kt`;
- watch face: `watchface/src/main/res/raw/watchface.xml`, `watchface/src/main/res/raw/third_party_notices.txt`.

FastMediaSorter's git log shows the changes on 2026-10-29, 2026-11-11 and 2026-11-12, so some are uncarried for two to three weeks. The content of the changes was not analysed. The numbers will have moved by the time this ticket is worked: re-measure first.

Goal: every uncovered row is either carried (port the new source, `record-port.ps1 ... -Refresh`, with the unit tests that travel with it) or deliberately not carried (`decide-drift.ps1` with a written reason pinned to that source version). A behaviour change that is carried also updates the help text and strings of its surface in every shipped locale in the same edit (see ticket `drift-carry-updates-help-and-docs`). Acceptance is handoff section 9.6: the drift check at the release candidate reports zero uncarried changes without a written decision.

Out of scope: the cadence of the check (ticket `drift-check-between-releases`), changes made on the Tourist side (ticket `local-divergence-ledger-and-carry-back`). FastMediaSorter is read-only from this repository.

