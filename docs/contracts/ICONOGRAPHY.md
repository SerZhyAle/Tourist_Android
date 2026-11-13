# Iconography - pointer

| Field | Value |
| --- | --- |
| **Ids** | `ICON-SET` 0.36, `ICON-RENDER` 0.24 (registry, read 2026-10-26, S0016) |
| **Home** | shared contracts catalog, `iconography/` |
| **Owner** | FastMediaSorter Android |
| **Role here** | consumer |

What this repository owes:

- One glyph and one name per meaning as `ICON-SET` states, coloured and sized as `ICON-RENDER` states, on
  every surface that draws a contract glyph. The ported screens keep the glyphs they were ported with; the
  usage rows are filed with the catalog as `iconography/PROPOSAL-2026-10-23-tourist-glyph-usage.md`
  (status proposed, S0004) - the owner's confirmation is owed, not this product's.
- `app.help` is drawn by the help button (S0014): the catalog glyph, named "Help (F1)" in the UI language,
  touch target at least 48 dp (`ICON-RENDER` rule 5). The glyph means help and nothing else in this product.
- `ICON-RENDER` 0.15 to 0.24 in one reading (S0016): 0.16 navigation identity inks stay scoped to Windows
  navigation; 0.17 and 0.18 are the Windows pointer-first and web hit-target floors, not Android; 0.19 is a
  Windows dialog-answer exception; 0.20 to 0.24 delegate button-surface rules to `BUTTONS`, group markers to
  `COLLAPSIBLE-GROUP`, settings-destination glyph policy to `SETTINGS-NAVIGATION` and target floors for
  covered components to `UI-SIZING`. The Android 48 dp target floor this product already holds is unchanged;
  the render rules this product renders by (vector geometry at actual size, no raster enlargement, theme
  roles) are unchanged. The button/glyph geometry work on this product's surfaces is migration S0015.
- `ICON-SET` 0.17 to 0.36 in one reading (S0016): additions are records for other products' surfaces plus
  `app.tour` (0.35) and `action.paste` activated (0.36); none of them names a glyph this product draws, so
  no vocabulary work follows for the shipped surfaces. The one drawing the vocabulary lacked
  (`ic_favorite`, the watch heart-rate tile) is the proposal's registration ask, covered by the usage row
  above. The version carrier `vocabulary.version` (section 5) is read by the owner's exporter, not by this
  product; this product reads no vocabulary file at build or run time.
