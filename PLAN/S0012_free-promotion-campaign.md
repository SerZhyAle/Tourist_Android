# S0012 - Free promotion campaign (pre-launch)

**Status:** Draft
**Status note:** Blocked by S0010: outward steps wait for owner decisions 4.5, 4.6, 4.7, 4.9 (S0010 section 3.3). The owner's go of 2026-10-09 (section 2.2) settled every question of this ticket that carried a recommendation, the contact address and the no-competitor-names rule; the S0010 decisions and the persona and category fork carry no recommendation and stay open.
**Phase:** outside the handoff phase plan (handoff section 8) - the canon's promotion ticket for a product not yet public (canon PROMOTION.md section 8)
**Verification level:** design only until S0010's decisions land - the positioning worksheet and the channel plan are written and reviewed; nothing is published, submitted, pushed or posted.

## 0. Source

- Canon PROMOTION.md (distilled 2026-10-09), sections 1 to 8, and the `promote` skill, Steps 0 to 4. Step 0 classifies
  this repository as **not yet public**: no remote, no release, no listing, distribution decisions open. The canon
  asks for the ticket now, gated on the release decisions, so the campaign is designed before launch.
- This repository: `README.md`, `CLAUDE.md` sections 1 and 3, `docs/BOOTSTRAP_HANDOFF.md` sections 1 to 5, `.sza-canon.json`,
  `PLAN/S0010_release-pipeline-dry-run.md` section 3.3.
- FastMediaSorter Android ticket S4024 (cited with its repository; read-only from here; status Archived, in
  `PLAN/archive/`), sections 3 (watch candidates, item 2), 5 and 5b. Paraphrased from the Russian source: the kit as
  a whole is the differentiator; the dashboard is low on its own, because phone compasses and altimeters are crowded,
  and medium-high as the base of the Tourist kit (5b); and section 5's advice to present each spin-off as an entry
  to FastMediaSorter (a link reading, in translation, "all this and more in FastMediaSorter"), which is now owner
  decision 4.6.
- **Dependency - portfolio indexing.** The host-root `robots.txt`, the host-root Search Console property, the Bing
  import and Yandex host verification are owned by the hub's ticket "sza.od.ua docs/PLAN-2026-10-09-free-promotion-campaign.md"
  (present on 2026-10-09 in the hub's working tree, status OPEN, not yet committed; re-check before P6). This ticket names that dependency and does not plan that work.
- **Dependency - release.** S0010 (this repository) and, through it, S0002 to S0009: nothing can be promoted that
  does not build.

## 1. Goal and why

**Goal.** When Tourist first ships, a stranger looking for a hike kit for the phone and the watch can find it on the
channels its distribution decision opens, every discoverability field says the same true thing, and the campaign's
results are read from public or owner-held counters only.

**Why.** S4024 (FastMediaSorter) found the category crowded (paraphrase of its section 5b: low on its own, because
phone compasses and altimeters are crowded). The
product's chance is the coherent kit - and that has to be said once, consistently, from one worksheet. Designing it
before launch means the first release, the one-shot launch moments and the listing are not wasted on a broken week
(canon PROMOTION.md section 6).

**Baseline - measured 2026-10-09** (read by the session that wrote this ticket; nothing estimated):

| Item | Value 2026-10-09 | How it was read |
| --- | --- | --- |
| Git remote | none | `git remote -v` empty |
| GitHub repository | does not exist | `gh api repos/SerZhyAle/Tourist_mob` and `repos/SerZhyAle/Tourist`: HTTP 404; no name matching `tour` in `gh repo list SerZhyAle --limit 300` (31 repositories) |
| Description, homepage, topics, stars, forks, `usesCustomOpenGraphImage` | not applicable - no repository | as above |
| Releases and asset `download_count` | none - no repository, no tag (`versionShape.tagRegex` null) | `.sza-canon.json` |
| Google Play listing `com.sza.tourist` | not published | `https://play.google.com/store/apps/details?id=com.sza.tourist`: HTTP 404 |
| Play Console installs | not applicable - no app in the console | - |
| Product site | none | `.sza-canon.json` `site.kind` "none"; `https://serzhyale.github.io/Tourist_mob/` and `/Tourist/`: HTTP 404 |
| Page heads, JSON-LD, sitemap, hreflang | none - no site | - |
| Privacy page | none | `.sza-canon.json` `privacy.page` null |
| Changelog ledger | none | `.sza-canon.json` `ledgerShape` "none" |
| Hub card | none | no match for "tourist" in the hub trees on this machine |
| Host-root `robots.txt` | HTTP 404 | `https://serzhyale.github.io/robots.txt` (hub ticket's dependency) |
| Search Console / Bing data for this product | unknown | owner-held; no property names this product |
| Positioning source, listing text, forbidden-terms list | none | repository search |

## 2. Scope

### 2.1 Non-goals

- Canon PROMOTION.md section 1 as this product's lines: no money (no ads, no paid directory, no paid review); no
  analytics script, tracker, click redirector or `utm_` parameter, in the app or on any page; no rating, review,
  download count or traffic figure invented anywhere, structured data included; no rate-us prompt, share button or
  notification that exists to feed a channel; no feature invented to qualify for a directory; no download-wrapper
  portal; no second account, no vote or review request.
- **SOS over-claim.** The SOS signal sounds, blinks and reaches the paired device; it sends no SMS and makes no call
  (handoff section 5, "Permissions and services"). No copy calls it an emergency service, a rescue service or a
  substitute for calling emergency services.
- **Water flashlight over-claim.** "No wet touch can switch it off" is a touch lock, not waterproofing. No copy
  implies the app makes a device water-resistant.
- **Instrument over-claim.** Speed, altitude, heading and coordinates come from phone and watch sensors and GNSS. No
  copy presents the kit as a certified navigation or safety instrument.
- **Location truth.** Location is foreground-only through the platform `LocationManager` (CLAUDE.md section 3). Copy,
  privacy page, permission list and the Play data-safety form say exactly that (S0010 scope).
- **Variant truth.** A listing advertises only what its variant contains. Capabilities that stay noLegal-only under
  owner decision 4.3 (handoff: heart rate, `BODY_SENSORS`, `ACTIVITY_RECOGNITION`, `USE_FULL_SCREEN_INTENT` today)
  never appear in the store listing.
- No `<meta name="keywords">`; no `robots.txt` `Disallow` of CSS, scripts or images; no `FAQPage` or `HowTo` markup for
  rich results; no `aggregateRating` or `review` unless real ratings by actual users are visible on the page.
- **No competitor's product or brand name in anything published** (canon PROMOTION.md section 1 rule 4; owner,
  2026-10-09): not in a keyword, GitHub topic, tag, Play listing field or search term, site page source, README in any
  locale or outreach text, and no "X alternative", "vs X", "instead of X", "replacement for X" or "like X" phrasing.
  A generic alternative is named instead ("unlike a drawer of single-purpose apps"). Names live only in the internal
  positioning worksheet and research notes. Play also forbids references to other apps in metadata (read 2026-10-09
  per canon PROMOTION.md section 2). Not competitor names: the platform names (Android, Wear OS, Google Play, GitHub),
  a bundled component credited in its notices, and FastMediaSorter - the owner's own product, decided by 4.6.
- Portfolio indexing work (host-root `robots.txt`, host-root console property, Bing import, Yandex host) - the hub's
  ticket owns it.

### 2.2 Decisions

- **Owner's go, 2026-10-09** (quoted from the owner, Russian source): "остальное всё по рекомендациям"; "Контактный
  адрес: sza@ukr.net или [private address removed]. - мне не важно"; "Убрать названия конкурентов". Resolved by it:
  - **Recommended options taken.** Every question of this ticket that carried a recommended or proposed option is
    resolved to that option: the channel order below (it was "proposed"). Questions with no recommendation stay open
    (section 8): S0010's 4.5, 4.6, 4.7, 4.9 and this ticket's persona and category fork.
  - **Contact address: sza@ukr.net.** The owner does not mind which; SITE-FAMILY-MAP rule 3 names one contact for
    every surface, so it is sza@ukr.net (GitHub: SerZhyAle). No surface of this repository carries a contact today;
    adding it is step P3a, not done in this edit.
  - **No competitor names.** Canon PROMOTION.md section 1 rule 4, as section 2.1 states it. The scan of 2026-10-09
    found none on any published-surface source (section 5.1).
- Canon PROMOTION.md section 1 rules 1 to 9 apply by reference; every outward step below carries its executor tag.
- **Gate.** No outward step (A·GO, A→O, O) runs before S0010's decisions 4.5, 4.6, 4.7 and 4.9 are recorded, and no
  launch moment fires before the first release on which canon PROMOTION.md section 6's gate holds.
- **Channel order** (the proposed order, accepted by the owner's go of 2026-10-09; which rows apply still follows 4.5): owned surfaces first (repository metadata, Play listing fields,
  pages if 4.9 opens a site), then Obtainium and SourceForge if a GitHub release exists, then AlternativeTo, then
  communities on a release.
- **AI disclosure.** This product is built with AI assistance (agent sessions write its tickets and code). Venues that
  refuse AI-made work are not used (IzzyOnDroid); venues that refuse AI-written text get the owner's own text only
  (Show HN, Habr); venues that ask get the disclosure.
- **Compass conflict.** The persona is an ordinary non-technical walker (canon AUTHOR.md compass). The store, the site
  and community posts speak in hike words; jargon (WFF, Data Layer, GNSS) is kept to a technical list if one is ever used.
- **Audience split by locale.** EN, RU and UK from the first release (handoff section 3). Search terms are researched per
  locale, never translated literally. Yandex Webmaster matters only for a Russian-speaking audience outside Ukraine
  and is a hub act.

### 2.3 Positioning worksheet

No positioning source exists in this repository. Writing it is this ticket's first step: **step P1 (A)** creates
`docs/POSITIONING.md` from the draft below once 4.6 settles the name; if the owner decides on a site (4.9) and the
contracts catalog binds the product to a positioning source (SITE-REPRESENTATION), that source is the worksheet's
published half and no second copy is kept. Draft, 2026-10-09, in canon PROMOTION.md section 2's order:

1. **Competitive alternatives** (what a walker does today without Tourist):
   - several single-purpose apps - a compass app, an altimeter app, a flashlight, a stopwatch - each with its own
     screens (source: S4024 section 5b, paraphrased: phone compasses and altimeters are crowded);
   - the phone's built-in torch tile and clock stopwatch;
   - "nothing" on the watch.
   The source requirement (real issues, reviews, forum threads) is not met yet: step P2 (A, read-only browsing)
   collects real threads and reviews before the list is final. Unsourced lines are cut there.
2. **Unique attributes** (each with a pointer; a line without one is cut):
   - one kit for phone and watch under one application id (`com.sza.tourist`, CLAUDE.md section 3), with a Data Layer
     pair: SOS started on either device starts on the other (handoff section 9 item 2; source classes
     `SosCommandFromWatchHandler`, `SendSosCommandToPhoneUseCase`, ported in S0006 and S0008);
   - the water flashlight that no wet touch can switch off (CLAUDE.md section 3; source `WaterFlashlightLockdownManager`,
     watch `WaterFlashlightExitGate` with its test `WaterFlashlightExitGateTest`; S0005, S0008);
   - the dashboard with compass hero tile, altitude, speed, coordinates, satellites, sunrise and sunset, trip distance, and
     steps - noLegal only until decision 4.3, since steps need `ACTIVITY_RECOGNITION` (handoff section 5) (source `ObserveTouristDashboardUseCase`, `TouristHeroTileManager`; S0004);
   - a multi-participant stopwatch with a watch ongoing activity (source `ui/stopwatch/`,
     `WearStopwatchOngoingNotificationManager`; S0007, S0008);
   - location foreground-only, no Google Play services dependency for it (CLAUDE.md section 3).
   Pointers become this repository's own paths as the port tickets land; the worksheet is re-checked then.
3. **Value** (jobs to be done, draft): when I am on a trail with wet or gloved hands, I want a light that stays on, so
   I can keep walking; when someone in the group needs attention, I want a loud, bright signal from the phone or the
   wrist, so I can be found nearby; when I plan the rest of the day, I want sunset, altitude and distance at a glance,
   so I can turn back in time.
4. **Target persona** (draft): a day hiker or casual walker with an Android phone, often a Wear OS watch, not technical.
   Fork for the owner: whether watch owners are the primary segment (the watch half is the strongest attribute) or
   all hikers with phones (larger, more crowded).
5. **Market category** (draft): an existing searched category - candidates "hiking app", "outdoor toolkit", "hiking
   compass". Picked by step P2's autocomplete and store-suggestion research per locale, then confirmed by the owner.

Internal positioning statement (draft, never published verbatim): *For day hikers with an Android phone and watch who
want their trail tools in one place, Tourist is a hiking kit that keeps light, signal, time and the dashboard on both
devices. Unlike a drawer of single-purpose apps, it pairs phone and watch, and its water flashlight cannot be switched
off by a wet touch.*

**Derived fields** (limits per canon PROMOTION.md section 2, read 2026-10-09; written in step P3 after 4.6):

| Field | Source rule | State |
| --- | --- | --- |
| Tagline | category + value, about 6-10 words | after P2 |
| Play title | name plus at most a category word, 30 chars | after 4.6 |
| Play short description | tagline + value, 80 chars | after 4.6 |
| Play full description | value first, attributes as features, 4000 chars, no ranking, price or promo wording, no keyword repetition | after 4.6 |
| GitHub description, topics | category noun first; at most 20 topics | after 4.7 |
| README first paragraph | tagline plus who it is for | after 4.6 |
| Site title, H1, meta description | only if 4.9 opens a site | after 4.9 |
| Per-locale search terms (EN, RU, UK) | researched per locale; no competitor name (section 2.1) | after P2 |
| Contact (README, Play listing email, privacy page, site footer) | SITE-FAMILY-MAP rule 3: sza@ukr.net | decided 2026-10-09; written in P3a |

### 2.4 Channels (canon PROMOTION.md section 5, shape A - Android app)

Read 2026-10-09 through the canon; every channel's own rules page is re-read before acting, and the date recorded.

| Channel | Decision | Reason or revisit condition |
| --- | --- | --- |
| Google Play listing | **planned** if 4.5 includes Play | Store variant only; a personal account created after November 13, 2023 must run a closed test with at least 12 testers opted in continuously for 14 days before production access (Play Console Help "App testing requirements for new personal developer accounts", read at source 2026-10-09; re-read before P8) - promotion follows production, not the tag |
| GitHub Releases (sideload, noLegal) | **planned** if 4.5 and 4.7 open a public repository | Distribution itself is S0010's; here only the repository metadata and an INSTALL-TRUST page if a sideload page ships (INSTALL-TRUST, conditional) |
| Obtainium | **planned** with GitHub Releases | Zero gate: an "Add to Obtainium" link from the releases |
| SourceForge import | **deferred** | Revisit after the first public GitHub release; mirrors unmodified, integrity check by downloading through its own button |
| AlternativeTo | **deferred** | Revisit after the first release on a durable channel; verified account (A→O), English, no links in the description |
| F-Droid | **deferred** | FLOSS build from source with no Play Services, Firebase or proprietary tracker; the watch pairing uses the Wear Data Layer (Play services). Revisit when S0002 and S0008 fix the dependency set; if any proprietary dependency remains in every variant, refused - no build variant is invented only to qualify (canon PROMOTION.md section 1 rule 8). Its AI policy is `[unverified]` - read before acting |
| IzzyOnDroid | **refused** | Refuses apps made in part with generative AI; this product is AI-assisted (canon PROMOTION.md section 1 rule 5) |
| APKMirror, APKPure | **not a channel** | Mirrors of Play builds; nothing to submit |
| awesome-* lists (Wear OS, Android) | **deferred** | Read the list's own contributing note at attempt time (age, stars, AI rule); one entry per PR; A·GO |
| sza.od.ua hub card | **deferred** to 4.9 | Through SITE-FAMILY-MAP rule set and the `feature-to-site` skill; a product session never edits the hub - it writes the request |
| winget, Microsoft Store, Scoop (main, extras, own bucket), Chocolatey, FossHub, Softpedia, MajorGeeks, Chrome / Edge / VS Code listings | **not applicable** | Windows, command-line or extension shapes |
| Softonic, Uptodown, FileHippo, CNET Download | **refused** | Wrapper and bundling history (canon PROMOTION.md section 5); if found listed, ask for a direct link or removal |

Launch moments and communities (canon PROMOTION.md section 6), all **deferred** to the first release on which phase A
is complete, the demo exists and every channel serves the current version:

| Venue | Decision | Executor |
| --- | --- | --- |
| Reddit (r/WearOS, r/hiking, r/Ultralight - candidates) | deferred; each subreddit's rules re-read before the post; several refuse largely AI-generated projects - disclose or skip | O |
| Show HN | deferred; owner's own text only (no generated or AI-edited text); account history gate | O |
| Product Hunt | deferred; AI rule read at attempt time; video slot is YouTube only - no embedded third-party player on our pages | O |
| Habr | deferred; owner's own text only, once, in "Я пиарюсь" | O |
| dev.to | deferred; substantial content, not primarily promotional; AI assistance disclosed and AI may not promote (owner's own promotional text) | O |
| Lobsters | deferred; invite only | O |

### 2.5 Steps (tactical folder `PLAN/S0012_free-promotion-campaign/` written only after approval)

Before S0010's decisions:

- **P1 (A)** - write `docs/POSITIONING.md` from section 2.3 (after 4.6 for the name).
- **P2 (A)** - read-only research: real threads and reviews for the alternatives; autocomplete and store suggestions
  per locale for the category and search terms; dated notes in the tactical folder.
- **P3 (A)** - a forbidden-terms list (over-claims of section 2.1, ranking words, competitor product and brand names and
  the "alternative / vs / instead of / replacement for / like" phrasings of section 2.1) and a test that keeps it out of
  every tag, listing field, README locale and page source the build owns (canon PROMOTION.md section 4 item 5).
- **P3a (A)** - the contact address sza@ukr.net (decided 2026-10-09, SITE-FAMILY-MAP rule 3) on every surface that
  carries a contact: a contact line in the README now, and the Play listing contact email, the privacy page and any
  site footer as they are written (P6, P8, S0010). No other address appears.

After S0010's decisions (each outward step needs the owner's go, quoted in the go ledger):

- **P4 (A·GO)** - `gh repo edit` description, homepage, topics from the worksheet (after 4.7).
- **P5 (A then A→O)** - one social-card artwork rendered by one generator at 1200x630 and 1280x640 (A); the
  repository social preview is set in the owner's signed-in browser (A→O).
- **P6 (A, then A·GO to publish)** - if 4.9 opens a site: page heads per canon PROMOTION.md section 3, one truthful
  `MobileApplication` JSON-LD block, the generated sitemap, the mandatory pages of DOCUMENTATION_CONCEPT section 4;
  the sitemap reaches the console through the hub's host-root property (dependency) - submitting it is A→O.
- **P7 (A)** - one demo capture: under a minute, silent, phone and watch, encoded per surface.
- **P8 (A, then A→O)** - Play listing fields per locale from the worksheet (A); entering them in Play Console (A→O).
- **P9 (A·GO / A→O)** - directories of section 2.4 in order, each with an outcome line.
- **P10 (O)** - community posts, one per community per release, never the same day everywhere; outreach texts in the
  tactical folder before use, each with a header naming the rules checked and their date.
- **P11 (A)** - metrics checkpoints by a script in this repository, at most fourteen days apart while outreach is active.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0010 (blocking: decisions 4.5, 4.6, 4.7, 4.9); S0002 to S0009 (the product the campaign
  describes); the hub's promotion ticket "sza.od.ua docs/PLAN-2026-10-09-free-promotion-campaign.md" (portfolio indexing
  dependency; uncommitted in the hub on 2026-10-09 - re-check before P6).
- **4.5 Distribution (open in S0010):** decides whether the Play listing, GitHub Releases, Obtainium and SourceForge rows
  are planned or deferred.
- **4.6 Name and branding (open in S0010):** the store name, icon and listing text, and **whether to name
  FastMediaSorter**. Options: (a) name it (S4024 section 5's Spam-policy advice, in translation "all this and more in FastMediaSorter"; ties
  the two listings together); (b) do not name it - Tourist stands alone, matching its independence (CLAUDE.md section 1);
  (c) name it only on the author's own pages (site, README "other products by the author") and never in Play metadata.
  Note for the decision: Play forbids references to other apps in metadata (canon PROMOTION.md section 2), so a
  FastMediaSorter mention in a Play title, short description or search field is excluded under every option; a link on
  a family page goes through SITE-FAMILY-MAP. Also: "Tourist" is generic and crowded as a search term - the name
  decides how much of the discoverability rests on the category words. FastMediaSorter is the owner's own product,
  not a competitor: the no-competitor-names rule (section 2.1) does not decide this question. Neither ticket
  recommends an option, so the owner's go of 2026-10-09 ("остальное всё по рекомендациям") leaves it open.
- **4.7 GitHub remote (open in S0010):** name and visibility decide criteria 1 and 4 and every GitHub-sourced metric.
- **4.9 Site (open in S0010):** decides criteria 2, 3 and 11 (void with "no site"), the hub card and the INSTALL-TRUST page.
- **This ticket's own fork:** primary persona (watch owners or all hikers with phones) and the market category
  (section 2.3 items 4 and 5), asked once P2's research exists. No recommendation yet - open.
- **Resolved 2026-10-09 by the owner's go** (section 2.2): the channel order (proposed option taken), the contact
  address (sza@ukr.net), no competitor names on any published surface.

## 4. Acceptance

Each a static predicate; a criterion voided by the shape is kept and marked.

- [ ] 1. Repository description, homepage and topics set from the worksheet: `gh api repos/SerZhyAle/<4.7 name> --jq '{description,homepage,topics}'` matches `docs/POSITIONING.md`; at most 20 topics, none on the forbidden-terms list. (Void while 4.7 keeps the repository private or absent - reason recorded then.)
- [ ] 2. Every page carries the canon PROMOTION.md section 3 head and one truthful `MobileApplication` JSON-LD block (no `aggregateRating`, no `review`, `softwareVersion` only from the manifest); the site gate is green. **Void if 4.9 decides "no site"** - then the hosted privacy page S0010 requires is the only page and is checked under criterion 9.
- [ ] 3. The sitemap is generated, lists every page, and the console (the hub's host-root property) reports it read with status "Success". **Void if 4.9 decides "no site".**
- [ ] 4. One social-card artwork at 1200x630 and 1280x640 from one generator; GraphQL `usesCustomOpenGraphImage` is true. (1200x630 void if no site; the repository half void while there is no public repository.)
- [ ] 5. One demo capture (phone and watch), referenced from the README and the site (the site half void if no site).
- [ ] 6. Every outreach text exists in `PLAN/S0012_free-promotion-campaign/` before use, with its rules-checked header and date.
- [ ] 7. One outcome line per attempted channel in section 5, refusals included (IzzyOnDroid, the wrapper portals).
- [ ] 8. Metrics recorded at baseline (section 1) and at each checkpoint, at most fourteen days apart while outreach is active, each produced by the checkpoint script as a dated file.
- [ ] 9. No analytics, tracker, telemetry or redirector added (a source search for analytics SDKs, `utm_` and redirector hosts finds nothing); the privacy page, permission list and data-safety form still literally true (foreground-only location, no SMS, no call).
- [ ] 10. Every text names the author, answers the venue's AI rule, and carries no term on the forbidden-terms list (P3 test green) - no competitor product or brand name and no "X alternative" phrasing in any topic, tag, listing field, search term, README locale, page source or outreach text (canon PROMOTION.md section 1 rule 4).
- [ ] 11. The mandatory pages of DOCUMENTATION_CONCEPT section 4 exist and are in the sitemap. **Void if 4.9 decides "no site"**, except the privacy page (criterion 9); the "What's new" page also needs a changelog ledger, which this repository does not have yet (`ledgerShape` "none").
- [ ] 12. Discoverability fields agree across channels: Play listing per locale, repository description and topics, README first paragraph and site head derive from `docs/POSITIONING.md`; a term dropped from one channel for policy reasons is dropped from all.
- [ ] 13. Every outward act has a go-ledger row in the tactical folder; every A→O and O act was the owner's.
- [ ] 14. Every surface that carries a contact states sza@ukr.net and no other address (SITE-FAMILY-MAP rule 3; owner's go 2026-10-09): a search of the README, listing sources and page sources for `@` addresses finds that one value.

## 5. Channel outcomes

None yet - no channel attempted (2026-10-09).

### 5.1 Progress

- **2026-10-09, competitor-name scan (canon PROMOTION.md section 1 rule 4, owner's go of that day).** Searched every
  tracked file outside `PLAN/spec-catalog.jsonl` and `temp/` - the published-surface sources are `README.md` and
  `docs/README.md` (no site, no listing source, no manifest tags, no outreach text exist yet) - for hiking, map, fitness
  and watch app names in this niche (AllTrails, Komoot, Gaia GPS, Wikiloc, OsmAnd, Locus, Organic Maps, maps.me,
  Strava, Outdooractive, ViewRanger, Garmin, Suunto, Polar, Coros, Fitbit, Samsung Health, Google Fit) and for
  "alternative", "vs", "instead of", "replacement for", "like X". **Removed: nothing - no competitor name found.**
  Hits were only this ticket's own channel names (AlternativeTo is a directory, not a competitor) and its internal
  worksheet. FastMediaSorter, named in README-linked `docs/BOOTSTRAP_HANDOFF.md` and `docs/README.md`, is the
  owner's own product and stays.
- **GitHub topics to remove: none** - no repository exists (`gh api repos/SerZhyAle/Tourist_mob` and
  `repos/SerZhyAle/Tourist`: HTTP 404, 2026-10-09). Topics are first set in P4 from the worksheet, already without
  competitor names; any topic edit and any live-channel tag (the Play listing fields) ride the next A·GO step (P4, P8).
- **Contact address** decided as sza@ukr.net; no surface changed yet (step P3a).

## 6. Metrics

Baseline 2026-10-09: section 1. Read later from public or owner-held counters only: GitHub release asset
`download_count` (checksum files excluded) and the traffic API (fourteen-day memory), stars, forks,
`usesCustomOpenGraphImage`, Play Console installs, Search Console and Bing queries, impressions, clicks and indexed
counts. A number that cannot be read is written "unknown".

## 7. Risks

- **Play Spam and minimum functionality.** The category is crowded (S4024); the listing must lead with the kit, not
  the compass. A listing that looks like FastMediaSorter's may meet the repetitive-content rule - see decision 4.6.
- **Health and sensitive-capability policy.** Under the S3178 precedent (FastMediaSorter) SOS and the water
  flashlight sat in noLegal; if decision 4.3 keeps that split, the store watch build loses SOS, the water flashlight
  and the dashboard (handoff section 4.3), so the phone-watch pairing attribute is noLegal-only; the phone store
  variant keeps SOS and the water flashlight. The worksheet must then be re-cut per variant.
- **Closed-test delay.** A personal account created after November 13, 2023 needs a closed test with at least twelve
  testers opted in continuously for fourteen days before production access (Play Console Help, read at source
  2026-10-09; re-read before P8); launch moments must wait for production.
- **Name.** "Tourist" is a generic, crowded word in store and web search.
- **Over-claim.** SOS, water flashlight and instrument wording (section 2.1) - a misleading-claims rejection or a user
  harmed by trust in a signal that calls no one.
- **AI rules.** Venues tighten AI rules without notice; every venue's rule is re-read at attempt time.
- **Hub dependency.** The hub's ticket is uncommitted on 2026-10-09 (re-check before P6). Without the hub's host-root console property no product sitemap is read; a per-product URL-prefix
  property is the fallback only if the hub's is not in place (canon PROMOTION.md section 3).
- **Id collision.** S0012 also exists in other repositories' S spaces; cite it with "Tourist_mob".

## 8. Open questions

- The four S0010 decisions (section 3.3) - open: no recommended option in either ticket, so the owner's go of
  2026-10-09 does not settle them.
- Persona and category forks (section 2.3), after P2 - open: no recommendation yet.
- **Resolved 2026-10-09** (owner's go, section 2.2): channel order -> the proposed order; contact address ->
  sza@ukr.net; competitor names -> none on any published surface (none were present).
- Whether F-Droid is reachable at all once S0002 and S0008 fix the dependency set.
- Which awesome-* lists exist for Wear OS or hiking apps and what their contributing notes say about AI-made work - read at attempt time.
