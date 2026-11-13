# S0040 - renew-or-close-expired-unified-ui-exception

**Ticket:** S0040
**Status:** Implemented
**Status note:** Implemented 2026-11-12, no compile command (no compiled source edited), not built, not tested: the Tourist Android unified-UI migration row in the catalog registry is renewed explicitly (not closed) to 2026-12-21 with fresh reasons, remediation ticket S0015 and a re-dated M2-M5 plan; replica of the CTR-EXPIRE rule finds 0 expired rows, fast docs check exit 0. Release: run check-contracts.ps1 (exit 0 expected). Assumed: until date, milestone dates, adoption rows left untouched - owner may overrule.
**Complexity:** low - rewritten at the spec pass (2026-11-12): one catalog registry row, one plan file re-dated and one progress line; no compiled source, no new file, no contract amendment; the only judgement is the new `until` date, recorded below as assumed
**Date:** 2026-11-12

## Captured request and migration scope

Agent-noticed finding, canon session, 2026-11-12.

`tools/check-contracts.ps1` is red on exactly one error, and it is this project's: `CTR-EXPIRE _meta/REGISTRY.md` - the migration exception "Tourist Android (Phone and Wear OS, watch face when applicable)" for UI-SIZING 1.1, BUTTONS 2.0, BOOLEAN-OPTION 2.0, COLLAPSIBLE-GROUP 1.1, SETTINGS-NAVIGATION 2.0 and ICON-RENDER 0.24 expired on 2026-11-08. The row says "Deadline retained without renewal" and names ticket S0015 (`PLAN/S0015_unified-ui-contracts/`) as the remediation.

A project edits its own exception rows, so the catalog session did not touch it. Decide and write one of two things in the registry row, with today's date:

1. Close it: the M1-M5 migration is done and the adoption rows name the real versions, or
2. Renew it explicitly with fresh reasons, a new `until` date and a named remediation ticket (`_meta/VERSIONING.md` section 8). Note the catalog moved today: BUTTONS is 2.1 (section 9, touch naming), SETTINGS-NAVIGATION is 2.1 (items A-D: a panel inside another screen, a narrow viewport, Wear OS single-column list, a child row that only names a block may carry no glyph), UI-SIZING 1.1 and COLLAPSIBLE-GROUP 1.1 unchanged - read them before choosing, the Wear OS list and the glyph-less child row may turn parts of the migration debt into permitted variants.

Why it matters: hub ticket S0005 (test-lab report) cannot reach its done-when `check-contracts` exit 0 until this row is closed or renewed.


## Spec pass (2026-11-12, unattended queue pass)

Rung: complex-lite - the ticket keeps its own record because the edit lands in the shared catalog (the product's
own exception row, `_meta/VERSIONING.md` section 8, `_meta/REGISTRY.md` header: a project edits its own rows), but
nothing compiled changes. Shared-contract boundary: touched only as the product's own exception row; no amendment,
version bump or conformance vector is involved, and no adoption row advances. No research item is open and no
owner input is needed: the choice between the two routes follows from facts in the tree.

### Decision - close or renew

Close is false and is refused. Its condition (the M1-M5 migration is done and the adoption rows name real
versions) does not hold:

- S0015 is `In Progress`; its adoption rows (`UI-SIZING`, `BUTTONS`, `BOOLEAN-OPTION`, `COLLAPSIBLE-GROUP`,
  `SETTINGS-NAVIGATION` of Tourist Android) read `pending`.
- The About resource set (official site and download, support, repository) waits on S0010, which is `Draft`; the
  owner retained those decisions there on 2026-10-27.
- M4 evidence is debug-only (2026-10-27); SETTINGS-NAVIGATION section 5 rule 4 asks for the release build and the
  minimum configuration, and the release key belongs to S0010. The last recorded capture log
  (`PLAN/S0015_unified-ui-contracts/evidence/ru-dark-narrow-large.log`) ends in FAIL "commands do not stretch".

Renew is chosen. The catalog 2.1 amendments were read before choosing (BUTTONS section 9, SETTINGS-NAVIGATION items
A-D): BUTTONS 2.1 is a web amendment and Tourist has no web surface; item B (portrait phone = narrow viewport,
drawer) is already the product's 840 dp drawer; item C (Wear OS single-column list) has nothing to bind because a
file-name search of `wear/` finds only data-layer preference stores, no settings page; item D (glyph-less child row)
has no child row, V4 having no child level. None moves open debt into a permitted variant, so the debt list is
unchanged. UI-SIZING 1.1 and COLLAPSIBLE-GROUP 1.1 are unchanged in the catalog.

Written: the `Tourist Android unified UI migration` row of `_meta/REGISTRY.md` (contracts column now names BUTTONS
2.1 and SETTINGS-NAVIGATION 2.1; renewal paragraph with three fresh reasons, the re-dated plan, the impact; `Until`
2026-12-21; the sentence "Deadline retained without renewal" removed). `PLAN/S0015_unified-ui-contracts/migration-plan.md`
re-dated (M3 2026-11-23, M2 2026-12-07, M4 2026-12-14, M5 2026-12-21; the plan said the two are renewed only
together) with a dated renewal section; one progress line added to S0015. The remediation ticket stays S0015.

### Assumed (unattended, 2026-11-12)

- **Until date 2026-12-21.** Taken because it is the final-slice date the sibling StreamsPlayer Android row already
  carries for its own migration, and it leaves five weeks for the owner's S0010 decisions and one measured release
  pass. Alternative weighed: 2026-12-07 (four weeks, the date at which the S0010 decisions must be in); the row
  states that 2026-12-07 is the point where it is renewed again if they are not. Owner may overrule.
- **Milestone order and dates** (M3 2026-11-23, M2 2026-12-07, M4 2026-12-14, M5 2026-12-21). Taken: on-target sweep
  first (needs no owner input), About resources next (owner decisions), release evidence after the key, close-out
  last. Alternative: one date for all four. Owner may overrule.
- **Adoption rows left untouched** (the five `pending` rows, two of which still say "target 2.0"). Taken: this pass
  read the 2.1 amendments, not the contracts in full, and adoption advances only after measured proof (the plan,
  the registry header). Alternative: rewrite the two target texts to 2.1 now. Owner may overrule.

### Done criteria

- [x] The Tourist Android migration row has `Until` after today and names its remediation ticket (replica of the
      `CTR-EXPIRE` rule over the whole registry: 0 expired rows, the row is 4 cells of 4).
- [x] `migration-plan.md` carries the same four dates as the row (grep: 6 hits of the four new dates).
- [x] `tools/fast-check.ps1 -Area docs` exits 0.
- [ ] RELEASE - the catalog gate itself: `pwsh -NoProfile -File P:/WEB/sza-unified-rules/tools/check-contracts.ps1`,
      expected exit 0 with no `CTR-EXPIRE` finding. Not run in this pass: the script has no recorded wall time here.

## Last Audit

**Date:** 26-11-12 | **Mode:** static | **Outcome:** Verified (static rungs; the catalog gate run is RELEASE)
**Counts:** PASS 5 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 2 | RELEASE 1

- PASS - row 1476 of `P:\Contracts\_meta\REGISTRY.md`: `Until` 2026-12-21, 4 cells of 4 (PowerShell replica of the
  `CTR-EXPIRE` rule, check-contracts.ps1 lines 107-115; expected `Until` > 2026-11-12 | actual 2026-12-21).
- PASS - same replica over every `Until` column of the registry: expired rows expected 0 | actual 0. (The replica
  also counted 10 rows with an unequal cell count at lines 795, 796, 809, 872, 873, 919, 920, 1057, 1070, 1078; none
  is this product's and none is expired - a limit of the replica's naive split, not a finding of this pass.)
- PASS - `migration-plan.md` dates equal the row's: grep of 2026-11-23, 2026-12-07, 2026-12-14, 2026-12-21 expected
  6 | actual 6; the S0015 ticket carries the renewal progress line.
- PASS - `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs`: exit 0, `docs-check: PASS (5 checks)`, 1.9 s.
- PASS - probe sweep: grep `S0040` in `phone/`, `wear/`, `watchface/` expected 0 | actual 0.
- EXEMPT - compile gate: no compiled source edited; `.sza-profile.json` declares no `commands.compileCheck`, so this
  is "no compile command", and nothing was built or tested.
- EXEMPT - contract boundary: the product's own exception row only (VERSIONING section 8); no amendment, no version
  bump, no adoption row advanced.
- RELEASE - `pwsh -NoProfile -File P:/WEB/sza-unified-rules/tools/check-contracts.ps1` - exit 0 expected; run by the
  hub S0005 test-lab report (its done-when) or the next canon session.

### Action items
1. None in this repository. S0015 stays open for M2-M5 on the new dates; S0010 owns the owner decisions it waits on.

### Manual / on-target
- None for this ticket.
