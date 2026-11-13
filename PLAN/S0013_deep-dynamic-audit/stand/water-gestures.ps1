#requires -Version 7.0
<#
.SYNOPSIS
    Isolates which single input leaves the phone's water flashlight (the C4 signature rule), each input
    -Repeat times from a fresh open of the lamp, with a screenshot after each - the R/V/T evidence a C4
    finding needs (ticket section 9).

.DESCRIPTION
    Inputs: a tap, a long press, a swipe across the glass, the left-edge and right-edge back gestures, the
    bottom home gesture, the top shade pull, KEYCODE_BACK, and the exit rule's KEYCODE_VOLUME_DOWN as the
    control. After each input: is WaterFlashlightActivity still resumed, is the torch still lit. Results go
    to temp/S0013/runs/<run-id>/water-gestures.json.

    Exit code: 0 no wet input left the lamp and the exit key did; 1 a wet input left the lamp (or the exit
    key did not); 2 the probe could not run (no live phone profile, the lamp did not open).
#>
[CmdletBinding(PositionalBinding = $false)]
param([string] $Recipe = 'phone', [ValidateRange(1, 100)] [int] $Repeat = 3, [string] $RunId, [string[]] $Only, [switch] $RequirePinning)

. (Join-Path $PSScriptRoot '_stand.ps1')
. (Join-Path $PSScriptRoot '_instruments.ps1')
. (Join-Path $PSScriptRoot '_judges.ps1')
. (Join-Path $PSScriptRoot 'scenes.ps1')
. (Join-Path $PSScriptRoot '_engine.ps1')
trap { Write-Host "water-gestures: $($_.Exception.Message)" -ForegroundColor Red; Exit-Verdict -Subject 'S0013 water-gestures' -Code 2 -Detail ($_.Exception.Message -replace '^INVALID:\s*', '') }

if (-not $RunId) { $RunId = New-RunId 'water-gestures' }
$dir = Get-RunDir $RunId
$serial = Get-ProfileSerial $Recipe
$ctx = New-SceneCtx -SceneId 'C4x' -Role 'phone' -Serial $serial -RunDir $dir -Uid (Get-AppUid $serial) -Density (Get-DensityDpi $serial) -WithM7 $false -SoakMinutes 0 -SoakScreen '' -Rep 1
$nav = (Invoke-Sh -Serial $serial -Command 'settings get secure navigation_mode').Trim()
$inputs = [ordered]@{
    'tap'              = 'input tap 540 1200'
    'long-press'       = 'input swipe 540 1200 540 1200 1500'
    'swipe-across'     = 'input swipe 300 900 800 1500 150'
    'edge-left-back'   = 'input swipe 0 1200 600 1200 150'
    'edge-right-back'  = 'input swipe 1079 1200 400 1200 150'
    'bottom-home'      = 'input swipe 540 2399 540 1400 200'
    'top-shade'        = 'input swipe 540 0 540 1200 200'
    'edge-left-twice'  = 'input swipe 0 1200 600 1200 150; sleep 0.4; input swipe 0 1200 600 1200 150'
    'edge-both-sides'  = 'input swipe 0 1200 600 1200 150; input swipe 1079 1200 400 1200 150'
    'bottom-home-twice' = 'input swipe 540 2399 540 1400 200; sleep 0.4; input swipe 540 2399 540 1400 200'
    'key-back'         = 'input keyevent KEYCODE_BACK'
    'key-volume-down'  = 'input keyevent KEYCODE_VOLUME_DOWN'
}
if ($Only) { $Only = @($Only | ForEach-Object { $_ -split ',' }); foreach ($k in @($inputs.Keys)) { if ($k -notin $Only) { $inputs.Remove($k) } } }
$results = [Collections.Generic.List[object]]::new()
foreach ($name in $inputs.Keys) {
    foreach ($i in 1..$Repeat) {
        Open-Phone $ctx water
        if ($RequirePinning) {
            # Use Android's real confirmation, never shell lock-task or a device-owner allowlist.
            [void](Invoke-TapNode -Serial $serial -Id screen_pinning_ok_button -AllWindows)
            Pause-Sec 1
            $tasks = Invoke-Sh -Serial $serial -Command 'dumpsys activity activities'
            if ($tasks -notmatch 'mLockTaskModeState=PINNED') { throw 'INVALID: Android screen pinning was not confirmed' }
        }
        if (-not (Wait-TopActivity $ctx 'WaterFlashlightActivity' 4)) { throw 'INVALID: the water flashlight did not open' }
        Pause-Sec 1.5
        $litBefore = Get-TorchOn $ctx
        if (-not $litBefore) { throw 'INVALID: the torch was dark before the input under test' }
        [void](Invoke-Sh -Serial $serial -Command $inputs[$name])
        Pause-Sec 2
        $still = Wait-TopActivity $ctx 'WaterFlashlightActivity' 1
        $lit = Get-TorchOn $ctx
        $shot = Save-Screenshot $ctx "$name-$i"
        $results.Add([ordered]@{ input = $name; rep = $i; litBefore = $litBefore; stillOn = $still; torchLit = $lit; top = (Get-Resumed $ctx); screenshot = $shot })
        Write-Output ("  {0,-16} #{1}: lamp {2}, torch {3}" -f $name, $i, $(if ($still) { 'stayed' } else { 'LEFT' }), $(if ($lit) { 'lit' } else { 'dark' }))
        [void](Invoke-Sh -Serial $serial -Command "am force-stop $script:Package; input keyevent KEYCODE_HOME")
        Pause-Sec 1
    }
}
if ($results.Count -eq 0) { throw 'INVALID: the input filter selected no cases' }
Write-JsonFile (Join-Path $dir 'water-gestures.json') ([ordered]@{ runId = $RunId; navigationMode = $nav; repeat = $Repeat; requirePinning = [bool]$RequirePinning; results = @($results) })
$wetLeft = @($results | Where-Object { $_.input -ne 'key-volume-down' -and (-not $_.stillOn -or -not $_.torchLit) } | ForEach-Object { $_.input } | Select-Object -Unique)
$exitFailed = @($results | Where-Object { $_.input -eq 'key-volume-down' -and ($_.stillOn -or $_.torchLit) }).Count
$code = if ($wetLeft.Count -or $exitFailed) { 1 } else { 0 }
Exit-Verdict -Subject 'S0013 water-gestures' -Code $code -Detail "navigation_mode=$nav; left by: $(if ($wetLeft.Count) { $wetLeft -join ', ' } else { 'nothing wet' }); exit key failed $exitFailed of $Repeat; $dir"
