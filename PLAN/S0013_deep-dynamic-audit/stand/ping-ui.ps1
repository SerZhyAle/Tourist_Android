#requires -Version 7.0
<#
.SYNOPSIS
    M1 - main-looper ping from outside: `am start -W` round-trips (cold, warm, hot) and the frame pacing of
    a window, judged against budgets 8.1a, 8.1b and 8.1d.

.DESCRIPTION
    Starts the app's launcher activity -Count times per kind on a live profile, then reads `dumpsys
    gfxinfo` over -WindowSec of whatever the app shows. A value within the control's own jitter of its
    limit is not a verdict (section 7).

    Exit code: 0 every start and the janky-frame share held their budgets; 1 a budget was breached;
    2 nothing could be measured (no live profile, no TotalTime, too few frames, probe failure).
#>
[CmdletBinding(PositionalBinding = $false)]
param([string] $Recipe = 'phone', [int] $Count = 5, [int] $WindowSec = 10)

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "ping-ui: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 ping-ui' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

$serial = Get-ProfileSerial $Recipe
Assert-DeviceAlive $serial
$role = if ((Get-Recipes)[$Recipe].role -eq 'watch') { 'watch' } else { 'phone' }
$ctx = @{ Role = $role; Serial = $serial; Starts = [Collections.Generic.List[object]]::new(); Latencies = [Collections.Generic.List[object]]::new() }
foreach ($kind in 'cold', 'warm', 'hot') { foreach ($i in 1..$Count) { [void](Start-Timed $ctx $kind "$kind#$i"); Start-Sleep -Milliseconds 600 } }
Reset-Gfx $serial
Start-Sleep -Seconds $WindowSec
$gfx = Read-Gfx $serial
$cell = New-Cell 'ping-ui' $role 'P1' 'M1'
[void](Invoke-JudgeP1 -Cell $cell -Ctx $ctx -Logcat $null -Gfx $gfx -Control (Get-ControlFor $Recipe))
foreach ($k in 'cold', 'warm', 'hot') {
    $v = @($ctx.Starts | Where-Object { $_.kind -eq $k -and $null -ne $_.totalMs } | ForEach-Object { [double]$_.totalMs })
    if ($v.Count) { Write-Output ("  {0}: median {1} ms, max {2} ms (n={3})" -f $k, (Get-Percentile $v 50), ($v | Measure-Object -Maximum).Maximum, $v.Count) }
}
Write-Output "  frames $($gfx.frames), janky $($gfx.jankyPct)%"
Exit-Verdict -Subject 'S0013 ping-ui' -Code $cell.code -Detail $cell.detail
