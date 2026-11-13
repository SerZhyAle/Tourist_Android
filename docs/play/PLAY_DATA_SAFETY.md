# Play Console - Data safety, App content and the content rating (S0027 P2)

Source of truth for the **App content answers** of Tourist on Google Play: Data safety, the health, ads, government,
financial and sign-in declarations, the target audience and the IARC questionnaire, for the two Play apps
`com.sza.tourist` (the phone app and the Wear OS app, one application id) and `com.sza.tourist.watchface` (the watch
face). The forms live only in the web console: the Play Developer API has no endpoint for them. Decide **here**, then
answer in the console; never decide inside the console.

Reconciled on 2026-11-13 against the tree of that day, the `store` variant of `phone` and `wear` and the `watchface`
module: the source manifests (`phone/src/main/AndroidManifest.xml`, `wear/src/main/AndroidManifest.xml`,
`wear/src/store/AndroidManifest.xml`, `watchface/src/main/AndroidManifest.xml`), the network code
(`phone/.../data/weather/OpenMeteoWeatherProvider.kt`) and `gradle/libs.versions.toml`. The merged release manifests are
read again at the release (S0027 P1). When a later ticket changes what the build stores or sends, this file, the
privacy pages `site/privacy*.html` and the permission list change in the same edit (canon invariant 14).

## The set that must say the same thing

- the privacy pages `site/privacy.html`, `site/privacy-ru.html`, `site/privacy-uk.html`
  (`https://serzhyale.github.io/Tourist_Android/privacy.html`), hand-written, section 5 the permission table;
- the store manifests named above;
- the listing texts [PLAY_LISTING.md](PLAY_LISTING.md) and their translations.

## What leaves the device, to whom, and the decision

The reading of Google's definitions (`https://support.google.com/googleplay/android-developer/answer/10787469`, re-read
on the day the form is answered): "collect" means transmitting data from the app off the user's device; "sharing" means
transferring user data to a third party, except to a service provider that processes it on the developer's behalf and
except a transfer the user starts by a specific action; data processed only on the device is not disclosed.

| Flow | What leaves the device | To whom | Decision |
| --- | --- | --- | --- |
| Weather at the current fix (phone) | latitude and longitude at full precision, temperature unit, time-zone mode | Open-Meteo `api.open-meteo.com` | **Declared**: Location, Precise location - collected, not shared (Open-Meteo answers the app's request as its weather provider), processed ephemerally, optional, App functionality |
| Place search for the weather (phone) | the typed place name and the app language | Open-Meteo `geocoding-api.open-meteo.com` | **Declared**: App activity, In-app search history - collected, not shared, processed ephemerally, optional, App functionality |
| Map, Share coordinates (phone) | the coordinates as an intent to the maps app or the share sheet | the app the user picks | Not declared: a transfer on a specific user-initiated action |
| SOS start and stop, shared settings (the store watch has no dashboard, so it sends no weather request) | Data Layer messages and data items under `/tourist/` | the user's own paired watch or phone, through Google Play services | Not declared: between the user's own devices; nothing reaches the developer |
| Steps and cadence (phone `ACTIVITY_RECOGNITION`), compass, barometer, gyroscope | nothing: read and shown on the device | nobody | Nothing to declare |
| Camera (`CAMERA`) | nothing: the torch is switched, no frame is read | nobody | Nothing to declare |
| Stopwatch track (`READ_MEDIA_AUDIO`) | nothing: the file is played on the device | nobody | Nothing to declare |
| Diagnostic ZIP (Settings, About) | build and platform values, written to a place the user picks in the system file picker | wherever the user saves it | Not declared: user-initiated, no location or user text in it |
| Analytics, crash reports, advertising | nothing: none in the build, no `AD_ID`, no Firebase | nobody | Nothing to declare |
| Watch app (store) and watch face | the watch has no `INTERNET` and no location; the face has no code | nobody | Nothing to declare |

Owner decision of 2026-11-13: declare by the facts ("strictly by the facts"), not the FastMediaSorter production answer
("no data collected"), because Tourist sends the exact fix while FastMediaSorter sends a coordinate quantized to a map
tile.

## The answers - `com.sza.tourist`

| Item | Answer |
| --- | --- |
| Privacy policy | `https://serzhyale.github.io/Tourist_Android/privacy.html` |
| Sign in details | No - no part of the app is restricted (owner decision of 2026-11-13, the answer FastMediaSorter gives in production) |
| Ads | No |
| Advertising ID | No - no `AD_ID`, ads, Firebase or analytics in the manifests or `gradle/libs.versions.toml` |
| Government apps | No |
| Financial features | My app doesn't provide any financial features |
| Health apps | Activity and fitness - the steps and cadence tiles of the phone dashboard (`ACTIVITY_RECOGNITION`, both variants, owner decision 4.3 of 2026-10-23); the store watch declares no health permission |
| Target audience | 18 and over (owner decision of 2026-11-13) |
| Data safety - collects or shares | Yes |
| Data safety - encrypted in transit | Yes (HTTPS to Open-Meteo) |
| Data safety - account creation | My app does not allow users to create an account |
| Data safety - deletion request | No: nothing is stored off the device by the developer; the request is processed ephemerally by Open-Meteo |
| Data safety - data types | Location: Precise location; App activity: In-app search history - each collected, not shared, processed ephemerally, optional (the user can deny location and need not search), purpose App functionality |
| Content rating | fresh IARC questionnaire, category All Other App Types, contact `sza@ukr.net`, every answer No (below) |

## The answers - `com.sza.tourist.watchface`

| Item | Answer |
| --- | --- |
| Privacy policy | the same page |
| Sign in details | No |
| Ads, Advertising ID, Government, Financial, Health | No / none - the package has no code (`android:hasCode="false"`), no permission and no network access |
| Target audience | 13-15, 16-17, 18 and over (owner decision of 2026-11-13) |
| Data safety | No data collected, no data shared |
| Content rating | fresh IARC questionnaire, category All Other App Types, every answer No |

## IARC answers, both apps

Answered by the facts (owner decision of 2026-11-13); a fresh questionnaire per app, never a copied rating id (canon
invariant 15).

| Section | Answer and the fact behind it |
| --- | --- |
| Downloaded app | ratings-relevant content in the package: No - instruments, lights, a signal and a stopwatch |
| User content sharing | users interact or exchange content: No - SOS and the Data Layer reach only the user's own paired device |
| Online content | content outside the package: No - the app fetches weather numbers, not media or third-party content |
| Age-restricted products | No |
| Miscellaneous | location sharing with other users: No (Share hands text to an app the user picks; the app shares nothing with other users itself); digital purchases: No; cash rewards or NFTs: No; web browser or search engine: No (the place search finds weather places only); news or educational: No |

## Console record (2026-11-13)

Filled in the owner's personal developer account (8446656778368889827) on the owner's order, with the owner's yes of the
same day for each one-way step (the package names, Free, the Developer Program Policies, the US export laws, the IARC
terms of use). Nothing is uploaded, nothing is sent for review.

### `com.sza.tourist` (app 4975092571968022444, "Tourist: Hiking Kit")

| Item | State | Answer |
| --- | --- | --- |
| Privacy policy | saved | `https://serzhyale.github.io/Tourist_Android/privacy.html` (answered 200 the same day) |
| Sign in details | saved | No - no part restricted |
| Ads | saved | No |
| Advertising ID | saved | No |
| Government apps | saved | No |
| Financial features | saved | My app doesn't provide any financial features |
| Health apps | saved | Activity and fitness; no regional requirement asked |
| Target audience | saved | 18 and over |
| Data safety | saved | collects or shares: Yes; encrypted in transit: Yes; account creation: "My app does not allow users to create an account", login with outside accounts: No; deletion request: No. Location - Precise location and App activity - In-app search history: collected, not shared, processed ephemerally, users can choose, App functionality. The store preview reads "No data collection declared / No data shared with third parties": Play does not show ephemerally processed data on the listing |
| Content rating | saved, fresh IARC questionnaire | category All Other App Types, contact `sza@ukr.net`, every answer No (the table above). Ratings: ESRB Everyone, PEGI 3, USK all ages, ClassInd all ages, IARC generic 3+, Google Play Russia and South Korea 3+ |

Overview after these: "You're all caught up".

### `com.sza.tourist.watchface` (app 4973503680609248842, "Tourist Watch Face")

Every App content item saved ("You're all caught up"): privacy policy as above; Sign in details No; ads No; advertising
ID No; government No; financial none; health none; target audience 13-15, 16-17, 18 and over; Data safety "no data
collected, no data shared"; IARC All Other App Types, every answer No: ESRB Everyone, ClassInd all ages (the same
authority set as the main app).
