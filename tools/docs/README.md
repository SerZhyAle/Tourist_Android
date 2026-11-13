# Documentation checks

`DOC-INTERNAL-QUALITY` rules 1-3, 5 and 6 over the internal corpus: `AGENTS.md`, `CLAUDE.md`, `README.md`,
every file under `docs/` and `PLAN/`, and the Markdown files under `tools/`. Rule 4 (generated documents) is the
help gate's (`tools/help/check-help.ps1`); rule 7 (offline safety) is held by reading.

| Script | When | What it does |
| --- | --- | --- |
| `check-docs.ps1 [-Check registry\|links\|style\|assets\|all]` | after any edit to the corpus, between releases | runs the checks below; `all` (the default) also runs the harness `document_registry/validate` and ends with `docs-check: <WORD>` |
| `check-docs.ps1 -Check <check> -UpdateBaseline` | when debt is accepted on purpose, with the reason in the ticket | rewrites that check's baseline entries and prints every identifier it accepts |
| `test-docs.ps1` | at release after editing these scripts | self-test on throw-away trees: each check passes clean and fails on its defect |

- **registry** - every record of both registries carries the harness fields, every declared path matches a file,
  every corpus file has exactly one record (or an ignore entry with a reason in `docs.config.json`), and a
  tracked registry names no git-ignored file while a local one names only ignored files. A registry is tracked
  or local by `git check-ignore --no-index` of its own file, so the answer follows `.gitignore`, not what is
  committed.
- **links** - relative Markdown links resolve to a file or folder and a `#fragment` to a heading of the target
  (GitHub's slug). Links inside a record marked `"archive": true` are not judged; links into it are.
- **style** - no three dots, no ellipsis character, no em or en dash in Markdown prose; fenced blocks, inline
  code and link targets are removed first.
- **assets** - image links, image paths written in code and numbered screenshot bookmarks resolve to files. A
  bookmark is the word screenshot followed by two-digit numbers in code (a hyphen between two numbers is a
  range) and names `NN_<what>.<image>` in the folder that carries the citing ticket's name. A tracked document
  may not cite a git-ignored asset.

The registries are `docs/DOCUMENT_REGISTRY.jsonl` (everything outside `PLAN/`) and `PLAN/DOCUMENT_REGISTRY.jsonl`
(beside the folder it indexes). A new document either matches an existing record's glob or gets a record of its
own; `generated: true` marks a render that is regenerated, never hand-edited.

Accepted debt lives in `baseline.txt`, a set baseline that may only fall: a finding it does not list fails,
and an entry no finding raises any more fails until it is removed. Exit codes: 0 PASS, 1 FAIL, 2 COULD NOT
VERIFY (unreadable configuration or registry, no git work tree, the harness missing).

Run every script as `pwsh -NoProfile -File tools/docs/<script>.ps1 ..` from the repository root, or the whole set
as `pwsh -NoProfile -File tools/fast-check.ps1 -Area docs`.
