# S0023 - verdict-bearing extract of the S0013 dumps (runs of 2026-10-25)

Copied from the disposable S0013 run folders of 2026-10-25 (runs `20261025-p5-phone` and `20261025-p5-watch`) on
2026-10-29. Each line is the stand's own verdict (`evidence.json` cell detail and M7 sweep) or one node of a
`uiautomator` dump, attributes trimmed to the ones the verdict reads.

## C3 phone (Lights), density 420

Cell detail: `3 breach(es): 8.5d lights: at font 2.0 clipped [] unlabelled [ImageButton]=1 limit 0 P2;
8.5e lights: interactive nodes without label [ImageButton] or state [row_camera_torch]=2 limit 0 P2;
8.5e front-light: interactive nodes without label [] or state [row_camera_torch]=1 limit 0 P2`

M7 `lights`: unlabelled `[ImageButton, ImageButton]`, stateless `[row_camera_torch]`, unlabelled at font 2.0
`[ImageButton x3]`, 9 dumps.

`m7-lights-en-US-1.xml`:

    class="android.widget.ImageButton" text="" content-desc="" bounds="[0,10][147,157]"
    resource-id="row_camera_torch" class="androidx.cardview.widget.CardView" content-desc="" checked="false"
      selected="false" state-description="" bounds="[21,497][1059,645]"

## C7 phone (Stopwatch), density 420

Cell detail: `8.5d/8.5e stopwatch-laps and stopwatch-stopped: unlabelled [ImageButton];
8.5c stopwatch-result: targets under 48 dp (btnStopwatchResultSend 20x48dp, btnStopwatchResultSend 13x48dp);
8.5d stopwatch-result: at font 2.0 clipped [btnStopwatchResultCancel, btnStopwatchResultSave]`

`m7-stopwatch-result-en-US-2.xml` (font 2.0): the three actions sit in an `android.widget.LinearLayout`;
`btnStopwatchResultSend` has bounds `[957,1788][957,1960]` (zero width) and `visible-to-user="false"`.

## C2 watch (Tourist dashboard after the compass storm), density 320

Cell detail: `1 breach(es): 8.5d compass-after-storm: at font 2.0 clipped [] unlabelled [View]=1 limit 0 P2`
(M7: unlabelled at font 2.0 `[View x5]` across the three locales, 9 dumps).

`m7-compass-after-storm-en-US-2.xml` (font 2.0), the clickable chips of the actions row:

    View clickable bounds="[68,290][252,368]"   child TextView "Reset Trip"
    View clickable bounds="[252,290][330,368]"  child TextView "Reset Steps" bounds="[285,303][297,355]"
    View clickable bounds="[76,374][308,384]"   no text child (the Athlete chip, cut by the list's bottom)

`m7-compass-after-storm-en-US-1.xml` (font 1.0): the same chip is `[111,305][273,384]` with child TextView
"🏃 Athlete".

## Reproduce

    pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1 -Scenes C3,C7 -Devices phone
    pwsh -NoProfile -File PLAN/S0013_deep-dynamic-audit/stand/locale-a11y.ps1 -Scenes C2 -Devices watch

Expected after S0023: no 8.5c, 8.5d or 8.5e breach in EN, RU and UK at font 1.0, 1.3 and 2.0 (S0032 tracks the
clipped Reset Steps label, which the stand does not flag).
