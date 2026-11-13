# S0028 - Add Tourist to the sza.od.ua hub and to every other product's documentation

**Status:** Draft
**Status note:** Written 2026-10-26 from the owner's order (section 0). Nothing is edited outside this repository. The work in the other repositories is delivered as request packs and applied by each repository's own session, or directly only if the owner lifts the read-only rule for this ticket (section 3.3). Blocked by S0026; the outward steps also wait for at least one live install channel.
**Phase:** outside the handoff phase plan (handoff section 8); last of the four publication tickets S0025 to S0028
**Verification level:** static predicates read from outside: the contract text in the catalog, the rendered footers and cards of the live sites, `git grep` of each sibling's documentation. This repository edits no other repository's files.

**Depends on:** S0026

## 0. Source

- **Owner's order, 2026-10-26** (Russian source, quoted): "create specifications ... 3 update sza.od.ua and add this application into all other products related with links and articles in their documentation".
- `SITE-FAMILY-MAP` 1.9 (pointer file `PRODUCT-WEB-PAGES`): rule 1 (the footer grid carries every sibling of the table), rule 2 (the hub carries no grid), rule 3 (one contact), rule 4 (contextual cross-links are body copy only where a tool genuinely covers what the other does not), rule 5 (a URL enters the table only once it answers), section 4 (adding a tool is additive; repointing or removing is MAJOR), section 5 (the `curl` conformance loop). The hub owns the contract; a consumer proposes through the catalog's `PROPOSAL-*` mechanism.
- Canon: the `feature-to-site` skill step 9 (hub: four edits per card, `index.html` and `embed.html` byte-identical, adding a product is an owner decision) and its closing paragraph (new product: the kit cross-link table, every sibling's "More tools by SZA" footer, the hub README product list); invariants 10 and 16; CLAUDE.md section 1 (FastMediaSorter Android and StreamsPlayer for Android are read-only from sessions here).
- S0012 section 2.4: "a product session never edits the hub - it writes the request".
- **Measured 2026-10-26.** The map lists eight tools plus the hub; Tourist is not among them. Targets and their repositories (GitHub name; local checkout):

| # | Target | GitHub repository | Local checkout |
| --- | --- | --- | --- |
| 1 | sza.od.ua hub (card, README list, host-root `robots.txt`, kit cross-link table) | `SerZhyAle/sza.od.ua`, user site `SerZhyAle/SerZhyAle.github.io` | `P:\WEB\sites.google.comsiteszaodua`, `P:\WEB\SerZhyAle.github.io` |
| 2 | Fast Media Sorter & Organizer (Android) | `SerZhyAle/FastMediaSorter_mob_v2` | `P:\ANDROID\FastMediaSorter_mob_v2` |
| 3 | Fast Media Sorter & Sharing for Windows | `SerZhyAle/FastMediaSorter_Lite` | `P:\WINDOWS\FastMediaSorter_Lite` |
| 4 | CyrFlip | `SerZhyAle/CyrFlip` | `P:\WINDOWS\CyrFlip` |
| 5 | doc-html-translate | `SerZhyAle/doc-html-translate` | `P:\WINDOWS\EPUB_2_HTML` |
| 6 | FileDO | `SerZhyAle/FileDO` | `P:\WINDOWS\FileDo` |
| 7 | STREAMS Player (Windows) | `SerZhyAle/StreamsPlayer` | `P:\WINDOWS\Streams_Player` |
| 8 | OneClickRunner | `SerZhyAle/OneClickRunner` | `P:\WINDOWS\OneClickRunner` |
| 9 | Universal Agent Kit | `SerZhyAle/universal-agent-kit` | `P:\WEB\universal-agent-kit` |
| 10 | StreamsPlayer for Android | no remote yet | `P:\ANDROID\StreamsPlayer_mob` |
| 11 | Canon (`sza-unified-rules`): product lists in its docs | `SerZhyAle/sza-unified-rules` | `P:\WEB\sza-unified-rules` |

Out by contract or by scope: ELTR (hub-only, SITE-FAMILY-MAP section 6); KnC (`knc_26`, not in the map; owner input 3.3 asks whether it joins).

## 1. Goal

Once Tourist's landing answers and the product can be installed from a live channel, the portfolio knows about it: SITE-FAMILY-MAP lists it, the sza.od.ua hub carries its card in English, Russian and Ukrainian, every other product's site footer and "More tools by SZA" list links it, and the documentation of the products that genuinely relate to it says so - each verified from outside, none by hand-copying into a repository this ticket does not own.

## 2. Scope

### 2.1 Non-goals

- No edit of any other repository's file from a Tourist session, unless owner input 3.3 lifts the read-only rule for this ticket. The deliverable is the request pack; its application is the target repository's own ticket.
- No store badge, install button or Play link on the hub card or in a footer: the hub card and the grid link the landing only (SITE-FAMILY-MAP rule 1, the grid links landings). The landing carries the channels.
- No change of the contract's existing rows, rules or the visual shape of the footer grid (PAGE-STYLE section 4.11).
- No body-copy article in a product whose function does not cover what Tourist does (rule 4), unless the owner amends the contract (owner input 3.3, option b).
- No analytics, no `utm_`, no redirector on any new link (S0012 section 2.1). No competitor name.

### 2.2 Decisions

- **Catalog first** (invariant 10, CONTRACTS section 4): the SITE-FAMILY-MAP amendment (new row, new consumer in the header, version 1.10 additive, dated document-log row) is filed as a catalog proposal before any page is edited; the hub, as owner of the contract, folds it in. A row enters only after the landing URL answers (rule 5).
- **Timing.** The proposal and the request packs are prepared as soon as S0026 deploys. The outward applications wait until at least one install channel is live (the repository does not count as an install channel; the first GitHub Release or the Play listing does): a hub card and sibling footers that point to a product nobody can obtain would be a placeholder. Default; the owner may release the gate earlier.
- **What every sibling gets (rule 1, mandatory):** the Tourist row in its footer grid (as the `Tool` and `Type` cells read, with the URL) and the entry in its "More tools by SZA" list, in every locale that surface ships.
- **What a sibling may get on top (rule 4, only where it holds):** FastMediaSorter Android is the one product that relates by origin - the hike kit was extracted from it and the kit's behaviour is kept in step with it (CLAUDE.md section 2) - so its documentation may carry a short article "the hike kit as a standalone app", in the form decided by 4.6 (a). Any other body-copy link is a case-by-case judgement that quotes rule 4.
- **Request pack per target** (`PLAN/S0028_family-cross-links/requests/<target>.md`): the exact strings in `en`, `ru`, `uk` (or the surface's own locale set, never normalised), the files and places to edit found by reading the target, the target's own gate and publish flow named, and the verification command. Header: date, contract versions read.
- **Hub specifics** (feature-to-site step 9): a card is four edits (inline fallback, `en`, `ru`, `uk` dictionaries); `index.html` and `embed.html` stay byte-identical (`diff -q` exit 0 before deploy); the product count or product list text, the hub README list and the published Google Sites render follow the hub's own rules; the host-root `robots.txt` gains `Sitemap: https://serzhyale.github.io/<name>/sitemap.xml` through the user-site source folder, then its copy procedure.

### 2.3 Steps

- **L1 (A)** Catalog proposal `product-web-pages/PROPOSAL-<date>-tourist-joins-family-map.md`: the row (Tool = display name of 4.6, Type "Android hike kit for phone and watch", URL, Help index per S0026), the consumers line, additive version, and the conformance loop extended with Tourist's URL. Filed through the `contract-sync` skill; the pointer in `docs/contracts/` is updated when the hub folds it in.
- **L2 (A)** Request packs for targets 1 to 11, each read-only against the target. A target with no site yet (10) gets a pack marked "applies when its landing answers".
- **L3 (A·GO per target)** Delivery: the pack is handed to the target's session or ticket store; the owner's go for each delivery and for each deploy is quoted in the go ledger. Targets 2 to 9 are independent of each other; target 1 goes first because the hub's card and the map are the index.
- **L4 (A)** Verification sweep from outside after the applications (section 4), recorded in `PLAN/S0028_family-cross-links/applied.md` as one row per target: applied, declined or deferred, date, evidence. The ledger is updated only by reading; it does not edit the targets.

**Contract boundary:** amends `SITE-FAMILY-MAP` (additive, catalog first); consumes `PAGE-CONTENT` hub contract and `PAGE-STYLE` section 4.11 unchanged.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0026 (blocker: the landing URL and its footer), S0027 (the Play link lives on the landing, not in these links), S0025 (the repository link), S0012 (the hub's promotion ticket and its host-root work; step P6's sitemap submission), S0010 (decision 4.9).
- **How the requests reach the other repositories, open.** Options: (a) request packs applied by each repository's own session - recommended: CLAUDE.md section 1 makes FastMediaSorter and StreamsPlayer read-only from here, S0012 says a product session never edits the hub, and each target has its own gate, publish flow and possibly uncommitted work of its own sessions; (b) the owner lifts the read-only rule for this ticket and this session edits the working trees directly - faster, but collides with whatever those trees hold and publishes through ten different flows from one session.
- **Article scope, open.** Options: (a) rule 4 as written: footer rows and "More tools" entries everywhere, one lineage article in FastMediaSorter Android - recommended: the contract is the owner's own and this ticket does not amend it; (b) widen: an article or documentation section about Tourist in every product - requires a catalog amendment of rule 4 first, spawned as its own ticket.
- **Hub card copy and position, open.** The one-sentence answer to "what is this product" in three locales comes from the positioning worksheet of S0012 once the name (4.6) is decided; the card's position among the hub's cards is the owner's (PAGE-CONTENT hub contract). Adding the product to the hub is the owner's decision, taken by this order.
- **KnC (`knc_26`):** joins the family footers or stays outside; it is not in the map today. Default: stays outside, no action.
- **Release of the install-channel gate:** default as in 2.2; the owner may name an earlier date.

## 4. Acceptance

- [ ] 1. The catalog's SITE-FAMILY-MAP lists Tourist with a dated document-log row, and the contract's section 5 loop (`for u in <section 2 URLs>; do curl -s -o /dev/null -w "%{http_code} $u\n" -L "$u"; done`) prints `200` for every URL including Tourist's.
- [ ] 2. Hub: `curl -sL https://sza.od.ua/` contains Tourist's landing URL; the `en`, `ru` and `uk` dictionaries each carry the card's keys; `diff -q index.html embed.html` exits 0 in the hub tree (hashes cited); the hub README product list names Tourist.
- [ ] 3. The host-root `robots.txt` served at `https://serzhyale.github.io/robots.txt` carries the Tourist sitemap line and equals the user-site source folder byte for byte.
- [ ] 4. For each of targets 2 to 9: `curl -sL <site URL> | grep -c <Tourist landing URL>` prints at least `1` in the rendered footer, and `git grep` of its README(s) finds the "More tools by SZA" entry in each locale the file ships.
- [ ] 5. The FastMediaSorter Android lineage article exists in every locale its documentation ships, reads as a present-tense shipped fact, names no competitor, and is the only body-copy cross-link added (or the owner's option 3.3 b is recorded and its catalog amendment exists).
- [ ] 6. `applied.md` has one row per target (11 rows plus KnC and ELTR marked out, with the reason); every row cites evidence; a declined or deferred row states why.
- [ ] 7. No file of another repository was edited from this repository's sessions unless option 3.3 b is recorded; each request pack carries its header.
- [ ] 8. The only contact in any new text is sza@ukr.net and GitHub SerZhyAle (SITE-FAMILY-MAP rule 3); the shipped-fact grep of feature-to-site finds nothing in the packs and in the applied text.
- [ ] 9. Tourist's own footer (S0026) still lists every tool of the table, including any added since.
- [ ] 10. Every delivery and every deploy has a go-ledger row with the owner's quoted go.

## 5. Risks

- **A footer or card published before the product can be obtained** is a placeholder; the timing gate of 2.2 is the guard.
- **Nine sites, nine publish flows.** A half-applied state is visible: some footers link Tourist, others do not (a shorter grid misstates nothing, SITE-FAMILY-MAP section 4); `applied.md` makes the state explicit.
- **The display name leaks into ten places** (4.6); a later change is a corrective map edit plus every footer.
- **Concurrent sessions** in the target repositories: a direct edit from here can collide with their uncommitted work (the reason for option 3.3 a).
- **Repurposing rule 4.** Pressure to put an article in every product reads as footer duplication in another surface; the amendment route is the honest one.

## 6. Open questions

- Whether the canon repository keeps a product list that names the family's tools (target 11); answered by L2 reading it. If a product table exists, the pack is a request to the canon session (a universal rule or list that looks wrong is reported there, never patched from a product repository).
- Whether StreamsPlayer for Android (target 10) joins the map as its own row or stays under the STREAMS Player row - the map's current convention has one row for the product; the Android twin's landing is that product's S0014 matter.
