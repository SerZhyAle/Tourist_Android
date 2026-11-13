# S0054 - play-publication-remaining-steps

**Ticket:** S0054
**Status:** Draft
**Complexity:** medium - no source; ordered owner and release steps, several one-way
**Date:** 2026-11-13

## Captured request and migration scope

Owner's rule (deviceinfo-mob session, 2026-11-13): "\"Что осталось до публикации\" это должен быть тикет в репозитори проекта, а не мне сюда"
Owner's order for this product (2026-11-13, relayed by the deviceinfo-mob session): "Выполни всё это теперь для Tourist_mob"

What remains before Tourist is published on Google Play, as of 2026-11-13. Already done the same day on the owner's order (owner decisions of the same day: the recommendations table - repository Tourist_Android, PLAN public, history rewritten to drop a private address, privacy pages in site/, store name "Tourist: Hiking Kit", Free, Sign in details "No", contact sza@ukr.net, graphics by script, categories Travel & Local and Personalization, IARC by the facts; both Play apps created; target audience main 18+, watch face 13+; Data safety strictly by the facts):
- Public remote https://github.com/SerZhyAle/Tourist_Android (MIT, issues on, wiki and projects off; commit 5db4d6e holds the product tree over the five rewritten foundation commits) and its Pages site (workflow .github/workflows/pages.yml, run 43456039842 green): privacy.html, privacy-ru.html, privacy-uk.html, sitemap.xml, robots.txt and the assets answer 200; the site root answers 404 (no landing yet, S0026), so the repository has no homepage and the listings no website.
- Play Console app com.sza.tourist ("Tourist: Hiking Kit", personal account 8446656778368889827, app 4975092571968022444, App, Free): every App content item saved, overview "You're all caught up" (privacy URL, Sign in details No, no ads, no advertising ID, not a government app, no financial features, health Activity and fitness, target audience 18 and over, Data safety precise location and in-app search history collected ephemerally and not shared, fresh IARC rating ESRB Everyone / PEGI 3 / IARC 3+); store settings Travel & Local and sza@ukr.net; default listing en-US with ru-RU and uk translations, icon and feature graphic; Wear OS form factor added. Record: docs/play/PLAY_DATA_SAFETY.md "Console record" and docs/play/PLAY_LISTING.md "Console fields".
- Play Console app com.sza.tourist.watchface ("Tourist Watch Face", app 4973503680609248842, App, Free): every App content item saved (no data collected, IARC Everyone, target audience 13 and over), store settings Personalization and sza@ukr.net, Wear OS form factor added; listing empty.

Remaining, in order:
1. Commit and push the records written on 2026-11-13 after the first push (CLAUDE.md "Public presence", docs/play/PLAY_DATA_SAFETY.md, docs/play/PLAY_LISTING.md, this ticket and the tickets filed the same day) - on the owner's word.
2. Phone screenshots (2 to 8, 16:9 or 9:16) and Wear OS screenshots for en-US, ru-RU and uk for the main app, from a real device check of the store variant (S0027 P3); the dashboard needs a satellite fix that is not the owner's real position.
3. Watch face listing: write its source in docs/play (title, short and full description in en, ru, uk; icon and feature graphic as render targets of tools/store/render-store-assets.ps1), then fill the watch face's default listing and its Wear OS screenshots.
4. Landing (S0026): once https://serzhyale.github.io/Tourist_Android/ answers 200, set it as the website in Store settings of both apps and as the repository homepage (gh repo edit --homepage).
5. GitHub social preview: upload docs/github/social-preview-1280x640.png in the repository settings (the owner's browser).
6. The ACCESS_FINE_LOCATION permission declaration of the phone store package (S0036 A1, deadline 2027-01-27): its text must say that the fix's latitude and longitude go to Open-Meteo for the weather, as the privacy pages and the Data safety answers now do.
7. Release side (S0010 and S0027, owner's order only): the owner's release keys (S0010 Q1) in P:\ANDROID\Tourist_credentials, the PACKAGE-VERSIONING fold (S0010 Q2), store bundles of phone, wear and watchface, pre-flight verdict, first upload to internal testing with the app signing key import (one-way), the update proof, closed testing if the account requires it, countries (all, never narrowed) and rollout.
8. Wear OS opt-in and the Wear OS review policy for both apps (console Wear OS step 3; allowed only after the Wear screenshots and a Wear bundle on a testing track; one-way, owner's yes).
9. Production and review (one-way, owner's order only).
10. After the landing and the Play listing answer 200: hub:S0011 (SITE-FAMILY-MAP row and hub card) and the footer-grid tickets filed in each map consumer become workable.
11. Account-level, not this app: "Declare your trader status" (due February 2027 for this account; the decision is the owner's).

