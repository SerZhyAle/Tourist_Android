# S0026 - Document Tourist as part of sza.od.ua: product site and outbound family links

**Status:** Draft
**Status note:** Written 2026-10-26 from the owner's order (section 0). Nothing is built, deployed or published; every outward step waits for the owner's go (section 3.3). The order settles S0010 decision 4.9 in direction (the product gets a site and joins the family); the name, the icon and the staging of the first deploy stay open.
**Phase:** outside the handoff phase plan (handoff section 8); second of the four publication tickets S0025 to S0028
**Verification level:** static predicates on the served site (HTTP status, rendered head and footer checks, a generator staleness check) and on the repository; rendered geometry of the landing at 1280 and 360 px. No hub or sibling repository is touched (S0028).

**Depends on:** S0025

## 0. Source

- **Owner's order, 2026-10-26** (Russian source, quoted): "create specifications ... 0 Document this project as a part of sza.od.ua and links on all related products and portals". Read here as: (1) Tourist has its own documentation site in the family style, reached from the sza.od.ua portfolio; (2) the Tourist surfaces link outward to the related products and to the portals where the product lives - the hub, the other tools of the family, the repository, the store listing when it exists, the trust page of the sideload channel. The inbound side (the hub card and the other products' footers and documentation pointing at Tourist) is S0028.
- Contract pointers `PAGE-CONTENT`, `PAGE-STYLE`, `SITE-FAMILY-MAP` (pointer file `PRODUCT-WEB-PAGES`, role "adopter, conditional until decision 4.9"), `INSTALL-TRUST`, `DOC-EXTERNAL-QUALITY`, `CONTEXT-HELP`. Canon: SITE_CONFIGURATION sections 1, 2, 5, 6; DOCUMENTATION_CONCEPT sections 3, 4, 5 (the shipped-fact rule); the `feature-to-site` skill.
- **Measured 2026-10-26:** `.sza-canon.json` has `site.kind` "none", `privacy.page` null, `channels` empty; no site tree and no generator in this repository; no Tourist row in SITE-FAMILY-MAP 1.9 (eight tools plus the hub); the hub is a hand-authored single page (`index.html` and its byte-identical `embed.html`) on GitHub Pages under the custom domain sza.od.ua. Precedent read-only: StreamsPlayer for Android generates a `site/` tree from `docs/` with its own build script (CLAUDE.md section 1: reuse the decision, never the code).
- S0012 (promotion) owns positioning, listing wording and channels; its step P6 (page heads, JSON-LD, sitemap, mandatory pages) is carried out by this ticket, so S0012 criteria 2, 3 and 11 stop being void.

## 1. Goal

`https://serzhyale.github.io/<name>/` answers in English, Russian and Ukrainian with a landing, a privacy page, a how-to guide and a support page, built from the family kit, ending in the family footer grid and the single contact, every claim true to the shipped product. A stranger who finds the page can tell what Tourist is, whom it is for, how to get it, what it asks of the phone and where else the author's tools are.

## 2. Scope

### 2.1 Non-goals

- The hub card, the SITE-FAMILY-MAP row, the sibling products' footers and articles, the host-root `robots.txt` line: S0028.
- Google Play listing, data-safety form and IARC: S0027 (it fills its forms from this ticket's privacy source).
- Positioning research, topics, social card, demo video: S0012.
- No analytics, tracker, `utm_`, rate-us prompt or invented rating (S0012 section 2.1). No `FAQPage`/`HowTo` markup, no `<meta name="keywords">`.
- Copy obeys S0012 section 2.1 line by line: SOS sounds, blinks and reaches the paired device and sends no SMS and makes no call; the water flashlight is a touch lock, not waterproofing; speed, altitude and heading are sensor and GNSS readings, not a certified instrument; location is foreground-only through the platform `LocationManager`; a store listing advertises only what the `store` variant contains; no competitor name; every text a present-tense shipped fact (invariant 17).

### 2.2 Decisions

- **Order of work, taken by the order's logic:** S0025 (repository) -> this ticket (site) -> S0027 (Play needs the privacy URL) -> S0028 (the family links only a URL that answers, SITE-FAMILY-MAP rule 5). The order's numbering (0 to 3) is not the dependency order.
- **One repository per property** (SITE_CONFIGURATION section 1): the site lives in this repository on GitHub Pages and has no custom domain. Its Pages source is read with `gh api repos/SerZhyAle/<name>/pages`, never assumed.
- **Generator before HTML** (feature-to-site step 1.3): a copy deck and templates in this repository produce the served tree; the served tree is never edited by hand. The kit CSS is the catalog's reference `sza-kit.css`, byte-identical.
- **Locales:** `en`, `ru`, `uk`; the header switcher reads RU / EN / UA and the language codes are `uk` everywhere; `hreflang` reciprocal with `x-default` (PAGE-CONTENT "The landing always carries" item 2; DOCUMENTATION_CONCEPT section 3).
- **Get-it block lists only channels that are live** (no badge, no link, no heading for a channel the product is not on). Stage 1: the repository (S0025). Stage 2: GitHub Releases at the first release. Stage 3: the Play listing (S0027). Each stage is a site edit in the ticket that opens the channel.
- **Staging of the first deploy** (default, owner may override): the site is deployed before the first installable release, because Play review needs the privacy URL. The hub card and the sibling links wait until a channel is live (S0028).
- **Help pages on the site:** DOCUMENTATION_CONCEPT section 4 asks a product with a UI to publish the window help pages it ships, from the same files. The generated pages of S0014 (`phone/src/main/assets/help/<locale>/`) are copied by the generator under `help/<locale>/`, never maintained twice. A help page is built from the kit but is not a site page (PAGE-STYLE 3.1 as amended 2026-10-17). The published addresses are permanent (CONTEXT-HELP rule 15), hence owner input 3.3.

### 2.3 Steps

- **W1 (A)** Site skeleton: generator, copy deck, templates, kit copy, `.nojekyll`, Pages configuration (workflow or branch source, as the generator needs), favicons and one share image of at least 1200x630 from the product icon (never redrawn, PAGE-CONTENT landing item 1).
- **W2 (A)** Landing: identity block (application icon, full name as the SITE-FAMILY-MAP `Tool` cell will read, 28 px at 1280), eyebrow, H1 and one-line outcome, what it is and for whom, proof (real captures of the phone and the watch, reviewed before use), get-it block (stage 1), three to six feature groups in user outcomes (dashboard, SOS, water flashlight, screen and camera lights, stopwatch, phone-and-watch pairing), the permission caveat next to the action it concerns.
- **W3 (A)** Privacy page from one source `docs/PRIVACY_POLICY.md` (+ `-ru`, `-uk`), written after a **permission truth audit** of the merged manifests of both variants: every permission, foreground-service type and hardware feature maps to a one-sentence user-visible job (SECURITY_AND_PRIVACY section 3); what stays on the device and what leaves it; "no telemetry" only if the audit proves it; foreground-only location; no SMS, no call. `.sza-canon.json` is updated in the same edit: `site.kind` "product", `site.root`, `site.pages`, `privacy.page`, `privacy.sensitiveAccess` true (location and camera torch).
- **W4 (A)** How-to guide (task-oriented: first walk, SOS pairing with the watch, the water flashlight, the stopwatch with participants) and the support page (issue tracker of S0025, contact sza@ukr.net, response expectation).
- **W5 (A)** Footer and outward links: the family grid with all eight tools of SITE-FAMILY-MAP 1.9 and the hub (rule 1), the contact block (rule 3), "More tools by SZA", the repository link. Contextual body links (rule 4) only where a sibling covers what Tourist does not; the one candidate is FastMediaSorter Android, subject to owner input 3.3 (name decision 4.6). Every URL is checked with the section 5 `curl` loop of the contract on the day it is written.
- **W6 (A)** SEO block on every page (DOCUMENTATION_CONCEPT section 3): title, description, canonical, Open Graph and Twitter, one H1, truthful JSON-LD (no rating, no review), `hreflang`, generated `sitemap.xml`. `robots.txt` is read only at the host root; the sitemap line is S0028's request to the hub.
- **W7 (A)** Conditional pages: the sideload trust page of `INSTALL-TRUST` when the GitHub Releases channel opens, and "What's new" with its changelog ledger at the first release (`ledgerShape` is "none" today; a page rendered from an empty ledger is a placeholder and is not published). Both ride the release that opens the channel.
- **W8 (A)** Contract pointers in `docs/contracts/`: `PRODUCT-WEB-PAGES` becomes "adopter" (no longer conditional), `INSTALL-TRUST` and `DOC-EXTERNAL-QUALITY` rows updated; registry rows through the `contract-sync` skill.
- **W9 (A then A·GO)** Deploy: the repository's own publish flow, after the generator's staleness check and the canon compliance gate; the owner's go is quoted in the go ledger. README gains the site link (stage after the site answers).

**Contract boundary:** this ticket adopts `PAGE-CONTENT`, `PAGE-STYLE`, `SITE-FAMILY-MAP` and `INSTALL-TRUST` as a consumer and changes none of them. The map row is S0028's catalog-first amendment.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0025 (blocker: the repository and its Pages), S0010 (decision 4.6 name and icon; versioning families for the ledger), S0012 (positioning, forbidden-terms list, P6 carried here), S0027 (consumes the privacy URL and the privacy source), S0028 (consumes the live landing URL).
- **Name and branding (4.6), open and blocking W2.** The landing's identity block, the title tags and the future map row need the full display name and the application icon. Options: (a) keep "Tourist" with the stand-in artwork for the first deploy and rename later through the generator - recommended only if the owner accepts that the name appears in the family footers (S0028) and is changed there by a MAJOR-or-corrective map edit; (b) decide the store name before W2; (c) hold the deploy. FastMediaSorter in copy: (a) name it, (b) do not, (c) only on the author's own pages - the 4.6 options of S0010; Play metadata never names another app.
- **Staging of the first deploy,** default as in 2.2 (deploy before the first channel except the repository). Options: (a) default; (b) hold the Pages deployment until the first installable release - delays the privacy URL that S0027 needs.
- **Help pages on the site, default publish** under `help/<locale>/` (the family precedent for the route is `help/en/`). Options: (a) publish, which makes the addresses permanent (CONTEXT-HELP rule 15) and fills the map's "Help index" cell in S0028; (b) keep the help in the app only and record the reason against DOCUMENTATION_CONCEPT section 4.
- **Contact:** sza@ukr.net, GitHub SerZhyAle (decided 2026-10-09, S0012 section 2.2). Resolved.

## 4. Acceptance

- [ ] 1. `gh api repos/SerZhyAle/<name>/pages --jq '[.build_type,.source.branch,.source.path,.html_url]|@tsv'` states the expected source; `curl -s -o /dev/null -w "%{http_code}"` returns `200` for the landing, privacy, how-to and support in each of `en`, `ru`, `uk` (12 URLs).
- [ ] 2. The generator's staleness check is clean and a second run leaves `git diff --stat` empty; no served file is hand-edited.
- [ ] 3. Every page carries the SEO block of DOCUMENTATION_CONCEPT section 3; the JSON-LD parses, has no `aggregateRating` or `review`; `sitemap.xml` lists every public page; `hreflang` is reciprocal with `x-default`.
- [ ] 4. The landing carries the seven items of PAGE-CONTENT "The landing always carries", observed at 1280 and 360 px with captures in `temp/S0026/`; the identity block's name equals the map `Tool` cell chosen under 4.6.
- [ ] 5. The footer lists the eight tools of SITE-FAMILY-MAP 1.9 and the hub, and the contract's section 5 `curl` loop returns `200` for each; the only contact is sza@ukr.net and GitHub SerZhyAle.
- [ ] 6. The privacy page matches the audit: for each permission of both variants' merged manifests (`aapt dump permissions` on the built packages) a sentence exists, and no sentence names a permission the manifests lack; `.sza-canon.json` carries the site and privacy fields of W3; the canon compliance gate reports no error.
- [ ] 7. The forbidden-terms and over-claim list of S0012 section 2.1 finds nothing in the served tree and the copy deck; the shipped-fact grep of feature-to-site (`soon|planned|upcoming|not yet|in development|will be added|experimental|beta|скоро|планируется|ещё не|бета|незабаром|планується`) finds nothing, each hit read.
- [ ] 8. The get-it block links only live channels (stage 1: the repository), and no served file links a page that does not answer.
- [ ] 9. `docs/contracts/` pointers and registry rows match W8; the README links the site.
- [ ] 10. The hub and every sibling repository are untouched by this ticket.
- [ ] 11. Every deploy has a go-ledger row with the owner's quoted go.

## 5. Risks

- **The name leaks into permanent places.** The landing, the title tags and the map row all show the display name; a later rename edits nine sites (SITE-FAMILY-MAP section 4).
- **The Pages address is permanent** once a map row exists, and so is a published help address.
- **Placeholder pressure.** A landing with no installable channel invites a "coming soon" line, which the canon forbids; the staged get-it block is the answer.
- **Over-claim.** SOS, the water flashlight and the instrument wording are the three places where a page could promise more than the product does (S0012 section 7).
- **Permission drift.** A permission added after the audit makes the privacy page false; the criterion 6 command runs again at every release.

## 6. Open questions

- "Portals" in the order is read as the hub, the repository, the store listing and the trust page. If the owner meant further portals, they are added to W5.
- Whether the sideload trust page (W7) needs its own locale set beyond `en`, `ru`, `uk` - follows INSTALL-TRUST at write time.
