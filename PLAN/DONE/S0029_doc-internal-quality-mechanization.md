# S0029 - Mechanize DOC-INTERNAL-QUALITY for the internal corpus

**Ticket:** S0029
**Status:** Implemented
**Status note:** Implemented 2026-10-29: registries for docs/ and PLAN/, tools/docs/check-docs.ps1 (registry, links, style, assets) via fast-check -Area docs, exit 0 in 1.8 s; self-test 21 cases green (8.7 s, release tier); catalog exception closed, adoption row 0.11. Parked: S0033.
**Complexity:** medium - assessed at the spec pass 2026-10-29, confirmed at the plan pass: new tooling only
(one check script with four checks, two registry files, a self-test, a fast-check area), no product code, no
Gradle, no shared format created; the catalog side is two registry rows and a pointer
**Date:** 2026-10-26

## Captured request and migration scope

Created by S0016 (the 2026-10-17 contract synchronization) as the remediation ticket of the dated
`DOC-INTERNAL-QUALITY` exception recorded in the catalog registry, section 3 (Tourist Android, until
2026-12-31): the internal corpus follows rules 1-3 and 5-6 by hand, with no machine check.

## 1. Problem and authority

`DOC-INTERNAL-QUALITY` 0.10 (draft, owner FastMediaSorter Android; pointer
`docs/contracts/DOCUMENTATION-QUALITY.md`) binds this repository as a product maintaining developer
documentation. The audit of 2026-10-26 found the generated-family sync (rule 4) and offline safety
(rule 7) held, but the mechanization of the remaining rules absent. This ticket builds it; until it
closes, no conformance beyond the exception's `partial` reading is claimed.

## 2. Scope

The corpus is the maintained developer documentation: `CLAUDE.md`, `README.md`, `docs/` and `PLAN/`.

1. **Registry (rule 1).** One `DOCUMENT_REGISTRY.jsonl` listing every maintained document with its
   topic, product area, update triggers and role (`source` or `render`). Per the 2026-10-17 amendment,
   a local gitignored internal corpus may keep its registry local; this repository's tracked docs are
   the corpus, so the registry is tracked beside it.
2. **Reverse coverage (rule 2).** A check walks registry to disk and disk to registry (every
   documentation file declared, or on an explicit ignore list) and fails on an undeclared new file.
3. **Cross-link and anchor integrity (rule 3).** Relative links and section anchors inside the corpus
   resolve; a moved document updates every referencing link in the same change.
4. **House text style (rule 5).** A style check for the prose rules that are mechanical here: plain
   hyphens, `..` rather than `...`, no em/en dashes in prose.
5. **Asset bookmarks (rule 6).** Every image a maintained document cites resolves to a tracked file.

The generated families already gated elsewhere stay out of scope: the help pages and screenshots are
`tools/help/check-help.ps1`'s business (rule 4), the ticket store is `spec_catalog/validate`'s, and
`PLAN/DONE/` is a frozen archive spelling (REPO-LAYOUT rule 6) treated as ignored, not maintained.

## 3. Acceptance

- The registry, the reverse-coverage, cross-link, style and bookmark checks exist under `tools/` and
  run through the harness-adjacent path this repository uses for its own gates; each prints one
  `CHECK-VERDICT` verdict line (that contract's cell format, exit 0/1/2).
- All checks exit 0 on the corpus as it stands, with any accepted debt in a baseline that may only
  fall; a deliberately broken link or undeclared file fails the corresponding check (negative case).
- The registry exception row in the catalog is closed or narrowed by this repository with fresh
  evidence; the pointer states what is mechanized.

## 4. Out of scope

No contract text is edited (the owner's); deviations are proposed beside the contract. No external
documentation corpus is created (`DOC-EXTERNAL-QUALITY` stays not applicable until a site opens,
decision 4.9 of S0010). No release is implied.

## 5. Spec pass (2026-10-29, unattended queue pass)

**Rung: complex.** New files (a check script, two registries, a self-test, a baseline), so the primitive test
fails on "no new files".

**Contract boundary.** None crossed. The checks read this repository's own documents and write nothing another
product reads. The two registries use the reference implementation's record shape (the shipped harness
`document_registry/validate` reads it), so no new format is invented. The catalog side is this product's own
adoption row, its exception row and its pointer, written after the evidence exists; no contract text changes.

**What the contract says today.** `DOC-INTERNAL-QUALITY` moved to 0.11 on 2026-10-29 (clarification, items J,
K, L, Q). Read for this pass:

- *J:* the default paths stand where the stamp names no `docRegistryFile`; a repository that holds both a
  tracked and a local registry names the tracked one in its stamp.
- *K:* in a partly tracked tree the tracked registry indexes only the tracked part and the local registry only
  the ignored part; neither names a file the other holds; the local registry sits beside the folder it indexes.
- *L:* rules 2 and 3 are shown only by a check that runs reverse coverage and link resolution, named in the
  adoption row.
- *Q:* a declared frozen archive is exempt from rule 3 for links inside it; links into it must resolve and rule
  2 still lists its files. This supersedes the capture's "`PLAN/DONE/` treated as ignored": the archive is
  registered, its inner links are not judged.

**Finding that shaped the design.** On disk `.gitignore` ends with `PLAN/` (an uncommitted edit), while
`CLAUDE.md` section 4 item 6 says `PLAN/` is tracked. Which one the owner means is not this ticket's call, so
the design holds under both: each registry is classed by `git check-ignore` of its own file - not ignored is a
tracked registry and may name only files that are not ignored; ignored is a local registry and may name only
ignored files. Today that makes `PLAN/DOCUMENT_REGISTRY.jsonl` local; if `PLAN/` is tracked again it becomes a
second tracked registry with no edit. The contradiction itself is parked (see the audit).

**Decisions (architecture-answered, not owner calls):**

1. **Corpus.** `AGENTS.md`, `CLAUDE.md`, `README.md`, every file under `docs/` and `PLAN/`, and the `*.md`
   files under `tools/` (the tool READMEs are maintained developer documents the same rules bind; the
   capture's list omitted them). Build output and `temp/` are outside.
2. **Two registries.** `docs/DOCUMENT_REGISTRY.jsonl` (the harness default path, tracked part) and
   `PLAN/DOCUMENT_REGISTRY.jsonl` (beside the folder it indexes). Record shape is the harness's required set
   (`id`, `title` as the topic, `category`, `audience`, `paths`, `published`, `indexable`, `product_areas`,
   `update_triggers`, `generated`); `generated: true` is role `render`, `false` is role `source`; an optional
   `archive: true` declares a frozen archive (item Q). The stamp names the tracked one (item J).
3. **Ignore list.** An explicit list in the check's configuration, empty unless a file is consciously left out.
4. **Links (rule 3).** Inline and reference-style Markdown links and image links outside code; a relative
   target resolves to a file or folder; a `#fragment` resolves to a heading of the target by GitHub's slug.
   External links are rule 7 and not judged.
5. **Style (rule 5).** In Markdown prose (fenced blocks and inline code removed): no `...` or `U+2026`, no
   em or en dash. Diacritics and code comments are not mechanical here and stay out. The archive and the
   handoff record (`docs/BOOTSTRAP_HANDOFF.md`, unedited by rule of `CLAUDE.md` section 1) are judged too, and
   any hit in them goes to the baseline rather than an edit.
6. **Bookmarks (rule 6).** Every image link resolves to a file; every image path written in code resolves; a
   numbered screenshot bookmark (the word screenshot followed by two-digit numbers in code, ranges
  included) resolves to `NN_*.<image>` in the
   folder that carries the citing ticket's name. A cited asset of a tracked document must not be git-ignored.
7. **Baseline.** A set baseline (CHECK-BASELINE rule 3), one identifier per line, sorted, beside the check: a
   finding not in it fails, an entry with no finding any more fails until removed - so it may only fall.
8. **Verdicts.** Each check prints `docs-<check>: <WORD> (<counts>)` as its last line, exit 0/1/2; `all` runs the
   four in one process, quotes their lines and the harness validator's (translated: its 0/1/2 map to
   PASS/FAIL/COULD NOT VERIFY, rule 15), and ends with the aggregate line by rule 14's precedence.
9. **Fast tier.** One process plus the harness child; it joins `tools/fast-check.ps1` as `-Area docs` if one
   timed call is 5 s or less, else it is release tier and says so.

**Open research items:** none.

## Owner inputs (Approval gate)

- **Owner decisions needed:** none - every choice above is answered by the contract (0.11 items J, K, L, Q),
  the harness record shape or `CLAUDE.md`; the tracked-or-ignored state of `PLAN/` is held either way.
- **Related tickets:** S0016 created this ticket and recorded the exception; S0014 owns the help gate (rule 4);
  S0030 owns the fast-check wrapper this ticket adds an area to. None blocks this ticket.

## 6. Plan

Coverage: acceptance 1 -> P1-P3; acceptance 2 -> P2 (baseline), P3 (negative cases); acceptance 3 -> P4;
scope items 1-2 -> P1-P2, 3 -> P2, 4 -> P2, 5 -> P2; decisions 1-9 -> P1-P3; out-of-scope: rule 4 (held by
the help gate), rule 7 (held in fact), `DOC-EXTERNAL-QUALITY` (no site).

Topology: P1 produces the registries and the config; P2 consumes them and produces the check and the baseline;
P3 consumes the check and produces the self-test and the fast-check area; P4 consumes all and produces the
catalog rows, the pointer and the stamp key. No phase consumes a later product.

- [x] P1 Registries and config: `docs/DOCUMENT_REGISTRY.jsonl`, `PLAN/DOCUMENT_REGISTRY.jsonl`,
  `tools/docs/docs.config.json`. Static check: both registries parse line by line as JSON; the harness
  `document_registry/validate` exits 0 on the tracked one.
- [x] P2 Check: `tools/docs/check-docs.ps1` (`-Check registry|links|style|assets|all`, `-Root`), baseline
  `tools/docs/baseline.txt`. Static check: `check-docs.ps1 -Check all` exits 0 on the corpus and prints the
  aggregate line last.
- [x] P3 Negative cases and fast tier: `tools/docs/test-docs.ps1` on a throw-away tree under `temp/`;
  `tools/fast-check.ps1 -Area docs`; `tools/docs/README.md`; `CLAUDE.md` section 7 line. Static check: the
  self-test exits 0 with every negative case failing its check; one timed fast-check call.
- [x] P4 Catalog and pointer: adoption row 0.11 naming the check (item L), the section 3 exception row closed,
  the pointer rewritten, the stamp's `docRegistryShape`/`docRegistryFile`. Static check: grep of each row and
  the stamp parses.

## 7. Evidence (2026-10-29)

Phase boundaries: P1 - both registries parse (12 and 8 records) and the harness `document_registry/validate`
exits 0 (`Document registry PASS: 12 record(s)`). P2 - the first run exposed one P1-class defect, fixed in the
phase: `git check-ignore --stdin` fed from PowerShell received CR-terminated paths and matched nothing, so
`PLAN/` read as tracked; paths now go as arguments, and the registry check classes `PLAN/DOCUMENT_REGISTRY.jsonl`
local. P3 - self-test all green. P4 - catalog rows, pointer, pointer index and stamp written.

- `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs`: exit 0, expected `docs-check: PASS` | actual
  `docs-check: PASS (5 checks)`, 1.757 s and 1.778 s (two Stopwatch-timed calls, process-inclusive) - fast tier.
  Children: `docs-registry: PASS (2 registries (docs/DOCUMENT_REGISTRY.jsonl tracked, PLAN/DOCUMENT_REGISTRY.jsonl
  local), 20 records, 187 files declared, 0 ignored)`, `docs-links: PASS (23 link(s), 0 anchor(s) in 53 file(s))`,
  `docs-style: PASS (63 file(s), 4 baselined)`, `docs-assets: PASS (45 citation(s))`,
  `docs-harness-registry: PASS (document_registry/validate exit 0)`.
- Bookmark count cross-checked by hand: 41 numbered screenshots in four tickets plus four on a wrapped line of
  S0004 = 45 citations, all resolving.
- `pwsh -NoProfile -File tools/docs/test-docs.ps1`: exit 0, expected every case held | actual
  `docs-selftest: PASS (run=21 skipped=0)`, 8.686 s - timed once on the expectation it was fast; it is not, so it
  is release tier. Negative cases held: undeclared file, declared file missing, tracked registry naming an
  ignored file, broken link, broken anchor, broken link into the archive, ellipsis and em dash in prose, stale
  baseline entry, missing screenshot, broken image link, tracked document citing an ignored asset, aggregate
  FAIL (exit 1 each); invalid registry JSON and no git work tree (exit 2, `COULD NOT VERIFY`); a broken link
  inside the archive and an ellipsis in code pass.
- Baseline accepted on purpose (`-Check style -UpdateBaseline`, four identifiers printed): the owner's orders
  quoted in S0025-S0028 carry three dots inside the quotation; the quotation stays verbatim.
- `pwsh -NoProfile -File tools/fast-check.ps1 -Area scripts -Path <each>` for `tools/fast-check.ps1`,
  `tools/docs/check-docs.ps1`, `tools/docs/test-docs.ps1`: exit 0 each, `fast-check: PASS (1 PowerShell script(s)
  parsed)`.
- Stamp: `.sza-canon.json` parses, `docRegistryShape=1 docRegistryFile=docs/DOCUMENT_REGISTRY.jsonl`.
- Catalog: `_meta/REGISTRY.md` adoption row `DOC-INTERNAL-QUALITY` / Tourist Android now reads 0.11, 2026-10-29,
  naming `tools/docs/check-docs.ps1` (grep count 1); the section 3 exception row is removed (grep count 0); CRLF
  and the missing BOM kept, only those two rows differ.
- `spec_catalog/validate`: exit 0, `8 OK, 2 WARN, 0 FAIL`; both warnings predate this ticket (id gap S0024, files
  moved to `PLAN/DONE/`).
- Probe sweep: `grep -rn S0029 phone/src wear/src watchface/src`, 0 hits, expected 0.
- Documentation context: affected - `CLAUDE.md` section 7, `docs/contracts/DOCUMENTATION-QUALITY.md`,
  `docs/contracts/README.md`, the new `tools/docs/README.md` and both registries; unchanged - the product README,
  help sources and generated help assets. No user-facing surface changes, so no ship-together pass.
- Parked: S0033 (`CLAUDE.md` section 4 item 6 says `PLAN/` is tracked; `.gitignore` ignores it).

## Last Audit

**Date:** 26-10-29 04:30 | **Mode:** static | **Outcome:** Verified
**Counts:** PASS 9 | WARN 0 | FAIL 0 | MANUAL 0 | UNCHECKABLE 0 | EXEMPT 3 | RELEASE 1

PASS: registry and config (acceptance 1, scope 1); reverse coverage (scope 2); links and anchors (scope 3);
style (scope 4); bookmarks (scope 5); verdict lines and exit codes (acceptance 1); exit 0 on the corpus with a
fall-only baseline (acceptance 2); negative cases (acceptance 2); catalog row and pointer (acceptance 3).
EXEMPT: rule 4 (held by the help gate), rule 7 (held in fact), `DOC-EXTERNAL-QUALITY` (no site).

### Action items

1. RELEASE: `pwsh -NoProfile -File tools/docs/test-docs.ps1` (8.7 s) runs at the release after an edit to
   `tools/docs/`; the release path itself is S0030's to extend. Not part of any Gradle build.
2. Parked S0033 for the owner; the checks hold under either answer.
