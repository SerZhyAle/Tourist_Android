# Help tools (S0014, `CONTEXT-HELP`)

Run from the repository root with `pwsh -NoProfile -File tools/help/<script>.ps1`. Exit codes follow
`CHECK-VERDICT` section 2; each script ends with one verdict line.

| Script | What it does |
| --- | --- |
| `check-help.ps1` | **The gate** (rule 19, rung 1 and the static half of rung 2): surfaces from the manifest, the dialog classes and the dialog call sites against `help-map.json`; the pages per shipped locale, their anatomy, links, request-free rule, house style, promises; the capture against the layouts and strings it shows; the viewer's no-script, no-network settings; `docs/help/baseline.json` (accepted debt, may only fall). |
| `build-help.ps1` | Renders `<locale>/<help>.html` and `index.html` from the map, `docs/help/src/*.json`, the UI's own `strings.xml` and the capture records. Exit 1 when a source is inconsistent. |
| `capture-help.ps1 -Serial emulator-NNNN` | Drives an emulator over adb: per-app language, night mode, pinned status bar, the screen reached by intent and taps, element bounds from the UI dump, screenshots to JPEG. Writes `img/` and `docs/help/shots/`. Emulator only; puts the language, theme and status bar back. |

After a screen change, inspect the changed sources and run `pwsh -NoProfile -File tools/fast-check.ps1 -Area help`.
The existing capture and baseline findings are tracked with S0014. At release, install the debug build on the
emulator, run `capture-help.ps1`, `build-help.ps1` and `check-help.ps1`, and rebuild the app with the pages.

`phone/src/main/assets/help/sza-kit.css` and `help.css` are copies of the catalog's reference files
(`product-web-pages/reference/sza-kit.css` and `in-app-help/reference/help.css`, read 2026-10-25); they make no
request and are refreshed from the catalog, never edited here.
