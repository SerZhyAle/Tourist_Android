# S0015 - Adopt mandatory unified UI contracts

**Ticket:** S0015
**Status:** Draft
**Date:** 2026-10-17

## Captured request and migration scope

опубликуй (деплой) новые правила
создай во всех подчиненных продуктах тикет/спеку для приведения продукта к соотвествиям новых правил и контрактов

## Product scope

Tourist Android: Phone and Wear toolkit screens, launcher, watch-face configuration surfaces when applicable, and official product web pages. Preserve port provenance; do not modify the extraction source.

## Purpose

Bring this product's user-facing surfaces into conformance with the mandatory shared UI rules published
in canon 2026.10.17.2 (plugin 2026.1017.2), commit 3019753, on 2026-10-17. This is an implementation
migration ticket, not evidence that the product already conforms. The scope is the product identified
in this ticket, including its in-repository editions, companion tools and official website where applicable.

## Contract baseline

Read the actual current catalog text before implementation; cite by contract id and section, not a local
drive link, and retain pointers instead of duplicating the contracts in this repository.

| Contract | Published target |
| --- | --- |
| UI-SIZING | 1.0 |
| BUTTONS | 2.0 |
| BOOLEAN-OPTION | 2.0 |
| COLLAPSIBLE-GROUP | 1.1 |
| SETTINGS-NAVIGATION | 2.0 |
| WINDOWS-UI | 0.9, where Windows UI applies |
| APP-SETTINGS | 0.7, where its settings lifecycle applies |
| ICON-RENDER | 0.24 |
| PAGE-STYLE | 3.0, for applicable official web surfaces |

Canon authority: UI_UX.md sections 2, 5 and 8. Preserve CONTEXT-HELP, localization, consent, privacy,
settings commit semantics, product identities and stored user data. Contract versions may have advanced
before execution; audit the current log and record the actual version implemented instead of assuming.

## Required migration outcomes

1. Inventory the product's actual buttons, boolean options, groups, settings destinations, dialogs and
   official web surfaces. Mark each contract/surface applicable, already compliant with fresh evidence,
   or requiring work; justify non-applicable shapes without pretending that the entire product is exempt.
2. Build/reuse shared components and size tokens. Support stable pointer/touch profiles, compact through
   very large interface sizes and independent text enlargement. Explicit compact mode may reduce hit
   targets even on touch; declare and test this product's smallest supported targets. Keep vector
   geometry sharp, names accessible and hit targets non-overlapping. Never resize on every input event.
3. Ordinary command widths follow content and widest known state; never stretch to container width.
   Equalize each visual row's heights to its tallest member, including multiline captions. Keep primary
   commands visible, wrap rows and allow secondary overflow under a recorded panel policy. Dialog answers
   remain visible. Semantic segmented controls/virtual keyboards may have equal-size parts. Simple obvious
   answers may be text-only; useful substantive commands retain recognizable semantic icons.
4. Boolean options use a leading visible checkbox, compact heading with optional semantic glyph and
   useful explanation. Caption/glyph activation works on every platform, including Android. Recognized
   scrolling cancels activation. Omit redundant explanations; short hover/focus help and full activated
   help share an authored source and never toggle the option. Preserve keyboard/screen-reader access.
5. Groups represent distinct topics, with distinct header treatment and current-state triangles. The
   whole header toggles; content does not. Bodies expand in normal flow across content width, retaining
   values and focus. A single-topic page normally needs no wrapper. Packed collapsed headers are an
   approved per-screen choice; row reflow is allowed while keeping the active header visible and its
   vertical anchor when feasible.
6. Every settings surface retains page navigation even in a small utility: compact start-side rail,
   one child level, short names and one coherent colored/mono glyph policy. Keep main/theme/language
   first, advanced later, About last. Narrow layouts retain the menu through documented adaptation.
   Every product owes the complete About/resource/diagnostic-export set; missing resources/functions
   are implementation work, not invented links or a silent omission. Related products are listed when
   they exist. Navigation preserves user edits, commit semantics and the outer window rectangle.
7. Present complete coherent page frames without flicker or intermediate layout. Warm page switching
   p95 is below 150 ms and first visit p95 below 500 ms on named configurations, with raw samples and
   maximum reported. Investigate outliers; repeated reproducible delays fail. Do not block UI on network
   or other long work, and prevent stale page results from replacing the current destination.
8. Document permanent allowed variants in the product profile/design. For remaining mandatory work,
   prepare a dated, milestone-based migration plan with an explicit deadline before implementation
   approval, and reference it in this product's registry migration/exception entry. Do not reschedule
   existing release packages or renew unrelated expired exceptions merely to create this ticket.

## Evidence and acceptance

- A traceable inventory maps every applicable requirement to implementation and a reproducible check.
- Real component/page captures cover smallest/ordinary/largest sizes, long translations, narrow/wide,
  light/dark and supported high contrast/RTL, plus selected risky combinations. Use baseline plus
  risk-based coverage rather than the full Cartesian product on each edit. Record device/build/locale/scale.
- Fresh geometry, actual contrast and keyboard/accessibility/input checks pass; negative cases include
  scroll-versus-tap, help-versus-toggle, no unwanted operations during navigation, safe destructive defaults,
  repeated activation, focus return, resize/large-text reflow and stale asynchronous results.
- Real timings/traces demonstrate complete-frame performance; screenshots alone do not establish it.
- Product documentation, authored locales, maintained document registry and contract pointers match
  the shipped UI. Only this product's adoption rows advance after measured proof; a plan is not conformance.
- Preserve original ids, file formats, credentials, user configuration and the product's existing release
  and publication boundaries. This ticket does not authorize releasing the product or publishing its site.

## Execution boundary

This ticket is captured as Draft. First reconcile overlapping product tickets and record an ordered,
deadline-bearing tactical migration plan. Then implement and verify in product-sized slices. Do not
reopen the sixteen already answered portfolio design questions; use the current mandatory contract.
Resolve only product-specific applicability/placement decisions not already settled by those contracts.

