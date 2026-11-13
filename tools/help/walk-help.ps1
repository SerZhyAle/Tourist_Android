#requires -Version 7.0
<#
.SYNOPSIS
    The walk (CONTEXT-HELP conformance rung 3, S0014 criteria 2, 4, 9 and 12): presses the help button and F1 on every
    surface, in every shipped locale and both themes, and writes one dated row per surface as the evidence.

.DESCRIPTION
    Per row: the screen is reached (docs/help/src/capture.json for the pages, docs/help/src/walk.json for the
    inheriting dialogs and the screens that must stay silent); the button is tapped; the page the viewer reports
    (the debug build logs "help page: <url>") must be the page of that surface in that locale with that theme, and
    the screen below must still be the one in front; Back must close the help and return to the screen; F1 must open
    the same page. A screen the map keeps silent (the water and front flashlights) must show no button and answer F1
    with nothing. On the SOS screen the signal must still be running while the help is open and after.

    Needs a debuggable emulator and the debug build (it reads the viewer's log line). Writes
    docs/help/walk-<date>.md. Exit codes (CHECK-VERDICT section 2): 0 every row passed, 1 a row failed, 2 could not run.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Serial,
    [string[]]$Locales,
    [string[]]$Only
)

$ErrorActionPreference = 'Stop'
if ($Locales) { $Locales = @($Locales | ForEach-Object { $_ -split ',' }) }
if ($Only) { $Only = @($Only | ForEach-Object { $_ -split ',' }) }
. (Join-Path $PSScriptRoot 'lib.ps1')

if ($Serial -notmatch '^emulator-\d+$') { Write-Host "help-walk: COULD NOT VERIFY (only an emulator is a walk target; got $Serial)"; exit 2 }
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path $adb)) { Write-Host "help-walk: COULD NOT VERIFY (no adb at $adb)"; exit 2 }

$capture = Read-Json (Join-Path $script:SrcRoot 'capture.json')
$walk = Read-Json (Join-Path $script:SrcRoot 'walk.json')
$pkg = $capture.package
$map = Read-HelpMap
$shipped = Get-ShippedLocales
if (-not $Locales) { $Locales = $shipped }
$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ('help-walk-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
. (Join-Path $PSScriptRoot 'adb-lib.ps1')

# the rows: every page surface, then the extras of walk.json (inheriting dialogs, silent screens)
$rows = @()
foreach ($s in @($map.surfaces | Where-Object { (Get-Prop $_ 'form') -eq 'page' })) {
    $page = Get-Prop $capture.pages $s.help
    $rows += [pscustomobject]@{ Surface = $s.id; Spec = $page; Expect = "$($s.help).html"; Anchor = ''; Silent = $false; ButtonId = ($s.elements | Where-Object anchor -eq 'e-help' | Select-Object -First 1).view; Sos = ($s.id -eq 'sos') }
}
foreach ($x in @($walk.extra)) {
    $row = @($map.surfaces | Where-Object { $_.id -eq $x.surface })[0]
    $target = $null
    if ($row -and (Get-Prop $row 'form') -eq 'inherit') {
        $p = @($map.surfaces | Where-Object { $_.id -eq $row.parent })[0]
        $target = $p.help; $anchor = Get-Prop $row 'anchor' ''
    } else { $anchor = '' }
    $rows += [pscustomobject]@{ Surface = $x.surface; Spec = $x; Expect = $(if ($target) { "$target.html" } else { '' }); Anchor = $anchor; Silent = [bool](Get-Prop $x 'silent' $false); ButtonId = (Get-Prop $x 'button' 'helpDialogButton'); Sos = $false }
}
if ($Only) { $rows = @($rows | Where-Object { $Only -contains $_.Surface }) }

function Get-HelpLog {
    $log = Invoke-Adb logcat -d
    $m = [regex]::Matches($log, 'help page: (file:///android_asset/help/\S+)')
    if ($m.Count -eq 0) { return '' }
    return $m[$m.Count - 1].Groups[1].Value
}
function Wait-HelpLog {
    # The viewer logs the page when it starts loading; a slow emulator can take a few seconds after the tap or the key.
    for ($i = 0; $i -lt 14; $i++) {
        Start-Sleep -Milliseconds 500
        $u = Get-HelpLog
        if ($u) { return $u }
    }
    return ''
}
function Test-HelpOpen { return ((Invoke-Adb shell dumpsys activity top) -match 'HelpDialogFragment') }
function Test-Sos { return ((Invoke-Adb shell dumpsys activity services $pkg) -match 'SosService') }
function Get-TopLine { return (((Invoke-Adb shell dumpsys activity activities) -split "`n" | Where-Object { $_ -match 'topResumedActivity' } | Select-Object -First 1)) }

$results = New-Object System.Collections.Generic.List[object]
$version = ''
try {
    if ((Invoke-Adb shell getprop ro.debuggable).Trim() -ne '1') { throw 'the target is not a debuggable image' }
    Invoke-Adb root | Out-Null
    & $adb -s $Serial wait-for-device | Out-Null
    Start-Sleep -Seconds 2
    $nightBefore = ((Invoke-Adb shell cmd uimode night) -replace 'Night mode:', '').Trim()
    $version = ((Invoke-Adb shell dumpsys package $pkg) | Select-String -Pattern 'versionName=(\S+)' | Select-Object -First 1).Matches[0].Groups[1].Value
    foreach ($locale in $Locales) {
        foreach ($theme in $script:ThemeNames) {
            Invoke-Adb shell cmd uimode night $(if ($theme -eq 'dark') { 'yes' } else { 'no' }) | Out-Null
            Start-Sleep -Milliseconds 1500
            foreach ($row in $rows) {
                if ($row.Silent -and $theme -eq 'dark') { continue }   # a silent screen is walked once per locale
                $r = [ordered]@{ surface = $row.Surface; locale = $locale; theme = $theme; button = '-'; f1 = '-'; back = '-'; note = '' }
                Write-Host "== $locale / $theme / $($row.Surface)"
                try {
                    $spec = $row.Spec
                    Invoke-Adb shell am force-stop $pkg | Out-Null
                    Invoke-Adb shell pm clear $pkg | Out-Null
                    Grant-All
                    Invoke-Adb shell cmd locale set-app-locales $pkg --locales $locale | Out-Null
                    Invoke-Adb shell pm grant $pkg android.permission.READ_MEDIA_AUDIO | Out-Null
                    if (Get-Prop $spec 'seed') { Seed-Prefs $spec.seed }
                    $script:DumpMode = 'hierarchy'
                    Invoke-Adb shell am start -W -n "$pkg/$($spec.start)" | Out-Null
                    Start-Sleep -Seconds 3
                    if (Get-Prop $spec 'steps') { Invoke-Steps $spec.steps @{} }
                    if (Get-Prop $spec 'dump') { $script:DumpMode = $spec.dump }
                    $before = Get-TopLine
                    $expectedUrl = "help/$locale/$($row.Expect)"
                    $sosBefore = if ($row.Sos) { Test-Sos } else { $true }

                    if ($row.Silent) {
                        $nodes = Get-UiNodes
                        $btn = @($nodes | Where-Object { $_.Id -match ':id/(action_help|helpButton|helpDialogButton)$' })
                        Invoke-Adb logcat -c | Out-Null
                        Invoke-Adb shell input keyevent KEYCODE_F1 | Out-Null
                        Start-Sleep -Seconds 1.5
                        $r.button = if ($btn.Count -eq 0) { 'absent (as designed)' } else { 'FAIL: a help control is on screen' }
                        $r.f1 = if ((Get-HelpLog) -eq '' -and -not (Test-HelpOpen)) { 'nothing opens (as designed)' } else { 'FAIL: F1 opened help' }
                        $r.back = 'n/a'
                    } else {
                        # --- the button
                        $nodes = Get-UiNodes
                        $btn = Find-ById $nodes $row.ButtonId
                        if ($null -eq $btn) { throw "no help button '$($row.ButtonId)' on screen" }
                        Invoke-Adb logcat -c | Out-Null
                        Invoke-Tap $btn
                        $url = Wait-HelpLog
                        $okUrl = $url -like "*$expectedUrl*" -and $url -like "*theme=$theme*" -and ($row.Anchor -eq '' -or $url -like "*#$($row.Anchor)")
                        $sosDuring = if ($row.Sos) { Test-Sos } else { $true }
                        $r.button = if ($okUrl -and (Test-HelpOpen) -and $sosDuring) { 'PASS' } else { "FAIL: opened '$url'" }
                        Invoke-Adb shell input keyevent KEYCODE_BACK | Out-Null
                        Start-Sleep -Seconds 1.5
                        $closed = -not (Test-HelpOpen)
                        $same = (Get-TopLine) -eq $before
                        $sosAfter = if ($row.Sos) { Test-Sos } else { $true }
                        $r.back = if ($closed -and $same -and $sosAfter) { 'PASS' } else { 'FAIL: help did not close or the screen changed' }
                        # --- F1
                        Invoke-Adb logcat -c | Out-Null
                        Invoke-Adb shell input keyevent KEYCODE_F1 | Out-Null
                        $url2 = Wait-HelpLog
                        $okUrl2 = $url2 -like "*$expectedUrl*" -and ($row.Anchor -eq '' -or $url2 -like "*#$($row.Anchor)")
                        $r.f1 = if ($okUrl2 -and (Test-HelpOpen)) { 'PASS' } else { "FAIL: opened '$url2'" }
                        Invoke-Adb shell input keyevent KEYCODE_BACK | Out-Null
                        if ($row.Sos) { $r.note = 'signal kept running while the help was open: ' + $(if ($sosDuring -and $sosAfter) { 'yes' } else { 'NO' }) }
                        $r.note += $(if ($url) { " $($url -replace 'file:///android_asset/', '')" })
                    }
                    if ($row.Sos) { Invoke-Adb shell input keyevent KEYCODE_BACK | Out-Null }
                } catch {
                    $r.note = "FAIL: $($_.Exception.Message)"
                    foreach ($k in 'button', 'f1', 'back') { if ($r[$k] -eq '-') { $r[$k] = 'FAIL' } }
                }
                $results.Add([pscustomobject]$r)
            }
        }
    }
} finally {
    Invoke-Adb shell am force-stop $pkg | Out-Null
    Invoke-Adb shell cmd locale set-app-locales $pkg --locales '' | Out-Null
    Invoke-Adb shell cmd uimode night $(if ($nightBefore -match 'yes') { 'yes' } elseif ($nightBefore -match 'auto') { 'auto' } else { 'no' }) | Out-Null
    Invoke-Adb unroot | Out-Null
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}

$failed = @($results | Where-Object { $_.button -like 'FAIL*' -or $_.f1 -like 'FAIL*' -or $_.back -like 'FAIL*' })
$date = Get-Date -Format 'yyyy-MM-dd'
$md = New-Object System.Text.StringBuilder
$null = $md.AppendLine("# Help walk - $date")
$null = $md.AppendLine('')
$null = $md.AppendLine("Written by ``tools/help/walk-help.ps1`` (CONTEXT-HELP rung 3, S0014 criterion 12) on $Serial against Tourist $version. One row per surface, locale and theme: the button, F1 and Back were pressed on the live screen; the page is the one the viewer logged. A surface the map keeps silent shows no button and answers F1 with nothing.")
$null = $md.AppendLine('')
$null = $md.AppendLine('| Surface | Locale | Theme | Button | F1 | Back | Evidence |')
$null = $md.AppendLine('| --- | --- | --- | --- | --- | --- | --- |')
foreach ($x in $results) { $null = $md.AppendLine("| ``$($x.surface)`` | $($x.locale) | $($x.theme) | $($x.button) | $($x.f1) | $($x.back) | $($x.note.Trim()) |") }
$verdict = if ($failed.Count -gt 0) { "help-walk: FAIL ($($failed.Count) of $($results.Count) row(s))" } else { "help-walk: PASS ($($results.Count) rows, $($Locales.Count) locale(s), 2 themes)" }
$null = $md.AppendLine('')
$null = $md.AppendLine("Verdict: ``$verdict``")
Write-Utf8 (Join-Path $script:RepoRoot "docs\help\walk-$date.md") $md.ToString()
foreach ($f in $failed) { Write-Host "FAIL  $($f.surface) $($f.locale) $($f.theme): button=$($f.button) f1=$($f.f1) back=$($f.back) $($f.note)" }
Write-Host $verdict
if ($failed.Count -gt 0) { exit 1 }
exit 0
