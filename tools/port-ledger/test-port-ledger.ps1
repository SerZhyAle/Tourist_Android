#requires -Version 7.0
<#
.SYNOPSIS
    Self-test of the port ledger tools on a throw-away tree.

.DESCRIPTION
    Builds a fake FastMediaSorter root and a fake destination root under temp/, points record-port,
    check-drift and decide-drift at them with -SourceRoot / -DestinationRoot / -Ledger, and asserts the
    exit codes and the ledger bytes of every mechanism of handoff section 6a item 2. Neither this
    repository nor FastMediaSorter is touched. Run after editing any script of this folder:

        pwsh -NoProfile -File tools/port-ledger/test-port-ledger.ps1

    Exit code: 0 every case passed; 1 at least one failed. Case names print as they run.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [string] $SourceRoot,
    [string] $Ledger,
    [string] $DestinationRoot
)

$ErrorActionPreference = 'Stop'
if ($SourceRoot -or $Ledger -or $DestinationRoot) {
    throw 'test-port-ledger takes no roots: it builds its own throw-away tree and touches nothing else.'
}

$script:Passed = 0
$script:Failed = 0

function Assert-Case {
    param([Parameter(Mandatory)][string] $Name, [Parameter(Mandatory)][bool] $Condition, [string] $Detail)
    if ($Condition) {
        $script:Passed++
        Write-Host "  PASS  $Name" -ForegroundColor Green
    }
    else {
        $script:Failed++
        Write-Host "  FAIL  $Name$(if ($Detail) { " - $Detail" })" -ForegroundColor Red
    }
}

function Invoke-Tool {
    # Runs one tool script as a child process so the case sees the real exit code the release run sees.
    param([Parameter(Mandatory)][string] $Script, [string[]] $Arguments = @())
    $output = & pwsh -NoProfile -File (Join-Path $PSScriptRoot $Script) @Arguments 2>&1
    return [pscustomobject]@{ Code = $LASTEXITCODE; Text = ($output -join "`n") }
}

$tree = Join-Path ([System.IO.Path]::GetTempPath()) ("tourist-port-ledger-selftest-" + [guid]::NewGuid().ToString('N'))
$fakeFms = Join-Path $tree 'fms'
$fakeRepo = Join-Path $tree 'repo'
$ledgerPath = Join-Path $fakeRepo 'docs\port-ledger.jsonl'
New-Item -ItemType Directory -Path (Join-Path $fakeFms 'app_v2\src\main\java\x'), (Join-Path $fakeRepo 'phone\src\main\java\x'), (Join-Path $fakeRepo 'docs') -Force | Out-Null

$common = @('-SourceRoot', $fakeFms, '-DestinationRoot', $fakeRepo, '-Ledger', $ledgerPath)

# Local reader so the assertions see the file the tools wrote, without dot-sourcing the core here.
function Get-LedgerRowsFromPath {
    $line = [System.IO.File]::ReadAllLines($ledgerPath) | Where-Object { $_ -match '\S' } | Select-Object -First 1
    return ($line | ConvertFrom-Json -AsHashtable)
}

try {
    Write-Host "port ledger self-test (tree: $tree)"

    $sourceRel = 'app_v2/src/main/java/x/Sensor.kt'
    $destRel = 'phone/src/main/java/x/Sensor.kt'
    Set-Content -LiteralPath (Join-Path $fakeFms 'app_v2\src\main\java\x\Sensor.kt') -Value 'class Sensor' -NoNewline
    Set-Content -LiteralPath (Join-Path $fakeRepo 'phone\src\main\java\x\Sensor.kt') -Value 'class Sensor' -NoNewline
    $expectedHash = (Get-FileHash -LiteralPath (Join-Path $fakeFms 'app_v2\src\main\java\x\Sensor.kt') -Algorithm SHA256).Hash.ToLowerInvariant()

    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'absent ledger is in step by definition' ($run.Code -eq 0) $run.Text

    $run = Invoke-Tool 'record-port.ps1' (@('-Source', $sourceRel, '-Destination', $destRel) + $common)
    Assert-Case 'record-port adds a row' ($run.Code -eq 0) $run.Text
    $row = (Get-LedgerRowsFromPath)
    Assert-Case 'the row hashes the source, not a typed value' ($row['sourceSha256'] -eq $expectedHash)

    $run = Invoke-Tool 'record-port.ps1' (@('-Source', $sourceRel, '-Destination', $destRel) + $common)
    Assert-Case 'a duplicate pair is refused' ($run.Code -eq 2) $run.Text

    Set-Content -LiteralPath (Join-Path $fakeFms 'app_v2\src\main\java\x\Other.kt') -Value 'class Other' -NoNewline
    $run = Invoke-Tool 'record-port.ps1' (@('-Source', 'app_v2/src/main/java/x/Other.kt', '-Destination', $destRel) + $common)
    Assert-Case 'a second source for one destination is refused' ($run.Code -eq 2) $run.Text

    $run = Invoke-Tool 'record-port.ps1' (@('-Source', 'app_v2/src/main/java/x/Nowhere.kt', '-Destination', $destRel) + $common)
    Assert-Case 'a missing source is refused' ($run.Code -eq 2) $run.Text

    $run = Invoke-Tool 'record-port.ps1' (@('-Source', '../escape.kt', '-Destination', $destRel) + $common)
    Assert-Case 'a path escaping the source root is refused' ($run.Code -eq 2) $run.Text

    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'a matching hash is in step' ($run.Code -eq 0 -and $run.Text -match '1 row\(s\)') $run.Text

    Set-Content -LiteralPath (Join-Path $fakeFms 'app_v2\src\main\java\x\Sensor.kt') -Value 'class Sensor2' -NoNewline
    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'a changed source is DRIFT (exit 1)' ($run.Code -eq 1 -and $run.Text -match 'DRIFT') $run.Text

    $run = Invoke-Tool 'check-drift.ps1' (@('-Json') + $common)
    $parsed = $run.Text | ConvertFrom-Json
    Assert-Case 'the JSON report counts the uncarried row' ($run.Code -eq 1 -and $parsed.uncarried.Count -eq 1) $run.Text

    $run = Invoke-Tool 'decide-drift.ps1' (@('-Source', $sourceRel, '-Note', 'the sorter-only branch of the fix') + $common)
    Assert-Case 'a decision is written next to the row' ($run.Code -eq 0) $run.Text
    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'a decided row is not uncarried' ($run.Code -eq 0 -and $run.Text -match 'decided') $run.Text

    Set-Content -LiteralPath (Join-Path $fakeFms 'app_v2\src\main\java\x\Sensor.kt') -Value 'class Sensor3' -NoNewline
    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'the next source change is drift again' ($run.Code -eq 1) $run.Text

    Set-Content -LiteralPath (Join-Path $fakeRepo 'phone\src\main\java\x\Sensor.kt') -Value 'class Sensor3' -NoNewline
    $run = Invoke-Tool 'record-port.ps1' (@('-Source', $sourceRel, '-Destination', $destRel, '-Refresh') + $common)
    Assert-Case 'a carried change refreshes the row' ($run.Code -eq 0) $run.Text
    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'a carried row is in step again' ($run.Code -eq 0) $run.Text
    $row = (Get-LedgerRowsFromPath)
    Assert-Case 'a refresh sets carriedOn and drops the decision' ($row.ContainsKey('carriedOn') -and -not $row.ContainsKey('decision'))

    Remove-Item -LiteralPath (Join-Path $fakeFms 'app_v2\src\main\java\x\Sensor.kt')
    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'a removed source is SOURCE GONE (exit 1)' ($run.Code -eq 1 -and $run.Text -match 'SOURCE GONE') $run.Text
    $run = Invoke-Tool 'decide-drift.ps1' (@('-Source', $sourceRel, '-Note', 'the source moved; superseded by our own S0005 change') + $common)
    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'a removal can be decided away' ($run.Code -eq 0) $run.Text

    Remove-Item -LiteralPath (Join-Path $fakeRepo 'phone\src\main\java\x\Sensor.kt')
    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'a missing destination is NO DEST (exit 1)' ($run.Code -eq 1 -and $run.Text -match 'NO DEST') $run.Text

    Set-Content -LiteralPath $ledgerPath -Value '{"source": "a.kt"}'
    $run = Invoke-Tool 'check-drift.ps1' $common
    Assert-Case 'a malformed ledger stops the check (exit 2)' ($run.Code -eq 2) $run.Text
}
finally {
    Remove-Item -LiteralPath $tree -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "port ledger self-test: $($script:Passed) passed, $($script:Failed) failed."
if ($script:Failed -gt 0) { exit 1 }
exit 0
