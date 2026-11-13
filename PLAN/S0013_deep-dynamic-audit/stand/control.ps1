#requires -Version 7.0
<#
.SYNOPSIS
    M8 - the no-app control run (S0013 section 7): the instrument set on a fresh profile of the recipe with
    the app not installed, measured on the platform's own Settings app as the reference process.

.DESCRIPTION
    Writes temp/S0013/control/<recipe>-<recipe hash>.json (and a copy into the run folder as control.json):
      m1      cold/warm/hot `am start -W` TotalTime of the reference, mean and 95% CI
      m2      injected-input -> frame latency of the reference: mean, CI, p95
      gfx     janky-frame share of the reference over a scroll window (information)
      slope   Theil-Sen slope %/h with bootstrap CI of the reference's PSS and threads over the soak window;
              fd: an exact counter the probe reads without jitter - band 0 (the reference's fd table is not
              readable without root, so its natural drift cannot be measured)
      cpu     background-idle CPU ms/min of the reference, mean and CI (the battery proxy band of 8.3h)
    Re-take it whenever the image, the host-load profile or an API pin changes (the recipe hash changes).

    Exit code: 0 the control was measured and written; 1 never; 2 it could not be measured (profile or
    probe failure, the app found installed on the profile).
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [string] $Recipe = 'phone',
    [int] $SoakMinutes = 10,
    [int] $IdleMinutes = 5,
    [string] $RunId,
    [switch] $ReuseProfile
)

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
trap { Write-Host "control: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject "S0013 control $Recipe" -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

if (-not $RunId) { $RunId = New-RunId "control-$Recipe" }
$runDir = Get-RunDir $RunId
$profileScript = Join-Path $PSScriptRoot 'profile.ps1'
$created = $false
if (-not ($ReuseProfile -and (Get-ActiveProfiles)[$Recipe])) {
    if ((Get-ActiveProfiles)[$Recipe]) { [void](Invoke-Native -FilePath 'pwsh' -Arguments @('-NoProfile', '-File', $profileScript, '-Action', 'Destroy', '-Recipe', $Recipe) -TimeoutSec 300 -AllowFail) }
    $c = Invoke-Native -FilePath 'pwsh' -Arguments @('-NoProfile', '-File', $profileScript, '-Action', 'Create', '-Recipe', $Recipe) -TimeoutSec 900 -AllowFail
    if ($c.ExitCode -ne 0) { throw "INVALID: profile create failed: $($c.Out.Trim())" }
    $created = $true
}
$serial = Get-ProfileSerial $Recipe
try {
    if ((Invoke-Sh -Serial $serial -Command "pm list packages $script:Package") -match "package:$([regex]::Escape($script:Package))\s*$") {
        throw "INVALID: $script:Package is installed on the control profile - the control must run without the app"
    }
    $resolved = (Invoke-Sh -Serial $serial -Command 'cmd package resolve-activity --brief -a android.settings.SETTINGS | tail -n 1').Trim()
    if ($resolved -notmatch '^[\w.]+/[\w.$]+$') { throw "INVALID: no Settings activity to use as the reference ($resolved)" }
    $refPkg = ($resolved -split '/')[0]
    $refUid = Get-AppUid $serial $refPkg
    Write-Output "  reference: $resolved (uid $refUid)"
    $size = (Invoke-Sh -Serial $serial -Command 'wm size') -replace '.*?(\d+)x(\d+).*', '$1 $2'
    $w, $h = $size.Trim() -split ' ' | ForEach-Object { [int]$_ }

    # M1
    $m1 = [ordered]@{}
    foreach ($kind in 'cold', 'warm', 'hot') {
        $vals = @(foreach ($i in 1..6) { (Measure-Start -Serial $serial -Component $resolved -Kind $kind).totalMs; Start-Sleep -Milliseconds 500 })
        $vals = @($vals | Where-Object { $null -ne $_ } | Select-Object -Skip 1)
        $m1[$kind] = Get-MeanCi ([double[]]$vals)
        $m1[$kind]['values'] = $vals
    }
    # M2 - a tap on the first list row of Settings, then back
    [void](Invoke-Sh -Serial $serial -Command "am start -W -f 0x10008000 -n $resolved")
    Start-Sleep -Seconds 2
    $lat = @()
    foreach ($i in 1..15) {
        $r = Measure-InputLatency -Serial $serial -Package $refPkg -InputCmd ("input tap {0} {1}" -f [int]($w / 2), [int]($h * 0.35)) -SettleMs 1200
        if ($r.ok) { $lat += [double]$r.latencyMs }
        [void](Invoke-Sh -Serial $serial -Command 'input keyevent KEYCODE_BACK'); Start-Sleep -Milliseconds 900
        [void](Invoke-Sh -Serial $serial -Command "am start -W -n $resolved"); Start-Sleep -Milliseconds 700
    }
    $m2 = Get-MeanCi ([double[]]$lat)
    $m2['p95'] = Get-Percentile ([double[]]$lat) 95
    $m2['values'] = $lat
    # Frame pacing of the reference over scrolls (information)
    Reset-Gfx $serial $refPkg
    foreach ($i in 1..10) { [void](Invoke-Sh -Serial $serial -Command ("input swipe {0} {1} {0} {2} 200" -f [int]($w / 2), [int]($h * 0.8), [int]($h * 0.3))); Start-Sleep -Milliseconds 600; [void](Invoke-Sh -Serial $serial -Command ("input swipe {0} {1} {0} {2} 200" -f [int]($w / 2), [int]($h * 0.3), [int]($h * 0.8))); Start-Sleep -Milliseconds 600 }
    $gfx = Read-Gfx $serial $refPkg

    # Slope band: the reference held in front, sampled like a soak
    [void](Invoke-Sh -Serial $serial -Command "am start -W -n $resolved")
    $csv = Join-Path $runDir 'control-soak.csv'
    $stop = Join-Path $runDir 'control.stop'
    $job = Start-Sampler -Serial $serial -Package $refPkg -Uid $refUid -CsvPath $csv -IntervalSec 2 -DurationSec ($SoakMinutes * 60) -StopFile $stop
    [void](Wait-Job $job -Timeout ($SoakMinutes * 60 + 120))
    $samples = @(Read-Samples $csv)
    if ($samples.Count -lt 20) { throw "INVALID: control soak produced $($samples.Count) samples" }
    $slope = [ordered]@{}
    foreach ($col in 'pssKb', 'threads') {
        $pts = @($samples | Where-Object { $null -ne $_[$col] })
        $sl = Get-SlopePctPerHour -TimesMs ([double[]]($pts | ForEach-Object { $_.ts })) -Values ([double[]]($pts | ForEach-Object { $_[$col] })) -Boot 600
        $slope[$col] = $sl
    }
    $slope['fd'] = [ordered]@{ pctPerHour = 0; ciLowPct = 0; ciHighPct = 0; note = 'exact counter; the reference fd table is not readable without root' }

    # CPU idle band: the reference in the background
    [void](Invoke-Sh -Serial $serial -Command 'input keyevent KEYCODE_HOME')
    $csv2 = Join-Path $runDir 'control-idle.csv'
    $job2 = Start-Sampler -Serial $serial -Package $refPkg -Uid $refUid -CsvPath $csv2 -IntervalSec 5 -DurationSec ($IdleMinutes * 60) -StopFile $stop
    [void](Wait-Job $job2 -Timeout ($IdleMinutes * 60 + 120))
    $idle = @(Read-Samples $csv2 | Where-Object { $null -ne $_.cpuTicks })
    $rates = @()
    for ($i = 6; $i -lt $idle.Count; $i += 6) {
        $a = $idle[$i - 6]; $b = $idle[$i]
        if ($a.pid -eq $b.pid) { $rates += (($b.cpuTicks - $a.cpuTicks) * 10) / (($b.ts - $a.ts) / 60000.0) }
    }
    $cpu = Get-MeanCi ([double[]]$rates)

    $control = [ordered]@{
        recipe = $Recipe; recipeHash = (Get-RecipeHash $Recipe); runId = $RunId; taken = (Get-Date).ToString('s')
        reference = $resolved; hostLoad = [ordered]@{ cpus = [Environment]::ProcessorCount; otherEmulators = @((Invoke-Adb -Arguments @('devices')).Out -split "`n" | Where-Object { $_ -match '^emulator-\d+\s+device' -and $_ -notmatch $serial } | ForEach-Object { ($_ -split '\s+')[0] }) }
        soakMinutes = $SoakMinutes; m1 = $m1; m2 = $m2; gfx = $gfx; slope = $slope; cpu = $cpu
    }
    $out = Join-Path $script:TempRoot "control\$Recipe-$($control.recipeHash).json"
    Write-JsonFile $out $control
    Write-JsonFile (Join-Path $runDir 'control.json') $control
} finally {
    if ($created) { [void](Invoke-Native -FilePath 'pwsh' -Arguments @('-NoProfile', '-File', $profileScript, '-Action', 'Destroy', '-Recipe', $Recipe) -TimeoutSec 300 -AllowFail) }
}
$d = "cold $([math]::Round($control.m1.cold.mean)) ms [CI {0:0}..{1:0}], m2 p95 {2:0} ms, pss slope CI {3:0.0}..{4:0.0} %/h, cpu idle {5:0.0} ms/min; {6}" -f $control.m1.cold.lo, $control.m1.cold.hi, $control.m2.p95, $control.slope.pssKb.ciLowPct, $control.slope.pssKb.ciHighPct, $control.cpu.mean, $out
Exit-Verdict -Subject "S0013 control $Recipe" -Code 0 -Detail $d
