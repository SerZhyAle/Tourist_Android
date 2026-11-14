# Play listing - watch face, English

The text of the three store fields of `com.sza.tourist.watchface` on Google Play. The Russian text is
[PLAY_LISTING_WATCHFACE-ru.md](PLAY_LISTING_WATCHFACE-ru.md) and the Ukrainian
[PLAY_LISTING_WATCHFACE-uk.md](PLAY_LISTING_WATCHFACE-uk.md); the headings are the console's field names in all three.
Limits: title 30 characters, short description 80, full description 4000 (read in the console on 2026-11-13). Written
from the face itself (`watchface/src/main/res/raw/watchface.xml`, `watchface/src/main/AndroidManifest.xml`, `gradle/libs.versions.toml`
`min-sdk-watchface`) and the over-claim rules of S0012 section 2.1: only what the face draws and does in the package
that ships is said; the face has no code, no permission and no network access; the slots start empty, because the kit's
watch app serves no complication data (parity map: the complication sources and the phone battery report are
absent-by-design), so nothing is promised of a slot's content, of a phone battery bar or of a style set from the phone.
Store name "Tourist Watch Face": owner decision of 2026-11-13.

## Title

Tourist Watch Face

## Short description

Big digital time, waves that move with your wrist, six slots and a battery bar

## Full description

Tourist Watch Face is a digital watch face for Wear OS 6 and newer, the companion of the Tourist hiking kit: a large time in the middle, the date beneath it, and a dark green sea of waves and drifting dots behind them.

Time and date
Big digits that fill the face, with the weekday, the day and the month beneath them.

Waves and dots
Four lines of waves sweep slowly across the face and a field of small dots drifts over them. Tilt your wrist and the dots shift, each layer by its own amount, so the face has depth. The direction of the drift changes every hour.

Always-on
In the always-on mode the face dims to the outline of the time and the date and one thin wave line, to spare the battery.

Battery bar
A thin bar above the time shows the charge of your watch: white, amber at 25 percent and below, red at 10 percent and below, and green with a bright contour while the watch charges.

Six slots
Six round slots, two above the time and four along the lower arc, hold the complications of your choice. Pick each one in the watch face editor of your watch, from the providers installed on it. A slot with nothing chosen draws nothing.

Opens Tourist
A tap on the time and the date opens the Tourist app on the watch, when it is installed.

Privacy
The watch face contains no code, asks for no permission, and collects and sends nothing.

## Console fields (to enter)

For `com.sza.tourist.watchface` (app `4973503680609248842`): App name, short and full description in en-US (default),
ru-RU and uk from this file and its two translations. Store settings are already saved: category Personalization,
contact email `sza@ukr.net`; the website stays empty until the landing answers (S0026), then the landing, never the
repository. Graphics: `icon-512.png` and `feature-graphic-1024x500.png` of this folder, the same two render targets of
`tools/store/render-store-assets.ps1` the main app uses - the pictures carry the product name only and the face's
launcher glyph is the same compass star, so no second pair is rendered. Screenshots: Wear OS, 1:1,
`docs/play/screenshots/watchface/01_watch_face.png` (454 x 454, the interactive face) and `02_watch_face_ambient.png`
(454 x 454, the always-on face), the same two for every language. The App content answers are in
[PLAY_DATA_SAFETY.md](PLAY_DATA_SAFETY.md), "Console record"; the console holds a copy of this text, never the master.
