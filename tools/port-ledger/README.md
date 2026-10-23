# Port ledger and drift check

Handoff section 6a, mechanism 2: this product copies FastMediaSorter's kit code instead of sharing it,
so a fix made in FastMediaSorter after the copy has to be found and carried across. The ledger
(`docs/port-ledger.jsonl`) records what was copied from which version; the scripts here keep it honest.
They read FastMediaSorter and never write to it.

| Script | When | What it does |
| --- | --- | --- |
| `record-port.ps1 -Source <fms path> -Destination <path here>` | right after porting a file | adds the row with the source's SHA-256 as it is now |
| `check-drift.ps1 [-History] [-Json]` | start of every release | lists every ported file whose source changed since; exit 1 if any is uncarried |
| `record-port.ps1 ... -Refresh` | after carrying a newer version of a file | the row takes the source's current hash and gets `carriedOn` |
| `decide-drift.ps1 -Source <fms path> -Note "<why>"` | a change is deliberately not carried | writes the decision next to the row, pinned to that source version |
| `test-port-ledger.ps1` | after editing these scripts | self-test on a throw-away tree; touches neither repository |

Paths are relative to their repository root, with forward slashes. The FastMediaSorter root is
`P:\ANDROID\FastMediaSorter_mob_v2` unless `-SourceRoot` or `$env:TOURIST_FMS_ROOT` says otherwise.

A decision stops covering a file the moment FastMediaSorter changes it again: the next change is drift
until it is carried or decided too. An empty ledger is in step by definition. Exit codes of
`check-drift.ps1`: 0 nothing uncarried, 1 at least one `DRIFT`, `SOURCE GONE` or `NO DEST` row, 2 the
check could not run.

Run every script as `pwsh -NoProfile -File tools/port-ledger/<script>.ps1 ..` from the repository root.
