#requires -Version 7.0
<#
.SYNOPSIS
    M5 - the F-group boundary failures (and the B4/F9 boot case), each followed by a state-consistency
    readback, judged against budgets 8.2a-8.2d and the known-outcome set (outcomes.json, decision 8.7).

.DESCRIPTION
    Runs the F scenes (default F1-F9 plus B4) through run-matrix on fresh profiles; the injections are in
    scenes.ps1: runtime revocation, location off, the sensorless profile, a camera held by another app, a
    call and DND mid-siren, a full /data, a hard kill, a reboot with a failing shortcut. Until outcomes.json
    carries the owner's approval date, 8.2a verdicts are printed but do not count (named as a skip).

    Exit code: 0 every fault landed in the outcome set with consistent state; 1 an outcome outside the set,
    an inconsistent state, a crash, raw exception text or a secret-class token; 2 a fault could not be
    injected or a pair scene had no pair.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [string[]] $Scenes = @('F1', 'F2', 'F3', 'F4', 'F5', 'F6', 'F7', 'F8', 'F9', 'B4'),
    [string[]] $Devices = @('phone', 'watch'),
    [string[]] $Properties = @('P2'),
    [ValidateSet('noLegal', 'store')] [string] $Variant = 'noLegal',
    [int] $Repeat = 1,
    [switch] $WithM7,
    [switch] $ReuseProfiles,
    [switch] $KeepProfiles,
    [string] $RunId
)

# `pwsh -File` hands a comma list over as one string.
foreach ($n in 'Scenes', 'Devices', 'Properties', 'SoakScreens') { $v = Get-Variable -Name $n -ValueOnly -ErrorAction SilentlyContinue; if ($v) { Set-Variable -Name $n -Value @($v | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ }) } }

. (Join-Path $PSScriptRoot '_stand.ps1')
trap { Write-Host "fault-inject: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 fault-inject' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }
$params = @{ Scenes = $Scenes; Devices = $Devices; Properties = $Properties; Variant = $Variant; Repeat = $Repeat; WithM7 = $WithM7; ReuseProfiles = $ReuseProfiles; KeepProfiles = $KeepProfiles }
if ($RunId) { $params.RunId = $RunId }
$lines = & (Join-Path $PSScriptRoot 'run-matrix.ps1') @params
$code = $LASTEXITCODE
$lines | ForEach-Object { Write-Output $_ }
$last = @($lines)[-1]
$tail = if ("$last" -match '\((.*)\)\s*$') { $Matches[1] } else { "$last" }
Exit-Verdict -Subject 'S0013 fault-inject' -Code $code -Detail $tail
