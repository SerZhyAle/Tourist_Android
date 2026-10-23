# PACKAGE-VERSIONING - pointer

| Field | Value |
| --- | --- |
| **Id** | `PACKAGE-VERSIONING` |
| **Version** | 0.2, draft (registry, 2026-10-02) |
| **Home** | shared contracts catalog, `package-versioning/` |
| **Owner** | FastMediaSorter Android |
| **Role here** | consumer, Android profile - versionName and versionCode of every package |

What this repository owes:

- Derive versionName and versionCode as the contract says, mechanically, from the product's registered families;
  never hand-bump.
- Before the first package ships: the package families added to the band table as a MINOR amendment written in
  the catalog first. Phone and watch share `com.sza.tourist` (and the noLegal variant shares it too), so they
  need disjoint bands; the watch face (`com.sza.tourist.watchface`) has its own family. The amendment is
  proposed through the canon session, never written from here (S0002).
- Not registered yet: the families wait for the frozen anchors, which the owner confirmed on 2026-10-02; the
  proposal is S0002's contract work.
- Proposal filed 2026-10-23 (S0002): `PROPOSAL-2026-10-23-tourist-android-bands.md` beside the contract,
  status `proposed` - phone `com.sza.tourist` band 0..5, watch app (shared id) band 6..9, watch face
  `com.sza.tourist.watchface` own namespace band 0..5, the noLegal edition on each family's row. The
  contract stays at its registry version until its owner folds the proposal in; nothing here reads a
  version the catalog has not accepted.
