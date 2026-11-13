# Play listing - English

The text of the three store fields of `com.sza.tourist` on Google Play. The Russian text is
[PLAY_LISTING-ru.md](PLAY_LISTING-ru.md) and the Ukrainian [PLAY_LISTING-uk.md](PLAY_LISTING-uk.md); the headings are the
console's field names in all three. Limits: title 30 characters, short description 80, full description 4000 (read in
the console on 2026-11-13). Written from the README paragraph and the over-claim rules of S0012 section 2.1: the
listing describes the `store` variant only; SOS sends no SMS and makes no call; the water flashlight is a touch lock, not
waterproofing; the readings are sensor and GNSS readings, not a certified instrument; no other app is named. Store name
"Tourist: Hiking Kit": owner decision of 2026-11-13.

## Title

Tourist: Hiking Kit

## Short description

Compass dashboard, SOS, flashlights and a stopwatch for hikes, phone and watch

## Full description

Tourist puts the tools of a day hike in one app on your phone and your Wear OS watch: a compass dashboard, an SOS signal, a flashlight that stays on in the rain, and a stopwatch for the whole group.

Dashboard
A compass hero tile with your heading, then speed, altitude, coordinates, satellites, sunrise and sunset, steps and cadence, the trip distance with the climb and descent, and the weather at your position. With a barometer, the magnetic-field tile shows the air pressure in millibars, and the gyroscope tile draws a 3D phone that turns with yours.

SOS
One button starts a loud siren and a bright Morse strobe. Started on the phone, the signal also starts on your paired watch, and the other way round, so the people near you can find you. It sends no SMS and makes no call.

Water flashlight
A full-screen light that no wet touch can switch off: rain, snow or a wet glove leave it on. You switch it off with a deliberate key press.

Screen and camera flashlights
A screen light in any colour, the camera flash as a torch, home screen widgets and a shortcut in the notification shade.

Stopwatch
A stopwatch for several participants with laps and results, music of your choice while it runs, and a home screen widget. On the watch it keeps running as an ongoing activity.

On your watch
The Wear OS app brings SOS, the water flashlight and the stopwatch to your wrist, with or without the phone nearby. The Tourist watch face is a separate package for the watch.

Privacy
The developer receives none of your data: no account, no analytics, no advertising, no tracking. Location is read only while a Tourist screen is open, and only the weather requests leave the phone, to the Open-Meteo weather service.

Help is built into every screen of the phone app, offline. Interface languages: English, Russian and Ukrainian.

## Console fields (2026-11-13)

Entered in the Play Console for `com.sza.tourist` from this file and its two translations: App name, short and full
description in en-US (default), ru-RU and uk. Store settings: category Travel & Local, contact email `sza@ukr.net`,
website empty until the landing `https://serzhyale.github.io/Tourist_Android/` answers (S0026), then the landing, never
the repository. Graphics: `icon-512.png` and `feature-graphic-1024x500.png` beside this file, render targets of
`tools/store/render-store-assets.ps1`; the other languages fall back to them. Screenshots (uploaded the same day): phone 6 and Wear OS 4 per language, en-US,
ru-RU and uk each with its own set from `docs/play/screenshots/` in the order of the file numbers. Form factors:
Wear OS added (of its three steps the Wear screenshots are done; a Wear bundle on a testing track and the
review-policy opt-in wait for the release); Android XR stays Active by Google's default. The watch face app `com.sza.tourist.watchface` has category
Personalization, the same contact and the Wear OS form factor; its listing has no source yet. The App content answers
are in [PLAY_DATA_SAFETY.md](PLAY_DATA_SAFETY.md), "Console record".
