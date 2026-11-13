# Tourist for Android

Tourist is a kit for a hike, on the phone and on the watch: a dashboard with speed,
altitude, heading, coordinates, satellites, sunrise and sunset, steps and trip distance. The magnetic-field tile
shows live ambient pressure in millibars when the phone has a barometer, and the gyroscope tile draws a 3D phone
that turns with yours in real time, with the turning speed in degrees per second. An SOS signal
sounds and blinks and reaches the paired device; a "water" flashlight that no wet touch can switch off; the
screen and camera flashlights; and a multi-participant stopwatch. Two installable packages share one
application id - the phone app and the Wear OS app - plus a watch face.

It is an independent product: it shares no application code with the other products of the family, only
the published contracts.

## Status

The phone, Wear OS and watch-face modules build with Gradle. The ticket store and release plan are in
`PLAN/`. Where the product came from: [docs/BOOTSTRAP_HANDOFF.md](docs/BOOTSTRAP_HANDOFF.md).

## Settings

Open Settings from the start screen to choose system/light/dark theme, language, measurement units and
whether instrument screens stay awake. Changes apply immediately. The page menu stays visible on wide
screens and opens from the Pages button on phones. About contains the running version, license, privacy,
offline help, related products and an explicit diagnostic ZIP export. The ZIP contains build and platform
values; it excludes location, history, music paths and user text, and is never uploaded automatically.

Boolean settings use a leading checkbox. Tap its square, caption or glyph; explanations do not toggle it.
Command rows wrap at narrow widths and large text sizes. Stopwatch settings keep their Apply/Cancel model.

## Help in the app

Every screen of the phone app has a `?` button in its top-right corner, and F1 does the same on a connected
keyboard - except the two full-screen lights, which no touch may end, and whose help is the Lights page. Each
opens a page about that screen, with a picture and the usual order of use, in English, Russian
or Ukrainian. The pages are inside the app and need no network. Where they come from and how to rebuild them:
[tools/help/README.md](tools/help/README.md); the prose lives in `docs/help/src`.

## Privacy

The developer receives none of your data: no account, no analytics, no advertising, no tracking. Location is read
only while a Tourist screen is open, and the only network requests are the weather requests to Open-Meteo. The full
policy, in English, Russian and Ukrainian: [privacy policy](https://serzhyale.github.io/Tourist_Android/privacy.html).

## Contact

Email [sza@ukr.net](mailto:sza@ukr.net), GitHub [SerZhyAle](https://github.com/SerZhyAle). Questions and bug reports are
welcome as [GitHub issues](https://github.com/SerZhyAle/Tourist_Android/issues).

This project is developed with heavy AI assistance; the author reviews every change.

## License

MIT - see [LICENSE](LICENSE).
