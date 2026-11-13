# S0015 inventory - unified UI surfaces

Refreshed 2026-10-27 from the current source. This inventory describes implementation and applicability;
it does not advance adoption. The source gate is `tools/ui/check-unified-ui.ps1`. The measured gate and
remaining release evidence are recorded in the ticket and the design record.

| Contract | Current version read | Product scope |
| --- | --- | --- |
| UI-SIZING | 1.1 | Phone and Wear covered controls; stable touch profile |
| BUTTONS | 2.0 | Product commands and answers; content/navigation cards retain their own semantics |
| BOOLEAN-OPTION | 2.0 | Four phone program options, app screen-awake option, Wear counter option |
| COLLAPSIBLE-GROUP | 1.1 | Shared component and About topics |
| SETTINGS-NAVIGATION | 2.0 | Phone Main and About; external resources depend on S0010 |
| ICON-RENDER | 0.24 | Vector geometry and delegated sizing; existing iconography pointer |
| APP-SETTINGS | 0.7 | Reversible edit lifecycle only; desktop window anatomy is not applicable |
| WINDOWS-UI | 0.10 | Not applicable: no Windows surface |
| PAGE-STYLE | 3.4 | Not applicable: no official site; S0010 owns decision 4.9 |

## Phone surfaces

| Surface | Applicable controls | Implementation/check |
| --- | --- | --- |
| Home | Navigation cards, settings entry, corner help | Settings opens from home; generated home help includes its entry |
| Dashboard, portrait and landscape | Content tiles; map/share/reset/SOS commands | Shared CommandFlowLayout for action panel; existing vector glyphs retained |
| Lights | Navigation/content rows; shade shortcut checkbox | SettingsCheckboxRow; stored value and permission flow retained |
| Front/water flashlight | Lamp/touch-lock surfaces | No ordinary command panel; signature behavior retained |
| Torch trampoline | No drawn UI | Not applicable to command geometry |
| SOS | Exclusive mode selector; stop command | Selector remains semantic selection; stop content-fits, minimum height 88 dp, caption grows |
| Stopwatch, both orientations | Region commands and bottom actions | Whole-command wrapping, per-row height equalization, widest known state reservation |
| Stopwatch settings | Two checkbox options, selection controls, Cancel/Apply | Four legacy switch uses removed across program surfaces; staged commit model retained |
| Stopwatch results | Cancel/Save/Send | Caption restored to Send; visible wrapping answer bar |
| Music/color/start-value dialogs | Lists, palette, answers | Content controls retain meaning; ordinary answer geometry follows common styles |
| Counter and history | Numeric commands, extra-step checkbox, reset/start commands | Content-fit command flows; safe cancellation on destructive confirmations |
| Settings Main | Theme, endonym language, units, screen awake | One editor per app preference; immediate reversible persistence |
| Settings About | Version, legal/privacy, help, resources, diagnostic export, related products | Independent topic groups; official external resources await S0010 |
| Four widgets | System-hosted RemoteViews navigation | No product configuration activity; existing vector rendering retained |

## Wear and watch face

- The counter's independent boolean option uses a leading visible checkbox. Radio alternatives remain
  selection controls. Text wraps without the former narrow-caption ellipsis.
- Ordinary chips use the shared rectangular command shape and content width. Watch action clouds wrap;
  alert answers put Cancel first. Stopwatch region commands retain full text and scroll inside their
  participant viewport instead of shrinking captions below the user's font scale.
- Instrument tiles, app catalog navigation and exclusive choices are not disguised command panels.
  The water flashlight and signalling SOS retain their locked input paths.
- The watch face has no Activity and no UserConfigurations node. Its complication editor belongs to
  Wear OS; there is no product-owned settings window to migrate.

## Preserved boundaries

Preferences, default values, application ids, signing configuration, Data Layer formats and files are
unchanged. Presentation theme/navigation state uses separate local preferences. Unit edits continue
through the existing settings repository and phone/watch synchronization. The extraction source is
read-only. Retired switch provenance now points to the replacement checkbox files; source hashes and
port dates are retained. A separate ledger decision records why source S4460 toolbar-title glyphs do
not apply to Tourist's current toolbar surface.

## Overlapping work and explicit debt

- S0010 owns official site/download, support and repository decisions, and the release signing key.
  The owner said to wait for S0010 on 2026-10-27. No placeholder or invented external link is shipped.
- S0014 owns the help system. This edit extends its map and EN/RU/UK source pages and regenerates captures.
  The former stopwatch settings-gear capture debt is removed only after fresh captures prove it visible.
- S0013 owns the broader dynamic audit and existing non-UI source drift; the five pre-existing Wear
  Data Layer/notification rows are not silently marked carried by this migration.
- S0016 owns non-UI contract synchronization; S0029 owns the missing document registry machinery.
- UI adoption remains pending until the required release/minimum-configuration measurements and complete
  About resource inventory exist. Debug measurements are retained as preliminary evidence only.
