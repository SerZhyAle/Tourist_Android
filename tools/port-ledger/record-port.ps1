#requires -Version 7.0
<#
.SYNOPSIS
    Records a ported file in the port ledger, or records that a later FastMediaSorter change was carried.

.DESCRIPTION
    Handoff section 6a, mechanism 2: every file ported from FastMediaSorter has a row - source path,
    the source's SHA-256 at port time, destination path, port date. The hash is always computed from the
    source file as it is now; nobody types one.

      record-port.ps1 -Source app_v2/src/main/java/.../Foo.kt -Destination phone/src/main/java/.../Foo.kt
          adds the row; refused if the pair is already in the ledger, and refused if the destination
          already has a row from another source (one destination has one source)

      record-port.ps1 -Source .../Foo.kt -Destination .../Foo.kt -Refresh
          the destination now carries FastMediaSorter's newer version: the row takes the source's
          current hash, gains carriedOn, and drops any decision made about an older version

    Run it after the destination file is in place. The destination must exist.

.PARAMETER Source
    Path of the source file relative to the FastMediaSorter root.
.PARAMETER Destination
    Path of the ported file relative to this repository's root.
.PARAMETER Refresh
    Record a carried change on an existing row instead of adding one.
.PARAMETER Date
    yyyy-MM-dd to record; defaults to today.
.PARAMETER SourceRoot
    FastMediaSorter root; defaults to $env:TOURIST_FMS_ROOT, then P:\ANDROID\FastMediaSorter_mob_v2.
.PARAMETER Ledger
    Ledger file; defaults to docs/port-ledger.jsonl.
.PARAMETER DestinationRoot
    Root the destination paths are relative to; defaults to this repository (the self-test points it
    at a throw-away tree).
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Mandatory)][string] $Source,
    [Parameter(Mandatory)][string] $Destination,
    [switch] $Refresh,
    [string] $Date = (Get-Date -Format 'yyyy-MM-dd'),
    [string] $SourceRoot,
    [string] $Ledger,
    [string] $DestinationRoot
)

. (Join-Path $PSScriptRoot '_ledger.ps1')
$ErrorActionPreference = 'Stop'
trap { Write-Host "record-port: $($_.Exception.Message)" -ForegroundColor Red; exit 2 }

if ($Date -notmatch '^\d{4}-\d{2}-\d{2}$') { throw "-Date '$Date' is not yyyy-MM-dd." }

$ledgerPath = Get-LedgerFile $Ledger
$sourceRootResolved = Get-SourceRoot $SourceRoot
$destinationRootResolved = Get-DestinationRoot $DestinationRoot
$sourceRel = ConvertTo-RelativeLedgerPath $Source 'Source'
$destinationRel = ConvertTo-RelativeLedgerPath $Destination 'Destination'

$sourceFile = Join-Path $sourceRootResolved $sourceRel
if (-not (Test-Path -LiteralPath $sourceFile -PathType Leaf)) {
    throw "Source file '$sourceRel' does not exist under $sourceRootResolved."
}
$destinationFile = Join-Path $destinationRootResolved $destinationRel
if (-not (Test-Path -LiteralPath $destinationFile -PathType Leaf)) {
    throw "Destination file '$destinationRel' does not exist yet; port the file first, then record it."
}

$rows = Get-LedgerRows $ledgerPath
$existing = Find-Rows $rows $sourceRel $destinationRel
$hash = Get-FileSha256 $sourceFile

if ($Refresh) {
    if ($existing.Count -ne 1) { throw "-Refresh needs exactly one ledger row for $sourceRel -> $destinationRel, found $($existing.Count)." }
    $previousHash = $existing[0]['sourceSha256']
    $existing[0]['sourceSha256'] = $hash
    $existing[0]['carriedOn'] = $Date
    $existing[0].Remove('decision')
    Save-LedgerRows $ledgerPath $rows
    if ($previousHash -eq $hash) {
        Write-Host "record-port: $sourceRel was already at $($hash.Substring(0, 12)); carriedOn set to $Date."
    }
    else {
        Write-Host "record-port: $sourceRel carried, $($previousHash.Substring(0, 12)) -> $($hash.Substring(0, 12))."
    }
    exit 0
}

if ($existing.Count -gt 0) {
    throw "The ledger already has $sourceRel -> $destinationRel. Use -Refresh to record a carried change."
}
$conflicts = @($rows | Where-Object { $_['destination'] -eq $destinationRel })
if ($conflicts.Count -gt 0) {
    throw "The ledger already ports $($conflicts[0]['source']) to $destinationRel; one destination has one source."
}

$rows.Add(@{ source = $sourceRel; sourceSha256 = $hash; destination = $destinationRel; portedOn = $Date })
Save-LedgerRows $ledgerPath $rows
Write-Host "record-port: $sourceRel -> $destinationRel at $($hash.Substring(0, 12))."
exit 0
