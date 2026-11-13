#requires -Version 7.0
<#
.SYNOPSIS
    Run a source-level check for one changed area between releases.
.DESCRIPTION
    The fast tier of CLAUDE.md section 7: each area takes 5 seconds or less. Its release counterpart runs in
    tools/release/preflight.ps1 on the judged source.
      ui       tools/strings/check-string-tables.ps1, then tools/ui/check-unified-ui.ps1
                                                          release: the same gates, plus the UI device matrix
      strings  tools/strings/check-string-tables.ps1      release: the same gate and its self-test
      help     tools/help/check-help.ps1 -SkipFreshness   release: the full gate with freshness, the walk
      docs     tools/docs/check-docs.ps1 -Check all       release: the same gate, plus tools/docs/test-docs.ps1
      scripts  parses each -Path script                   release: the self-tests and the build-logic tests
.NOTES
    Exit codes: 0 PASS, 1 FAIL, 2 invalid input or could not verify.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet('ui', 'strings', 'help', 'docs', 'scripts')][string]$Area,
    [string[]]$Path
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
switch ($Area) {
    'ui' {
        # Strings first, so the last line stays the unified-ui verdict that tickets cite.
        & pwsh -NoProfile -File (Join-Path $root 'tools/strings/check-string-tables.ps1'); $strings = $LASTEXITCODE
        & pwsh -NoProfile -File (Join-Path $root 'tools/ui/check-unified-ui.ps1'); $ui = $LASTEXITCODE
        exit ([Math]::Max($strings, $ui))
    }
    'strings' { & pwsh -NoProfile -File (Join-Path $root 'tools/strings/check-string-tables.ps1'); exit $LASTEXITCODE }
    'help' { & pwsh -NoProfile -File (Join-Path $root 'tools/help/check-help.ps1') -SkipFreshness; exit $LASTEXITCODE }
    'docs' { & pwsh -NoProfile -File (Join-Path $root 'tools/docs/check-docs.ps1') -Check all; exit $LASTEXITCODE }
    'scripts' {
        if (-not $Path) { Write-Host 'fast-check: specify at least one PowerShell script with -Path'; exit 2 }
        foreach ($item in $Path) {
            if (-not (Test-Path -LiteralPath $item -PathType Leaf) -or [IO.Path]::GetExtension($item) -ne '.ps1') {
                Write-Host "fast-check: not a PowerShell script: $item"; exit 2
            }
            $tokens = $null; $errors = $null
            [void][System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path -LiteralPath $item).Path, [ref]$tokens, [ref]$errors)
            if ($errors.Count) { $errors | ForEach-Object { Write-Host $_ }; Write-Host "fast-check: FAIL $item"; exit 1 }
        }
        Write-Host "fast-check: PASS ($($Path.Count) PowerShell script(s) parsed)"
        exit 0
    }
}
