# CONTEXT-HELP - pointer

| Field | Value |
| --- | --- |
| **Id** | `CONTEXT-HELP` |
| **Version** | 0.1, draft (registry, read 2026-10-25) |
| **Home** | shared contracts catalog, `in-app-help/` |
| **Owner** | shared |
| **Role here** | consumer - F1 and a `?` button on the product's screens and dialogs, a help page for each, a help map and a gate |

What this repository owes:

- **The ask (rules 1 to 3).** F1 on a connected keyboard and the `app.help` button of the top app bar open the
  same help through one resolver; F1 is never a system-wide chord. The `?` only adds to a corner and displaces no
  control. The screens whose touch must never be spent (the water flashlight, the front flashlight) wire neither,
  and the map says why (owner decision of S0014).
- **The answer (rules 5 to 13).** Pages only - the `layer` and `both` forms are not built. Bundled pages in the
  package assets, one per surface per shipped locale, shown in a full-screen dialog over the live screen
  with script off and the network blocked; the screen below keeps its lifecycle, so a running signal, torch
  or stopwatch is never stopped by asking.
- **The map (rule 17).** `phone/src/main/assets/help/help-map.json`, one row per surface, read by the resolver and
  by the gate. Prose in `docs/help/src`, generated pages and screenshots in the assets, tools in `tools/help`
  (README there). Named in `CLAUDE.md`.
- **The gate (rule 19).** `tools/help/check-help.ps1` prints one `CHECK-VERDICT` line; accepted debt lives in
  `docs/help/baseline.json`, which may only fall.
- **Ship together (rule 18).** A change to what a screen shows changes its prose and its capture in the same edit,
  in the locales the author writes; the gate fails on a capture older than the layouts or strings it shows.
- **Not owed:** the watch (S0008 and S0009), a first-run tour, single-control tooltips, the site and portal
  structure. The product's own wording is its own; nothing of the contract text is restated here.

Adoption state is recorded in the catalog registry (row `CONTEXT-HELP`, Tourist Android).
