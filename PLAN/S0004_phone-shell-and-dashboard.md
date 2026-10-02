# S0004 - Phone shell and the Tourist dashboard

**Status:** Draft
**Phase:** 4 of 10 (handoff section 8)
**Verification level:** device - the dashboard shows altitude, heading, coordinates and sunrise/sunset from a
real fix on a phone with FastMediaSorter not installed; unit tests of the ported classes.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 4, section 5 (phone module: Tourist dashboard, compass, seams),
sections 2, 3, 6a, 7 (INPUT-PARITY, ICON-SET).

## 1. Goal

The phone app opens on the kit: a start screen of its own and the Tourist dashboard with its compass hero tile,
the map and share actions, and the home-screen widget.

## 2. Scope

Source paths relative to `P:\ANDROID\FastMediaSorter_mob_v2` (read-only), `A` =
`app_v2/src/main/java/com/sza/fastmediasorter`.

- Dashboard: `A/ui/tourist/` (`TouristInfoActivity`, `TouristInfoViewModel`, `TouristActionsManager`,
  `TouristHeroTileManager`, `TouristSecondaryTilesAdapter`, `TouristTileValueFormatter`);
  `A/domain/model/tourist/` and `A/domain/usecase/tourist/` (`ObserveTouristDashboardUseCase`);
  `A/widget/TouristInfoWidgetProvider.kt`; layouts `activity_tourist_info.xml` with its `layout-land` twin,
  `item_tourist_tile.xml`, `widget_tourist_info.xml`.
- Compass: not a screen - the `COMPASS` hero tile (`TouristHeroTileManager`), fed by the S0003 orientation
  source and `ObserveCompassUseCase`.
- Seams, each its own step: `A/core/ui/BaseActivity.kt` (keep-screen-awake), `AppSettings`, backup mapping,
  device-profile presets. The program registry that launches the kit inside FastMediaSorter
  (`SubProgramCatalog`, `InternalRouteCatalog`, `AppLaunchPanelRouteIntents`, the programs menu,
  `HomeWidgetCatalog`) does not come along. `NotificationIds` / `NotificationIcons` are this product's own
  registry, not a copy.
- The `enable_tourist` switch exists because the program is optional inside a sorter: keep-or-drop is this
  phase's decision, written down with the stored-state list of handoff section 5.
- Localization EN/RU/UK from the first screen; every user-visible string obeys a communication policy
  (FastMediaSorter's `docs/COMMUNICATION_POLICY.md` is the reference to adapt - adapted here, not copied).
  Accessibility: TalkBack labels, touch targets, state not shown by colour alone. INPUT-PARITY: touch,
  hardware keys, gamepad and TV remote from the first screen.
- Permissions: fine and coarse location (foreground only, platform `LocationManager`, no Google Play services
  for it), `POST_NOTIFICATIONS`, `ACTIVITY_RECOGNITION` for steps (see 3.3, decision 4.3).
- Known defects of the source at hand-off, carried across by the drift check when fixed there: FastMediaSorter
  S4068 (the weather and dew-point tiles are never populated) and S4069 (the dashboard never asks for the
  location or activity-recognition grant; the watch does). Acceptance needs a working grant flow, so how this
  port carries S4069 - wait for the FastMediaSorter fix, or port the watch's existing grant pattern - is
  decided at ticket start and recorded here; never a rewrite of a shipped behaviour.
- Unit tests ported: `TouristInfoViewModelTest`. Uncovered in the source: `ObserveTouristDashboardUseCase`,
  `TouristTileValueFormatter` - ported first, tested here afterwards.
- ICON-SET / ICON-RENDER: decide whether the ported screens keep the contract glyphs; if they do, add this
  product's registry rows in this ticket.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0003 (core port); S0002 (skeleton).
- **Start screen placement (UI decision, open):** this product needs its own start screen, settled with the
  owner before implementation, asked once in question-with-options form right before this phase.
- **4.3 Store-variant composition (open, carried by S0008):** the steps tile needs `ACTIVITY_RECOGNITION`, which
  FastMediaSorter declares in noLegal only. If this phase reaches that manifest line before S0008, the question
  is asked then, once, and the answer is recorded in S0008.
- **4.6 Name and branding (open, carried by S0010):** this phase uses the working name "Tourist" and stand-in
  artwork.

## 4. Acceptance

- [ ] On a phone without FastMediaSorter the app opens on the start screen and the dashboard shows altitude,
      heading, coordinates and sunrise/sunset from a real fix.
- [ ] Map and share actions work; the widget updates.
- [ ] Ported unit tests pass; every ported file has a ledger row; drift check clean or decided.
- [ ] Every action is reachable by touch, hardware keys, gamepad and TV remote (INPUT-PARITY).
