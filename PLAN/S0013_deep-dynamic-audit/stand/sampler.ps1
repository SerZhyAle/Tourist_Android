#requires -Version 7.0
<#
.SYNOPSIS
    M3 - the periodic outside harvest over a window: meminfo, /proc fd and task counts, sensor and location
    registrations, wakelocks, alarms, services, notifications, audio focus, torch, data-dir size, plus the
    logcat crash/ANR count and the secret-class scan (decision 8.8).

.DESCRIPTION
    Samples the app on a live profile every -IntervalSec for -DurationSec into a CSV, harvests logcat over
    the same window, and judges what needs no scene context: crashes (8.1f), secrets (8.2c), and - with
    -Idle - the idle log rate (8.3g) and the battery proxy (8.3h). Scene-bound budgets (8.3a-f, 8.4b) are
    judged by run-matrix, which knows the feature spans.

    Exit code: 0 every judged budget held; 1 a crash, a secret-class token, or an idle budget breached;
    2 the sampler could not run (no profile, three probe failures, fewer than three samples).
#>
[CmdletBinding(PositionalBinding = $false)]
param([string] $Recipe = 'phone', [int] $DurationSec = 120, [double] $IntervalSec = 2, [switch] $Idle, [string] $RunId)

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "sampler: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 sampler' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

if (-not $RunId) { $RunId = New-RunId "sampler-$Recipe" }
$dir = Get-RunDir $RunId
$serial = Get-ProfileSerial $Recipe
Assert-DeviceAlive $serial
$uid = Get-AppUid $serial
$csv = Join-Path $dir 'samples.csv'
Clear-Logcat $serial
$t0 = Get-NowMs; $mono0 = Get-DeviceUptimeMs $serial
$job = Start-Sampler -Serial $serial -Package $script:Package -Uid $uid -CsvPath $csv -IntervalSec $IntervalSec -DurationSec $DurationSec
[void](Wait-Job $job -Timeout ($DurationSec + 120))
$t1 = Get-NowMs
$log = Join-Path $dir 'logcat.txt'
Save-Logcat -Serial $serial -Path $log
$samples = @(Read-Samples $csv)
if ($samples.Count -lt 3) { throw "INVALID: $($samples.Count) sample(s)" }
$facts = Get-LogcatFacts -Path $log -Uid $uid -InjectedCoords @($script:BaseFix[1], $script:BaseFix[0])
$ctx = New-SceneCtx -SceneId 'sampler' -Role 'phone' -Serial $serial -RunDir $dir -Uid $uid -Density 0 -WithM7 $false -SoakMinutes 0 -SoakScreen '' -Rep 1
if ($Idle) { $ctx.IdleWindow = [ordered]@{ from = $t0; to = $t1 }; $ctx.IdleBackground = $true; $ctx.IdleLogLines = $facts.appLines }
$p2 = New-Cell 'sampler' 'device' 'P2' 'M3'
[void](Invoke-JudgeP2 -Cell $p2 -Ctx $ctx -Logcat $facts)
$p3 = New-Cell 'sampler' 'device' 'P3' 'M3'
[void](Invoke-JudgeP3 -Cell $p3 -Ctx $ctx -Samples $samples -Logcat $facts -Control (Get-ControlFor $Recipe) -IntervalMs ($IntervalSec * 1000))
Write-Output ("  {0} samples; last: pss {1} KB, fd {2}, threads {3}, sensors {4}, location {5}, wakelocks {6}/{7}/{8}, notifications {9}" -f $samples.Count, $samples[-1].pssKb, $samples[-1].fd, $samples[-1].threads, $samples[-1].sensors, $samples[-1].locRegs, $samples[-1].wlOwn, $samples[-1].wlAttributed, $samples[-1].wlScreen, $samples[-1].notifs)
Write-Output "  $(Format-CellLine $p2)"
Write-Output "  $(Format-CellLine $p3)"
$code = Merge-Codes @($p2.code, $(if ($Idle) { $p3.code } else { 0 }))
Exit-Verdict -Subject 'S0013 sampler' -Code $code -Detail "$($samples.Count) samples, $($facts.appLines) app log lines, $($facts.crashes) crash(es), $($facts.secrets.Count) secret token(s); $csv"
