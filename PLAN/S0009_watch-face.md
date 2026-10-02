# S0009 - Watch face

**Status:** Draft
**Phase:** 9 of 10 (handoff section 8)
**Verification level:** device - the watch face installs and shows the time with the product branding.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 9, section 4.4, section 3 (extraction-only non-goal), section 7
(PACKAGE-VERSIONING).

## 1. Goal

The watch face package under `com.sza.tourist.watchface` exists, installs and shows the time with the product's
branding. Owner decision 4.4 settled that the module exists; this phase settles what is in it.

## 2. Scope

- The module created empty by S0002 gets its content. FastMediaSorter's watch face is a Watch Face Format
  (WFF) face with no tourist content: no altitude, compass, sunrise or steps complication; its only tourist link
  is a shortcut slot that opens the program. The handoff's source map does not name that module's path: locate
  it in `P:\ANDROID\FastMediaSorter_mob_v2` (read-only) at ticket start and record it in the port ledger.
- If the face is a clone, it is ported in full and rebranded (full port, never a rewrite); if tourist
  complications are added, that is new code - an explicit exception to the extraction-only non-goal, written
  into this ticket before any line is written.
- The face's version family is the one registered for it in PACKAGE-VERSIONING by S0002's amendment.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002 (module, frozen id, version family); S0008 (the watch app the face accompanies).
- **4.4 Watch face composition (open, asked right before this phase):** a rebranded clone of FastMediaSorter's
  face, or a clone plus tourist complications (new code). Existence of the module was answered on 2026-10-02.

## 4. Acceptance

- [ ] The watch face installs on a Wear OS device or emulator and shows the time with the product branding.
- [ ] Its versionName and versionCode come from its registered family; the id is `com.sza.tourist.watchface`.
- [ ] Every ported file has a ledger row; drift check clean or decided.
