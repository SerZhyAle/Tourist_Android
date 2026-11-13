# S0015 tactical migration plan - dated milestones

Recorded 2026-10-25 before implementation, per the ticket's execution boundary. Each milestone is
a product-sized slice; the ticket stays open (Partial) until M4's measured proof lands. Deadlines
are working days of 2026; slipping a deadline re-dates this file and the registry migration row,
never silently.

| # | Milestone | Scope | Deadline |
| --- | --- | --- | --- |
| M1 | Shared kit | UI-SIZING tokens (spacing/radius/icon/target families, FMS-mirrored names); command/dialog button radius 4 + `Widget.Tourist.Button.Command`; `SettingsCheckboxRow` (BOOLEAN-OPTION 2.0) replacing the four phone switch rows; `CollapsibleGroup` (COLLAPSIBLE-GROUP 1.1) component; unit tests | 2026-10-27 |
| M2 | Settings surface | `SettingsActivity` with start-side page menu (drawer < 840 dp, persistent rail >= 840 dp - the documented breakpoint); Main page: theme system/light/dark, language (endonyms, per-app locales), unit system; About last: description, version, license, privacy, functional local resources and external-resource dependencies; collapsible groups where About topics justify; start-screen entry | 2026-12-07 (was 2026-10-29; shell landed, official resources wait on S0010) |
| M3 | Surface sweep | remaining BUTTONS geometry on every phone screen (row-height equalization, state-width reservation, overflow policy record), wear boolean rows to the checkbox pattern, watch-face configuration surface check | 2026-11-23 (was 2026-11-03) |
| M4 | Measured evidence | component showcase; captures smallest/ordinary/largest font scale, long translations, narrow/wide, light/dark; contrast measurements; keyboard/D-pad and scroll-versus-tap negative cases; page-switch timings (p95, 20 warm + 20 first-visit samples per destination, named configuration); walk matrix | 2026-12-14 (was 2026-11-07; needs the S0010 release key) |
| M5 | Records close-out | pointers (`docs/contracts/UNIFIED-UI.md`), registry adoption rows advanced only to what was measured, catalog migration row updated/closed, ticket Partial -> Implemented after the evidence gate | 2026-12-21 (was 2026-11-08) |

Permanent variants (design choices, no expiring exception):

- V1 No compact mode or pointer profile is exposed; smallest supported target is 48 x 48 dp touch.
- V2 No in-app interface-size selector; interface scale composes OS density with `dp` tokens and
  independent text enlargement via `sp` + font scale.
- V3 Settings rail glyph policy: monochrome (`colorOnSurfaceVariant`), one treatment for all
  destinations.
- V4 Settings pages: two destinations (Main, About); program parameters keep their editors on their
  program screens; no advanced page while nothing advanced exists.
- V5 Packed collapsed headers: not chosen; groups stack full-width (few groups per screen).
- V6 About ships local license/privacy/help, resources, related products and diagnostic ZIP export.
  Official site/support/download/repository resources await S0010, confirmed by the owner 2026-10-27.

Migration debt with a deadline: every M-milestone above that is not yet landed at any moment.
The catalog registry carries one migration row for this product pointing at this file
(deadline 2026-12-21 since the renewal below, renewed only by re-dating both).

## Renewal - 2026-11-12 (Tourist_mob S0040)

The 2026-11-08 deadline passed with M2 (official resources), M3 (on-target sweep) and M4 (release evidence)
open. The registry row was renewed explicitly, not closed, and the milestone dates above were moved with it.
Remediation ticket stays S0015; the owner-dependent half waits on S0010.

- Fresh reasons: official site/download, support and repository pages (SETTINGS-NAVIGATION section 3 rule 3)
  are the owner's S0010 decisions; the release-build and minimum-configuration evidence (section 5 rule 4) needs
  the S0010 release key; the last recorded debug capture log (`evidence/ru-dark-narrow-large.log`, 2026-10-27)
  ends in FAIL "commands do not stretch" with no passing re-run recorded.
- Catalog 2.1 read 2026-11-12: BUTTONS 2.1 section 9 (web touch naming) and SETTINGS-NAVIGATION 2.1 items A-D
  move no open item into a permitted variant. Item B is already held by the 840 dp drawer (V4 and
  `docs/ui/UNIFIED_UI.md`); item C finds no Wear settings page; item D has no child row here.
- V6 is two things: the local license/privacy/help/resources/export half is a permanent shape; the official-resource
  half is migration debt under M2, not a permanent variant.
- If the owner's S0010 decisions are not in by 2026-12-07, the row is renewed again with that reason.

## Execution checkpoint - 2026-10-27

| Milestone | Current result | Gate |
| --- | --- | --- |
| M1 | Four switch uses replaced; checkbox, group and flow components with behavioral tests | Ten targeted tests, zero failures; source gate PASS |
| M2 | Main/About shell, theme/language/units/screen-awake editors, local resources and diagnostic ZIP | External resource set blocked by S0010, owner confirmed 2026-10-27 |
| M3 | Phone command flows and state-width reservation, Wear checkbox/command presentation; face applicability checked | Final build and on-target sweep in progress |
| M4 | Debug fixture, pointer/key checks, contrast/geometry and real frame-commit timing runner | Preliminary debug measurements; full release/minimum-configuration gate remains open |
| M5 | Contract pointer and design record written | Adoption/migration closure waits for M2/M4; deadline unchanged |

No release, publication, commit or push is part of this session. The document registry machinery remains
S0029's existing scope; affected maintained records are the README, UI design, contract pointers and the
generated help family. The origin handoff, release channels and frozen identities are unchanged.
