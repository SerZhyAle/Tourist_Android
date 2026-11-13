<#
.SYNOPSIS
    Pre-release sweep, device half: proves a candidate package updates a real prior install and keeps its data.

.DESCRIPTION
    On an emulator only (the script uninstalls the package first, which deletes its data):
      1. both packages carry one application id and one signing certificate, the candidate's versionCode is higher
      2. the prior package is installed fresh and launched, so the app writes its own data
      3. the app's data files are listed (adb root, a userdebug image; SKIP on an image that refuses root)
         with versionCode and install times
      4. the candidate is installed over it with adb install -r
      5. versionCode is the candidate's, firstInstallTime is unchanged, every prior data file is still there
      6. the candidate is launched: its process lives and the crash buffer stays empty

    -NoLaunch skips the launches, for a package without a launcher activity (the watch face).
    Writes update-proof-<candidate>.json beside the candidate's pre-flight verdict (or into -Out).

.EXAMPLE
    pwsh -NoProfile -File tools/release/update-proof.ps1 -Serial emulator-5602 `
        -Prior temp/release/2.61.0282.031/packages/tourist-phone-store-2.61.0282.031.apk `
        -Candidate temp/release/2.61.0282.052/packages/tourist-phone-store-2.61.0282.052.apk

.NOTES
    Exit codes: 0 PASS, 1 FAIL, 2 could not run (no device, not an emulator, unreadable package).
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string] $Serial,
    [Parameter(Mandatory)][string] $Prior,
    [Parameter(Mandatory)][string] $Candidate,
    [string] $Out,
    [switch] $NoLaunch,
    [int] $SettleSeconds = 8
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '_release.ps1')

$adb = Get-Adb
$steps = [System.Collections.Generic.List[object]]::new()
function Add-Step([string] $Id, [bool] $Ok, [string] $Detail, [switch] $Skip) {
    $status = if ($Skip) { 'SKIP' } elseif ($Ok) { 'PASS' } else { 'FAIL' }
    $steps.Add([pscustomobject]@{ id = $Id; status = $status; detail = $Detail })
    Write-Output ("  {0,-5} {1,-12} {2}" -f $status, $Id, $Detail)
}
function Invoke-Adb { & $adb -s $Serial @args 2>&1 }

function Read-PackageState([string] $Package) {
    $dump = Invoke-Adb shell dumpsys package $Package
    $state = [ordered]@{ versionCode = $null; firstInstallTime = $null; lastUpdateTime = $null }
    foreach ($line in $dump) {
        if (-not $state.versionCode -and $line -match 'versionCode=(\d+)') { $state.versionCode = [int64]$Matches[1] }
        if (-not $state.firstInstallTime -and $line -match 'firstInstallTime=(.+)$') { $state.firstInstallTime = $Matches[1].Trim() }
        if (-not $state.lastUpdateTime -and $line -match 'lastUpdateTime=(.+)$') { $state.lastUpdateTime = $Matches[1].Trim() }
    }
    return [pscustomobject]$state
}

function Read-DataFiles([string] $Package) {
    $listing = Invoke-Adb shell "cd /data/data/$Package 2>/dev/null && find . -type f ! -path './cache/*' ! -path './code_cache/*'"
    if ($LASTEXITCODE -ne 0) { return $null }
    return @($listing | ForEach-Object { "$_".Trim() } | Where-Object { $_ } | Sort-Object)
}

function Test-Launch([string] $Package) {
    Invoke-Adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
    Start-Sleep -Seconds $SettleSeconds
    $procId = ("$(Invoke-Adb shell pidof $Package)").Trim()
    $crash = @(Invoke-Adb logcat -d -b crash | Where-Object { $_ -match [regex]::Escape($Package) })
    return [pscustomobject]@{ pid = $procId; alive = [bool]($procId -match '^\d+'); crashes = $crash.Count }
}

$rooted = $false
try {
    if ($Serial -notmatch '^emulator-\d+$') { throw "$Serial is not an emulator; this proof uninstalls the package and runs on emulators only." }
    if (("$(Invoke-Adb get-state)").Trim() -ne 'device') { throw "$Serial is not attached." }
    $Prior = (Resolve-Path $Prior).Path
    $Candidate = (Resolve-Path $Candidate).Path
    if (-not $Out) { $Out = Split-Path (Split-Path $Candidate -Parent) -Parent }

    $p = Read-ApkBadging $Prior
    $c = Read-ApkBadging $Candidate
    $ps = Read-Signer $Prior
    $cs = Read-Signer $Candidate
    $pkg = $c.package
    Add-Step 'identity' ($p.package -eq $c.package -and $ps -and $cs -and $ps.sha256 -eq $cs.sha256) `
        "$($p.package) -> $($c.package), certificate $(if ($ps -and $cs -and $ps.sha256 -eq $cs.sha256) { 'shared' } else { 'DIFFERS or missing' })"
    Add-Step 'order' ($c.versionCode -gt $p.versionCode) "$($p.versionName) ($($p.versionCode)) -> $($c.versionName) ($($c.versionCode))"

    Invoke-Adb uninstall $pkg | Out-Null
    $install = Invoke-Adb install $Prior
    Add-Step 'prior' ($LASTEXITCODE -eq 0 -and ($install -match 'Success')) "fresh install of $(Split-Path $Prior -Leaf): $(($install | Select-Object -Last 1))"

    Invoke-Adb logcat -b crash -c | Out-Null
    if (-not $NoLaunch) {
        $run = Test-Launch $pkg
        Add-Step 'prior-run' ($run.alive -and $run.crashes -eq 0) "pid $($run.pid), crash lines $($run.crashes)"
        Invoke-Adb shell am force-stop $pkg | Out-Null
    }

    $rootOut = Invoke-Adb root
    $rooted = ($LASTEXITCODE -eq 0 -and -not ($rootOut -match 'cannot run as root'))
    Invoke-Adb wait-for-device | Out-Null
    $before = Read-PackageState $pkg
    $filesBefore = if ($rooted) { Read-DataFiles $pkg } else { $null }

    $update = Invoke-Adb install -r $Candidate
    Add-Step 'update' ($LASTEXITCODE -eq 0 -and ($update -match 'Success')) "install -r $(Split-Path $Candidate -Leaf): $(($update | Select-Object -Last 1))"

    $after = Read-PackageState $pkg
    Add-Step 'version' ($after.versionCode -eq $c.versionCode) "device reports $($after.versionCode), candidate $($c.versionCode)"
    Add-Step 'same-install' ($before.firstInstallTime -and $after.firstInstallTime -eq $before.firstInstallTime -and $after.lastUpdateTime -ne $before.lastUpdateTime) `
        "firstInstallTime $($before.firstInstallTime) kept, lastUpdateTime $($before.lastUpdateTime) -> $($after.lastUpdateTime)"

    if ($rooted) {
        $filesAfter = Read-DataFiles $pkg
        $lost = @($filesBefore | Where-Object { $_ -notin $filesAfter })
        Add-Step 'data' ($lost.Count -eq 0) "$(@($filesBefore).Count) data files before, $(@($filesAfter).Count) after$(if ($lost.Count) { ", lost: $($lost -join ', ')" })"
    } else {
        Add-Step 'data' $false 'adb root refused on this image: data files not listed; same-install shows an update, not a reinstall' -Skip
    }

    if (-not $NoLaunch) {
        $run = Test-Launch $pkg
        Add-Step 'candidate-run' ($run.alive -and $run.crashes -eq 0) "pid $($run.pid), crash lines $($run.crashes)"
    }

    $verdict = if ($steps | Where-Object status -eq 'FAIL') { 'FAIL' } else { 'PASS' }
    $result = [ordered]@{
        verdict = $verdict; package = $pkg; serial = $Serial
        device = ("$(Invoke-Adb shell getprop ro.product.model) API $(Invoke-Adb shell getprop ro.build.version.sdk)").Trim()
        prior = [ordered]@{ file = (Split-Path $Prior -Leaf); versionName = $p.versionName; versionCode = $p.versionCode }
        candidate = [ordered]@{ file = (Split-Path $Candidate -Leaf); versionName = $c.versionName; versionCode = $c.versionCode }
        before = $before; after = $after; dataFiles = $filesBefore
        provedAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz'); steps = $steps
    }
    $file = Join-Path $Out ("update-proof-{0}.json" -f [System.IO.Path]::GetFileNameWithoutExtension($Candidate))
    $result | ConvertTo-Json -Depth 6 | Set-Content $file -Encoding utf8
    Write-Output ''
    Write-Output "UPDATE PROOF: $verdict for $pkg $($p.versionName) -> $($c.versionName) on $Serial -> $file"
    exit $(if ($verdict -eq 'PASS') { 0 } else { 1 })
} catch {
    Write-Error "update-proof could not run: $($_.Exception.Message)" -ErrorAction Continue
    exit 2
} finally {
    if ($rooted) { & $adb -s $Serial unroot 2>&1 | Out-Null }
}
