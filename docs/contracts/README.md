# Contract pointers

One pointer per shared contract this product produces, consumes or adopts: id, version, home, role, and what
this repository owes to stay conformant. The contract text lives only in the shared contracts catalog, which
`CLAUDE.md` locates; nothing here restates it. Cite a contract by id and section, never by link or path.
Versions were read from the catalog registry on 2026-10-02, for the rule-adoption family again on 2026-10-25
(S0011), and for the whole table again on 2026-10-26 (S0016, the 2026-10-17 proposal consolidation) - re-read
the registry, not this table, before work at a boundary. The five unified UI contracts (`UI-SIZING`, `BUTTONS`,
`BOOLEAN-OPTION`, `COLLAPSIBLE-GROUP`, `SETTINGS-NAVIGATION`) apply to this product and are migrated under
ticket S0015 (dated plan in `PLAN/S0015_unified-ui-contracts/migration-plan.md`, deadline 2026-11-08); the
pointer is [UNIFIED-UI.md](UNIFIED-UI.md), with scope and evidence limits recorded explicitly.

**Contracts owned here: none.** The kit's behaviour is written in no catalog contract (handoff section 6a item
3), so the port ledger is the whole guarantee. If a behaviour must be shared with a third product - the SOS
signal or its Data Layer messages, for example - it is written in the catalog first, and a pointer appears here.

| Pointer | Contract | Version | Role | State |
| --- | --- | --- | --- | --- |
| [PACKAGE-VERSIONING.md](PACKAGE-VERSIONING.md) | `PACKAGE-VERSIONING` | 0.2 | consumer (Android profile) | families not registered yet; S0002 |
| [INPUT-PARITY.md](INPUT-PARITY.md) | `INPUT-PARITY` | 0.4 | adopter | declared; S0004 onward; 0.4 read 2026-10-26 (S0016), no new surface binds |
| [ICONOGRAPHY.md](ICONOGRAPHY.md) | `ICON-SET`, `ICON-RENDER` | 0.36 / 0.24 | consumer | the ported screens keep the contract glyphs; usage rows filed with the catalog 2026-10-23 (S0004); `app.help` is drawn by the help button (S0014); 0.17-0.36 / 0.15-0.24 read 2026-10-26 (S0016) |
| [DIAGNOSTIC-REPORT.md](DIAGNOSTIC-REPORT.md) | `DIAGNOSTIC-REPORT` | 0.14 | producer | explicit allowlisted ZIP export from About; S0015 |
| [INSTALL-TRUST.md](INSTALL-TRUST.md) | `INSTALL-TRUST` | 1.1 | adopter, conditional | only if a sideload page ships (decision 4.5) |
| [RULE-ADOPTION.md](RULE-ADOPTION.md) | `REPO-STAMP`, `HARNESS-PROFILE`, `REPO-LAYOUT`, `RULE-DELIVERY` | 0.13 / 0.13 / 0.11 / 0.12 | adopter | adopted in S0001, re-read 2026-10-25 (S0011) |
| [PRODUCT-WEB-PAGES.md](PRODUCT-WEB-PAGES.md) | `PAGE-CONTENT`, `PAGE-STYLE`, `SITE-FAMILY-MAP` | 1.2 / 1.2 / 1.2 | adopter, conditional | only if the owner decides on a site (decision 4.9) |
| [CONTEXT-HELP.md](CONTEXT-HELP.md) | `CONTEXT-HELP` | 0.1 | consumer | F1 and a `?` button on the phone screens, bundled pages in EN, RU and UK, the help map and its gate (S0014); the watch is out |
| [TEST-LAB.md](TEST-LAB.md) | `TEST-LAB` | 0.1 | reporter | report filed 2026-10-23 (S0017), proposed |
| [DOCUMENTATION-QUALITY.md](DOCUMENTATION-QUALITY.md) | `DOC-INTERNAL-QUALITY`, `DOC-EXTERNAL-QUALITY` | 0.11 / 0.13 | consumer, internal corpus only | rules 1-3, 5, 6 held by `tools/docs/check-docs.ps1` since S0029 (2026-10-29), rule 4 by the help gate, rule 7 in fact; the 2026-10-26 exception closed; external not applicable until a site opens (decision 4.9) |

Unified component/navigation contracts: [UNIFIED-UI.md](UNIFIED-UI.md).
