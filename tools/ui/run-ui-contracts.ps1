#requires -Version 7.0
<#
.SYNOPSIS
    Runs real controls and committed-frame measurements on a dedicated Tourist emulator.
.DESCRIPTION
    Install the current noLegal debug APK first. Only an AVD named S0015_* is accepted.
    Captures and raw results go under temp/S0015/ui. Device display, font, theme and locale
    are restored even on failure. No package data is cleared. Exit 0 PASS, 1 FAIL, 2 unavailable.
#>
[CmdletBinding()]
param([Parameter(Mandatory)][string]$Serial, [string[]]$Scenario, [string]$OutputRoot = 'temp/S0015/ui')
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$package = 'com.sza.tourist'
$validated = $false
function Adb {
    $result = & $adb -s $Serial @args 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) { throw $result }
    $result.Trim()
}
if (-not (Test-Path -LiteralPath $adb)) { Write-Host 'unified-ui: COULD NOT VERIFY (adb missing)'; exit 2 }
try {
    $avd = Adb emu avd name
    if ($Serial -notmatch '^emulator-\d+$' -or $avd -notmatch '^S0015_') {
        Write-Host 'unified-ui: COULD NOT VERIFY (a dedicated S0015_* emulator is required)'; exit 2
    }
    $validated = $true
    $oldFont = Adb shell settings get system font_scale
    $oldSize = Adb shell wm size
    $oldDensity = Adb shell wm density
    $oldTheme = Adb shell cmd uimode night
    $oldLocale = Adb shell cmd locale get-app-locales $package
    $scenarios = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'scenarios.json') -Raw | ConvertFrom-Json).scenarios
    if ($Scenario) { $scenarios = @($scenarios | Where-Object { $Scenario -contains $_.id }) }
    if (@($scenarios).Count -eq 0) { throw 'No scenarios selected' }
    New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null
    foreach ($item in $scenarios) {
        Write-Host "Running $($item.id)"
        Adb shell am force-stop $package | Out-Null
        Adb shell wm size $item.size | Out-Null
        Adb shell wm density $item.density | Out-Null
        Adb shell settings put system font_scale $item.fontScale | Out-Null
        Adb shell cmd uimode night $item.night | Out-Null
        Adb shell cmd locale set-app-locales $package --locales $item.locale | Out-Null
        $result = Adb shell am instrument -w -e scenario $item.id -e timings $item.timings "$package/.ui.contracts.UiContractInstrumentation"
        $result | Set-Content -LiteralPath (Join-Path $OutputRoot "$($item.id).log")
        try { Adb pull "/sdcard/Android/data/$package/files/ui-contracts/$($item.id)" $OutputRoot | Out-Null }
        catch { if ($result -match 'ui-contracts: PASS') { throw } else { Write-Host 'The failed process left no result files.' } }
        if ($result -notmatch 'ui-contracts: PASS') { throw "$($item.id): $result" }
        $record = Get-Content -LiteralPath (Join-Path $OutputRoot "$($item.id)/results.json") -Raw | ConvertFrom-Json
        if ($result -notmatch 'ui-contracts: PASS' -or $record.failure) { throw "$($item.id) failed: $($record.failure)" }
        if ($record.densityDpi -ne $item.density -or [math]::Abs($record.fontScale - [double]$item.fontScale) -gt .01 -or
            -not $record.locale.StartsWith($item.locale)) { throw "$($item.id): requested device configuration was not applied" }
        foreach ($group in @($record.timings | Group-Object { "$($_.destination)/$($_.kind)" })) {
            $samples = @($group.Group.ms | Sort-Object)
            if ($samples.Count -ne 20) { throw "$($group.Name): expected 20 samples, got $($samples.Count)" }
            $p95 = $samples[[math]::Ceiling(.95 * $samples.Count) - 1]
            $maximum = $samples[-1]
            $budget = if ($group.Name -match '/warm$') { 150 } else { 500 }
            $over = @($samples | Where-Object { $_ -ge $budget }).Count
            Write-Host "$($group.Name): n=20 p95=$p95 ms max=$maximum ms over-budget=$over"
            if ($p95 -ge $budget -or $over -gt 1) { throw "$($group.Name): timing budget failed" }
        }
        Write-Host "$($item.id): PASS"
    }
    $exitCode = 0
} catch { Write-Host $_; $exitCode = 1 }
finally {
    if ($validated) {
    try {
    if ($oldFont -eq 'null') { Adb shell settings delete system font_scale | Out-Null }
    elseif ($oldFont) { Adb shell settings put system font_scale $oldFont | Out-Null }
    if ($oldSize) { Adb shell wm size $(if ($oldSize -match 'Override size: (\d+x\d+)') { $Matches[1] } else { 'reset' }) | Out-Null }
    if ($oldDensity) { Adb shell wm density $(if ($oldDensity -match 'Override density: (\d+)') { $Matches[1] } else { 'reset' }) | Out-Null }
    if ($oldTheme) { Adb shell cmd uimode night $(if ($oldTheme -match 'yes') { 'yes' } elseif ($oldTheme -match 'auto') { 'auto' } else { 'no' }) | Out-Null }
    if ($oldLocale) { Adb shell cmd locale set-app-locales $package --locales $(if ($oldLocale -match '\[([^\]]*)\]') { $Matches[1] } else { '' }) | Out-Null }
    Adb shell am force-stop $package | Out-Null
    } catch { Write-Host "Device restoration could not finish: $_"; $exitCode = 2 }
    }
}
Write-Host "unified-ui: $(if ($exitCode -eq 0) { 'PASS' } elseif ($exitCode -eq 2) { 'COULD NOT VERIFY' } else { 'FAIL' })"
exit $exitCode
