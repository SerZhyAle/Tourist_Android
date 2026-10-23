#requires -Version 7.0
<#
.SYNOPSIS
    Lists every ported file whose FastMediaSorter source changed since it was ported - the to-do list
    for carrying a fix across.

.DESCRIPTION
    Handoff section 6a, mechanism 2. Re-hashes the source of every ledger row and compares it with the
    hash recorded at port time. Runs at the start of every release of this product.

    A row is one of:
      in step      the source still hashes to the recorded value
      decided      the source changed, and a decision written for exactly this source version covers it
      DRIFT        the source changed and nobody carried it or wrote a decision
      SOURCE GONE  the source file no longer exists (removed or moved) and no decision covers that
      NO DEST      the destination file is missing from this repository - the ledger is wrong

    An empty (or absent) ledger is in step by definition: nothing has been ported yet.

    Exit code: 0 nothing uncarried; 1 at least one DRIFT, SOURCE GONE or NO DEST; 2 the check could not
    run (no source root, malformed ledger).

.PARAMETER History
    For every DRIFT row also print the FastMediaSorter commits that touched the file since its port
    date (needs git and a git checkout at the source root).
.PARAMETER Json
    Print one JSON object with the counts and the uncarried rows instead of the text report.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [string] $SourceRoot,
    [string] $Ledger,
    [switch] $History,
    [switch] $Json,
    [string] $DestinationRoot
)

. (Join-Path $PSScriptRoot '_ledger.ps1')
$ErrorActionPreference = 'Stop'
trap { Write-Host "check-drift: $($_.Exception.Message)" -ForegroundColor Red; exit 2 }

$ledgerPath = Get-LedgerFile $Ledger
$sourceRootResolved = Get-SourceRoot $SourceRoot
$destinationRootResolved = Get-DestinationRoot $DestinationRoot
$rows = Get-LedgerRows $ledgerPath

$states = foreach ($row in $rows) {
    $sourceFile = Join-Path $sourceRootResolved $row['source']
    $destinationFile = Join-Path $destinationRootResolved $row['destination']
    $current = if (Test-Path -LiteralPath $sourceFile -PathType Leaf) { Get-FileSha256 $sourceFile } else { 'missing' }
    $decision = if ($row.ContainsKey('decision')) { $row['decision'] } else { $null }
    $covered = $null -ne $decision -and $decision['sourceSha256'] -eq $current

    $state = if (-not (Test-Path -LiteralPath $destinationFile -PathType Leaf)) { 'NO DEST' }
             elseif ($current -eq $row['sourceSha256']) { 'in step' }
             elseif ($covered) { 'decided' }
             elseif ($current -eq 'missing') { 'SOURCE GONE' }
             else { 'DRIFT' }

    [pscustomobject]@{
        State       = $state
        Source      = $row['source']
        Destination = $row['destination']
        AnchoredOn  = if ($row.ContainsKey('carriedOn')) { $row['carriedOn'] } else { $row['portedOn'] }
        Recorded    = $row['sourceSha256']
        Current     = $current
        Note        = if ($covered) { $decision['note'] } else { $null }
    }
}
$states = @($states)
$uncarried = @($states | Where-Object { $_.State -in 'DRIFT', 'SOURCE GONE', 'NO DEST' })

if ($Json) {
    [ordered]@{
        ledger    = $ledgerPath
        rows      = $states.Count
        inStep    = @($states | Where-Object State -eq 'in step').Count
        decided   = @($states | Where-Object State -eq 'decided').Count
        uncarried = @($uncarried | ForEach-Object {
            [ordered]@{ state = $_.State; source = $_.Source; destination = $_.Destination
                        since = $_.AnchoredOn; recorded = $_.Recorded; current = $_.Current }
        })
    } | ConvertTo-Json -Depth 5
    if ($uncarried.Count) { exit 1 } else { exit 0 }
}

Write-Host "Port ledger: $($states.Count) row(s) in $ledgerPath"
Write-Host "Source root: $sourceRootResolved"
foreach ($settled in @($states | Where-Object State -eq 'decided')) {
    Write-Host ("  decided      {0}`n               {1}" -f $settled.Source, $settled.Note)
}
foreach ($open in $uncarried) {
    Write-Host ("  {0,-12} {1}  ->  {2}  (anchored on {3})" -f $open.State, $open.Source, $open.Destination, $open.AnchoredOn) -ForegroundColor Yellow
    if ($History -and $open.State -eq 'DRIFT') {
        $commits = git -C $sourceRootResolved log --since=$open.AnchoredOn --format='               %h %ad %s' --date=short -n 8 -- $open.Source 2>$null
        if ($commits) { $commits | ForEach-Object { Write-Host $_ } }
        else { Write-Host '               (no git history available for this file)' }
    }
}
$inStepCount = @($states | Where-Object State -eq 'in step').Count
$decidedCount = @($states | Where-Object State -eq 'decided').Count
if ($uncarried.Count -eq 0) {
    Write-Host "OK: nothing uncarried ($inStepCount in step, $decidedCount decided)."
    exit 0
}
Write-Host "UNCARRIED: $($uncarried.Count) of $($states.Count) row(s) need a carry (record-port.ps1 -Refresh) or a decision (decide-drift.ps1)." -ForegroundColor Yellow
exit 1
