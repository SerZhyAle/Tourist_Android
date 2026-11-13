# Tourist UI design record

Recorded 2026-10-27 for S0015. Contract text is referenced by id; this record contains only product choices.

- Stable touch profile throughout the phone and watch. No pointer profile or compact selector is exposed.
  Smallest supported target is 48 x 48 dp. OS density scales dp/vector geometry once; independent font scaling
  supports 0.85 through 2.0. Heading glyphs remain 20 dp, group markers 16 dp; text enlargement grows text and
  layout without enlarging those glyphs. The checkbox vector paints a 24 dp square.
- Main and About are the settings destinations, with no child level and no empty advanced page. Main owns
  theme, language, units and screen-awake settings. Program parameters retain their original editors and
  commit semantics. Main changes persist immediately; stopwatch settings retain their Apply/Cancel model.
- At 840 dp and above the reading-start menu remains visible; below that it occupies a start-side drawer.
  Both destinations use monochrome glyphs, a selected border and bold caption. Rows grow for translations.
  Page selection, each viewport anchor/offset and group expansion use stable ids across openings.
- About topics are License, Privacy, Resources and Related products. License and Privacy start collapsed;
  Resources and Related products start expanded. Later choices persist independently. Headers stack at full
  content width; packing is not chosen because four topics fit a short vertical index. Expansion keeps
  the active header's vertical anchor where the scroll bounds allow.
- Command panels start at reading start and wrap whole buttons with 8 dp gaps, then equalize each visual
  row's heights. All commands remain visible; no panel uses secondary overflow. Dialog answers always retain
  captions. The stopwatch reserves the widest known action label at the current text scale. Obvious answers
  and numeric counter steps may be text-only; recognizable substantive commands keep semantic glyphs.
- Boolean options have one accessible checkbox node. Checkbox, heading and glyph activate it; explanation
  and background do not. Native parent scrolling cancels pending activation. Option-local help is omitted
  because the authored heading/explanation describes both states; corner help and F1 remain available.
- Navigation cards, instrument tiles and the SOS exclusive mode selector retain their content/selection
  semantics. The water flashlight's touch lock and the watch SOS signal retain their ported input rules.
- The watch face has no product configuration activity or UserConfigurations node. System complication
  selection is system-owned UI. No watch-face button or settings editor is migrated by this ticket.

About resource inventory:

| Resource | Owning source or action |
| --- | --- |
| Description | Authored settings strings, EN/RU/UK |
| Running version | Generated build metadata |
| License | Root LICENSE, embedded mechanically in BuildConfig |
| Privacy | Authored local page text, EN/RU/UK |
| Help | Bundled help and its canonical help map |
| Diagnostics | Explicit system destination picker, bounded allowlisted ZIP, no upload |
| Necessary resources | Local help, legal text and companion watch-face resource index |
| Related products | FastMediaSorter and StreamsPlayer, purpose labels |
| Official site and downloads | S0010 decision 4.9 and distribution decision 4.5 |
| Support/contact | S0010 official resource decision |
| Repository access | S0010 decision 4.7 |

The owner explicitly retained the last three entries in S0010 on 2026-10-27. No invented URL or inactive
control represents them. Their absence keeps SETTINGS-NAVIGATION section 3.3 and S0015 open.

Evidence procedure: [UI tools](../../tools/ui/README.md). RTL is not shipped (EN/RU/UK only); desktop
pointer, browser zoom and Windows high contrast have no applicable surface. Android accessible text,
native focus, light/dark and density/font-scale changes remain in scope.
