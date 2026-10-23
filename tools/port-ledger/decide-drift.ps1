#requires -Version 7.0
<#
.SYNOPSIS
    Writes, next to its ledger row, the decision not to carry a FastMediaSorter change.

.DESCRIPTION
    Handoff section 6a: a change left uncarried is a decision written next to its ledger row, never
    silence. The decision is pinned to the source version it was made about, so it stops covering the
    file the moment FastMediaSorter changes it again - the next change is drift until it is decided too.

    The note is mandatory and is the reason, in words ("the sorter-only branch of the fix", "superseded
    by our own change in S0005"; never "n/a"). A source file that FastMediaSorter has removed or moved
    is decided the same way; the decision then covers the removal.

.PARAMETER Source
    Path of the source file relative to the FastMediaSorter root, as in the ledger row.
.PARAMETER Destination
    The row's destination, when one source is ported to more than one place.
.PARAMETER Note
    Why the change is not carried.
.PARAMETER Date
    yyyy-MM-dd to record; defaults to today.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Mandatory)][string] $Source,
    [Parameter(Mandatory)][string] $Note,
    [string] $Destination,
    [string] $Date = (Get-Date -Format 'yyyy-MM-dd'),
    [string] $SourceRoot,
    [string] $Ledger,
    [string] $DestinationRoot
)

. (Join-Path $PSScriptRoot '_ledger.ps1')
$ErrorActionPreference = 'Stop'
trap { Write-Host "decide-drift: $($_.Exception.Message)" -ForegroundColor Red; exit 2 }

if ([string]::IsNullOrWhiteSpace($Note)) { throw 'The note is mandatory: a decision without a reason is silence.' }
if ($Date -notmatch '^\d{4}-\d{2}-\d{2}$') { throw "-Date '$Date' is not yyyy-MM-dd." }

$ledgerPath = Get-LedgerFile $Ledger
$sourceRootResolved = Get-SourceRoot $SourceRoot
$sourceRel = ConvertTo-RelativeLedgerPath $Source 'Source'

$rows = Get-LedgerRows $ledgerPath
$matches = Find-Rows $rows $sourceRel $Destination
if ($matches.Count -eq 0) { throw "No ledger row for source '$sourceRel'$(if ($Destination) { " -> $Destination" })." }
if ($matches.Count -gt 1 -and -not $Destination) {
    throw "Source '$sourceRel' is ported to $($matches.Count) destinations; pass -Destination to pick one of: $(($matches | ForEach-Object { $_['destination'] }) -join ', ')."
}

$sourceFile = Join-Path $sourceRootResolved $sourceRel
$currentHash = if (Test-Path -LiteralPath $sourceFile -PathType Leaf) { Get-FileSha256 $sourceFile } else { 'missing' }

foreach ($row in $matches) {
    $row['decision'] = @{ sourceSha256 = $currentHash; note = $Note; decidedOn = $Date }
}
Save-LedgerRows $ledgerPath $rows

$subject = if ($currentHash -eq 'missing') { 'its removal' } else { "version $($currentHash.Substring(0, 12))" }
Write-Host "decide-drift: $sourceRel - decision recorded against $subject."
Write-Host "  $Note"
exit 0
