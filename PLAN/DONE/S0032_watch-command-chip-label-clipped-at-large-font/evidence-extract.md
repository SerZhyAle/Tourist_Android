# S0032 evidence extract

Copied 2026-10-29 from disposable scratch; the sources are the S0013 run `20261025-p5-watch` and the Gradle cache.

## S0013 C2 watch dumps at font 2.0 (run 20261025-p5-watch, scene C2-watch-r1)

Files `m7-compass-after-storm-<locale>-2.xml`, all written 2026-10-25 22:06. Nodes below the hero card that carry
text or are clickable, in document order: the Reset Trip chip, the Reset Steps chip, the Athlete chip cut by the
list bottom.

    en-US
    View clickable [68,290][252,368]    TextView "Reset Trip" [87,310][236,348]
    View clickable [252,290][330,368]   TextView "Reset Steps" [285,303][297,355]
    View clickable [76,374][308,384]
    ru-RU
    View clickable [69,295][264,372]    TextView "Сброс пути" [88,315][261,353]
    View clickable [264,295][341,372]   (no text child: "Сброс шагов" fully clipped)
    View clickable [69,379][315,384]
    uk-UA
    View clickable [70,298][276,375]    TextView "Скидання шляху" [89,311][276,362]
    View clickable [276,298][353,375]   (no text child: "Скидання кроків" fully clipped)
    View clickable [70,381][314,384]

In every locale the two reset chips abut on one line (the first ends where the second starts) and the second
gets about 77 px: the layout of a plain `Row`, which hands its second child only what the first left.

## Source timeline

- `wear/src/main/java/com/sza/tourist/wear/ui/common/WearCommandChip.kt`: last written 2026-10-27 14:34, after
  the dumps. No C2 watch dump exists after 2026-10-27 14:30 under the S0013 runs.
- FastMediaSorter `wear/.../apps/tourist/TouristActionsRow.kt` SHA-256 `584f6820e700539d74b66b7f389ed6ca19d38e60648039439a95fc1c7daec852`,
  equal to the port-ledger row of 2026-10-25: a `Row` of two `androidx.wear.compose.material.CompactChip`.

## Compose foundation-layout 1.7.6, FlowLayout.kt `breakDownItems`

From `foundation-layout-android-1.7.6-sources.jar` (BOM 2024.12.01). Every fixed-size child is measured with
`measureConstraints` (main axis `0..mainAxisMax`, the full line), never with the line's remainder `leftOver`.

    // FlowLayout.kt lines 1184-1194
    1184:     val subsetConstraints = OrientationIndependentConstraints(
    1185:         0,
    1186:         mainAxisMax,
    1187:         0,
    1188:         crossAxisMax
    1189:     )
    1190:     val measureConstraints = subsetConstraints.copy(
    1191:         mainAxisMin = 0
    1192:     ).toBoxConstraints(
    1193:         if (measurePolicy.isHorizontal) LayoutOrientation.Horizontal else LayoutOrientation.Vertical
    1194:     )
    // FlowLayout.kt lines 1214-1222
    1214:     var nextSize = measurablesIterator.hasNext().run {
    1215:         measurable = if (!this) null else measurablesIterator.safeNext(lineInfo)
    1216:         measurable?.measureAndCache(
    1217:             measurePolicy,
    1218:             measureConstraints
    1219:         ) { placeable ->
    1220:             placeableItem = placeable
    1221:         }
    1222:     }
    // FlowLayout.kt lines 1296-1307
    1296:         nextSize = measurablesIterator.hasNext().run {
    1297:             measurable = if (!this) null else measurablesIterator.safeNext(lineInfo)
    1298:             placeableItem = null
    1299:             measurable?.measureAndCache(
    1300:                 measurePolicy,
    1301:                 measureConstraints
    1302:             ) { placeable ->
    1303:                 placeableItem = placeable
    1304:             }
    1305:         }
    1306:         nextMainAxisSize = nextSize?.first?.plus(spacing)
    1307:         nextCrossAxisSize = nextSize?.second
