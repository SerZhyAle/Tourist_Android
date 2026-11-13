#requires -Version 7.0
# Shared library of the S0013 measurement stand. Dot-sourced by every M-script; never run on its own.
#
# The stand reads the app from outside only (ticket section 5): adb, dumpsys, /proc through run-as, logcat,
# uiautomator dumps. Nothing here asks the app for cooperation.
#
# Error convention: a probe that cannot answer throws a message that starts with 'INVALID:'. Every entry
# script traps it and exits 2 (COULD NOT VERIFY) - an invalid run is never a finding (ticket section 7).

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 1.0

$script:StandRoot = $PSScriptRoot
$script:RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$script:TicketFile = Join-Path $script:RepoRoot 'PLAN\S0013_deep-dynamic-audit.md'
$script:TempRoot = Join-Path $script:RepoRoot 'temp\S0013'
$script:Package = 'com.sza.tourist'

function Get-SdkRoot {
    if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) { return $env:ANDROID_SDK_ROOT }
    $props = Join-Path $script:RepoRoot 'local.properties'
    if (Test-Path $props) {
        $line = Get-Content $props | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) { return ($line -replace '^sdk\.dir=', '' -replace '\\:', ':' -replace '\\\\', '\') }
    }
    throw 'INVALID: Android SDK not found (ANDROID_SDK_ROOT or local.properties sdk.dir)'
}

$script:Sdk = Get-SdkRoot
$script:AdbPath = Join-Path $script:Sdk 'platform-tools\adb.exe'
$script:EmulatorPath = Join-Path $script:Sdk 'emulator\emulator.exe'
$script:AvdManagerPath = Join-Path $script:Sdk 'cmdline-tools\latest\bin\avdmanager.bat'

function Get-Aapt2Path {
    $bt = Get-ChildItem (Join-Path $script:Sdk 'build-tools') -Directory | Sort-Object { [version]($_.Name -replace '[^0-9.]', '') } -Descending
    foreach ($d in $bt) { $p = Join-Path $d.FullName 'aapt2.exe'; if (Test-Path $p) { return $p } }
    throw 'INVALID: aapt2 not found under build-tools'
}

# ---------------------------------------------------------------------------------------------------------
# Verdict line (CHECK-VERDICT section 2): the last stdout line, one word of the closed set, code travels
# with the word.
# ---------------------------------------------------------------------------------------------------------

function Get-VerdictWord([int] $Code, [switch] $NotApplicable) {
    switch ($Code) {
        0 { if ($NotApplicable) { 'NOT APPLICABLE' } else { 'PASS' } }
        1 { 'FAIL' }
        2 { 'COULD NOT VERIFY' }
        default { 'FAIL' }
    }
}

function Exit-Verdict {
    param([string] $Subject, [int] $Code, [string] $Detail, [switch] $NotApplicable)
    $word = Get-VerdictWord $Code -NotApplicable:$NotApplicable
    $line = if ($Detail) { "${Subject}: $word ($Detail)" } else { "${Subject}: $word" }
    [Console]::Out.Flush()
    Write-Output $line
    exit $Code
}

# Aggregation precedence of CHECK-VERDICT 2.14 over child codes: any 1 -> 1, else any 2 -> 2, else 0.
function Merge-Codes([int[]] $Codes) {
    if (@($Codes | Where-Object { $_ -notin 0, 2 }).Count -gt 0) { return 1 }
    if ($Codes -contains 2) { return 2 }
    return 0
}

# ---------------------------------------------------------------------------------------------------------
# Process helpers
# ---------------------------------------------------------------------------------------------------------

function Invoke-Native {
    param(
        [Parameter(Mandatory)] [string] $FilePath,
        [string[]] $Arguments = @(),
        [int] $TimeoutSec = 60,
        [string] $OutFile,
        [string] $InFile,
        [string] $StdinText,
        [switch] $AllowFail
    )
    $psi = [Diagnostics.ProcessStartInfo]::new($FilePath)
    foreach ($a in $Arguments) { $psi.ArgumentList.Add($a) }
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.RedirectStandardInput = [bool]$InFile -or [bool]$StdinText
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $p = [Diagnostics.Process]::Start($psi)
    $fs = $null
    try {
        if ($OutFile) {
            $fs = [IO.File]::Create($OutFile)
            $outTask = $p.StandardOutput.BaseStream.CopyToAsync($fs)
        } else {
            $outTask = $p.StandardOutput.ReadToEndAsync()
        }
        $errTask = $p.StandardError.ReadToEndAsync()
        if ($StdinText) { $p.StandardInput.Write($StdinText); $p.StandardInput.Close() }
        if ($InFile) {
            $in = [IO.File]::OpenRead($InFile)
            try { $in.CopyTo($p.StandardInput.BaseStream) } finally { $in.Dispose(); $p.StandardInput.Close() }
        }
        if (-not $p.WaitForExit($TimeoutSec * 1000)) {
            try { $p.Kill($true) } catch { }
            throw "INVALID: probe timeout after ${TimeoutSec}s: $([IO.Path]::GetFileName($FilePath)) $($Arguments -join ' ')"
        }
        $p.WaitForExit()
        # A grandchild that inherited the pipe (an emulator) keeps it open after the child exits; never
        # block on its EOF.
        if (-not $outTask.Wait(15000)) { throw "INVALID: output pipe of $([IO.Path]::GetFileName($FilePath)) still held open by a descendant" }
        $stdout = if ($OutFile) { '' } else { $outTask.Result }
        $stderr = if ($errTask.Wait(5000)) { $errTask.Result } else { '' }
    } finally {
        if ($fs) { $fs.Dispose() }
    }
    if ($p.ExitCode -ne 0 -and -not $AllowFail) {
        throw "INVALID: $([IO.Path]::GetFileName($FilePath)) $($Arguments -join ' ') exited $($p.ExitCode): $($stderr.Trim()) $($stdout.Trim())"
    }
    [pscustomobject]@{ ExitCode = $p.ExitCode; Out = $stdout; Err = $stderr }
}

function Invoke-Adb {
    param([string] $Serial, [string[]] $Arguments, [int] $TimeoutSec = 60, [string] $OutFile, [string] $InFile, [switch] $AllowFail)
    $all = @()
    if ($Serial) { $all += '-s', $Serial }
    $all += $Arguments
    Invoke-Native -FilePath $script:AdbPath -Arguments $all -TimeoutSec $TimeoutSec -OutFile $OutFile -InFile $InFile -AllowFail:$AllowFail
}

# One device shell command line; returns stdout text. adb shell returns the remote exit code since API 24.
function Invoke-Sh {
    param([Parameter(Mandatory)] [string] $Serial, [Parameter(Mandatory)] [string] $Command, [int] $TimeoutSec = 60, [switch] $AllowFail)
    (Invoke-Adb -Serial $Serial -Arguments @('shell', $Command) -TimeoutSec $TimeoutSec -AllowFail:$AllowFail).Out
}

function Invoke-RunAs {
    param([string] $Serial, [string] $Command, [string] $Package = $script:Package, [int] $TimeoutSec = 60, [switch] $AllowFail)
    Invoke-Sh -Serial $Serial -Command "run-as $Package sh -c '$($Command -replace "'", "'\''")'" -TimeoutSec $TimeoutSec -AllowFail:$AllowFail
}

# Probe self-check (ticket section 7): the device answers a trivial command quickly, or the run is invalid.
function Assert-DeviceAlive([string] $Serial) {
    $r = Invoke-Adb -Serial $Serial -Arguments @('shell', 'echo ok') -TimeoutSec 15 -AllowFail
    if ($r.ExitCode -ne 0 -or $r.Out.Trim() -ne 'ok') { throw "INVALID: probe self-check failed on $Serial (adb drop or emulator wedge)" }
}

function Get-DeviceUptimeMs([string] $Serial) {
    $t = (Invoke-Sh -Serial $Serial -Command 'cat /proc/uptime').Trim().Split(' ')[0]
    [long]([double]::Parse($t, [Globalization.CultureInfo]::InvariantCulture) * 1000)
}

function Get-NowMs { [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() }

# ---------------------------------------------------------------------------------------------------------
# Run folder and event journal
# ---------------------------------------------------------------------------------------------------------

function New-RunId([string] $Kind) { '{0}-{1}' -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $Kind }

function Get-RunDir([string] $RunId) {
    $d = Join-Path $script:TempRoot "runs\$RunId"
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Path $d -Force | Out-Null }
    $d
}

function Write-JsonFile([string] $Path, $Object, [int] $Depth = 12) {
    $dir = Split-Path $Path -Parent
    if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    ($Object | ConvertTo-Json -Depth $Depth) | Set-Content -LiteralPath $Path -Encoding utf8NoBOM
}

function Read-JsonFile([string] $Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable
}

function Add-Event {
    param([string] $RunDir, [string] $Kind, [hashtable] $Data = @{})
    $e = [ordered]@{ ts = Get-NowMs; kind = $Kind }
    foreach ($k in $Data.Keys) { $e[$k] = $Data[$k] }
    ($e | ConvertTo-Json -Compress -Depth 6) | Add-Content -LiteralPath (Join-Path $RunDir 'events.jsonl') -Encoding utf8NoBOM
}

# ---------------------------------------------------------------------------------------------------------
# Build under test (canon TESTING_AND_QA section 4: confirm the device runs the new build)
# ---------------------------------------------------------------------------------------------------------

function Get-ApkIdentity([string] $ApkPath) {
    if (-not (Test-Path -LiteralPath $ApkPath)) { throw "INVALID: APK not found: $ApkPath" }
    $badging = (Invoke-Native -FilePath (Get-Aapt2Path) -Arguments @('dump', 'badging', $ApkPath)).Out
    if ($badging -notmatch "package: name='([^']+)' versionCode='([^']+)' versionName='([^']*)'") { throw "INVALID: unreadable badging of $ApkPath" }
    [ordered]@{
        package     = $Matches[1]
        versionCode = $Matches[2]
        versionName = $Matches[3]
        sha256      = (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash.ToLowerInvariant()
        apk         = $ApkPath
    }
}

# The version readback of ticket section 5: dumpsys names the build, and the installed base.apk hashes to
# the file under test - a same-version debug build cannot silently keep old code past this.
function Assert-BuildUnderTest {
    param([string] $Serial, [hashtable] $Identity)
    $dump = Invoke-Sh -Serial $Serial -Command "dumpsys package $($Identity.package)"
    if ($dump -notmatch 'versionCode=(\d+)') { throw "INVALID: version readback: $($Identity.package) is not installed on $Serial" }
    $code = $Matches[1]
    $name = if ($dump -match 'versionName=(\S+)') { $Matches[1] } else { '' }
    $path = ((Invoke-Sh -Serial $Serial -Command "pm path $($Identity.package)") -split "`n" | Where-Object { $_ -match 'base\.apk' } | Select-Object -First 1) -replace '^package:', ''
    $deviceSha = ((Invoke-Sh -Serial $Serial -Command "sha256sum $($path.Trim())").Trim() -split '\s+')[0].ToLowerInvariant()
    $ok = ($code -eq $Identity.versionCode) -and ($name -eq $Identity.versionName) -and ($deviceSha -eq $Identity.sha256)
    if (-not $ok) {
        throw "INVALID: version readback mismatch on ${Serial}: device $code/$name/$deviceSha, under test $($Identity.versionCode)/$($Identity.versionName)/$($Identity.sha256)"
    }
    [ordered]@{ serial = $Serial; versionCode = $code; versionName = $name; sha256 = $deviceSha }
}

function Get-AppUid([string] $Serial, [string] $Package = $script:Package) {
    $d = Invoke-Sh -Serial $Serial -Command "dumpsys package $Package"
    # API 33 prints userId=, API 37 appId=.
    if ($d -match '\b(?:userId|appId)=(\d+)') { return [int]$Matches[1] }
    throw "INVALID: no uid for $Package on $Serial"
}

function Get-AppPid([string] $Serial, [string] $Package = $script:Package) {
    $o = (Invoke-Sh -Serial $Serial -Command "pidof $Package" -AllowFail).Trim()
    if ($o -match '^\d+') { return [int](($o -split '\s+')[0]) }
    return $null
}

# ---------------------------------------------------------------------------------------------------------
# Logcat marks: the stand writes its own tag (S0013), never through the app.
# ---------------------------------------------------------------------------------------------------------

function Add-Mark([string] $Serial, [string] $Text) {
    [void](Invoke-Sh -Serial $Serial -Command "log -t S0013 '$Text'")
}

function Clear-Logcat([string] $Serial) {
    [void](Invoke-Adb -Serial $Serial -Arguments @('logcat', '-b', 'all', '-c') -AllowFail -TimeoutSec 20)
}

function Save-Logcat {
    param([string] $Serial, [string] $Path)
    [void](Invoke-Adb -Serial $Serial -Arguments @('logcat', '-b', 'main,system,crash', '-d', '-v', 'threadtime,uid,monotonic') -OutFile $Path -TimeoutSec 60)
    [void](Invoke-Adb -Serial $Serial -Arguments @('logcat', '-b', 'events', '-d', '-v', 'threadtime,monotonic') -OutFile ($Path -replace '\.txt$', '.events.txt') -TimeoutSec 60)
}

# ---------------------------------------------------------------------------------------------------------
# UI: uiautomator dumps and node lookup
# ---------------------------------------------------------------------------------------------------------

$script:ProbePackage = 'com.sza.s0013probe'
$script:ProbeComponent = 'com.sza.s0013probe/com.sza.s0013probe.ProbeService'
$script:ProbePersistent = @{}

# The accessibility probe (probe-a11y/) is the dumper: uiautomator waits for an idle UI, and a live screen
# never idles. The service is bound only around a dump unless a sweep keeps it bound (Set-ProbePersistent),
# so the app runs with accessibility off while it is measured.
function Install-Probe([string] $Serial) {
    $apk = Join-Path $script:TempRoot 'probe\s0013probe.apk'
    if (-not (Test-Path $apk)) { throw 'INVALID: probe APK missing - run probe-a11y/build.ps1' }
    [void](Invoke-Adb -Serial $Serial -Arguments @('install', '-r', '-t', $apk) -TimeoutSec 120)
}

function Set-ProbeBound([string] $Serial, [bool] $On) {
    if ($On) {
        [void](Invoke-Sh -Serial $Serial -Command "settings put secure enabled_accessibility_services $script:ProbeComponent; settings put secure accessibility_enabled 1")
    } else {
        [void](Invoke-Sh -Serial $Serial -Command 'settings delete secure enabled_accessibility_services; settings put secure accessibility_enabled 0' -AllowFail)
    }
}

function Set-ProbePersistent([string] $Serial, [bool] $On) {
    $script:ProbePersistent[$Serial] = $On
    Set-ProbeBound $Serial $On
}

function Get-ProbeDump([string] $Serial, [switch] $AllWindows) {
    $persistent = $script:ProbePersistent[$Serial]
    if (-not $persistent) { Set-ProbeBound $Serial $true }
    try {
        $extra = if ($AllWindows) { ' --ez all true' } else { '' }
        $deadline = (Get-Date).AddSeconds(8)
        do {
            $o = Invoke-Sh -Serial $Serial -Command "am broadcast -n $script:ProbePackage/.DumpReceiver$extra" -AllowFail
            if ($o -match 'data="ok \d+"') {
                return (Invoke-Adb -Serial $Serial -Arguments @('exec-out', "run-as $script:ProbePackage cat files/dump.xml") -TimeoutSec 30).Out
            }
            Start-Sleep -Milliseconds 250
        } while ((Get-Date) -lt $deadline)
        throw "INVALID: accessibility probe did not answer on $Serial ($($o.Trim()))"
    } finally {
        if (-not $persistent) { Set-ProbeBound $Serial $false }
    }
}

function Get-UiDump {
    param([string] $Serial, [string] $SaveAs, [switch] $AllWindows, [switch] $Uiautomator)
    if (-not $Uiautomator) {
        $text = Get-ProbeDump -Serial $Serial -AllWindows:$AllWindows
        if ($SaveAs) { Set-Content -LiteralPath $SaveAs -Value $text -Encoding utf8NoBOM }
        return [xml]$text
    }
    for ($i = 0; $i -lt 3; $i++) {
        $r = Invoke-Adb -Serial $Serial -Arguments @('exec-out', 'uiautomator dump /dev/tty') -TimeoutSec 40 -AllowFail
        $text = $r.Out
        $start = $text.IndexOf('<?xml')
        $end = $text.LastIndexOf('</hierarchy>')
        if ($start -ge 0 -and $end -gt $start) {
            $xmlText = $text.Substring($start, $end - $start + '</hierarchy>'.Length)
            if ($SaveAs) { Set-Content -LiteralPath $SaveAs -Value $xmlText -Encoding utf8NoBOM }
            return [xml]$xmlText
        }
        Start-Sleep -Milliseconds 700
    }
    throw "INVALID: uiautomator dump failed on $Serial"
}

function ConvertFrom-Bounds([string] $Bounds) {
    if ($Bounds -match '\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]') {
        return [pscustomobject]@{ L = [int]$Matches[1]; T = [int]$Matches[2]; R = [int]$Matches[3]; B = [int]$Matches[4] }
    }
    $null
}

function Find-Node {
    param([xml] $Dump, [string] $Id, [string] $Text, [string] $TextMatch, [string] $Desc)
    $nodes = $Dump.SelectNodes('//node')
    foreach ($n in $nodes) {
        if ($n.GetAttribute("visible-to-user") -eq "false") { continue }
        if ($Id -and $n.'resource-id' -notmatch "(:id/|^)$([regex]::Escape($Id))$") { continue }
        if ($Text -and $n.text -ne $Text) { continue }
        if ($TextMatch -and $n.text -notmatch $TextMatch) { continue }
        if ($Desc -and $n.'content-desc' -ne $Desc) { continue }
        return $n
    }
    $null
}

function Get-NodeCenter($Node) {
    $b = ConvertFrom-Bounds $Node.bounds
    [pscustomobject]@{ X = [int](($b.L + $b.R) / 2); Y = [int](($b.T + $b.B) / 2) }
}

# Taps a node found by id or text, waiting for it to appear. Returns the dump it acted on.
function Invoke-TapNode {
    param([string] $Serial, [string] $Id, [string] $Text, [string] $TextMatch, [string] $Desc, [int] $WaitSec = 10, [switch] $AllWindows)
    $deadline = (Get-Date).AddSeconds($WaitSec)
    do {
        $dump = Get-UiDump -Serial $Serial -AllWindows:$AllWindows
        $n = Find-Node -Dump $dump -Id $Id -Text $Text -TextMatch $TextMatch -Desc $Desc
        if ($n) {
            $c = Get-NodeCenter $n
            [void](Invoke-Sh -Serial $Serial -Command "input tap $($c.X) $($c.Y)")
            return $dump
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "INVALID: node not found on ${Serial}: id='$Id' text='$Text$TextMatch' desc='$Desc'"
}

function Wait-ForNode {
    param([string] $Serial, [string] $Id, [string] $Text, [string] $TextMatch, [int] $WaitSec = 10)
    $deadline = (Get-Date).AddSeconds($WaitSec)
    do {
        $dump = Get-UiDump -Serial $Serial
        $n = Find-Node -Dump $dump -Id $Id -Text $Text -TextMatch $TextMatch
        if ($n) { return $n }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    $null
}

function Get-TopActivity([string] $Serial) {
    $o = Invoke-Sh -Serial $Serial -Command 'dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity|ResumedActivity:" | head -n 1' -AllowFail
    if ($o -match '\{[^}]*\s(\S+/\S+)\s') { return $Matches[1] }
    $o.Trim()
}

function Get-DensityDpi([string] $Serial) {
    $o = Invoke-Sh -Serial $Serial -Command 'wm density'
    $m = [regex]::Matches($o, '(\d+)')
    [int]$m[$m.Count - 1].Value
}

# ---------------------------------------------------------------------------------------------------------
# Statistics: percentiles, mean CI, Theil-Sen with bootstrap CI (compiled once - O(n^2) per resample)
# ---------------------------------------------------------------------------------------------------------

if (-not ('S0013.Stats' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
namespace S0013 {
public static class Stats {
    public static double Median(List<double> v) {
        if (v.Count == 0) return double.NaN;
        v.Sort();
        int n = v.Count;
        return n % 2 == 1 ? v[n / 2] : (v[n / 2 - 1] + v[n / 2]) / 2.0;
    }
    public static double TheilSen(double[] x, double[] y, int[] idx) {
        var s = new List<double>(idx.Length * idx.Length / 2);
        for (int i = 0; i < idx.Length; i++)
            for (int j = i + 1; j < idx.Length; j++) {
                double dx = x[idx[j]] - x[idx[i]];
                if (Math.Abs(dx) > 1e-12) s.Add((y[idx[j]] - y[idx[i]]) / dx);
            }
        return Median(s);
    }
    // Returns [slope, ciLow, ciHigh] with a percentile bootstrap over resampled points.
    public static double[] TheilSenCi(double[] x, double[] y, int boot, int seed) {
        int n = x.Length;
        var all = new int[n];
        for (int i = 0; i < n; i++) all[i] = i;
        double slope = TheilSen(x, y, all);
        var rnd = new Random(seed);
        var bs = new List<double>(boot);
        var idx = new int[n];
        for (int b = 0; b < boot; b++) {
            for (int i = 0; i < n; i++) idx[i] = rnd.Next(n);
            Array.Sort(idx);
            double v = TheilSen(x, y, idx);
            if (!double.IsNaN(v)) bs.Add(v);
        }
        bs.Sort();
        if (bs.Count == 0) return new double[] { slope, double.NaN, double.NaN };
        double lo = bs[(int)Math.Floor(0.025 * (bs.Count - 1))];
        double hi = bs[(int)Math.Ceiling(0.975 * (bs.Count - 1))];
        return new double[] { slope, lo, hi };
    }
}
}
'@
}

function Get-Percentile([double[]] $Values, [double] $P) {
    if (-not $Values -or $Values.Count -eq 0) { return [double]::NaN }
    $s = $Values | Sort-Object
    $rank = [math]::Ceiling($P / 100 * $s.Count) - 1
    if ($rank -lt 0) { $rank = 0 }
    [double]$s[[int]$rank]
}

# Mean with a normal-approximation 95% CI (t is close enough to 1.96 above n = 30; below it 2.0-2.6 is used).
function Get-MeanCi([double[]] $Values) {
    $n = $Values.Count
    if ($n -eq 0) { return [ordered]@{ n = 0; mean = $null; lo = $null; hi = $null; sd = $null } }
    $mean = ($Values | Measure-Object -Average).Average
    if ($n -lt 2) { return [ordered]@{ n = $n; mean = $mean; lo = $mean; hi = $mean; sd = 0 } }
    $var = ($Values | ForEach-Object { ($_ - $mean) * ($_ - $mean) } | Measure-Object -Sum).Sum / ($n - 1)
    $sd = [math]::Sqrt($var)
    $t = if ($n -ge 30) { 1.96 } elseif ($n -ge 10) { 2.26 } else { 2.78 }
    $half = $t * $sd / [math]::Sqrt($n)
    [ordered]@{ n = $n; mean = $mean; lo = $mean - $half; hi = $mean + $half; sd = $sd }
}

# Slope of y over hours, as % of the series median per hour, with a bootstrap CI.
function Get-SlopePctPerHour {
    param([double[]] $TimesMs, [double[]] $Values, [int] $Boot = 400)
    if ($Values.Count -lt 8) { return $null }
    $t0 = $TimesMs[0]
    $x = [double[]]($TimesMs | ForEach-Object { ($_ - $t0) / 3600000.0 })
    $y = [double[]]$Values
    $r = [S0013.Stats]::TheilSenCi($x, $y, $Boot, 13)
    $median = [S0013.Stats]::Median([Collections.Generic.List[double]]::new($y))
    if ($median -eq 0) { $median = 1 }
    [ordered]@{
        n = $Values.Count
        hours = [math]::Round($x[$x.Count - 1], 4)
        median = $median
        slopePerHour = $r[0]
        pctPerHour = 100 * $r[0] / $median
        ciLowPct = 100 * $r[1] / $median
        ciHighPct = 100 * $r[2] / $median
    }
}

# ---------------------------------------------------------------------------------------------------------
# Profile recipes (ticket section 6) - committed beside this file, never an image
# ---------------------------------------------------------------------------------------------------------

function Get-Recipes { Read-JsonFile (Join-Path $script:StandRoot 'profile.recipe.json') }

function Get-RecipeHash([string] $Name) {
    $r = (Get-Recipes)[$Name]
    if (-not $r) { throw "INVALID: unknown profile recipe '$Name'" }
    $json = $r | ConvertTo-Json -Depth 6 -Compress
    $sha = [Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($json))
    ([Convert]::ToHexString($sha)).Substring(0, 12).ToLowerInvariant()
}

function Get-ActiveProfiles {
    $f = Join-Path $script:TempRoot 'profiles.json'
    $p = Read-JsonFile $f
    if (-not $p) { $p = @{} }
    $p
}

function Get-ProfileSerial([string] $Recipe) {
    $p = (Get-ActiveProfiles)[$Recipe]
    if (-not $p) { throw "INVALID: no live profile for recipe '$Recipe' (run profile.ps1 -Action Create)" }
    $p.serial
}
