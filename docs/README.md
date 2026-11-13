# docs - index

- [BOOTSTRAP_HANDOFF.md](BOOTSTRAP_HANDOFF.md) - the starting brief of 2026-10-02: product definition, owner
  decisions, extraction source map, section 6a (staying in step with FastMediaSorter). Written by the
  FastMediaSorter session of its ticket S4070 before this repository's first commit; not edited from this
  repository's sessions. Its open items now live as tickets in `PLAN/`.
- [contracts/](contracts/README.md) - one pointer per shared contract this product produces, consumes or adopts.

- [UI design](ui/UNIFIED_UI.md) - product profiles, command/boolean/group choices, settings and resources.
- [play/](play/PLAY_LISTING.md) - Google Play: the listing texts ([en](play/PLAY_LISTING.md),
  [ru](play/PLAY_LISTING-ru.md), [uk](play/PLAY_LISTING-uk.md)), the App content, Data safety and IARC answers with the
  console record ([PLAY_DATA_SAFETY.md](play/PLAY_DATA_SAFETY.md)), and the graphics `play/icon-512.png` and
  `play/feature-graphic-1024x500.png`, render targets of `tools/store/render-store-assets.ps1`.
- `github/social-preview-1280x640.png` - the repository's social preview, a render target of the same script, uploaded
  in the repository settings by the owner.
- The hosted privacy pages are hand-written HTML in `site/` (`privacy.html`, `privacy-ru.html`, `privacy-uk.html`),
  checked by `tools/site/check-site.ps1` and published by `.github/workflows/pages.yml`.

Tickets and the release package plan live in `PLAN/` (overlay B spec home), not under `docs/`.
