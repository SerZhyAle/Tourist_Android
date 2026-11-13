# S0038 - tourist-magnetometer-panel

**Ticket:** S0038
**Status:** BlockExternal
**Status note:** Phone target with magnetometer and target without sensor are missing for on-device availability, live updates, and lifecycle verification.
**Date:** 2026-11-09

## Captured request and migration scope

for Tourist sub app the panels Magnetometer showing the realtime μΤ. After its realisation - add the ticke to Tourist idependet mobile android application

## Existing implementation and follow-up

The phone dashboard already contains a magnetic-field tile fed by live magnetometer readings. Verify the tile on a device, including sensor availability, updating and lifecycle behavior. Correct the English microtesla unit in phone/src/main/res/values/strings.xml: it currently displays an extra Cyrillic character before µT. Check the localized unit resources for the same defect.

Source request: FastMediaSorter S4524.

## Triage and progress - 2026-11-11

Rung: fix. The known defect is one incorrect character in an existing string; the ticket also requires
on-device evidence for the existing dashboard tile. No shared-contract boundary is changed.

- Corrected the English unit to `µT`; the Russian and Ukrainian resources use `мкТл`.
- Static inspection found the magnetic sensor flow, magnitude calculation, dashboard state propagation,
  availability gate, listener cleanup and a five-second subscription stop timeout. These are not on-device
  evidence. The dashboard help already covers sensor-dependent tile visibility; no new product behavior or
  documentation surface is introduced by the unit correction.
- `adb devices -l` on 2026-11-11 listed two Wear OS emulators and no phone. A phone with a magnetometer and
  a phone or emulator without one are needed to observe both capability paths. On the sensor-equipped phone,
  check that the tile displays `µT`, changes when the field changes, stops collecting after the dashboard
  is stopped (allow the five-second timeout), and resumes when reopened. Check the missing-sensor device
  does not offer the tile. Record device models, observations and date here.
- RELEASE: `pwsh -NoProfile -File tools/release/preflight.ps1` builds all modules and runs their unit suites;
  complete the device checks in `tools/release/README.md`. Not built, not tested until the release.
- Fast evidence, 2026-11-11: `pwsh -NoProfile -File tools/fast-check.ps1 -Area ui` exited 0,
  `unified-ui-source: PASS`; `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs` exited 0,
  `docs-check: PASS (5 checks)`. Resource inspection found `µT`, `мкТл`, `мкТл` in English, Russian,
  Ukrainian respectively. `rg -n 'S0038' phone/src wear/src watchface/src` exited 1 with no matches:
  no ticket probe is present.
