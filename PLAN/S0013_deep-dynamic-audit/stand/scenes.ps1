#requires -Version 7.0
# Scene drivers of the S0013 matrix (section 3.2) and the verbs they are written in. Dot-sourced by
# run-matrix.ps1 (M10), storm.ps1 (M6), fault-inject.ps1 (M5) and locale-a11y.ps1 (M7).
#
# A driver acts on the device only through adb input, am, cmd, settings and the emulator console; it
# records marks, feature spans, starts (M1), latencies (M2), torch releases, faults (M5) and checkpoints
# (M7) into $ctx. Judging happens afterwards, in _judges.ps1.

$script:PhoneMain = 'com.sza.tourist/.MainActivity'
$script:WatchMain = 'com.sza.tourist/com.sza.tourist.wear.MainActivity'
$script:WatchFace = 'com.sza.tourist.watchface'
$script:BaseFix = @(30.72331, 46.48251)   # lon, lat - synthetic, still treated as sensitive (section 11)

# ---------------------------------------------------------------------------------------------------------
# Verbs
# ---------------------------------------------------------------------------------------------------------

function Sx($ctx, [string] $Command, [switch] $AllowFail, [int] $TimeoutSec = 60) { Invoke-Sh -Serial $ctx.Serial -Command $Command -AllowFail:$AllowFail -TimeoutSec $TimeoutSec }
function Emu($ctx, [string] $Command) { (Invoke-Adb -Serial $ctx.Serial -Arguments (@('emu') + ($Command -split ' ')) -AllowFail -TimeoutSec 20).Out }
function Pause-Sec([double] $Seconds) { Start-Sleep -Milliseconds ([int]($Seconds * 1000)) }

function Mark($ctx, [string] $Name) {
    Add-Mark $ctx.Serial "$($ctx.SceneId) $Name"
    $ctx.Marks.Add([ordered]@{ ts = Get-NowMs; name = $Name })
}

function Begin-Feature($ctx, [string] $Feature) {
    Mark $ctx "begin:$Feature"
    $ctx.Spans.Add([ordered]@{ feature = $Feature; start = Get-NowMs; exit = $null })
}

function End-Feature($ctx, [string] $Feature) {
    $span = $ctx.Spans | Where-Object { $_.feature -eq $Feature -and -not $_.exit } | Select-Object -Last 1
    if ($span) { $span.exit = Get-NowMs }
    Mark $ctx "end:$Feature"
}

# A resource the feature must hold for its whole span (the signatures: SOS keeps its service through
# screen-off and Doze, the water flashlight keeps the screen awake and the torch lit).
function Expect-Hold($ctx, [string] $Feature, [string] $Column, [double] $Min, [string] $What, [string] $Severity = 'P0', [int] $SettleMs = 3000) {
    $ctx.Holds.Add([ordered]@{ feature = $Feature; column = $Column; min = $Min; what = $What; severity = $Severity; settleMs = $SettleMs; from = $null; to = $null })
}

function Note($ctx, [string] $Text) { $ctx.Notes.Add($Text) }

function Wake-Device($ctx) {
    [void](Sx $ctx 'input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard' -AllowFail)
    Pause-Sec 0.6
}

function Go-Home($ctx) {
    Wake-Device $ctx
    $main = if ($ctx.Role -eq 'watch') { $script:WatchMain } else { $script:PhoneMain }
    [void](Sx $ctx "am start -W -f 0x10008000 -n $main")
    Pause-Sec 1.2
}

function Open-Watch($ctx, [string] $Destination) {
    $ctx.Marks.Add([ordered]@{ ts = Get-NowMs; name = "open:$Destination" })
    Wake-Device $ctx
    [void](Sx $ctx "am start -W -f 0x10008000 -n $script:WatchMain --es com.sza.tourist.wear.launch.mode destination --es com.sza.tourist.wear.launch.destination $Destination")
    Pause-Sec 2
}

function Tap($ctx, [string] $Id, [string] $Text, [string] $TextMatch, [string] $Desc, [switch] $Measured, [int] $WaitSec = 10, [switch] $AllWindows) {
    $deadline = (Get-Date).AddSeconds($WaitSec)
    do {
        $dump = Get-UiDump -Serial $ctx.Serial -AllWindows:$AllWindows
        $n = Find-Node -Dump $dump -Id $Id -Text $Text -TextMatch $TextMatch -Desc $Desc
        if ($n) { break }
        Pause-Sec 0.5
    } while ((Get-Date) -lt $deadline)
    if (-not $n) { throw "INVALID: $($ctx.SceneId): control not found (id='$Id' text='$Text$TextMatch' desc='$Desc')" }
    $c = Get-NodeCenter $n
    if ($Measured) {
        $r = Measure-InputLatency -Serial $ctx.Serial -InputCmd "input tap $($c.X) $($c.Y)"
        $r['label'] = "tap $Id$Text$Desc"
        $ctx.Latencies.Add($r)
    } else {
        [void](Sx $ctx "input tap $($c.X) $($c.Y)")
    }
    $c
}

function Tap-At($ctx, [int] $X, [int] $Y, [switch] $Measured, [string] $Label = 'tap') {
    if ($Measured) { $r = Measure-InputLatency -Serial $ctx.Serial -InputCmd "input tap $X $Y"; $r['label'] = $Label; $ctx.Latencies.Add($r) }
    else { [void](Sx $ctx "input tap $X $Y") }
}

function Key($ctx, [string] $Code, [switch] $Measured, [string] $Source = '') {
    $cmd = if ($Source) { "input $Source keyevent $Code" } else { "input keyevent $Code" }
    if ($Measured) { $r = Measure-InputLatency -Serial $ctx.Serial -InputCmd $cmd; $r['label'] = "key $Code"; $ctx.Latencies.Add($r) }
    else { [void](Sx $ctx $cmd) }
}

# The watch stopwatch starts when it opens and keeps running (with its ongoing notification) after the
# screen is left - by design. A scene that opened it stops it, so its notification is not read as a leak.
function Stop-WatchStopwatch($ctx) {
    Open-Watch $ctx STOPWATCH
    if (Wait-ForNode -Serial $ctx.Serial -Text Stop -WaitSec 3) { [void](Tap $ctx -Text Stop); Pause-Sec 1 }
    Key $ctx KEYCODE_HOME
}

function Rotary($ctx, [int] $Steps, [switch] $Measured) {
    $cmd = "input rotaryencoder scroll --axis SCROLL,$Steps"
    if ($Measured) { $r = Measure-InputLatency -Serial $ctx.Serial -InputCmd $cmd; $r['label'] = "rotary $Steps"; $ctx.Latencies.Add($r) }
    else { [void](Sx $ctx $cmd -AllowFail) }
}

function Start-Timed($ctx, [ValidateSet('cold', 'warm', 'hot')] [string] $Kind, [string] $Label = 'main', [string] $Extras = '') {
    $comp = if ($ctx.Role -eq 'watch') { $script:WatchMain } else { $script:PhoneMain }
    $r = Measure-Start -Serial $ctx.Serial -Component $comp -Kind $Kind -Extras $Extras
    $r['label'] = $Label
    $ctx.Starts.Add($r)
    $r
}

function Open-Phone($ctx, [ValidateSet('home', 'dashboard', 'lights', 'water', 'front', 'sos', 'stopwatch')] [string] $Screen, [switch] $Measured) {
    if ($Screen -ne 'home') { $ctx.Marks.Add([ordered]@{ ts = Get-NowMs; name = "open:$Screen" }) }
    Go-Home $ctx
    switch ($Screen) {
        'home' { }
        'dashboard' { [void](Tap $ctx -Id row_dashboard -Measured:$Measured); Pause-Sec 2.5 }
        'lights' { [void](Tap $ctx -Id row_lights -Measured:$Measured); Pause-Sec 1.2 }
        'water' { [void](Tap $ctx -Id row_lights); Pause-Sec 1.2; [void](Tap $ctx -Id row_water_flashlight -Measured:$Measured); Pause-Sec 1.5 }
        'front' { [void](Tap $ctx -Id row_lights); Pause-Sec 1.2; [void](Tap $ctx -Id row_front_flashlight -Measured:$Measured); Pause-Sec 1.5 }
        'sos' { [void](Tap $ctx -Id row_sos -Measured:$Measured); Pause-Sec 2 }
        'stopwatch' { [void](Tap $ctx -Id row_stopwatch -Measured:$Measured); Pause-Sec 1.5 }
    }
}

function Get-Resumed($ctx) { Get-TopActivity $ctx.Serial }

# The newest torch event per camera decides; 'on' if any camera's newest event is an on of a live client
# (Measure-TorchLit, S0031 item 3).
function Get-TorchOn($ctx) {
    $o = Sx $ctx $script:TorchProbeCmd -AllowFail
    (Measure-TorchLit $o) -gt 0
}

# Resolves a control's tap command before any clock starts.
function Get-TapCmd($ctx, [string] $Id, [string] $Text, [switch] $AllWindows) {
    $n = Find-Node -Dump (Get-UiDump -Serial $ctx.Serial -AllWindows:$AllWindows) -Id $Id -Text $Text
    if (-not $n) { throw "INVALID: $($ctx.SceneId): control '$Id$Text' not found" }
    $c = Get-NodeCenter $n
    "input tap $($c.X) $($c.Y)"
}

# 8.3d: inject the off input (already resolved to one shell command, with the M2 mark in the same
# round-trip), then poll the camera service until the torch reports off. The clock starts at injection.
function Measure-TorchRelease($ctx, [string] $Label, [string] $InputCmd, [switch] $Measured, [int] $TimeoutMs = 3000) {
    $t0 = Get-Date
    $mark = Send-MarkedInput $ctx.Serial $InputCmd
    $issued = ((Get-Date) - $t0).TotalMilliseconds
    $polls = 0; $release = $null; $pollMs = 0
    $start = Get-Date
    while (((Get-Date) - $start).TotalMilliseconds -lt $TimeoutMs) {
        $p0 = Get-Date
        $on = Get-TorchOn $ctx
        $polls++
        $pollMs = [math]::Max($pollMs, ((Get-Date) - $p0).TotalMilliseconds)
        if (-not $on) { $release = [math]::Round(((Get-Date) - $t0).TotalMilliseconds); break }
    }
    $ctx.Torch.Add([ordered]@{ label = $Label; releaseMs = $release; pollMs = [math]::Round($pollMs); issuedMs = [math]::Round($issued); polls = $polls; timeoutMs = $TimeoutMs })
    if ($Measured) {
        Pause-Sec 0.6
        $r = Resolve-InputLatency -Serial $ctx.Serial -Mark $mark
        $r['label'] = $Label
        $ctx.Latencies.Add($r)
    }
}

# App-visible state: the preference files plus the system resources a toggle can leave behind.
function Get-StateHash($ctx, [switch] $Detail) {
    $files = Invoke-RunAs -Serial $ctx.Serial -Command 'find shared_prefs files/datastore databases -type f -exec sha256sum {} + 2>/dev/null | sort' -AllowFail
    $s = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
    $sys = "torch=$($s.torchOn) fgs=$($s.fgServices) notifs=$($s.notifs) sensors=$($s.sensors) loc=$($s.locRegs) wl=$($s.wlOwn)"
    $text = "$($files.Trim())`n$sys"
    $hash = ([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($text)))).Substring(0, 16)
    if ($Detail) { return [ordered]@{ hash = $hash; text = $text } }
    $hash
}

# Process death of a backgrounded app: `am kill` first; a perceptible process it refuses is killed the way
# the low-memory killer does it, by SIGKILL.
function Invoke-Reclaim($ctx) {
    $before = Get-AppPid $ctx.Serial
    [void](Sx $ctx "am kill $script:Package"); Pause-Sec 2
    if ($before -and (Get-AppPid $ctx.Serial) -eq $before) {
        Note $ctx 'am kill refused (the process is perceptible); reclaimed by SIGKILL'
        [void](Invoke-RunAs -Serial $ctx.Serial -Command "kill -9 $before" -AllowFail); Pause-Sec 2
    }
}

function Test-Toggle($ctx, [string] $Name, [scriptblock] $On, [scriptblock] $Off, [double] $HoldSec = 2) {
    $before = Get-StateHash $ctx -Detail
    & $On
    Pause-Sec $HoldSec
    & $Off
    Pause-Sec 1.5
    $after = Get-StateHash $ctx -Detail
    $diff = ''
    if ($before.hash -ne $after.hash) {
        $a = $before.text -split "`n"; $b = $after.text -split "`n"
        $diff = (@(Compare-Object $a $b | ForEach-Object { "$($_.SideIndicator)$($_.InputObject -replace '^[0-9a-f]{64}\s+', '')" }) -join '; ')
    }
    $ctx.Toggles.Add([ordered]@{ name = $Name; before = $before.hash; after = $after.hash; diff = $diff })
}

# State shown by colour alone (handoff section 3): the toggled control's accessibility attributes must
# change between off and on.
function Test-ToggleExposesState($ctx, [string] $Id, [scriptblock] $Toggle) {
    $a = Find-Node -Dump (Get-UiDump -Serial $ctx.Serial) -Id $Id
    & $Toggle
    Pause-Sec 1.2
    $b = Find-Node -Dump (Get-UiDump -Serial $ctx.Serial) -Id $Id
    if (-not $a -or -not $b) { return }
    $sig = { param($n) $kids = @($n.SelectNodes('.//node') | ForEach-Object { "$($_.text)|$($_.'content-desc')|$($_.checked)|$($_.selected)|$($_.'state-description')" }); "$($n.text)|$($n.'content-desc')|$($n.checked)|$($n.selected)|$($n.'state-description')|$($kids -join ';')" }
    if ((& $sig $a) -eq (& $sig $b)) { $ctx.Stateless.Add($Id) }
}

function Checkpoint($ctx, [string] $Label, [switch] $AllWindows) {
    $path = Join-Path $ctx.SceneDir ("ui-{0:00}-{1}.xml" -f ($ctx.Checkpoints.Count + 1), ($Label -replace '[^\w-]', '_'))
    try { [void](Get-UiDump -Serial $ctx.Serial -SaveAs $path -AllWindows:$AllWindows) } catch { Note $ctx "checkpoint ${Label}: $($_.Exception.Message)"; return }
    $ctx.Checkpoints.Add([ordered]@{ label = $Label; path = $path })
    if ($ctx.WithM7) { $ctx.M7.Add((Invoke-M7Sweep $ctx $Label -AllWindows:$AllWindows)) }
}

function Set-AppLocale($ctx, [string] $Locale) {
    $arg = if ($Locale) { "--locales $Locale" } else { "--locales ''" }
    [void](Sx $ctx "cmd locale set-app-locales $script:Package $arg" -AllowFail)
}

function Set-FontScale($ctx, [double] $Scale) {
    [void](Sx $ctx ("settings put system font_scale {0}" -f $Scale.ToString([Globalization.CultureInfo]::InvariantCulture)))
}

# M7 sweep of whatever is on screen: EN/RU/UK x font 1.0/1.3/2.0. A per-app locale or font change
# recreates the activity, so the state on screen must survive a configuration change - the sweep reads
# the screen as a user who changes these settings would see it.
function Invoke-M7Sweep($ctx, [string] $Label, [switch] $AllWindows) {
    $res = [ordered]@{ label = $Label; invalid = $null; fallback = @(); small = @(); unlabelled = @(); stateless = @(); clipped2 = @(); unlabelled2 = @(); dumps = 0 }
    $minDp = if ($ctx.Role -eq 'watch') { 40 } else { 48 }
    $texts = @{}
    Set-ProbePersistent $ctx.Serial $true
    try {
        foreach ($loc in 'en-US', 'ru-RU', 'uk-UA') {
            Set-AppLocale $ctx $loc
            Pause-Sec 2.5
            foreach ($scale in 1.0, 1.3, 2.0) {
                Set-FontScale $ctx $scale
                Pause-Sec 2
                $p = Join-Path $ctx.SceneDir ("m7-{0}-{1}-{2}.xml" -f ($Label -replace '[^\w-]', '_'), $loc, $scale)
                $d = Get-UiDump -Serial $ctx.Serial -SaveAs $p -AllWindows:$AllWindows
                $res.dumps++
                $f = Get-A11yFacts -Dump $d -DensityDpi $ctx.Density -MinDp $minDp -FontScale $scale
                if ($scale -eq 1.0) {
                    $base = $d
                    $texts[$loc] = $f.texts
                    $res.small += $f.small
                    $res.unlabelled += $f.unlabelled
                }
                if ($scale -eq 2.0) { $res.clipped2 += $f.clipped + @(Get-DroppedLabels -Base $base -Large $d); $res.unlabelled2 += $f.unlabelled }
            }
        }
        if (-not $texts['en-US'] -or $texts['en-US'].Count -eq 0) { $res.invalid = 'no app text in the EN dump (screen not in front?)' }
        $res.fallback = @((Get-FallbackStrings $texts['en-US'] $texts['ru-RU']) | ForEach-Object { "ru: $_" }) + @((Get-FallbackStrings $texts['en-US'] $texts['uk-UA']) | ForEach-Object { "uk: $_" })
        $res.stateless = @($ctx.Stateless)
    } catch {
        $res.invalid = $_.Exception.Message -replace '^INVALID:\s*', ''
    } finally {
        Set-FontScale $ctx 1.0
        Set-AppLocale $ctx ''
        Set-ProbePersistent $ctx.Serial $false
        Pause-Sec 2
    }
    $res
}

# M5 record: what the fault produced, judged against the known-outcome set (decision 8.7).
function Add-Fault($ctx, [string] $Fault, [string] $Observed, [string] $Detail = '', $Consistent = $null, [string] $Invalid = $null, [string] $Severity = 'P1') {
    $set = Get-OutcomeSet
    $allowed = @($set.outcomes[$Fault])
    $ctx.Faults.Add([ordered]@{ fault = $Fault; observed = $Observed; inSet = ($Observed -in $allowed); allowed = $allowed; consistent = $Consistent; invalid = $Invalid; detail = $Detail; severity = $Severity })
}

function Get-OutcomeSet { Read-JsonFile (Join-Path $script:StandRoot 'outcomes.json') }

# Writes one string preference into the app's own SharedPreferences file while the app is stopped - the
# stored state a setting screen would write; read back, edited on the host, pushed, copied in by run-as.
function Set-PrefString($ctx, [string] $File, [string] $Key, [string] $Value) {
    $path = "shared_prefs/$File.xml"
    $xml = Invoke-RunAs -Serial $ctx.Serial -Command "cat $path 2>/dev/null" -AllowFail
    if ($xml -notmatch '<map') { $xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>`n<map>`n</map>`n" }
    $entry = "    <string name=`"$Key`">$Value</string>"
    if ($xml -match "<string name=`"$([regex]::Escape($Key))`">[^<]*</string>") { $xml = $xml -replace "\s*<string name=`"$([regex]::Escape($Key))`">[^<]*</string>", "`n$entry" }
    else { $xml = $xml -replace '<map>', "<map>`n$entry" }
    $tmp = Join-Path $ctx.SceneDir "$File.xml"
    [IO.File]::WriteAllText($tmp, ($xml -replace "`r", ''))
    [void](Invoke-Adb -Serial $ctx.Serial -Arguments @('push', $tmp, "/data/local/tmp/s0013-$File.xml"))
    [void](Sx $ctx "chmod 644 /data/local/tmp/s0013-$File.xml")
    [void](Invoke-RunAs -Serial $ctx.Serial -Command "mkdir -p shared_prefs && cp /data/local/tmp/s0013-$File.xml $path")
    [void](Sx $ctx "rm -f /data/local/tmp/s0013-$File.xml" -AllowFail)
}

function Get-PrefsKeySet($ctx) {
    $o = Invoke-RunAs -Serial $ctx.Serial -Command 'for f in shared_prefs/*.xml; do [ -f "$f" ] && sed -n "s/.*name=\"\([^\"]*\)\".*/$(basename $f .xml):\1/p" "$f"; done' -AllowFail
    @($o -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -match ':' } | Sort-Object -Unique)
}

function Get-VisibleTexts($ctx, [switch] $AllWindows) {
    $d = Get-UiDump -Serial $ctx.Serial -AllWindows:$AllWindows
    @($d.SelectNodes('//node') | ForEach-Object { "$($_.text) $($_.'content-desc')".Trim() } | Where-Object { $_ })
}

# A stopwatch reading as the watch draws it - `M:SS.hh` below an hour, `H:MM:SS.hh` from an hour up
# (WearStopwatchTimeFormatter) - in milliseconds; $null when the text holds no such reading. A clock (`10:32`) has no
# hundredths and does not match.
function ConvertFrom-StopwatchText([string] $Text) {
    if ($Text -match '(?<!\d)(?:(\d+):)?(\d+):(\d{2})\.(\d{2})(?!\d)') {
        $h = if ($Matches[1]) { [long]$Matches[1] } else { 0L }
        return ((($h * 60 + [long]$Matches[2]) * 60 + [long]$Matches[3]) * 1000 + [long]$Matches[4] * 10)
    }
    $null
}

# The first stopwatch reading on the watch screen, polled for -WaitSec; $null when none appeared.
function Get-StopwatchMs($ctx, [int] $WaitSec = 4) {
    $deadline = (Get-Date).AddSeconds($WaitSec)
    do {
        foreach ($t in (Get-VisibleTexts $ctx)) {
            $ms = ConvertFrom-StopwatchText $t
            if ($null -ne $ms) { return $ms }
        }
        Pause-Sec 0.5
    } while ((Get-Date) -lt $deadline)
    $null
}

# F8-watch (decision 8.7). The stopwatch is a stored time origin: a hard kill leaves the measurement stored and its
# ongoing notification in the shade, and the next open shows the measurement still running. $Before/$After are the
# readings (ms) taken just before the kill and after the next open; $Notifs is the app's notification count read
# between the two. $null when either reading is missing.
function Get-StopwatchKillOutcome($Before, $After, [int] $Notifs) {
    if ($null -eq $Before -or $null -eq $After) { return $null }
    if ($After -ge $Before) { return 'stopwatch-resumed' }
    if ($Notifs -gt 0) { return 'stale-notification-left' }
    'ended-cleanly'
}

function Wait-TopActivity($ctx, [string] $Pattern, [int] $Sec = 8) {
    $deadline = (Get-Date).AddSeconds($Sec)
    do { $top = Get-Resumed $ctx; if ($top -match $Pattern) { return $true }; Pause-Sec 0.4 } while ((Get-Date) -lt $deadline)
    $false
}

function Restart-Sampler($ctx) {
    if ($ctx.Sampler) { New-Item -ItemType File -Path $ctx.StopFile -Force | Out-Null; [void](Wait-Job $ctx.Sampler -Timeout 30); Remove-Item $ctx.StopFile -Force -ErrorAction SilentlyContinue }
    $ctx.Sampler = Start-Sampler -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid -CsvPath $ctx.Csv -IntervalSec $ctx.IntervalSec -StopFile $ctx.StopFile
}

function Stop-SamplerFor($ctx) {
    if ($ctx.Sampler) { New-Item -ItemType File -Path $ctx.StopFile -Force | Out-Null; [void](Wait-Job $ctx.Sampler -Timeout 40); Remove-Item $ctx.StopFile -Force -ErrorAction SilentlyContinue; $ctx.Sampler = $null }
}

function Invoke-Reboot($ctx) {
    Stop-SamplerFor $ctx
    Save-Logcat -Serial $ctx.Serial -Path (Join-Path $ctx.SceneDir 'logcat-before-reboot.txt')
    [void](Invoke-Adb -Serial $ctx.Serial -Arguments @('reboot') -AllowFail -TimeoutSec 30)
    Pause-Sec 10
    [void](Invoke-Adb -Serial $ctx.Serial -Arguments @('wait-for-device') -TimeoutSec 300 -AllowFail)
    $deadline = (Get-Date).AddSeconds(300)
    do { Pause-Sec 3; $b = (Invoke-Adb -Serial $ctx.Serial -Arguments @('shell', 'getprop sys.boot_completed') -AllowFail -TimeoutSec 15).Out.Trim() } while ($b -ne '1' -and (Get-Date) -lt $deadline)
    if ($b -ne '1') { throw "INVALID: $($ctx.SceneId): device did not come back from reboot" }
    $ctx.BootCompletedAt = Get-NowMs
    Wake-Device $ctx
    Restart-Sampler $ctx
}

function Send-SensorStorm($ctx, [int] $Seconds, [string[]] $Kinds = @('magnetic-field')) {
    $end = (Get-Date).AddSeconds($Seconds); $i = 0
    while ((Get-Date) -lt $end) {
        $a = [math]::Sin($i / 5.0) * 40; $c = [math]::Cos($i / 5.0) * 40
        foreach ($k in $Kinds) {
            switch ($k) {
                'magnetic-field' { [void](Emu $ctx ("sensor set magnetic-field {0:0.0}:{1:0.0}:-30" -f $a, $c)) }
                'acceleration' { [void](Emu $ctx ("sensor set acceleration {0:0.00}:{1:0.00}:9.81" -f ($a / 20), ($c / 20))) }
                'pressure' { [void](Emu $ctx ("sensor set pressure {0:0.0}" -f (1013 + $a / 4))) }
                'geo' { [void](Emu $ctx ("geo fix {0:0.000000} {1:0.000000} {2:0}" -f ($script:BaseFix[0] + $i * 0.00002), ($script:BaseFix[1] + $i * 0.00001), (50 + $i % 30))) }
            }
        }
        $i++
    }
    $i
}

# ---------------------------------------------------------------------------------------------------------
# Drivers. Key: scene id -> device role -> scriptblock($ctx). A missing role makes that scene's cells on
# that device COULD NOT VERIFY with "no driver" - never a silent pass.
# ---------------------------------------------------------------------------------------------------------

$script:Drivers = [ordered]@{}

# --- A. Lifecycle ----------------------------------------------------------------------------------------

$script:Drivers['A1'] = @{
    phone = { param($ctx)
        foreach ($i in 1..5) {
            Begin-Feature $ctx "app$i"
            [void](Start-Timed $ctx cold "cold#$i")
            Pause-Sec 1.5
            [void](Tap $ctx -Id row_dashboard -Measured); Pause-Sec 2
            Key $ctx KEYCODE_BACK -Measured; Pause-Sec 1
            Key $ctx KEYCODE_HOME; End-Feature $ctx "app$i"; Pause-Sec 2
        }
        Go-Home $ctx; Checkpoint $ctx 'home'
        Key $ctx KEYCODE_HOME
    }
    watch = { param($ctx)
        foreach ($i in 1..5) {
            Begin-Feature $ctx "app$i"
            [void](Start-Timed $ctx cold "cold#$i")
            Pause-Sec 2
            Rotary $ctx 2 -Measured; Pause-Sec 1
            Key $ctx KEYCODE_HOME; End-Feature $ctx "app$i"; Pause-Sec 2
        }
        Go-Home $ctx; Checkpoint $ctx 'home'
        Key $ctx KEYCODE_HOME
    }
}

$script:Drivers['A2'] = @{
    phone = { param($ctx)
        [void](Start-Timed $ctx cold 'prime')
        $ctx.Starts.RemoveAt($ctx.Starts.Count - 1)
        foreach ($i in 1..5) { [void](Start-Timed $ctx warm "warm#$i"); Pause-Sec 1; [void](Tap $ctx -Id row_lights -Measured); Pause-Sec 1; Key $ctx KEYCODE_BACK; Pause-Sec 1 }
        foreach ($i in 1..5) { [void](Start-Timed $ctx hot "hot#$i"); Pause-Sec 1; [void](Tap $ctx -Id row_stopwatch -Measured); Pause-Sec 1.5; Key $ctx KEYCODE_BACK; Pause-Sec 1 }
        Checkpoint $ctx 'home-after-returns'
        Key $ctx KEYCODE_HOME
    }
    watch = { param($ctx)
        [void](Start-Timed $ctx cold 'prime')
        $ctx.Starts.RemoveAt($ctx.Starts.Count - 1)
        foreach ($i in 1..5) { [void](Start-Timed $ctx warm "warm#$i"); Pause-Sec 1.5; Rotary $ctx 2 -Measured; Pause-Sec 1 }
        foreach ($i in 1..5) { [void](Start-Timed $ctx hot "hot#$i"); Pause-Sec 1.5; Rotary $ctx 2 -Measured; Pause-Sec 1 }
        Checkpoint $ctx 'home-after-returns'
        Key $ctx KEYCODE_HOME
    }
}

$script:Drivers['A3'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        Pause-Sec 5
        Checkpoint $ctx 'dashboard'
        Key $ctx KEYCODE_BACK; Pause-Sec 1
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard
        Pause-Sec 8
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard-recents
        Pause-Sec 4
        Key $ctx KEYCODE_APP_SWITCH; Pause-Sec 2
        [void](Sx $ctx 'input swipe 540 1300 540 150 250')
        End-Feature $ctx dashboard-recents
        Pause-Sec 2
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
    watch = { param($ctx)
        Open-Watch $ctx TOURIST
        Begin-Feature $ctx dashboard
        Pause-Sec 5
        Checkpoint $ctx 'dashboard'
        Key $ctx KEYCODE_BACK; Pause-Sec 1
        Key $ctx KEYCODE_BACK; Pause-Sec 1
        Key $ctx KEYCODE_HOME
        End-Feature $ctx dashboard
        Pause-Sec 10
    }
}

$script:Drivers['A4'] = @{
    phone = { param($ctx)
        Open-Phone $ctx stopwatch
        Begin-Feature $ctx stopwatch
        Pause-Sec 3
        $hashBefore = Get-StateHash $ctx
        Key $ctx KEYCODE_HOME; Pause-Sec 2
        $pidBefore = Get-AppPid $ctx.Serial
        [void](Sx $ctx "am kill $script:Package")
        Pause-Sec 2
        $pidAfter = Get-AppPid $ctx.Serial
        if ($pidAfter -and $pidAfter -eq $pidBefore) { Note $ctx 'am kill refused (the process is perceptible); reclaimed the way the low-memory killer does, by SIGKILL'; [void](Invoke-RunAs -Serial $ctx.Serial -Command "kill -9 $pidBefore" -AllowFail); Pause-Sec 2 }
        $r = Measure-Start -Serial $ctx.Serial -Component $script:PhoneMain -Kind hot
        $r['label'] = 'restore after process death'; $ctx.Starts.Add($r)
        Pause-Sec 2
        $top = Get-Resumed $ctx
        $restored = $top -match 'StopwatchActivity'
        $texts = Get-VisibleTexts $ctx
        $running = ($texts -match '^(Lap|Stop)$').Count -ge 1
        $observed = if ($restored -and $running) { 'restored-running' } elseif ($restored) { 'restored-stopped' } else { 'restarted-home' }
        Add-Fault $ctx 'A4' $observed -Detail "top=$top" -Consistent ($observed -ne 'restored-stopped')
        Checkpoint $ctx 'after-restore'
        Key $ctx KEYCODE_BACK
        End-Feature $ctx stopwatch
        Pause-Sec 6
    }
    watch = { param($ctx)
        Open-Watch $ctx STOPWATCH
        Begin-Feature $ctx stopwatch
        Pause-Sec 3
        Key $ctx KEYCODE_HOME; Pause-Sec 2
        $pidBefore = Get-AppPid $ctx.Serial
        [void](Sx $ctx "am kill $script:Package")
        Pause-Sec 2
        if ((Get-AppPid $ctx.Serial) -eq $pidBefore -and $pidBefore) { Note $ctx 'am kill refused (the process is perceptible); reclaimed by SIGKILL'; [void](Invoke-RunAs -Serial $ctx.Serial -Command "kill -9 $pidBefore" -AllowFail); Pause-Sec 2 }
        $r = Measure-Start -Serial $ctx.Serial -Component $script:WatchMain -Kind hot
        $r['label'] = 'restore after process death'; $ctx.Starts.Add($r)
        Pause-Sec 2
        $texts = Get-VisibleTexts $ctx
        $observed = if (($texts -join ' ') -match '\d+:\d\d') { 'restored-running' } else { 'restarted-home' }
        Add-Fault $ctx 'A4' $observed -Detail ($texts -join ' / ') -Consistent $true
        Checkpoint $ctx 'after-restore'
        if (Wait-ForNode -Serial $ctx.Serial -Text Stop -WaitSec 2) { [void](Tap $ctx -Text Stop) }
        Key $ctx KEYCODE_HOME
        End-Feature $ctx stopwatch
        Pause-Sec 6
    }
}

# --- B. Power and session ----------------------------------------------------------------------------------

$script:Drivers['B1'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        Pause-Sec 4
        Key $ctx KEYCODE_SLEEP
        Mark $ctx 'screen-off'
        End-Feature $ctx dashboard
        Pause-Sec 14
        $ctx.Marks.Add([ordered]@{ ts = Get-NowMs; name = 'open:wake' })
        Key $ctx KEYCODE_WAKEUP -Measured
        [void](Sx $ctx 'wm dismiss-keyguard' -AllowFail)
        Begin-Feature $ctx dashboard-again
        Pause-Sec 4
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard-again
        # SOS must keep its service and wakelock through screen-off (signature).
        Open-Phone $ctx sos
        Begin-Feature $ctx sos
        Expect-Hold $ctx sos fgServices 1 'SOS foreground service through screen-off'
        Expect-Hold $ctx sos wlOwn 1 'SOS wakelock through screen-off'
        Pause-Sec 3
        Key $ctx KEYCODE_SLEEP; Mark $ctx 'screen-off-sos'
        Pause-Sec 15
        $ctx.Marks.Add([ordered]@{ ts = Get-NowMs; name = 'open:wake' })
        Key $ctx KEYCODE_WAKEUP -Measured
        [void](Sx $ctx 'wm dismiss-keyguard' -AllowFail)
        Pause-Sec 2
        [void](Tap $ctx -Id btnSosStop -Measured)
        End-Feature $ctx sos
        Pause-Sec 6
    }
    watch = { param($ctx)
        Open-Watch $ctx TOURIST
        Begin-Feature $ctx dashboard
        Pause-Sec 4
        Key $ctx KEYCODE_SLEEP; Mark $ctx 'screen-off'
        End-Feature $ctx dashboard
        Pause-Sec 14
        $ctx.Marks.Add([ordered]@{ ts = Get-NowMs; name = 'open:wake' })
        Key $ctx KEYCODE_WAKEUP -Measured
        Pause-Sec 3
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

$script:Drivers['B2'] = @{
    phone = { param($ctx)
        Open-Phone $ctx sos
        Begin-Feature $ctx sos
        Expect-Hold $ctx sos fgServices 1 'SOS foreground service through Doze'
        Expect-Hold $ctx sos wlOwn 1 'SOS wakelock through Doze'
        Pause-Sec 3
        Key $ctx KEYCODE_SLEEP
        [void](Sx $ctx 'dumpsys battery unplug; dumpsys deviceidle force-idle' -AllowFail)
        Mark $ctx 'force-idle'
        Pause-Sec 25
        $idle = Sx $ctx 'dumpsys deviceidle get deep' -AllowFail
        [void](Sx $ctx 'dumpsys deviceidle unforce; dumpsys battery reset' -AllowFail)
        Key $ctx KEYCODE_WAKEUP
        [void](Sx $ctx 'wm dismiss-keyguard' -AllowFail)
        $r = Measure-Start -Serial $ctx.Serial -Component $script:PhoneMain -Kind hot
        $r['label'] = 'resume after Doze'; $ctx.Starts.Add($r)
        $alive = (Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid).fgServices -gt 0
        Add-Fault $ctx 'B2' $(if ($alive) { 'sos-kept-sounding' } else { 'sos-stopped' }) -Detail "deviceidle=$($idle.Trim())" -Consistent $alive -Severity 'P0'
        Open-Phone $ctx sos
        [void](Tap $ctx -Id btnSosStop)
        End-Feature $ctx sos
        # App standby: the dashboard backgrounded into the rare bucket.
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        Pause-Sec 3
        Key $ctx KEYCODE_HOME
        End-Feature $ctx dashboard
        [void](Sx $ctx "am set-inactive $script:Package true; am set-standby-bucket $script:Package rare" -AllowFail)
        Pause-Sec 15
        $r = Measure-Start -Serial $ctx.Serial -Component $script:PhoneMain -Kind hot
        $r['label'] = 'resume from standby'; $ctx.Starts.Add($r)
        [void](Sx $ctx "am set-inactive $script:Package false; am set-standby-bucket $script:Package active" -AllowFail)
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
    watch = { param($ctx)
        Open-Watch $ctx STOPWATCH
        Begin-Feature $ctx stopwatch
        Pause-Sec 3
        Key $ctx KEYCODE_SLEEP
        [void](Sx $ctx 'dumpsys battery unplug; dumpsys deviceidle force-idle' -AllowFail)
        Pause-Sec 25
        [void](Sx $ctx 'dumpsys deviceidle unforce; dumpsys battery reset' -AllowFail)
        Key $ctx KEYCODE_WAKEUP
        $r = Measure-Start -Serial $ctx.Serial -Component $script:WatchMain -Kind hot
        $r['label'] = 'resume after Doze'; $ctx.Starts.Add($r)
        $texts = Get-VisibleTexts $ctx
        Add-Fault $ctx 'B2' $(if (($texts -join ' ') -match '\d+:\d\d') { 'stopwatch-kept-time' } else { 'stopwatch-lost' }) -Detail ($texts -join ' / ') -Consistent $true
        if (Wait-ForNode -Serial $ctx.Serial -Text Stop -WaitSec 2) { [void](Tap $ctx -Text Stop) }
        Key $ctx KEYCODE_HOME
        End-Feature $ctx stopwatch
        Pause-Sec 6
    }
}

$script:Drivers['B4'] = @{
    phone = { param($ctx)
        Open-Phone $ctx lights
        [void](Tap $ctx -Id row_shade_shortcut)
        Pause-Sec 2
        $ctx.Persist['notifs'] = 1
        Begin-Feature $ctx shortcut
        Key $ctx KEYCODE_HOME
        Invoke-Reboot $ctx
        $deadline = (Get-Date).AddSeconds(60); $seen = $null
        do {
            $n = (Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid).notifs
            if ($n -ge 1) { $seen = Get-NowMs; break }
            Pause-Sec 1
        } while ((Get-Date) -lt $deadline)
        $ctx.Starts.Add([ordered]@{ kind = 'cold'; label = 'shortcut back after boot_completed'; launchState = 'boot'; totalMs = $(if ($seen) { $seen - $ctx.BootCompletedAt } else { $null }); waitMs = $null })
        Add-Fault $ctx 'B4' $(if ($seen) { 'shortcut-restored' } else { 'shortcut-missing' }) -Consistent ([bool]$seen)
        [void](Sx $ctx 'cmd statusbar expand-notifications' -AllowFail); Pause-Sec 2
        Checkpoint $ctx 'shade-after-boot' -AllWindows
        [void](Sx $ctx 'cmd statusbar collapse' -AllowFail)
        Open-Phone $ctx lights
        [void](Tap $ctx -Id row_shade_shortcut)
        End-Feature $ctx shortcut
        $ctx.Persist['notifs'] = 0
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

$script:Drivers['B5'] = @{
    watch = { param($ctx)
        Open-Watch $ctx TOURIST
        Begin-Feature $ctx dashboard
        Pause-Sec 3
        [void](Sx $ctx 'dumpsys battery set ac 1; dumpsys battery set status 2' -AllowFail)
        Key $ctx KEYCODE_SLEEP; Mark $ctx 'docked-ambient'
        Pause-Sec 12
        $ctx.Marks.Add([ordered]@{ ts = Get-NowMs; name = 'open:wake' })
        Key $ctx KEYCODE_WAKEUP -Measured
        [void](Sx $ctx 'dumpsys battery reset' -AllowFail); Mark $ctx 'undocked'
        Pause-Sec 3
        Checkpoint $ctx 'after-undock'
        Key $ctx KEYCODE_HOME
        End-Feature $ctx dashboard
        Pause-Sec 6
    }
}

# --- C. Main actions ---------------------------------------------------------------------------------------

$script:Drivers['C1'] = @{
    phone = { param($ctx)
        [void](Emu $ctx ("geo fix {0} {1} 50 8" -f $script:BaseFix[0], $script:BaseFix[1]))
        Open-Phone $ctx dashboard -Measured
        Begin-Feature $ctx dashboard
        foreach ($i in 1..12) {
            [void](Emu $ctx ("geo fix {0:0.000000} {1:0.000000} {2} 8" -f ($script:BaseFix[0] + $i * 0.00012), ($script:BaseFix[1] + $i * 0.00008), (50 + $i)))
            [void](Emu $ctx ("sensor set acceleration {0:0.0}:0.5:9.7" -f ($i % 3)))
            Pause-Sec 1
        }
        Checkpoint $ctx 'dashboard-live'
        [void](Sx $ctx 'input swipe 540 1700 540 700 300'); Pause-Sec 1.5
        Checkpoint $ctx 'dashboard-scrolled'
        [void](Tap $ctx -Id btnResetTrip -Measured); Pause-Sec 1.5
        [void](Sx $ctx 'input swipe 540 700 540 1800 300'); Pause-Sec 1
        Emu $ctx 'rotate' | Out-Null; Pause-Sec 3
        Checkpoint $ctx 'dashboard-landscape'
        Emu $ctx 'rotate' | Out-Null; Emu $ctx 'rotate' | Out-Null; Emu $ctx 'rotate' | Out-Null; Pause-Sec 3
        Key $ctx KEYCODE_BACK -Measured
        End-Feature $ctx dashboard
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['C2'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx compass
        Reset-Gfx $ctx.Serial
        $job = Start-ThreadJob -ArgumentList (Join-Path $script:StandRoot '_stand.ps1'), (Join-Path $script:StandRoot '_instruments.ps1'), (Join-Path $script:StandRoot 'scenes.ps1'), $ctx.Serial -ScriptBlock {
            param($lib, $ins, $sc, $serial) . $lib; . $ins; . $sc
            Send-SensorStorm @{ Serial = $serial } 30 @('magnetic-field', 'acceleration')
        }
        Pause-Sec 4
        foreach ($i in 1..6) { Tap-At $ctx 540 1200 -Measured -Label 'tap during compass storm'; Pause-Sec 3 }
        $events = Receive-Job $job -Wait -AutoRemoveJob
        Note $ctx "compass storm: $events emulator sensor commands in 30 s"
        Checkpoint $ctx 'compass-after-storm'
        Key $ctx KEYCODE_BACK
        End-Feature $ctx compass
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
    watch = { param($ctx)
        Open-Watch $ctx TOURIST
        Begin-Feature $ctx compass
        $job = Start-ThreadJob -ArgumentList (Join-Path $script:StandRoot '_stand.ps1'), (Join-Path $script:StandRoot '_instruments.ps1'), (Join-Path $script:StandRoot 'scenes.ps1'), $ctx.Serial -ScriptBlock {
            param($lib, $ins, $sc, $serial) . $lib; . $ins; . $sc
            Send-SensorStorm @{ Serial = $serial } 30 @('magnetic-field', 'acceleration')
        }
        Pause-Sec 4
        foreach ($i in 1..6) { Rotary $ctx 2 -Measured; Pause-Sec 3 }
        [void](Receive-Job $job -Wait -AutoRemoveJob)
        Checkpoint $ctx 'compass-after-storm'
        Key $ctx KEYCODE_HOME
        End-Feature $ctx compass
        Pause-Sec 8
    }
}

$script:Drivers['C3'] = @{
    phone = { param($ctx)
        Open-Phone $ctx lights
        Test-ToggleExposesState $ctx row_camera_torch { [void](Tap $ctx -Id row_camera_torch) }
        if (Get-TorchOn $ctx) { Measure-TorchRelease $ctx 'state-probe' (Get-TapCmd $ctx -Id row_camera_torch) }
        Test-Toggle $ctx 'camera torch' {
            Begin-Feature $ctx torch
            [void](Tap $ctx -Id row_camera_torch -Measured)
            Pause-Sec 1
            if (-not (Get-TorchOn $ctx)) { Note $ctx 'camera torch did not report on after the tap' }
        } {
            Measure-TorchRelease $ctx 'camera torch off' (Get-TapCmd $ctx -Id row_camera_torch) -Measured
            End-Feature $ctx torch
        }
        Checkpoint $ctx 'lights'
        [void](Tap $ctx -Id row_front_flashlight -Measured)
        Begin-Feature $ctx front
        Expect-Hold $ctx front wlScreen 1 'front light keeps the screen awake' 'P2'
        Pause-Sec 4
        Checkpoint $ctx 'front-light'
        Key $ctx KEYCODE_BACK -Measured
        End-Feature $ctx front
        Pause-Sec 1
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['C4'] = @{
    phone = { param($ctx)
        Open-Phone $ctx lights
        [void](Tap $ctx -Id row_water_flashlight -Measured)
        Begin-Feature $ctx water
        Expect-Hold $ctx water torchOn 1 'water flashlight torch lit'
        Expect-Hold $ctx water wlScreen 1 'water flashlight keeps the screen awake'
        Pause-Sec 3
        Checkpoint $ctx 'water-on'
        # The wet-touch storm: taps, swipes, edge swipes and long presses all over the glass.
        $rnd = [Random]::new(13)
        foreach ($i in 1..40) { Tap-At $ctx $rnd.Next(30, 1050) $rnd.Next(150, 2300) -Measured:($i % 8 -eq 0) -Label 'wet tap' }
        foreach ($i in 1..8) { [void](Sx $ctx ("input swipe {0} {1} {2} {3} 120" -f $rnd.Next(0, 1080), $rnd.Next(200, 2300), $rnd.Next(0, 1080), $rnd.Next(200, 2300))) }
        [void](Sx $ctx 'input swipe 0 1200 600 1200 150'); [void](Sx $ctx 'input swipe 1079 1200 400 1200 150')
        [void](Sx $ctx 'input swipe 540 2399 540 1400 200')
        [void](Sx $ctx 'input swipe 540 1200 540 1200 1200')
        Pause-Sec 1.5
        $still = Wait-TopActivity $ctx 'WaterFlashlightActivity' 2
        $lit = Get-TorchOn $ctx
        Add-Fault $ctx 'C4-wet' $(if ($still -and $lit) { 'water-stays-on' } elseif ($still) { 'water-on-torch-dark' } else { 'water-left-by-touch' }) -Consistent ($still -and $lit) -Severity 'P0'
        Key $ctx KEYCODE_BACK; Pause-Sec 1
        $afterBack = Wait-TopActivity $ctx 'WaterFlashlightActivity' 2
        Note $ctx "after KEYCODE_BACK the water flashlight is $(if ($afterBack) { 'still on' } else { 'gone' })"
        if ($afterBack) {
            Measure-TorchRelease $ctx 'water exit by volume key' 'input keyevent KEYCODE_VOLUME_DOWN' -Measured
            $gone = -not (Wait-TopActivity $ctx 'WaterFlashlightActivity' 1)
            Add-Fault $ctx 'C4-exit' $(if ($gone) { 'left-by-hardware-key' } else { 'exit-key-ignored' }) -Consistent $gone -Severity 'P1'
        } else {
            Add-Fault $ctx 'C4-exit' 'left-by-back' -Consistent $true
        }
        End-Feature $ctx water
        Pause-Sec 1
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
    watch = { param($ctx)
        Open-Watch $ctx WATER_FLASHLIGHT
        Begin-Feature $ctx water
        Expect-Hold $ctx water wlScreen 1 'water flashlight keeps the watch screen awake'
        Pause-Sec 3
        Checkpoint $ctx 'water-on'
        $rnd = [Random]::new(13)
        foreach ($i in 1..30) { Tap-At $ctx $rnd.Next(20, 360) $rnd.Next(20, 360) -Measured:($i % 6 -eq 0) -Label 'wet tap' }
        # Wet swipes run leftward or vertically: three fast rightward back swipes are the documented exit, and one
        # single edge swipe below must not be enough.
        foreach ($i in 1..6) { $x1 = $rnd.Next(120, 384); [void](Sx $ctx ("input swipe {0} {1} {2} {3} 120" -f $x1, $rnd.Next(0, 384), ($x1 - $rnd.Next(0, 110)), $rnd.Next(0, 384))) }
        [void](Sx $ctx 'input swipe 5 192 350 192 150')
        Pause-Sec 1.5
        $texts = Get-VisibleTexts $ctx
        $top = Get-Resumed $ctx
        $still = ($texts -join ' ') -match '(?i)locked|swipe back'
        Add-Fault $ctx 'C4-wet' $(if ($still) { 'water-stays-on' } else { 'water-left-by-touch' }) -Detail ($texts -join ' / ') -Consistent $still -Severity 'P0'
        Key $ctx KEYCODE_BACK; Pause-Sec 0.2; Key $ctx KEYCODE_BACK; Pause-Sec 0.2; Key $ctx KEYCODE_BACK -Measured
        Pause-Sec 1
        End-Feature $ctx water
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['C5'] = @{
    phone = { param($ctx)
        Open-Phone $ctx sos -Measured
        Begin-Feature $ctx sos
        Expect-Hold $ctx sos fgServices 1 'SOS foreground service while signalling'
        Expect-Hold $ctx sos wlOwn 1 'SOS wakelock while signalling'
        Pause-Sec 4
        Checkpoint $ctx 'sos-running'
        [void](Tap $ctx -Id btnSosModeSoundOnly -Measured); Pause-Sec 4
        [void](Tap $ctx -Id btnSosModeLightOnly -Measured); Pause-Sec 4
        [void](Tap $ctx -Id btnSosModeAll -Measured); Pause-Sec 4
        Measure-TorchRelease $ctx 'sos stop' (Get-TapCmd $ctx -Id btnSosStop) -Measured
        End-Feature $ctx sos
        Pause-Sec 1
        Checkpoint $ctx 'after-sos-stop'
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['C7'] = @{
    phone = { param($ctx)
        Open-Phone $ctx stopwatch -Measured
        Begin-Feature $ctx stopwatch
        Pause-Sec 2
        foreach ($i in 1..5) { [void](Tap $ctx -Id btnStopwatchRegionPrimary -Measured); Pause-Sec 0.8 }
        Checkpoint $ctx 'stopwatch-laps'
        Key $ctx KEYCODE_HOME; Pause-Sec 3
        Mark $ctx 'stopwatch-in-background'
        Pause-Sec 5
        [void](Sx $ctx 'cmd statusbar expand-notifications' -AllowFail); Pause-Sec 2
        Checkpoint $ctx 'shade-ongoing' -AllWindows
        [void](Sx $ctx 'cmd statusbar collapse' -AllowFail)
        $r = Measure-Start -Serial $ctx.Serial -Component $script:PhoneMain -Kind hot; $r['label'] = 'return to running stopwatch'; $ctx.Starts.Add($r)
        Pause-Sec 1
        [void](Tap $ctx -Id btnStopwatchRegionSecondary -Measured); Pause-Sec 1
        Checkpoint $ctx 'stopwatch-stopped'
        [void](Tap $ctx -Id btnStopwatchResult -Measured); Pause-Sec 1.5
        Checkpoint $ctx 'stopwatch-result'
        Key $ctx KEYCODE_BACK; Pause-Sec 1
        [void](Tap $ctx -Id btnStopwatchResetAll -Measured); Pause-Sec 1
        Key $ctx KEYCODE_BACK
        End-Feature $ctx stopwatch
        Pause-Sec 1
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
    watch = { param($ctx)
        Open-Watch $ctx STOPWATCH
        Begin-Feature $ctx stopwatch
        Pause-Sec 2
        Checkpoint $ctx 'stopwatch'
        foreach ($i in 1..4) { [void](Tap $ctx -Text Lap -Measured); Pause-Sec 1 }
        Key $ctx KEYCODE_HOME; Pause-Sec 5
        $r = Measure-Start -Serial $ctx.Serial -Component $script:WatchMain -Kind hot -Extras '--es com.sza.tourist.wear.launch.mode destination --es com.sza.tourist.wear.launch.destination STOPWATCH'
        $r['label'] = 'return to stopwatch'; $ctx.Starts.Add($r)
        Pause-Sec 1
        Checkpoint $ctx 'stopwatch-return'
        if (Wait-ForNode -Serial $ctx.Serial -Text Stop -WaitSec 2) { [void](Tap $ctx -Text Stop -Measured); Pause-Sec 1 }
        foreach ($i in 1..3) { Key $ctx KEYCODE_BACK; Pause-Sec 0.4 }
        End-Feature $ctx stopwatch
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

function Find-AnyNode($ctx, [string] $Pattern, [switch] $AllWindows, [switch] $Clickable) {
    $d = Get-UiDump -Serial $ctx.Serial -AllWindows:$AllWindows
    $d.SelectNodes('//node') | Where-Object { "$($_.text) $($_.'content-desc')" -match $Pattern -and (-not $Clickable -or $_.clickable -eq 'true' -or $_.'long-clickable' -eq 'true') -and $_.GetAttribute('visible-to-user') -ne 'false' } | Select-Object -First 1
}

# Places one of the app's widgets on the launcher's home screen through the widget picker, the way a user
# does: long-press the wallpaper, Widgets, search the app, drag a preview home.
function Add-HomeWidget($ctx, [string] $PreviewPattern) {
    Key $ctx KEYCODE_HOME; Pause-Sec 1.5
    [void](Sx $ctx 'input swipe 540 1500 540 1500 1200'); Pause-Sec 1.5
    $w = Find-AnyNode $ctx '^\s*Widgets\s*$' -AllWindows
    if (-not $w) { throw 'INVALID: C8: the launcher offered no Widgets entry on a long-press' }
    $c = Get-NodeCenter $w; Tap-At $ctx $c.X $c.Y; Pause-Sec 2.5
    $search = Find-AnyNode $ctx '(?i)^\s*search' -AllWindows
    if ($search) { $c = Get-NodeCenter $search; Tap-At $ctx $c.X $c.Y; Pause-Sec 1; [void](Sx $ctx 'input text Tourist'); Pause-Sec 2; Key $ctx KEYCODE_BACK; Pause-Sec 1 }
    $app = Find-AnyNode $ctx '^\s*Tourist\b' -AllWindows
    if (-not $app) { throw 'INVALID: C8: the widget picker lists no Tourist entry' }
    $c = Get-NodeCenter $app; Tap-At $ctx $c.X $c.Y; Pause-Sec 2
    $preview = Find-AnyNode $ctx $PreviewPattern -AllWindows
    if (-not $preview) { throw "INVALID: C8: no widget preview matching '$PreviewPattern'" }
    $c = Get-NodeCenter $preview
    [void](Sx $ctx "input draganddrop $($c.X) $($c.Y) 540 1100 2500"); Pause-Sec 3
    Key $ctx KEYCODE_HOME; Pause-Sec 1.5
}

$script:Drivers['C8'] = @{
    phone = { param($ctx)
        $widgets = [ordered]@{ tourist = '(?i)tourist|dashboard'; front = '(?i)front'; water = '(?i)water'; stopwatch = '(?i)stopwatch' }
        foreach ($k in $widgets.Keys) {
            Add-HomeWidget $ctx $widgets[$k]
            Begin-Feature $ctx "widget-$k"
            Checkpoint $ctx "home-with-$k-widget" -AllWindows
            $node = Find-AnyNode $ctx $widgets[$k] -AllWindows -Clickable
            if (-not $node) { Note $ctx "widget $k not found on the home screen after the drop"; End-Feature $ctx "widget-$k"; continue }
            $c = Get-NodeCenter $node
            Tap-At $ctx $c.X $c.Y -Measured -Label "widget $k"
            Pause-Sec 2.5
            $top = Get-Resumed $ctx
            Note $ctx "widget $k opened $top"
            switch ($k) {
                'water' { Key $ctx KEYCODE_VOLUME_DOWN }
                'stopwatch' { Pause-Sec 1; Tap-At $ctx $c.X $c.Y; Pause-Sec 1 }
                default { Key $ctx KEYCODE_BACK }
            }
            Pause-Sec 1.5
            Key $ctx KEYCODE_HOME
            End-Feature $ctx "widget-$k"
            Pause-Sec 3
        }
        # The launcher's layout is the profile's, not the app's: reset it so no widget outlives the scene.
        $launcher = ((Sx $ctx 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1').Trim() -split '/')[0]
        if ($launcher) { [void](Sx $ctx "pm clear $launcher" -AllowFail); Mark $ctx "launcher reset ($launcher)" }
        Pause-Sec 4
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

$script:Drivers['C9'] = @{
    phone = { param($ctx)
        Open-Phone $ctx lights
        Test-Toggle $ctx 'shade shortcut' {
            Begin-Feature $ctx shortcut
            [void](Tap $ctx -Id row_shade_shortcut -Measured)
            Pause-Sec 2
            [void](Sx $ctx 'cmd statusbar expand-notifications' -AllowFail); Pause-Sec 2
            Checkpoint $ctx 'shade-shortcut' -AllWindows
            $d = Get-UiDump -Serial $ctx.Serial -AllWindows
            # The notification row is clickable; its title is a child text node. The QS 'Flashlight' tile is not ours.
            $node = $d.SelectNodes('//node') | Where-Object { $_.package -eq 'com.android.systemui' -and $_.text -match '^(Camera flashlight|Tap to switch the light (on|off))$' } | Select-Object -First 1
            if ($node) {
                $c = Get-NodeCenter $node
                Tap-At $ctx $c.X $c.Y -Measured -Label 'shade shortcut on'
                Pause-Sec 1.5
                $on = Get-TorchOn $ctx
                Add-Fault $ctx 'C9' $(if ($on) { 'shortcut-lights-torch' } else { 'shortcut-did-nothing' }) -Consistent $on -Severity 'P2'
                if ($on) {
                    $d2 = Get-UiDump -Serial $ctx.Serial -AllWindows
                    $n2 = $d2.SelectNodes('//node') | Where-Object { $_.package -eq 'com.android.systemui' -and $_.text -match '^(Camera flashlight|Tap to switch the light (on|off))$' } | Select-Object -First 1
                    $tapOff = if ($n2) { Get-NodeCenter $n2 } else { $c }
                    Measure-TorchRelease $ctx 'shade shortcut off' "input tap $($tapOff.X) $($tapOff.Y)" -Measured
                }
            } else { Note $ctx 'the shortcut notification was not found in the shade' ; Add-Fault $ctx 'C9' 'shortcut-missing' -Consistent $false -Severity 'P2' }
            [void](Sx $ctx 'cmd statusbar collapse' -AllowFail); Pause-Sec 1
        } {
            Open-Phone $ctx lights
            [void](Tap $ctx -Id row_shade_shortcut -Measured)
            End-Feature $ctx shortcut
        }
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['C10'] = @{
    phone = { param($ctx)
        [void](Emu $ctx ("geo fix {0} {1} 50 8" -f $script:BaseFix[0], $script:BaseFix[1]))
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        Pause-Sec 4
        [void](Sx $ctx 'input swipe 540 1900 540 400 300'); Pause-Sec 1
        [void](Sx $ctx 'input swipe 540 1900 540 400 300'); Pause-Sec 1
        [void](Tap $ctx -Id btnOpenMap -Measured); Pause-Sec 3
        $top = Get-Resumed $ctx
        Checkpoint $ctx 'open-map' -AllWindows
        Add-Fault $ctx 'C10-map' $(if ($top -match 'com\.sza\.tourist') { 'stayed-with-message' } else { 'handed-to-other-app' }) -Detail "top=$top" -Consistent $true -Severity 'P2'
        Key $ctx KEYCODE_BACK; Pause-Sec 1.5
        if (-not (Wait-TopActivity $ctx 'TouristInfoActivity' 2)) { Key $ctx KEYCODE_BACK; Pause-Sec 1 }
        [void](Tap $ctx -Id btnShareLocation -Measured); Pause-Sec 3
        $top = Get-Resumed $ctx
        Checkpoint $ctx 'share-sheet' -AllWindows
        Add-Fault $ctx 'C10-share' $(if ($top -match 'ResolverActivity|ChooserActivity|intentresolver') { 'share-sheet' } elseif ($top -match 'com\.sza\.tourist') { 'stayed-with-message' } else { 'handed-to-other-app' }) -Detail "top=$top" -Consistent $true -Severity 'P2'
        Key $ctx KEYCODE_BACK; Pause-Sec 1.5
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

# --- D. Storms ---------------------------------------------------------------------------------------------

$script:Drivers['D1'] = @{
    phone = { param($ctx)
        Open-Phone $ctx lights
        $c = Tap $ctx -Id row_camera_torch
        [void](Tap $ctx -Id row_camera_torch)
        Begin-Feature $ctx torch-storm
        foreach ($i in 1..100) { Tap-At $ctx $c.X $c.Y -Measured:($i % 10 -eq 0) -Label 'torch toggle storm'; Pause-Sec 0.15 }
        Pause-Sec 1.5
        if (Get-TorchOn $ctx) { Note $ctx 'torch on after an even number of toggles' }
        End-Feature $ctx torch-storm
        # S0031 item 3: the torch-storm exit is judged on the quiet samples before the next span (8.3d: torch dark 2 s
        # after exit). Leave a gap of dark torch for them, and open the water flashlight - which lights the torch -
        # inside the water-storm span, not in that window.
        Pause-Sec 6
        Begin-Feature $ctx water-storm
        $w = Tap $ctx -Id row_water_flashlight
        Pause-Sec 1.5
        Key $ctx KEYCODE_VOLUME_DOWN; Pause-Sec 1
        foreach ($i in 1..20) {
            Tap-At $ctx $w.X $w.Y -Measured:($i % 5 -eq 0) -Label 'water open'
            Pause-Sec 1
            Key $ctx KEYCODE_VOLUME_DOWN
            Pause-Sec 0.8
        }
        End-Feature $ctx water-storm
        Pause-Sec 1
        if (Get-TorchOn $ctx) { Note $ctx 'torch on after the water storm ended' }
        Key $ctx KEYCODE_BACK
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['D2'] = @{
    watch = { param($ctx)
        Open-Watch $ctx TOURIST
        Begin-Feature $ctx locked-dashboard
        $d = Get-UiDump -Serial $ctx.Serial
        $lock = $d.SelectNodes('//node') | Where-Object { $_.clickable -eq 'true' -and ("$($_.text) $($_.'content-desc')" -match '(?i)lock') } | Select-Object -First 1
        if (-not $lock) { throw "INVALID: D2: no lock control found on the watch dashboard" }
        $c = Get-NodeCenter $lock
        Tap-At $ctx $c.X $c.Y
        Pause-Sec 1.5
        $rnd = [Random]::new(7)
        foreach ($i in 1..60) { Tap-At $ctx $rnd.Next(20, 360) $rnd.Next(20, 360) -Measured:($i % 10 -eq 0) -Label 'tap on locked'; if ($i % 6 -eq 0) { [void](Sx $ctx 'input rotaryencoder scroll --axis SCROLL,3' -AllowFail) } }
        $texts = Get-VisibleTexts $ctx -AllWindows
        Add-Fault $ctx 'D2' $(if (($texts -join ' ') -match '(?i)unlock|press a watch button') { 'lock-held' } else { 'lock-broken' }) -Detail ($texts -join ' / ') -Consistent $true -Severity 'P1'
        Key $ctx KEYCODE_STEM_1 -Measured
        Pause-Sec 1
        Key $ctx KEYCODE_HOME
        End-Feature $ctx locked-dashboard
        Pause-Sec 8
    }
}

$script:Drivers['D3'] = @{
    phone = { param($ctx)
        Open-Phone $ctx sos
        Begin-Feature $ctx sos
        $ids = 'btnSosModeSoundOnly', 'btnSosModeLightOnly', 'btnSosModeAll'
        $d = Get-UiDump -Serial $ctx.Serial
        $pts = @($ids | ForEach-Object { Get-NodeCenter (Find-Node -Dump $d -Id $_) })
        foreach ($i in 0..59) { $p = $pts[$i % 3]; Tap-At $ctx $p.X $p.Y -Measured:($i % 10 -eq 0) -Label 'sos mode flip'; Pause-Sec 0.2 }
        Pause-Sec 2
        Measure-TorchRelease $ctx 'sos stop after storm' (Get-TapCmd $ctx -Id btnSosStop) -Measured
        End-Feature $ctx sos
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
    watch = { param($ctx)
        # The watch asks for the mode first; the flip storm is start-in-a-mode, leave, start-in-the-next.
        $modes = 'Sound and light', 'Sound only', 'Light only'
        Begin-Feature $ctx sos
        foreach ($i in 0..11) {
            Open-Watch $ctx SOS
            if ($i -eq 0) { Checkpoint $ctx 'watch-sos-modes' }
            [void](Tap $ctx -Text $modes[$i % 3] -Measured:($i % 4 -eq 0))
            Pause-Sec 1.2
            if ($i -eq 0) { Checkpoint $ctx 'watch-sos-running' }
            foreach ($k in 1..3) { Key $ctx KEYCODE_BACK; Pause-Sec 0.2 }
            Pause-Sec 0.6
        }
        End-Feature $ctx sos
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['D4'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        Reset-Gfx $ctx.Serial
        $job = Start-ThreadJob -ArgumentList (Join-Path $script:StandRoot '_stand.ps1'), (Join-Path $script:StandRoot '_instruments.ps1'), (Join-Path $script:StandRoot 'scenes.ps1'), $ctx.Serial -ScriptBlock {
            param($lib, $ins, $sc, $serial) . $lib; . $ins; . $sc
            Send-SensorStorm @{ Serial = $serial } 60 @('magnetic-field', 'acceleration', 'pressure', 'geo')
        }
        Pause-Sec 5
        foreach ($i in 1..8) { Tap-At $ctx 540 1300 -Measured -Label 'tap during sensor storm'; Pause-Sec 5 }
        $n = Receive-Job $job -Wait -AutoRemoveJob
        Note $ctx "sensor storm: $n rounds of four injections in 60 s"
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['D5'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        foreach ($round in 1..3) {
            foreach ($loc in 'ru-RU', 'uk-UA', 'en-US') { Set-AppLocale $ctx $loc; Pause-Sec 2.5; Tap-At $ctx 540 1300 -Measured -Label "tap after locale $loc" }
            foreach ($h in '12', '24') { [void](Sx $ctx "settings put system time_12_24 $h"); Pause-Sec 1.5; Tap-At $ctx 540 1300 -Measured -Label "tap after ${h}h" }
        }
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard
        # Unit system: no in-app control on the phone; the stored setting is flipped between runs of the app.
        foreach ($u in 'IMPERIAL', 'METRIC') {
            [void](Sx $ctx "am force-stop $script:Package")
            Set-PrefString $ctx 'settings' 'unit_system' $u
            Open-Phone $ctx dashboard
            Begin-Feature $ctx "dashboard-$u"
            Pause-Sec 3
            Checkpoint $ctx "dashboard-$u"
            Key $ctx KEYCODE_BACK
            End-Feature $ctx "dashboard-$u"
        }
        [void](Sx $ctx 'settings delete system time_12_24' -AllowFail)
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['D6'] = @{
    phone = { param($ctx)
        Go-Home $ctx
        Begin-Feature $ctx ping-pong
        foreach ($i in 1..15) {
            foreach ($row in 'row_dashboard', 'row_lights', 'row_stopwatch') {
                [void](Tap $ctx -Id $row -Measured:($i % 5 -eq 0))
                Pause-Sec 1.2
                Key $ctx KEYCODE_BACK
                Pause-Sec 0.6
            }
        }
        End-Feature $ctx ping-pong
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
    watch = { param($ctx)
        Begin-Feature $ctx ping-pong
        foreach ($i in 1..12) {
            foreach ($dest in 'TOURIST', 'STOPWATCH') {
                Open-Watch $ctx $dest
                if ($i % 4 -eq 0) { Key $ctx KEYCODE_BACK -Measured } else { Key $ctx KEYCODE_BACK }
                Pause-Sec 0.6
            }
        }
        End-Feature $ctx ping-pong
        Stop-WatchStopwatch $ctx
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

# --- E. Large inputs ---------------------------------------------------------------------------------------

$script:Drivers['E1'] = @{
    phone = { param($ctx)
        Open-Phone $ctx stopwatch
        [void](Tap $ctx -Id btnStopwatchSettings); Pause-Sec 1.5
        [void](Tap $ctx -Id text_input_end_icon); Pause-Sec 1.5
        $d = Get-UiDump -Serial $ctx.Serial -AllWindows
        $opts = @($d.SelectNodes('//node') | Where-Object { $_.text -match '^\d+$' -and $_.clickable -eq 'true' -or ($_.text -match '^\d+$' -and $_.ParentNode.clickable -eq 'true') })
        $max = $opts | Sort-Object { [int]$_.text } | Select-Object -Last 1
        if ($max) { $c = Get-NodeCenter $max; Tap-At $ctx $c.X $c.Y; Note $ctx "participants set to $($max.text)" } else { Key $ctx KEYCODE_BACK }
        Pause-Sec 1
        [void](Tap $ctx -Id btnStopwatchSettingsApply); Pause-Sec 1.5
        Begin-Feature $ctx stopwatch
        $d = Get-UiDump -Serial $ctx.Serial
        $starts = @($d.SelectNodes('//node') | Where-Object { $_.'resource-id' -match 'btnStopwatchRegionPrimary' })
        $all = Find-Node -Dump $d -Id btnStopwatchStartAll
        if ($all) { $c = Get-NodeCenter $all; Tap-At $ctx $c.X $c.Y; Pause-Sec 1; $d = Get-UiDump -Serial $ctx.Serial; $starts = @($d.SelectNodes('//node') | Where-Object { $_.'resource-id' -match 'btnStopwatchRegionPrimary' }) }
        $pts = @($starts | ForEach-Object { Get-NodeCenter $_ })
        Note $ctx "$($pts.Count) participant lap button(s)"
        $laps = 0
        foreach ($i in 1..600) { $p = $pts[$i % [math]::Max(1, $pts.Count)]; Tap-At $ctx $p.X $p.Y -Measured:($i % 50 -eq 0) -Label 'lap at scale'; $laps++ }
        Note $ctx "$laps lap taps"
        Checkpoint $ctx 'stopwatch-many-laps'
        Key $ctx KEYCODE_HOME; Pause-Sec 2
        Invoke-Reclaim $ctx
        $r = Measure-Start -Serial $ctx.Serial -Component $script:PhoneMain -Kind hot; $r['label'] = 'restore with thousands of laps'; $ctx.Starts.Add($r)
        Pause-Sec 2
        $restored = Wait-TopActivity $ctx 'StopwatchActivity' 3
        Add-Fault $ctx 'E1' $(if ($restored) { 'restored-with-laps' } else { 'restarted-home' }) -Consistent $restored -Severity 'P2'
        Checkpoint $ctx 'after-restore'
        if ($restored) { [void](Tap $ctx -Id btnStopwatchResetAll) }
        Key $ctx KEYCODE_BACK
        End-Feature $ctx stopwatch
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
    watch = { param($ctx)
        Open-Watch $ctx STOPWATCH
        Begin-Feature $ctx stopwatch
        $lapNode = Wait-ForNode -Serial $ctx.Serial -Text Lap
        if (-not $lapNode) { throw 'INVALID: E1: the watch stopwatch shows no Lap control' }
        $lap = Get-NodeCenter $lapNode
        foreach ($i in 1..400) { Tap-At $ctx $lap.X $lap.Y -Measured:($i % 50 -eq 0) -Label 'lap at scale' }
        Checkpoint $ctx 'stopwatch-many-taps'
        Key $ctx KEYCODE_HOME; Pause-Sec 2
        Invoke-Reclaim $ctx
        $r = Measure-Start -Serial $ctx.Serial -Component $script:WatchMain -Kind hot; $r['label'] = 'restore at scale'; $ctx.Starts.Add($r)
        Pause-Sec 2
        Checkpoint $ctx 'after-restore'
        Add-Fault $ctx 'E1' 'restored-with-laps' -Consistent $true -Severity 'P2' -Detail 'watch: restore read from the screen only'
        foreach ($i in 1..3) { Key $ctx KEYCODE_BACK; Pause-Sec 0.3 }
        End-Feature $ctx stopwatch
        Stop-WatchStopwatch $ctx
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['E2'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        $fixes = [ordered]@{ 'north-pole' = '0.0 89.9999 2800 8'; 'south-pole' = '0.0 -89.9999 2835 8'; 'equator-meridian' = '0.0 0.0 0 8'; 'date-line' = '179.9999 0.0 0 8'; 'zero-satellites' = "$($script:BaseFix[0]) $($script:BaseFix[1]) 50 0"; 'polar-night' = '15.6 78.2 10 8' }
        foreach ($k in $fixes.Keys) {
            [void](Emu $ctx "geo fix $($fixes[$k])")
            Pause-Sec 3
            Tap-At $ctx 540 1300 -Measured -Label "tap at $k"
            Checkpoint $ctx "fix-$k"
        }
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard
        [void](Emu $ctx ("geo fix {0} {1} 50 8" -f $script:BaseFix[0], $script:BaseFix[1]))
        Add-Fault $ctx 'E2' 'tiles-degrade-in-words' -Consistent $true -Severity 'P2' -Detail 'judged by the crash scan and the checkpoint wording'
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['E3'] = @{
    watch = { param($ctx)
        Open-Watch $ctx STOPWATCH
        Begin-Feature $ctx stopwatch
        Pause-Sec 2
        Checkpoint $ctx 'stopwatch-names'
        Note $ctx 'the watch stopwatch has no participant-name entry on the watch; names arrive from the phone settings (pair) - long names are exercised through the stored setting'
        foreach ($i in 1..3) { Key $ctx KEYCODE_BACK; Pause-Sec 0.3 }
        End-Feature $ctx stopwatch
        Stop-WatchStopwatch $ctx
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

# --- F. Boundary failures (M5) -----------------------------------------------------------------------------

$script:Drivers['F1'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        Pause-Sec 3
        $before = Get-StateHash $ctx
        [void](Sx $ctx "pm revoke $script:Package android.permission.ACCESS_FINE_LOCATION; pm revoke $script:Package android.permission.ACCESS_COARSE_LOCATION" -AllowFail)
        Mark $ctx 'revoked-location'
        End-Feature $ctx dashboard
        Pause-Sec 2
        Open-Phone $ctx dashboard -Measured
        Begin-Feature $ctx dashboard-denied
        Pause-Sec 3
        $texts = Get-VisibleTexts $ctx
        $card = ($texts -join ' ') -match '(?i)location|permission|allow|grant'
        $sample = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
        $observed = if ($card -and $sample.locRegs -eq 0) { 'permission-card-shown' } elseif ($sample.locRegs -gt 0) { 'location-still-requested' } else { 'silent-missing-data' }
        Add-Fault $ctx 'F1-location' $observed -Detail (($texts | Select-Object -First 12) -join ' / ') -Consistent $true
        Checkpoint $ctx 'dashboard-location-denied'
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard-denied
        [void](Sx $ctx "pm grant $script:Package android.permission.ACCESS_FINE_LOCATION; pm grant $script:Package android.permission.ACCESS_COARSE_LOCATION" -AllowFail)
        # Notifications revoked: SOS must still sound (its notification is the cost).
        [void](Sx $ctx "pm revoke $script:Package android.permission.POST_NOTIFICATIONS" -AllowFail)
        Open-Phone $ctx sos
        Begin-Feature $ctx sos
        Expect-Hold $ctx sos fgServices 1 'SOS service without the notification grant'
        Pause-Sec 4
        $s = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
        Add-Fault $ctx 'F1-notifications' $(if ($s.fgServices -gt 0) { 'sos-runs-without-notification' } else { 'sos-refused' }) -Detail "fgs=$($s.fgServices) notifs=$($s.notifs)" -Consistent $true -Severity 'P0'
        Checkpoint $ctx 'sos-without-notification-grant'
        [void](Tap $ctx -Id btnSosStop)
        End-Feature $ctx sos
        [void](Sx $ctx "pm grant $script:Package android.permission.POST_NOTIFICATIONS" -AllowFail)
        # Activity recognition (steps).
        [void](Sx $ctx "pm revoke $script:Package android.permission.ACTIVITY_RECOGNITION" -AllowFail)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard-no-steps
        Pause-Sec 3
        Checkpoint $ctx 'dashboard-no-activity-recognition'
        Add-Fault $ctx 'F1-activity' 'tile-explains' -Consistent $true -Severity 'P2' -Detail 'wording judged by M7'
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard-no-steps
        [void](Sx $ctx "pm grant $script:Package android.permission.ACTIVITY_RECOGNITION" -AllowFail)
        $after = Get-StateHash $ctx
        if ($before -ne $after) { Note $ctx "state hash before $before after $after (grants restored)" }
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
    watch = { param($ctx)
        Open-Watch $ctx TOURIST
        Begin-Feature $ctx dashboard
        Pause-Sec 3
        [void](Sx $ctx "pm revoke $script:Package android.permission.ACCESS_FINE_LOCATION; pm revoke $script:Package android.permission.ACCESS_COARSE_LOCATION" -AllowFail)
        End-Feature $ctx dashboard
        Pause-Sec 2
        Open-Watch $ctx TOURIST
        Begin-Feature $ctx dashboard-denied
        Pause-Sec 3
        $texts = Get-VisibleTexts $ctx
        $s = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
        $observed = if ($s.locRegs -gt 0) { 'location-still-requested' } elseif (($texts -join ' ') -match '(?i)location|permission|allow|grant') { 'permission-card-shown' } else { 'silent-missing-data' }
        Add-Fault $ctx 'F1-location' $observed -Detail (($texts | Select-Object -First 12) -join ' / ') -Consistent $true
        Checkpoint $ctx 'watch-location-denied'
        Key $ctx KEYCODE_HOME
        End-Feature $ctx dashboard-denied
        [void](Sx $ctx "pm grant $script:Package android.permission.ACCESS_FINE_LOCATION; pm grant $script:Package android.permission.ACCESS_COARSE_LOCATION" -AllowFail)
        Pause-Sec 6
    }
}

$script:Drivers['F2'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        Pause-Sec 3
        [void](Sx $ctx 'cmd location set-location-enabled false' -AllowFail)
        Mark $ctx 'location-off'
        Pause-Sec 4
        $texts = Get-VisibleTexts $ctx
        $observed = if (($texts -join ' ') -match '(?i)location (is )?(off|disabled)|turn on location|enable location|location services') { 'location-off-explained' } else { 'silent-stale-data' }
        Add-Fault $ctx 'F2' $observed -Detail (($texts | Select-Object -First 14) -join ' / ') -Consistent $true -Severity 'P2'
        Checkpoint $ctx 'dashboard-location-off'
        [void](Sx $ctx 'cmd location set-location-enabled true' -AllowFail)
        Pause-Sec 3
        Checkpoint $ctx 'dashboard-location-back'
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

$script:Drivers['F3'] = @{
    # Runs on the phone-nobaro profile (no barometer, no magnetometer, no orientation sensor).
    phone = { param($ctx)
        $list = Sx $ctx 'dumpsys sensorservice | grep -i -E "pressure|magnetic" | head -n 3' -AllowFail
        if ($list -match '(?i)pressure|magnetic') { Note $ctx "this profile still lists: $($list.Trim())" }
        Open-Phone $ctx dashboard
        Begin-Feature $ctx dashboard
        Pause-Sec 4
        $texts = Get-VisibleTexts $ctx
        Add-Fault $ctx 'F3' $(if (($texts -join ' ') -match '(?i)not available|unavailable|no sensor|not supported|no compass|no barometer') { 'tile-says-unavailable' } else { 'tile-silent' }) -Detail (($texts | Select-Object -First 16) -join ' / ') -Consistent $true -Severity 'P2'
        Checkpoint $ctx 'dashboard-without-sensors'
        Key $ctx KEYCODE_BACK
        End-Feature $ctx dashboard
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

$script:Drivers['F3'].watch = { param($ctx)
    # Runs on the wear-nobaro profile (no barometer, magnetometer, orientation or heart-rate sensor).
    Open-Watch $ctx TOURIST
    Begin-Feature $ctx dashboard
    Pause-Sec 4
    $texts = Get-VisibleTexts $ctx
    Add-Fault $ctx 'F3' $(if (($texts -join ' ') -match '(?i)not available|unavailable|no sensor|not supported') { 'tile-says-unavailable' } else { 'tile-silent' }) -Detail (($texts | Select-Object -First 16) -join ' / ') -Consistent $true -Severity 'P2'
    Checkpoint $ctx 'watch-dashboard-without-sensors'
    Rotary $ctx 4; Pause-Sec 1
    Checkpoint $ctx 'watch-dashboard-scrolled'
    Key $ctx KEYCODE_HOME
    End-Feature $ctx dashboard
    Pause-Sec 6
}

$script:Drivers['F4'] = @{
    phone = { param($ctx)
        [void](Sx $ctx 'am start -W -a android.media.action.STILL_IMAGE_CAMERA' -AllowFail)
        Pause-Sec 4
        $camTop = Get-Resumed $ctx
        $openClients = Sx $ctx 'dumpsys media.camera | grep -i -E "Client package|Device [0-9]+ is open" | head -n 4' -AllowFail
        Note $ctx "camera app: $camTop; $($openClients.Trim())"
        # Split screen is not scriptable here; the camera stays open in the background long enough on API 33.
        Open-Phone $ctx lights
        Begin-Feature $ctx torch
        [void](Tap $ctx -Id row_camera_torch -Measured)
        Pause-Sec 2
        $on = Get-TorchOn $ctx
        $texts = Get-VisibleTexts $ctx -AllWindows
        $observed = if ($on) { 'torch-lit-camera-yielded' } elseif (($texts -join ' ') -match '(?i)camera.*(busy|in use|another|unavailable)|in use|unavailable') { 'busy-explained' } else { 'silent-no-light' }
        Add-Fault $ctx 'F4' $observed -Detail (($texts | Select-Object -First 10) -join ' / ') -Consistent $true -Severity 'P2'
        Checkpoint $ctx 'torch-camera-busy' -AllWindows
        if ($on) { Measure-TorchRelease $ctx 'torch off (camera busy case)' (Get-TapCmd $ctx -Id row_camera_torch) }
        End-Feature $ctx torch
        Key $ctx KEYCODE_HOME
        [void](Sx $ctx 'am force-stop com.android.camera2; am force-stop com.google.android.GoogleCamera' -AllowFail)
        Pause-Sec 6
    }
}

$script:Drivers['F5'] = @{
    phone = { param($ctx)
        Open-Phone $ctx sos
        Begin-Feature $ctx sos
        Expect-Hold $ctx sos fgServices 1 'SOS service through a call and DND'
        Pause-Sec 3
        [void](Tap $ctx -Id btnSosModeSoundOnly)
        Pause-Sec 2
        [void](Emu $ctx 'gsm call 5551234')
        Mark $ctx 'incoming-call'
        Pause-Sec 6
        $s1 = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
        [void](Emu $ctx 'gsm cancel 5551234')
        Pause-Sec 2
        [void](Sx $ctx 'cmd notification set_dnd on' -AllowFail)
        Mark $ctx 'dnd-on'
        Pause-Sec 5
        $s2 = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
        [void](Sx $ctx 'cmd notification set_dnd off' -AllowFail)
        [void](Sx $ctx 'media volume --stream 4 --set 0' -AllowFail)
        Mark $ctx 'alarm-volume-zero'
        Pause-Sec 4
        $vol = Sx $ctx 'media volume --stream 4 --get' -AllowFail
        $sounding = ($s1.wlAttributed -gt 0 -or $s1.fgServices -gt 0) -and $s2.fgServices -gt 0
        Add-Fault $ctx 'F5' $(if ($sounding) { 'siren-continues' } else { 'siren-stopped' }) -Detail "call: fgs=$($s1.fgServices) audio-wl=$($s1.wlAttributed); dnd: fgs=$($s2.fgServices) audio-wl=$($s2.wlAttributed); volume after zero: $($vol.Trim())" -Consistent $sounding -Severity 'P0'
        Checkpoint $ctx 'sos-after-audio-faults'
        [void](Tap $ctx -Id btnSosStop)
        End-Feature $ctx sos
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

# Storage full: /data is filled to a few hundred KB of headroom. While it is full the stand writes nothing on
# the device - controls are located before the fill, the evidence is a screenshot streamed over stdout.
function Fill-Data($ctx, [int] $HeadroomKb = 256) {
    $free = Sx $ctx 'df -k /data | tail -n 1' -AllowFail
    $avail = if ($free -match '\s(\d+)\s+\d+%') { [long]$Matches[1] } else { 0 }
    if ($avail -le $HeadroomKb) { return "could not read free space: $free" }
    [void](Sx $ctx "fallocate -l $($avail - $HeadroomKb)k /data/local/tmp/s0013-fill" -AllowFail -TimeoutSec 300)
    $after = Sx $ctx 'df -k /data | tail -n 1' -AllowFail
    Mark $ctx "storage-full ($(($after -split '\s+')[3]) KB free)"
    $null
}

function Clear-Fill($ctx) { [void](Sx $ctx 'rm -f /data/local/tmp/s0013-fill' -AllowFail); Mark $ctx 'storage-freed' }

function Save-Screenshot($ctx, [string] $Label) {
    $p = Join-Path $ctx.SceneDir ("shot-{0}.png" -f ($Label -replace '[^\w-]', '_'))
    [void](Invoke-Adb -Serial $ctx.Serial -Arguments @('exec-out', 'screencap -p') -OutFile $p -TimeoutSec 30 -AllowFail)
    $p
}

$script:Drivers['F7'] = @{
    phone = { param($ctx)
        Open-Phone $ctx lights
        $shortcut = Get-NodeCenter (Find-Node -Dump (Get-UiDump -Serial $ctx.Serial) -Id row_shade_shortcut)
        Key $ctx KEYCODE_BACK; Pause-Sec 1
        $before = Get-StateHash $ctx
        $err = Fill-Data $ctx
        if ($err) { Add-Fault $ctx 'F7' 'not-run' -Invalid $err; return }
        try {
            [void](Sx $ctx 'am start -W -n com.sza.tourist/.MainActivity' -AllowFail)
            Begin-Feature $ctx storage-full
            [void](Sx $ctx "am force-stop $script:Package")
            [void](Sx $ctx 'am start -W -n com.sza.tourist/.MainActivity' -AllowFail)
            Pause-Sec 2
            [void](Save-Screenshot $ctx 'home-storage-full')
            Key $ctx KEYCODE_BACK; Pause-Sec 0.5
            Go-Home $ctx
            Tap-At $ctx 540 520 -Label 'lights row (located before the fill)'; Pause-Sec 1.5
            Tap-At $ctx $shortcut.X $shortcut.Y -Measured -Label 'shade shortcut on, storage full'; Pause-Sec 1.5
            [void](Save-Screenshot $ctx 'lights-shortcut-storage-full')
            Tap-At $ctx $shortcut.X $shortcut.Y -Label 'shade shortcut off, storage full'; Pause-Sec 1
            Key $ctx KEYCODE_BACK; Pause-Sec 1
            End-Feature $ctx storage-full
        } finally { Clear-Fill $ctx }
        Pause-Sec 2
        [void](Sx $ctx "am force-stop $script:Package")
        Open-Phone $ctx stopwatch
        $alive = Wait-TopActivity $ctx 'StopwatchActivity' 4
        Checkpoint $ctx 'stopwatch-after-storage-freed'
        Key $ctx KEYCODE_BACK
        Open-Phone $ctx lights
        Checkpoint $ctx 'lights-after-storage-freed'
        $keys = Get-PrefsKeySet $ctx
        Add-Fault $ctx 'F7' $(if ($alive) { 'writes-failed-quietly-app-intact' } else { 'app-broken-after-full-storage' }) -Detail "prefs keys after: $($keys.Count)" -Consistent $alive -Severity 'P0'
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

$script:Drivers['F7'].watch = { param($ctx)
    Open-Watch $ctx STOPWATCH
    $lapNode = Wait-ForNode -Serial $ctx.Serial -Text Lap
    if (-not $lapNode) { Add-Fault $ctx 'F7' 'not-run' -Invalid 'no Lap control on the watch stopwatch'; return }
    $lap = Get-NodeCenter $lapNode
    Key $ctx KEYCODE_HOME; Pause-Sec 1
    $err = Fill-Data $ctx
    if ($err) { Add-Fault $ctx 'F7' 'not-run' -Invalid $err; return }
    try {
        [void](Sx $ctx "am force-stop $script:Package")
        [void](Sx $ctx "am start -W -n $script:WatchMain --es com.sza.tourist.wear.launch.mode destination --es com.sza.tourist.wear.launch.destination STOPWATCH" -AllowFail)
        Begin-Feature $ctx stopwatch-full
        Pause-Sec 2
        foreach ($i in 1..8) { Tap-At $ctx $lap.X $lap.Y -Measured:($i -eq 4) -Label 'lap, storage full'; Pause-Sec 0.3 }
        [void](Save-Screenshot $ctx 'watch-stopwatch-storage-full')
        Key $ctx KEYCODE_HOME
        End-Feature $ctx stopwatch-full
    } finally { Clear-Fill $ctx }
    [void](Sx $ctx "am force-stop $script:Package")
    Open-Watch $ctx STOPWATCH
    $texts = Get-VisibleTexts $ctx
    $alive = ($texts -join ' ') -match '\d+:\d\d'
    Checkpoint $ctx 'watch-stopwatch-after-storage-freed'
    Add-Fault $ctx 'F7' $(if ($alive) { 'writes-failed-quietly-app-intact' } else { 'app-broken-after-full-storage' }) -Consistent $alive -Severity 'P0' -Detail ($texts -join ' / ')
    if (Wait-ForNode -Serial $ctx.Serial -Text Stop -WaitSec 2) { [void](Tap $ctx -Text Stop) }
    Key $ctx KEYCODE_HOME
    Pause-Sec 6
}

$script:Drivers['F8'] = @{
    phone = { param($ctx)
        Open-Phone $ctx sos
        Begin-Feature $ctx sos
        Pause-Sec 3
        $procId = Get-AppPid $ctx.Serial
        [void](Invoke-RunAs -Serial $ctx.Serial -Command "kill -9 $procId" -AllowFail)
        Mark $ctx 'hard-kill-sos'
        Pause-Sec 6
        $s = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
        $torch = Get-TorchOn $ctx
        $observed = if ($s.fgServices -gt 0) { 'sos-service-restarted' } elseif ($s.notifs -gt 0) { 'stale-notification-left' } else { 'sos-ended-cleanly' }
        Add-Fault $ctx 'F8-sos' $observed -Detail "fgs=$($s.fgServices) notifs=$($s.notifs) torch=$torch" -Consistent (-not $torch -or $s.fgServices -gt 0) -Severity 'P1'
        if ($s.fgServices -gt 0) { Open-Phone $ctx sos; [void](Tap $ctx -Id btnSosStop) }
        End-Feature $ctx sos
        Open-Phone $ctx water
        Begin-Feature $ctx water
        Pause-Sec 2
        $procId = Get-AppPid $ctx.Serial
        [void](Invoke-RunAs -Serial $ctx.Serial -Command "kill -9 $procId" -AllowFail)
        Mark $ctx 'hard-kill-water'
        Pause-Sec 4
        $torch = Get-TorchOn $ctx
        Add-Fault $ctx 'F8-water' $(if ($torch) { 'torch-left-lit' } else { 'torch-released-by-system' }) -Consistent (-not $torch) -Severity 'P1'
        End-Feature $ctx water
        $r = Measure-Start -Serial $ctx.Serial -Component $script:PhoneMain -Kind cold; $r['label'] = 'start after hard kill'; $ctx.Starts.Add($r)
        Pause-Sec 1.5
        Checkpoint $ctx 'after-hard-kill'
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
    watch = { param($ctx)
        Open-Watch $ctx STOPWATCH
        Begin-Feature $ctx stopwatch
        # Long enough that a stopwatch started afresh by the next open reads less than this one does now.
        Pause-Sec 6
        $before = Get-StopwatchMs $ctx
        $procId = Get-AppPid $ctx.Serial
        [void](Invoke-RunAs -Serial $ctx.Serial -Command "kill -9 $procId" -AllowFail)
        Mark $ctx 'hard-kill'
        Pause-Sec 4
        $s = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
        End-Feature $ctx stopwatch
        $r = Measure-Start -Serial $ctx.Serial -Component $script:WatchMain -Kind cold; $r['label'] = 'start after hard kill'; $ctx.Starts.Add($r)
        Checkpoint $ctx 'after-hard-kill'
        # S0031 item 2: a notification with no process is the designed state of a stored time origin, so the judge
        # opens the stopwatch again and reads whether the measurement is still running.
        Open-Watch $ctx STOPWATCH
        $after = Get-StopwatchMs $ctx
        $observed = Get-StopwatchKillOutcome $before $after $s.notifs
        if ($observed) {
            Add-Fault $ctx 'F8-watch' $observed -Detail "notifs=$($s.notifs) pid=$($s.pid) before=$before ms after=$after ms" -Consistent $true -Severity 'P2'
        } else {
            Add-Fault $ctx 'F8-watch' 'not-run' -Invalid "no stopwatch reading on screen (before=$before ms, after=$after ms)"
        }
        Stop-WatchStopwatch $ctx
        Pause-Sec 6
    }
}

$script:Drivers['F9'] = @{
    phone = { param($ctx)
        Open-Phone $ctx lights
        [void](Tap $ctx -Id row_shade_shortcut); Pause-Sec 1.5
        [void](Sx $ctx "pm revoke $script:Package android.permission.POST_NOTIFICATIONS" -AllowFail)
        Mark $ctx 'shortcut-on-notifications-revoked'
        Key $ctx KEYCODE_HOME
        Invoke-Reboot $ctx
        Pause-Sec 20
        $s = Get-Sample -Serial $ctx.Serial -Package $script:Package -Uid $ctx.Uid
        $ctx.Starts.Add([ordered]@{ kind = 'cold'; label = 'boot with a failing shortcut'; launchState = 'boot'; totalMs = $null; waitMs = $null })
        Add-Fault $ctx 'F9' $(if ($s.notifs -eq 0) { 'shortcut-skipped-quietly' } else { 'notification-despite-denial' }) -Detail "notifs=$($s.notifs)" -Consistent $true -Severity 'P2'
        [void](Sx $ctx "pm grant $script:Package android.permission.POST_NOTIFICATIONS" -AllowFail)
        Open-Phone $ctx lights
        Checkpoint $ctx 'lights-after-failed-boot'
        [void](Tap $ctx -Id row_shade_shortcut); Pause-Sec 1
        Key $ctx KEYCODE_HOME
        Pause-Sec 6
    }
}

# --- G. Idle soak --------------------------------------------------------------------------------------------

function Invoke-Soak($ctx, [string] $Feature, [int] $Minutes) {
    Begin-Feature $ctx $Feature
    $ctx.IdleWindow = [ordered]@{ from = Get-NowMs; to = $null }
    $end = (Get-Date).AddMinutes($Minutes)
    while ((Get-Date) -lt $end) { Pause-Sec 15; Assert-DeviceAlive $ctx.Serial }
    $ctx.IdleWindow.to = Get-NowMs
}

# One descriptor table on the plateau, to name what the count is made of (S0021). An aid, never a verdict: a miss
# is a note.
function Save-FdListing($ctx) {
    $p = Get-AppPid $ctx.Serial
    $o = if ($p) { Sx $ctx "run-as $script:Package ls -l /proc/$p/fd" -AllowFail } else { '' }
    if ($o -and $o.Trim()) { Set-Content -LiteralPath (Join-Path $ctx.SceneDir 'fd-listing.txt') -Value $o -Encoding utf8NoBOM }
    else { Note $ctx 'fd listing: nothing read' }
}

$script:Drivers['G1'] = @{
    phone = { param($ctx)
        $min = $ctx.SoakMinutes
        $which = $ctx.SoakScreen
        switch ($which) {
            'dashboard' { Open-Phone $ctx dashboard; Invoke-Soak $ctx dashboard $min; Key $ctx KEYCODE_BACK; End-Feature $ctx dashboard }
            'water' {
                Open-Phone $ctx water
                Expect-Hold $ctx water torchOn 1 'water flashlight torch lit through the soak'
                Invoke-Soak $ctx water $min
                Save-FdListing $ctx
                Key $ctx KEYCODE_VOLUME_DOWN; End-Feature $ctx water
            }
            'sos' {
                Open-Phone $ctx sos
                Expect-Hold $ctx sos fgServices 1 'SOS service through the soak'
                Invoke-Soak $ctx sos $min
                [void](Tap $ctx -Id btnSosStop); End-Feature $ctx sos
            }
            'stopwatch' { Open-Phone $ctx stopwatch; Invoke-Soak $ctx stopwatch $min; [void](Tap $ctx -Id btnStopwatchRegionSecondary); [void](Tap $ctx -Id btnStopwatchResetAll); Key $ctx KEYCODE_BACK; End-Feature $ctx stopwatch }
        }
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
    watch = { param($ctx)
        $min = $ctx.SoakMinutes
        $dest = @{ dashboard = 'TOURIST'; water = 'WATER_FLASHLIGHT'; sos = 'SOS'; stopwatch = 'STOPWATCH' }[$ctx.SoakScreen]
        Open-Watch $ctx $dest
        if ($ctx.SoakScreen -eq 'sos') { [void](Tap $ctx -Text 'Sound and light'); Expect-Hold $ctx sos wlScreen 1 'watch SOS keeps its screen lit through the soak' 'P0' }
        Invoke-Soak $ctx $ctx.SoakScreen $min
        foreach ($i in 1..3) { Key $ctx KEYCODE_BACK; Pause-Sec 0.3 }
        End-Feature $ctx $ctx.SoakScreen
        Stop-WatchStopwatch $ctx
        Key $ctx KEYCODE_HOME
        Pause-Sec 8
    }
}

$script:Drivers['G2'] = @{
    phone = { param($ctx)
        Open-Phone $ctx dashboard
        Pause-Sec 3
        Key $ctx KEYCODE_HOME
        [void](Sx $ctx "am set-standby-bucket $script:Package rare" -AllowFail)
        Mark $ctx 'background'
        $ctx.IdleWindow = [ordered]@{ from = Get-NowMs; to = $null }
        $ctx.IdleBackground = $true
        $end = (Get-Date).AddMinutes($ctx.SoakMinutes)
        while ((Get-Date) -lt $end) { Pause-Sec 15; Assert-DeviceAlive $ctx.Serial }
        $ctx.IdleWindow.to = Get-NowMs
        [void](Sx $ctx "am set-standby-bucket $script:Package active" -AllowFail)
        Pause-Sec 4
    }
    watch = { param($ctx)
        Open-Watch $ctx TOURIST
        Pause-Sec 3
        Key $ctx KEYCODE_HOME
        $ctx.IdleBackground = $true
        $ctx.IdleWindow = [ordered]@{ from = Get-NowMs; to = $null }
        $end = (Get-Date).AddMinutes($ctx.SoakMinutes)
        while ((Get-Date) -lt $end) { Pause-Sec 15; Assert-DeviceAlive $ctx.Serial }
        $ctx.IdleWindow.to = Get-NowMs
        Pause-Sec 4
    }
}

# --- H. Watch face -----------------------------------------------------------------------------------------

$script:Drivers['H1'] = @{
    watch = { param($ctx)
        $installed = Sx $ctx "pm list packages $script:WatchFace" -AllowFail
        if ($installed -notmatch 'package:') { throw 'INVALID: H1: the watch face package is not installed on the watch profile' }
        $svc = Sx $ctx "dumpsys package $script:WatchFace | grep -i -E 'WatchFace|wallpaper' | head -n 5" -AllowFail
        Note $ctx "face components: $($svc.Trim())"
        Begin-Feature $ctx face
        # Selecting a face from the shell: the watch face picker is a long-press on the current face.
        Key $ctx KEYCODE_HOME; Pause-Sec 2
        [void](Sx $ctx 'input swipe 192 192 192 192 1500'); Pause-Sec 3
        Checkpoint $ctx 'face-picker' -AllWindows
        Key $ctx KEYCODE_BACK; Pause-Sec 1
        Key $ctx KEYCODE_HOME
        End-Feature $ctx face
        Pause-Sec 6
    }
}
