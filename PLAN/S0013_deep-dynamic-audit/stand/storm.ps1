#requires -Version 7.0
<#
.SYNOPSIS
    M6 - the D-group drivers: torch and water toggle storm, key/tap storm on the locked watch, SOS mode-flip
    storm, sensor storm at the emulator's maximum injection rate, locale/unit/clock flip-flop, rapid screen
    ping-pong - judged against budgets 8.1 (and every other property the cells name).

.DESCRIPTION
    Runs the D scenes through run-matrix on fresh profiles. Emulator sensor and rotation injection go through
    the emulator console (`adb emu sensor set`, `adb emu rotate`, `adb emu geo fix`); the locale flip-flop
    through `cmd locale set-app-locales`.

    Exit code: 0 every judged cell passed; 1 a budget was breached in at least one cell; 2 no cell failed
    and at least one could not be verified.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [string[]] $Scenes = @('D1', 'D2', 'D3', 'D4', 'D5', 'D6'),
    [string[]] $Devices = @('phone', 'watch'),
    [string[]] $Properties = @('P1', 'P2', 'P3', 'P4', 'P5'),
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
trap { Write-Host "storm: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 storm' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }
$params = @{ Scenes = $Scenes; Devices = $Devices; Properties = $Properties; Variant = $Variant; Repeat = $Repeat; WithM7 = $WithM7; ReuseProfiles = $ReuseProfiles; KeepProfiles = $KeepProfiles }
if ($RunId) { $params.RunId = $RunId }
$lines = & (Join-Path $PSScriptRoot 'run-matrix.ps1') @params
$code = $LASTEXITCODE
$lines | ForEach-Object { Write-Output $_ }
$last = @($lines)[-1]
$tail = if ("$last" -match '\((.*)\)\s*$') { $Matches[1] } else { "$last" }
Exit-Verdict -Subject 'S0013 storm' -Code $code -Detail $tail
