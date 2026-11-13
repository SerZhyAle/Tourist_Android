#requires -Version 7.0
<#
.SYNOPSIS
    Generates the help screenshots and the marker bounds from the real build on an emulator
    (S0014, CONTEXT-HELP rule 14): per page, per locale, per theme, with demo content.

.DESCRIPTION
    Host-side, over adb, so the app needs no cooperation: the locale is set per app (cmd locale), the theme with
    cmd uimode, the status bar is pinned with the System UI demo mode, the screen is reached by an Activity
    intent and taps found by resource id or by the UI's own text, the bounds of every element come from the
    UI dump (uiautomator) and not from a person's pointer, the image from screencap.

    Demo content only: the position of the dashboard is the synthetic fix in docs/help/src/capture.json; no
    account, path, name or key is ever on screen. Nothing is edited by hand afterwards.

    Writes, per page and locale:
      phone/src/main/assets/help/<locale>/img/<help>-light.jpg and -dark.jpg
      docs/help/shots/<locale>/<help>.json  (screen size, image size, element bounds, product version, date)

    It changes the emulator's per-app language, night mode and demo status bar, and puts them back at the end.
    Only an emulator is accepted as the target.

    Exit codes (CHECK-VERDICT section 2): 0 captured, 1 an element or a screen was not found, 2 could not run.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Serial,
    [string[]]$Pages,
    [string[]]$Locales,
    [int]$ImageWidth = 540,
    [int]$JpegQuality = 80
)

$ErrorActionPreference = 'Stop'
if ($Pages) { $Pages = @($Pages | ForEach-Object { $_ -split "," }) }
if ($Locales) { $Locales = @($Locales | ForEach-Object { $_ -split "," }) }
. (Join-Path $PSScriptRoot 'lib.ps1')
Add-Type -AssemblyName System.Drawing

if ($Serial -notmatch '^emulator-\d+$') {
    Write-Host 'help-capture: COULD NOT VERIFY (only an emulator is a capture target; got ' $Serial ')'
    exit 2
}
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path $adb)) { Write-Host "help-capture: COULD NOT VERIFY (no adb at $adb)"; exit 2 }

$spec = Read-Json (Join-Path $script:SrcRoot 'capture.json')
$pkg = $spec.package
$map = Read-HelpMap
$shipped = Get-ShippedLocales
if (-not $Locales) { $Locales = $shipped }
$surfaces = @($map.surfaces | Where-Object { (Get-Prop $_ 'form') -eq 'page' })
if ($Pages) { $surfaces = @($surfaces | Where-Object { $Pages -contains $_.help }) }

$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ('help-capture-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

. (Join-Path $PSScriptRoot 'adb-lib.ps1')

function Save-Screenshot([string]$Dest, [int]$CropHeight = 0) {
    # screencap on the device, pulled, scaled to the page's image width and stored as JPEG.
    Invoke-Adb shell screencap -p /sdcard/help-shot.png | Out-Null
    $png = Join-Path $tmp 'shot.png'
    Invoke-Adb pull /sdcard/help-shot.png $png | Out-Null
    $src = [System.Drawing.Image]::FromFile($png)
    try {
        $srcH = if ($CropHeight -gt 0 -and $CropHeight -lt $src.Height) { $CropHeight } else { $src.Height }
        $h = [int][math]::Round($srcH * $ImageWidth / $src.Width)
        $bmp = New-Object System.Drawing.Bitmap($ImageWidth, $h)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.DrawImage($src, (New-Object System.Drawing.Rectangle(0, 0, $ImageWidth, $h)), 0, 0, $src.Width, $srcH, [System.Drawing.GraphicsUnit]::Pixel)
        $g.Dispose()
        $enc = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
        $p = New-Object System.Drawing.Imaging.EncoderParameters(1)
        $p.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter([System.Drawing.Imaging.Encoder]::Quality, [long]$JpegQuality)
        $dir = Split-Path -Parent $Dest
        if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
        $bmp.Save($Dest, $enc, $p)
        $bmp.Dispose()
        return [pscustomobject]@{ Width = $src.Width; Height = $srcH; ImageWidth = $ImageWidth; ImageHeight = $h }
    } finally { $src.Dispose() }
}

function Get-MeanLuma([string]$Path) {
    # Used only to skip an SOS frame caught while the screen is flashing white.
    $bmp = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $sum = 0.0; $n = 0
        foreach ($yy in 0..9) { foreach ($xx in 0..9) {
            $c = $bmp.GetPixel([int]($bmp.Width * ($xx + 0.5) / 10), [int]($bmp.Height * ($yy + 0.5) / 10))
            $sum += 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B; $n++ } }
        return $sum / $n
    } finally { $bmp.Dispose() }
}

# ---- state of the device before, so it can be put back ----
$nightBefore = ((Invoke-Adb shell cmd uimode night) -replace 'Night mode:', '').Trim()
Write-Host "help-capture: target $Serial, night mode was '$nightBefore'"

try {
    # The screens are not exported, so am start needs root; this is an emulator image of type userdebug.
    if ((Invoke-Adb shell getprop ro.debuggable).Trim() -ne '1') { throw 'the target is not a debuggable image; adb root is not possible'  }
    Invoke-Adb root | Out-Null
    & $adb -s $Serial wait-for-device | Out-Null
    Start-Sleep -Seconds 2
    # a pinned, clean status bar so two captures of one screen differ in nothing but the screen
    Invoke-Adb shell settings put global sysui_demo_allowed 1 | Out-Null
    # am broadcast -a com.android.systemui.demo -e command <name> [-e key value ..]
    foreach ($demo in @(
            @('enter'),
            @('clock', '-e', 'hhmm', '1000'),
            @('battery', '-e', 'level', '100', '-e', 'plugged', 'false'),
            @('network', '-e', 'wifi', 'show', '-e', 'level', '4'),
            @('network', '-e', 'mobile', 'hide'),
            @('notifications', '-e', 'visible', 'false'))) {
        $bargs = @('shell', 'am', 'broadcast', '-a', 'com.android.systemui.demo', '-e', 'command') + $demo
        Invoke-Adb @bargs | Out-Null
    }
    Grant-All

    $version = ((Invoke-Adb shell dumpsys package $pkg) | Select-String -Pattern 'versionName=(\S+)' | Select-Object -First 1).Matches[0].Groups[1].Value
    if (-not $version) { throw "$pkg is not installed on $Serial" }
    $today = Get-Date -Format 'yyyy-MM-dd'
    $layoutsHash = Get-LayoutsHash
    $script:Crop = @{}

    foreach ($locale in $Locales) {
        $strings = Read-Strings $locale
        $stringsHash = Get-StringsHash $locale
        $records = @{}
        foreach ($theme in $script:ThemeNames) {
            Invoke-Adb shell cmd uimode night $(if ($theme -eq 'dark') { 'yes' } else { 'no' }) | Out-Null
            Wait-Seconds 1.5
            foreach ($surface in $surfaces) {
                $helpId = $surface.help
                $page = Get-Prop $spec.pages $helpId
                if ($null -eq $page) { Add-Failure "$helpId : no entry in capture.json"; continue }
                Write-Host "== $locale / $theme / $helpId"
                try {
                    Invoke-Adb shell am force-stop $pkg | Out-Null
                    Invoke-Adb shell pm clear $pkg | Out-Null
                    Grant-All
                    # pm clear also clears the per-app language, so it is set after it
                    Invoke-Adb shell cmd locale set-app-locales $pkg --locales $locale | Out-Null
                    if (Get-Prop $page 'seed') { Seed-Prefs $page.seed }
                    $script:DumpMode = 'hierarchy'
                    if (Get-Prop $page 'geo' $false) {
                        Invoke-Adb emu geo fix $spec.geo.lon $spec.geo.lat | Out-Null
                    }
                    Invoke-Adb shell am start -W -n "$pkg/$($page.start)" | Out-Null
                    Wait-Seconds ([double](Get-Prop $page 'settle' 3))
                    if (Get-Prop $page 'steps') { Invoke-Steps $page.steps $strings }
                    if (Get-Prop $page 'dump') { $script:DumpMode = $page.dump }
                    Assert-Screen $page.expect
                    Wait-Seconds ([double](Get-Prop $page 'wait' 1))

                    $nodes = Get-UiNodes
                    $dest = Join-Path $script:AssetHelp "$locale\img\$helpId-$theme.jpg"
                    $info = Save-Screenshot $dest
                    if (Get-Prop $page 'avoidWhite' $false) {
                        for ($k = 0; $k -lt 8 -and (Get-MeanLuma (Join-Path $tmp 'shot.png')) -gt 200; $k++) {
                            Wait-Seconds 0.35
                            $info = Save-Screenshot $dest
                        }
                    }
                    $cropKey = "$locale/$helpId"
                    if ($theme -eq 'dark' -and $script:Crop.ContainsKey($cropKey) -and $script:Crop[$cropKey] -gt 0) {
                        $info = Save-Screenshot $dest $script:Crop[$cropKey]
                    }
                    if ($theme -eq 'light') {
                        $bounds = [ordered]@{}
                        $missing = @()
                        foreach ($e in $surface.elements) {
                            if ((Get-Prop $e 'shot' $true) -eq $false) { continue }
                            $n = Find-ById $nodes $e.view
                            # a view the app itself fails to show in this language is recorded, not hidden: capture.json
                            # names it under allowMissing, the gate lists it, and the baseline carries it until it is fixed
                            $allowed = @(Get-Prop (Get-Prop $page 'allowMissing') $locale @())
                            $absent = ($null -eq $n) -or (($n.R - $n.L) -lt 24)
                            if ($absent -and $allowed -contains $e.anchor) {
                                $missing += $e.anchor
                                Write-Host "  note: '$($e.anchor)' is not visible in $locale (accepted in capture.json)"
                                continue
                            }
                            if ($null -eq $n) { Add-Failure "$locale/$helpId : view '$($e.view)' for '$($e.anchor)' is not in the view tree"; continue }
                            # clipped to the screen: a view scrolled out of sight must not get a marker
                            $cl = [math]::Max($n.L, 0); $ct = [math]::Max($n.T, 0)
                            $cr = [math]::Min($n.R, $info.Width); $cb = [math]::Min($n.B, $info.Height)
                            if (($cr - $cl) -lt 24 -or ($cb - $ct) -lt 24) { Add-Failure "$locale/$helpId : view '$($e.view)' for '$($e.anchor)' is outside the screen ($($n.L),$($n.T)-$($n.R),$($n.B))"; continue }
                            $bounds[$e.anchor] = [ordered]@{ l = $cl; t = $ct; r = $cr; b = $cb }
                        }
                        # the empty bottom of a short screen is cut off; a dialog keeps the dimmed screen around it
                        if ($script:DumpMode -ne 'accessibility' -and -not (Get-Prop $page 'avoidWhite' $false) -and $bounds.Count -gt 0) {
                            $lowest = ($bounds.Values | ForEach-Object { $_.b } | Measure-Object -Maximum).Maximum
                            $cropTo = [int]([math]::Ceiling(($lowest + 0.07 * $info.Height) / 8) * 8)
                            if ($cropTo -lt $info.Height - 80) {
                                $script:Crop[$cropKey] = $cropTo
                                $info = Save-Screenshot $dest $cropTo
                            }
                        }
                        $records[$helpId] = [ordered]@{
                            help = $helpId; locale = $locale; version = $version; capturedOn = $today
                            layoutsSha256 = $layoutsHash; stringsSha256 = $stringsHash
                            width = $info.Width; height = $info.Height; imageWidth = $info.ImageWidth; imageHeight = $info.ImageHeight
                            elements = $bounds
                            missing = @($missing)
                        }
                    }
                    if (Get-Prop $page 'cleanup') { Invoke-Steps $page.cleanup $strings }
                } catch {
                    Add-Failure "$locale/$theme/$helpId : $($_.Exception.Message)"
                }
            }
        }
        foreach ($helpId in $records.Keys) {
            Write-Utf8 (Join-Path $script:ShotsRoot "$locale\$helpId.json") (($records[$helpId] | ConvertTo-Json -Depth 6) + "`n")
        }
    }
} finally {
    Invoke-Adb shell am force-stop $pkg | Out-Null
    Invoke-Adb unroot | Out-Null
    Invoke-Adb shell cmd locale set-app-locales $pkg --locales '' | Out-Null
    Invoke-Adb shell cmd uimode night $(if ($nightBefore -match 'yes') { 'yes' } elseif ($nightBefore -match 'auto') { 'auto' } else { 'no' }) | Out-Null
    Invoke-Adb shell am broadcast -a com.android.systemui.demo -e command exit | Out-Null
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}

if ($script:Failures.Count -gt 0) {
    Write-Host "help-capture: FAIL ($($script:Failures.Count) problem(s))"
    exit 1
}
Write-Host "help-capture: PASS ($($surfaces.Count) page(s), $($Locales.Count) locale(s), 2 themes)"
exit 0
