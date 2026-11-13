#requires -Version 7.0
<#
.SYNOPSIS
    M2 - injected-input latency: `input keyevent` / `tap` (and the watch's hardware key and rotary source)
    against the next presented frame, judged against budget 8.1c.

.DESCRIPTION
    Injects -Count inputs into whatever the app shows, each behind a logcat mark in the same shell
    round-trip; the response frame is the first frame that handled input after the mark (framestats
    HandleInputStart), else the next vsync. If the control's own injection p95 already exceeds the budget
    the instrument cannot resolve it and the run is invalid.

    Exit code: 0 p95 within 8.1c; 1 p95 above it; 2 no matched frame, no live profile, or the control
    injection is slower than the budget.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [string] $Recipe = 'phone',
    [ValidateSet("tap", "key", "rotary")] [string] $Kind = "tap",
    [int] $X = -1, [int] $Y = -1,
    [string] $Key = 'KEYCODE_DPAD_DOWN',
    [int] $Count = 15
)

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "key-latency: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 key-latency' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

$serial = Get-ProfileSerial $Recipe
Assert-DeviceAlive $serial
$role = if ((Get-Recipes)[$Recipe].role -eq 'watch') { 'watch' } else { 'phone' }
if ($X -lt 0) { $size = (Invoke-Sh -Serial $serial -Command 'wm size') -replace '.*?(\d+)x(\d+).*', '$1 $2'; $w, $h = $size.Trim() -split ' ' | ForEach-Object { [int]$_ }; $X = [int]($w / 2); $Y = [int]($h / 2) }
$cmd = switch ($Kind) {
    'tap' { "input tap $X $Y" }
    'key' { "input keyevent $Key" }
    'rotary' { 'input rotaryencoder scroll --axis SCROLL,2' }
}
$ctx = @{ Role = $role; Serial = $serial; Starts = [Collections.Generic.List[object]]::new(); Latencies = [Collections.Generic.List[object]]::new() }
foreach ($i in 1..$Count) { $r = Measure-InputLatency -Serial $serial -InputCmd $cmd; $ctx.Latencies.Add($r); Start-Sleep -Milliseconds 400 }
$cell = New-Cell 'key-latency' $role 'P1' 'M2'
[void](Invoke-JudgeP1 -Cell $cell -Ctx $ctx -Logcat $null -Gfx $null -Control (Get-ControlFor $Recipe))
$ok = @($ctx.Latencies | Where-Object { $_.ok } | ForEach-Object { [double]$_.latencyMs })
if ($ok.Count) { Write-Output ("  {0}: n={1} p50 {2} ms p95 {3} ms" -f $cmd, $ok.Count, (Get-Percentile $ok 50), (Get-Percentile $ok 95)) }
Exit-Verdict -Subject 'S0013 key-latency' -Code $cell.code -Detail $cell.detail
