# S0033 - CLAUDE.md says PLAN/ is tracked while .gitignore ignores it

**Ticket:** S0033
**Status:** Verified
**Complexity:** low - rewritten at the plan pass of 2026-11-08: the owner's answer drops one `.gitignore` line and
four records that describe `PLAN/` as ignored or local follow it (five existing files, wording only); no code, no
build, no schema, no new file
**Date:** 2026-10-29

## Captured request and migration scope

Parked by S0029 (2026-10-29) as an out-of-scope finding; nothing was changed for it.

**Symptom.** `CLAUDE.md` section 4, release package plan item 6, says `PLAN/` is **tracked** ("there is no
remote yet, so an ignored ticket store would exist in exactly one place, and the tickets carry the owner's
recorded decisions"). On disk `.gitignore` ends with a `PLAN/` line (an uncommitted edit), so every new file
under `PLAN/` is ignored while the files committed earlier stay in the index.

**Evidence.** `git check-ignore -v --no-index PLAN/S0029_doc-internal-quality-mechanization.md` prints
`.gitignore:51:PLAN/`; `git ls-files PLAN` still lists the earlier commits' files.

**What is owed.** An owner call: either `PLAN/` is tracked (drop the `.gitignore` line) or it is local (rewrite
`CLAUDE.md` section 4 item 6, and decide what happens to the files already in the index). The document checks
of S0029 hold under both: `tools/docs/check-docs.ps1` classes `PLAN/DOCUMENT_REGISTRY.jsonl` tracked or local
from `.gitignore` alone, so either answer needs no change there.

## Spec pass 2026-10-29 (unattended queue run)

**Rung:** primitive spec by size (one or two files, one line each), held at the spec stage because the one
deferred decision below is the owner's - the primitive test needs "no deferred decisions", so nothing is
implemented until it is answered.

**Shared-contract boundary:** none. Whether `PLAN/` is committed changes no format, wire shape or behaviour any
other product reads.

### Research - what the records already say (re-checked this pass)

- `CLAUDE.md` section 4, release package plan item 6: `PLAN/` is **tracked**, because there is no remote and
  the tickets carry the owner's recorded decisions. The canon contrib record `rules/contrib/tourist_mob.md`
  (plugin 2026.1029.10, lines 47 and 103) records the same as this repository's divergence from the canon.
- The canon default is the opposite: `NEW_PROJECT_CHECKLIST` section 1 and the harness `SCHEMA.md` put `PLAN/`
  under `.gitignore`, and most portfolio repositories ignore it.
- On disk: `.gitignore` line 51 is `PLAN/`, an uncommitted edit with no trailing newline; no ticket and no
  decision record names who added it or why. `git check-ignore -v --no-index PLAN/S0033_plan-tracked-or-ignored.md`
  prints `.gitignore:51:PLAN/` (exit 0); `git ls-files PLAN` lists 32 files; `git remote -v` prints nothing, so
  the reason given for tracking still holds.
- `tools/docs/check-docs.ps1` (S0029) reads the tracked/local class of `PLAN/DOCUMENT_REGISTRY.jsonl` from
  `.gitignore` alone; neither answer needs a change there.

The recorded decision (tracked) and the newest edit on disk (ignored) disagree, and the edit carries no author.
Picking either would be guessing the owner's intent, so this is asked, not settled.

### Owner input - decided (2026-11-08, quiz)

**Question 1.** Is `PLAN/` committed to git in this repository?

**Decided (owner, 2026-11-08): 1, tracked.** The answer-2 steps below are not taken.

1. **Tracked (recommended)** - drop the `PLAN/` line from `.gitignore`. Matches `CLAUDE.md` section 4 item 6
   and the canon contrib record as written; the ticket store and its recorded decisions keep a second copy in
   git while there is no remote. Files touched: `.gitignore`.
2. **Local (canon default)** - keep the `.gitignore` line, rewrite `CLAUDE.md` section 4 item 6 to say `PLAN/`
   is local, and record the changed divergence in `rules/contrib/tourist_mob.md`. Follow-up question: the 32
   files already in the index stay there frozen, or leave it with `git rm -r --cached PLAN` in the next commit
   (they stay on disk either way).

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0029 (the document-internal quality mechanization that parked this finding; its
  `tools/docs/check-docs.ps1` reads the tracked or local class of `PLAN/DOCUMENT_REGISTRY.jsonl` from
  `.gitignore` alone, so it needs no change under this answer).
- **Is `PLAN/` committed (Question 1):** decided 2026-11-08 (owner, quiz): tracked; the `PLAN/` line leaves
  `.gitignore`, and `CLAUDE.md` section 4 item 6 and the canon contrib record stay as written.

### Quiz decisions (2026-11-08)

- Is `PLAN/` committed to git in this repository? -> Tracked (answer 1: drop the `PLAN/` line from `.gitignore`;
  it matches `CLAUDE.md` section 4 item 6 and the canon contrib record, and there is still no remote, so an
  ignored ticket store would exist in one place only; the owner took the recommended option).
- Resolved without asking (facts re-read 2026-11-08): `.gitignore` line 51 is still `PLAN/` (an uncommitted
  addition with no trailing newline); `CLAUDE.md` section 4 item 6 still reads "tracked"; `git remote -v`
  prints nothing. The 32 indexed files and the answer-2 follow-up (freeze or `git rm -r --cached PLAN`) fall
  away with answer 1.

### Plan, per answer

- Answer 1: step 1 - delete line 51 of `.gitignore`; check: `git check-ignore -v --no-index PLAN/S0033_plan-tracked-or-ignored.md`
  exits 1. Step 2 - `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs` exits 0.
- Answer 2: step 1 - rewrite `CLAUDE.md` section 4 item 6; check: grep finds "local" there and no "tracked".
  Step 2 - the contrib record line in the canon repository's `rules/contrib/tourist_mob.md` (written from this
  repository's session, the one canon file it may edit). Step 3 - only if the owner chose it, `git rm -r
  --cached PLAN` in the next commit flow; check: `git ls-files PLAN` prints nothing. Step 4 - `-Area docs`
  exits 0.

## Plan pass 2026-11-08 (unattended queue run)

**Rung moved up:** complex, not primitive. The primitive test allows at most three existing files; answer 1 touches
five, because four records outside `.gitignore` state that `PLAN/` is ignored or local and become false with the
line. The steps below replace "Plan, per answer".

**Shared-contract boundary:** still none. `DOC-INTERNAL-QUALITY` 0.11 lets an internal corpus be local and does not
require it; its item J keeps `docRegistryFile` in the stamp as the registry a program reads, and item K only binds a
folder of which part is ignored, which `PLAN/` stops being.

**Read and left alone, because they stay true:** `tools/release/preflight.ps1` (`temp/` stays ignored, and its
snapshot still copies without `PLAN/`), `tools/docs/test-docs.ps1` (its `.gitignore` is a fixture in a throw-away
tree), `tools/docs/check-docs.ps1` and `docs.config.json` (they class a registry by `git check-ignore`, whichever
way it comes out), the harness (`spec_catalog` asks git for the branch name and nothing else), `CLAUDE.md` section 4 item 6 and the canon
contrib record (owner decision: as written).

### Coverage

| Line of the contract | Step |
| --- | --- |
| Owner answer 1: the `PLAN/` line leaves `.gitignore` | 1 |
| Records that say `PLAN/` is ignored or local stop saying it | 2, 3, 4, 5 |
| `tools/docs/check-docs.ps1` needs no change under the answer | 6 |
| The second copy in git | out-of-scope: a commit, which nobody asked for in this pass |
| Answer 2 and its follow-up | out-of-scope: the owner chose answer 1 |

### Steps

Every step produces its own file and consumes nothing a later step produces.

1. `.gitignore`: delete the `PLAN/` line and the blank line above it. Check: `git check-ignore -v --no-index
   PLAN/S0033_plan-tracked-or-ignored.md` exits 1 and `git diff --quiet -- .gitignore` exits 0.
2. `docs/DOCUMENT_REGISTRY.jsonl`, record `document-registry`: the title stops saying "tracked corpus" and the notes
   stop saying "ignored". Check: grep for `tracked corpus` and `ignored PLAN` finds nothing.
3. `PLAN/DOCUMENT_REGISTRY.jsonl`, record `plan-document-registry`: the notes stop saying "local half". Check: grep
   for `local half` finds nothing.
4. `tools/docs/README.md`: the docs registry stops being "tracked part". Check: grep for `tracked part` finds nothing.
5. `docs/contracts/DOCUMENTATION-QUALITY.md`, rule 1 of the pointer: same wording. Check: grep as in step 4.
6. Run `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs`. Check: exit 0, and `docs-registry` reports
   `PLAN/DOCUMENT_REGISTRY.jsonl tracked`.

## Last Audit

**Date:** 26-11-08 17:43 | **Mode:** static | **Outcome:** Verified
**Counts:** PASS 9 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 2 | RELEASE 1

| # | Check | Command or source | Outcome | Expected / actual |
| --- | --- | --- | --- | --- |
| 1 | The `PLAN/` line left `.gitignore` | `git check-ignore -v --no-index PLAN/S0033_plan-tracked-or-ignored.md` | PASS | exit 1 / exit 1, no rule printed |
| 2 | `.gitignore` is back to its committed bytes | `git diff --quiet -- .gitignore` | PASS | exit 0 / exit 0 |
| 3 | Nothing under `PLAN/` stays ignored by another rule (`*token*`, `*secret*`) | `git ls-files -o -i --exclude-standard PLAN` | PASS | 0 files / 0 files |
| 4 | The four records stopped saying ignored or local | grep for `tracked corpus`, `ignored PLAN`, `local half`, `tracked part` | PASS | no hit / no hit |
| 5 | No other live document claims `PLAN/` is ignored | grep of the repository outside the frozen `PLAN/DONE/` | PASS | only `CLAUDE.md` section 4 item 6 and this ticket's own history |
| 6 | The decision records stay as written | `CLAUDE.md` section 4 item 6; canon contrib record of plugin 2026.1107.1, lines 47 and 103 | PASS | "tracked" / "tracked" in all three |
| 7 | The document checks hold under the answer, with no change to them | `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs` | PASS | exit 0, `PLAN/DOCUMENT_REGISTRY.jsonl tracked` / exit 0, `docs-check: PASS (5 checks)`, that class printed, 1.93 s |
| 8 | Files the line hid carry no secret | scan of the 83 text files under `PLAN/` for key, password-value and token shapes; a control word proved the files were read | PASS | 0 hits / 0 hits; the 45 images were not read |
| 9 | No shared-contract boundary | `DOC-INTERNAL-QUALITY` 0.11, amendment 2026-10-17 and items J, K | PASS | a local corpus is allowed, not required / nothing in the catalog changes |
| 10 | Script self-test | `tools/docs/test-docs.ps1` | EXEMPT | no script was edited |
| 11 | The second copy in git | a commit | EXEMPT | not asked for in this pass; the files are no longer hidden from one |
| 12 | The docs gate over a commit that carries `PLAN/` | `pwsh -NoProfile -File tools/release/preflight.ps1` | RELEASE | not run; release tier, and no such commit exists yet |

Not built, not tested until the release; no code changed, so no Gradle rung applies to this ticket. No probe was
placed: grep for the ticket id under the three module source trees finds nothing.

### Action items

None.

### Manual / on-target

None.
