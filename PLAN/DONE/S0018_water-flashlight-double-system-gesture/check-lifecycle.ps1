#requires -Version 7.0
<#
.SYNOPSIS
    Exercises Android pinning consent, rotation, screen-off and the water lamp's hardware exits.
    Uses the S0013 disposable phone profile and its external-only instruments. No device-owner setup.
#>
[CmdletBinding(PositionalBinding = $false)]
param([string] $Recipe = 'phone', [string] $RunId = '20261027-S0018-lifecycle')

$stand = Join-Path $PSScriptRoot '../S0013_deep-dynamic-audit/stand'
foreach ($file in '_stand.ps1', '_instruments.ps1', '_judges.ps1', 'scenes.ps1', '_engine.ps1') {
    . (Join-Path $stand $file)
}
trap { Exit-Verdict -Subject 'S0018 lifecycle' -Code 2 -Detail $_.Exception.Message }

$dir = Get-RunDir $RunId
$serial = Get-ProfileSerial $Recipe
$ctx = New-SceneCtx -SceneId 'C4pin' -Role 'phone' -Serial $serial -RunDir $dir -Uid (Get-AppUid $serial) -Density (Get-DensityDpi $serial) -WithM7 $false -SoakMinutes 0 -SoakScreen '' -Rep 1
$results = [Collections.Generic.List[object]]::new()
function Is-Pinned { (Invoke-Sh -Serial $serial -Command 'dumpsys activity activities') -match 'mLockTaskModeState=PINNED' }
function Record([string] $Name, [bool] $Passed) {
    $shot = Save-Screenshot $ctx $Name
    $results.Add([ordered]@{ case = $Name; passed = $Passed; top = (Get-Resumed $ctx); torchLit = (Get-TorchOn $ctx); pinned = (Is-Pinned); screenshot = $shot })
    Write-Output "$Name : $(if ($Passed) { 'PASS' } else { 'FAIL' })"
}
function Wait-LampExit {
    # Activity resume, camera callbacks and task unpinning are separate asynchronous transitions.
    $deadline = (Get-Date).AddSeconds(5)
    do {
        $top = Get-Resumed $ctx
        $lit = Get-TorchOn $ctx
        $pinned = Is-Pinned
        if ($top -match 'LightsActivity' -and -not $lit -and -not $pinned) { return $true }
        Pause-Sec 0.2
    } while ((Get-Date) -lt $deadline)
    Write-Host "Exit did not settle: top=$top; torch=$lit; pinned=$pinned"
    return $false
}
function Open-PinnedLamp {
    Open-Phone $ctx water
    if (Get-TorchOn $ctx) { throw 'INVALID: the torch lit before Android consent' }
    [void](Invoke-TapNode -Serial $serial -Id screen_pinning_ok_button -AllWindows)
    Pause-Sec 1
    if (-not (Is-Pinned) -or -not (Get-TorchOn $ctx)) { throw 'INVALID: confirmation did not arm the pinned lamp' }
}

$rotation = (Invoke-Sh -Serial $serial -Command 'settings get system user_rotation').Trim()
$autoRotate = (Invoke-Sh -Serial $serial -Command 'settings get system accelerometer_rotation').Trim()
try {
    Open-Phone $ctx water
    $darkBeforeConsent = -not (Get-TorchOn $ctx)
    [void](Invoke-TapNode -Serial $serial -Id screen_pinning_cancel_button -AllWindows)
    Pause-Sec 0.5
    Record 'dismiss-consent-unarmed' ($darkBeforeConsent -and -not (Get-TorchOn $ctx) -and -not (Is-Pinned))
    [void](Invoke-TapNode -Serial $serial -Id button2 -AllWindows)
    Pause-Sec 1
    Record 'cancel-consent' ($darkBeforeConsent -and (Wait-TopActivity $ctx 'LightsActivity' 3) -and -not (Get-TorchOn $ctx) -and -not (Is-Pinned))

    Open-PinnedLamp
    [void](Invoke-Sh -Serial $serial -Command 'settings put system accelerometer_rotation 0; settings put system user_rotation 1')
    Pause-Sec 2
    $landscape = (Invoke-Sh -Serial $serial -Command 'dumpsys window displays') -match 'mRotation=ROTATION_90'
    Record 'rotate-pinned' ($landscape -and (Wait-TopActivity $ctx 'WaterFlashlightActivity' 3) -and (Get-TorchOn $ctx) -and (Is-Pinned))
    [void](Invoke-Sh -Serial $serial -Command 'input keyevent KEYCODE_VOLUME_DOWN')
    Pause-Sec 1
    Record 'exit-after-rotation' (Wait-LampExit)
    [void](Invoke-Sh -Serial $serial -Command 'settings put system user_rotation 0')
    Pause-Sec 1

    Open-PinnedLamp
    [void](Invoke-Sh -Serial $serial -Command 'input keyevent KEYCODE_SLEEP')
    Pause-Sec 1
    Record 'screen-off-torch-off' (-not (Get-TorchOn $ctx))
    [void](Invoke-Sh -Serial $serial -Command 'input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard')
    Pause-Sec 1
    Record 'wake-pinned' ((Wait-TopActivity $ctx 'WaterFlashlightActivity' 3) -and (Get-TorchOn $ctx) -and (Is-Pinned))
    [void](Invoke-Sh -Serial $serial -Command 'input keyevent KEYCODE_VOLUME_DOWN')
    Pause-Sec 1

    Open-PinnedLamp
    # Android's own emergency unpin (swipe up and hold under gesture navigation) must stay available.
    # It is an OS escape, not an app exit: only the pin is asserted, the lamp's own state is recorded.
    [void](Invoke-Sh -Serial $serial -Command 'input swipe 540 2399 540 1500 2500')
    Pause-Sec 1
    Record 'os-unpin-escape' (-not (Is-Pinned))
    [void](Invoke-Sh -Serial $serial -Command 'input keyevent KEYCODE_VOLUME_DOWN')
    Pause-Sec 1

    foreach ($key in 'VOLUME_UP', 'VOLUME_DOWN', 'VOLUME_MUTE', 'CAMERA', 'HEADSETHOOK', 'MEDIA_PLAY_PAUSE') {
        Open-PinnedLamp
        [void](Invoke-Sh -Serial $serial -Command "input keyevent KEYCODE_$key")
        Pause-Sec 1
        Record "exit-$key" (Wait-LampExit)
        [void](Invoke-Sh -Serial $serial -Command "am force-stop $script:Package")
    }
} finally {
    [void](Invoke-Sh -Serial $serial -Command "input keyevent KEYCODE_WAKEUP; input keyevent KEYCODE_VOLUME_DOWN; am force-stop $script:Package" -AllowFail)
    foreach ($setting in @{ user_rotation = $rotation; accelerometer_rotation = $autoRotate }.GetEnumerator()) {
        $command = if ($setting.Value -eq 'null') { "settings delete system $($setting.Key)" } else { "settings put system $($setting.Key) $($setting.Value)" }
        [void](Invoke-Sh -Serial $serial -Command $command -AllowFail)
    }
    Write-JsonFile (Join-Path $dir 'lifecycle.json') @($results)
}
$failed = @($results | Where-Object { -not $_.passed }).Count
Exit-Verdict -Subject 'S0018 lifecycle' -Code $(if ($failed) { 1 } else { 0 }) -Detail "$($results.Count) cases; $failed failed; $dir"
