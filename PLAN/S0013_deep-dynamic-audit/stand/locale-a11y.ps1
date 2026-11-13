#requires -Version 7.0
<#
.SYNOPSIS
    M7 - the understandability sweep: per-locale UI dumps (EN, RU, UK) at font scale 1.0, 1.3 and 2.0 of
    every checkpoint a scene reaches, judged against budgets 8.5a-8.5e.

.DESCRIPTION
    Runs the selected scenes (default: group C, Phase 4) through run-matrix with the sweep on. The dumps come
    from the stand's accessibility probe (probe-a11y/), which reads the window tree as a screen reader does
    and, unlike uiautomator, does not wait for a live screen to go idle. Per checkpoint it reports:
      8.5a  English fallback - a text shown in RU or UK identical to the EN text with a Latin word in it
      8.5b  toggle reversibility - the state hash after on -> off equals the hash before (Test-Toggle)
      8.5c  touch targets under 48 dp (phone) / 40 dp (watch)
      8.5d  clipped or unlabelled controls at font scale 2.0
      8.5e  interactive nodes without a label, and toggles whose state changes no accessibility attribute
            (state shown by colour alone, handoff section 3)

    Exit code: 0 every judged checkpoint held; 1 a budget was breached; 2 no checkpoint could be swept.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [string[]] $Scenes = @('C1', 'C2', 'C3', 'C4', 'C5', 'C7', 'C8', 'C9', 'C10'),
    [string[]] $Devices = @('phone', 'watch'),
    [ValidateSet('noLegal', 'store')] [string] $Variant = 'noLegal',
    [switch] $ReuseProfiles,
    [switch] $KeepProfiles,
    [string] $RunId
)

# `pwsh -File` hands a comma list over as one string.
foreach ($n in 'Scenes', 'Devices', 'Properties', 'SoakScreens') { $v = Get-Variable -Name $n -ValueOnly -ErrorAction SilentlyContinue; if ($v) { Set-Variable -Name $n -Value @($v | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ }) } }

. (Join-Path $PSScriptRoot '_stand.ps1')
trap { Write-Host "locale-a11y: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 locale-a11y' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }
$params = @{ Scenes = $Scenes; Devices = $Devices; Properties = @('P5'); Variant = $Variant; WithM7 = $true; ReuseProfiles = $ReuseProfiles; KeepProfiles = $KeepProfiles }
if ($RunId) { $params.RunId = $RunId }
$lines = & (Join-Path $PSScriptRoot 'run-matrix.ps1') @params
$code = $LASTEXITCODE
$lines | ForEach-Object { Write-Output $_ }
$last = @($lines)[-1]
$tail = if ("$last" -match '\((.*)\)\s*$') { $Matches[1] } else { "$last" }
Exit-Verdict -Subject 'S0013 locale-a11y' -Code $code -Detail $tail
