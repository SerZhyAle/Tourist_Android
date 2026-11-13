# String-table gate

`pwsh -NoProfile -File tools/fast-check.ps1 -Area strings` (also run by `-Area ui`) judges every XML file under
`<module>/src/<source set>/res/values*` of `phone`, `wear` and `watchface`, all locales, byte by byte. It fails on
double-encoded text, invalid UTF-8, a BOM, U+FFFD, a C1 control character or a NUL. It needs no Gradle and no
device. Measured 2026-11-13 with a process-inclusive Stopwatch: gate 0.37 s direct, 0.6 s through
`fast-check.ps1`; `-Area ui` (this gate and the unified-UI gate) 0.95 s; self-test 1.8 s.

| File | Role |
| --- | --- |
| `check-string-tables.ps1` | The gate. `-Root <dir>` judges another checkout, `-Path <files>` exactly those files. Exit 0 PASS, 1 FAIL, 2 invalid input or no table found. Prints `file:line [string name] rule: detail`. |
| `lib.ps1` | The rules and the generated signature tables, dot-sourced by the two scripts. |
| `test-string-tables.ps1` | Self-test: planted damage must fail, clean Russian, Ukrainian and English text must pass, the exit codes and the discovery of the three modules are proved on throw-away trees. Run it after any edit of `lib.ps1`. |

The release runs both scripts: `tools/release/preflight.ps1` carries the `strings` and `strings-selftest` checks.

## What the gate recognises

A writer reads a UTF-8 file through a single-byte code page and writes the text back as UTF-8; every non-ASCII
character turns into two or three others. The degree sign `C2 B0` becomes U+0412 U+00B0 (cp1251) or U+00C2 U+00B0
(cp1252); the bullet `E2 80 A2` becomes U+0432 U+0402 U+045E or U+00E2 U+20AC U+00A2. The signatures are generated,
not listed by hand: about 140 typographic symbols and Latin-1 letters through five readers (cp1251, cp1252, cp1250,
cp866, ISO-8859-1), a three-byte symbol also by its first two bytes so a sequence that lost its last byte is found,
and three or more Cyrillic letters in a row read as pairs for whole-text damage.

Known blind spot, by design: the superscripts two and three read through cp1251 are U+0412 U+0406 and U+0412 U+0456,
the opening pair of the ordinary Ukrainian words for "distance" and "wind". A sequence that is only Russian or
Ukrainian letters in a natural case shape is never a signature, so those two (and a few siblings) are caught only
when the damage also hits another character, or through another reader. A value that needs such a character writes
it as an XML reference (`&#xB2;`) and is immune.

## The writer (S0042 finding)

No script of this repository writes a string table (`tools/` was searched for every writer of `strings.xml`;
`build-help.ps1` and `check-unified-ui.ps1` only read it). The damage of 2026-10-27 and 2026-11-11 came from an
editing step outside the repository tooling, and the sessions that did it left no transcript line that shows the
write, so the exact tool is not proven. What is proven, by reproduction on this machine on 2026-11-13: the system
ANSI code page is 1251, and Windows PowerShell 5.1 (`powershell.exe`) reads a file with no `-Encoding` through it.

| Writer | Result on a UTF-8 file with the degree sign |
| --- | --- |
| `powershell.exe`: `Get-Content -Raw` then `Set-Content -Encoding UTF8` | `EF BB BF` BOM, then `D0 92 C2 B0` |
| `powershell.exe`: `Get-Content -Raw` then `[IO.File]::WriteAllText(.., UTF8Encoding($false))` | no BOM, `D0 92 C2 B0` - the exact bytes S0041 found |
| `pwsh`: text read through code page 1251, `Set-Content -Encoding utf8NoBOM` | no BOM, `D0 92 C2 B0` |
| `powershell.exe`: `Get-Content` then `Set-Content`, no encoding either side | bytes unchanged (ANSI in, ANSI out) |

So the suspect is any step that decodes the table with Windows PowerShell 5.1 `Get-Content` (or any ANSI read) and
encodes it as UTF-8, with or without a BOM. The earlier correct edit seen in the 2026-10-29 transcript read and
wrote UTF-8 explicitly (`[IO.File]::ReadAllText`, `UTF8Encoding($false)`), and so is not the writer. To edit a
table from a script: `pwsh` (7), `[IO.File]::ReadAllText($path, [Text.UTF8Encoding]::new($false))` and
`[IO.File]::WriteAllText($path, $text, [Text.UTF8Encoding]::new($false))`; never `Get-Content` without `-Encoding utf8`
from `powershell.exe`. The gate is the net under that rule: run `-Area strings` (or `-Area ui`) after any table edit.
