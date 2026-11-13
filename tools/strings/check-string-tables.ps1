#requires -Version 7.0
<#
.SYNOPSIS
    Fails when a string table holds double-encoded (mojibake) text. Source-level: no Gradle, no device.
.DESCRIPTION
    Judges every XML file under <module>/src/<source set>/res/values* of phone, wear and watchface, all
    locales, byte by byte (rules in lib.ps1). The fast tier of CLAUDE.md section 7: `-Area strings` and
    `-Area ui` of tools/fast-check.ps1 run it; tools/release/preflight.ps1 runs it on the judged source.
.PARAMETER Root
    Repository root; default is two levels above this script, so a judged checkout judges itself.
.PARAMETER Path
    Judge exactly these files instead of the repository's tables (the self-test uses this).
.NOTES
    Exit codes: 0 PASS, 1 FAIL, 2 invalid input or no string table found.
#>
[CmdletBinding()]
param(
    [string]$Root,
    [string[]]$Path
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib.ps1')
if (-not $Root) { $Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot) }
try {
    $files = if ($Path) { @($Path | ForEach-Object { Get-Item -LiteralPath $_ -ErrorAction Stop }) } else { @(Get-StringTableFiles -Root $Root) }
} catch {
    Write-Host "string-tables: cannot read an input file: $($_.Exception.Message)"
    exit 2
}
if (-not $files.Count) {
    Write-Host "string-tables: no string table found under $Root"
    exit 2
}
$rootFull = [IO.Path]::GetFullPath($Root).TrimEnd('\', '/')
$failures = 0
foreach ($file in $files) {
    $name = if ($file.FullName.StartsWith($rootFull, [StringComparison]::OrdinalIgnoreCase)) { $file.FullName.Substring($rootFull.Length + 1) } else { $file.FullName }
    foreach ($finding in @(Test-StringTableBytes -Bytes ([IO.File]::ReadAllBytes($file.FullName)))) {
        $failures++
        $where = "$($name -replace '\\', '/'):$($finding.Line)"
        $key = if ($finding.Key) { " [$($finding.Key)]" } else { '' }
        Write-Host "FAIL: $where$key $($finding.Rule): $($finding.Detail)"
    }
}
Write-Host "Checked $($files.Count) string table file(s) for double encoding, invalid UTF-8, BOM, replacement and C1 characters."
Write-Host "string-tables: $(if ($failures) { "FAIL ($failures finding(s))" } else { 'PASS' })"
exit $(if ($failures) { 1 } else { 0 })
